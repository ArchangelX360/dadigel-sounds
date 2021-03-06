package se.dorne.discordbot.services

import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.VoiceState
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.rest.util.Snowflake
import discord4j.voice.AudioProvider
import discord4j.voice.VoiceConnection
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.dorne.discordbot.utils.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds

suspend fun DiscordClient.newSession(): DiscordSession {
    val client = login().awaitFirst()
    return DiscordSession(client)
}

/**
 * Used to encapsulate state updates and client interactions.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class DiscordSession(
        private val client: GatewayDiscordClient,
        botInactiveTimeout: Duration = 10.minutes
) {
    /** Parent scope for state update coroutines in this session */
    private val scope = CoroutineScope(Job() + CoroutineName("discord-session"))

    /** Useful to detect when to reconnect */
    var connected: Boolean = true

    private val guildWatcher: GuildWatcher = scope.watchGuilds(client)

    private val connectionsByGuild: MutableMap<Snowflake, VoiceConnection> = ConcurrentHashMap()

    private val joinedChannelsByGuild: MutableMap<Snowflake, MutableStateFlow<VoiceChannel?>> = ConcurrentHashMap()

    val activityMonitor: ActivityMonitor

    init {
        // FIXME not sure this is really useful
        // Shuts down this session in case of disconnection
        scope.launch {
            val disconnectEvent = client.onDisconnect().awaitFirstOrNull()
            if (disconnectEvent != null) {
                logger.warn("Discord session disconnected, shutting down")
                shutdown()
            }
        }

        // Automatically logs out when inactive for too long
        activityMonitor = scope.launchInactiveTimeout(botInactiveTimeout) {
            logger.info("Bot inactive for $botInactiveTimeout, logging out")
            close()
        }

        // Clean up when bot is kicked or leaves guild via some external way
        scope.launch {
            client.watchSelfGuildLeave().collect {
                logger.warn("Kicked/left guild ${it.guildId}")
                ensureVoiceDisconnectedFrom(it.guildId)
            }
        }

        // Clean up when bot is disconnected from voice channel externally
        // (also triggered when disconnecting by calling leave, but should be a noop in this case)
        scope.launch {
            client.watchSelfVoiceStateUpdates().collect {
                val oldState: VoiceState? = it.old.orElse(null)
                val prevChannelId = oldState?.channelId?.orElse(null)
                if (prevChannelId != null) {
                    ensureVoiceDisconnectedFrom(oldState.guildId)
                }
            }
        }
    }

    fun watchGuilds(): Flow<List<Guild>> = guildWatcher.guilds

    fun watchChannels(guildId: Snowflake): Flow<List<VoiceChannel>> = guildWatcher.watchChannels(guildId)

    fun watchedJoinedChannel(guildId: Snowflake): Flow<VoiceChannel?> = getOrCreateJoinedChannelState(guildId)

    private fun getOrCreateJoinedChannelState(guildId: Snowflake) =
            joinedChannelsByGuild.computeIfAbsent(guildId) { MutableStateFlow(null) }

    suspend fun join(guildId: Snowflake, channelId: Snowflake, audioProvider: AudioProvider) {
        activityMonitor.notify()
        ensureVoiceDisconnectedFrom(guildId)
        val alreadyInChannel = isInChannel(guildId, channelId)
        if (alreadyInChannel) {
            logger.warn("Trying to join channel $channelId in guild $guildId while already in it")
            return
        }
        performJoin(guildId, channelId, audioProvider)
    }

    private fun isInChannel(guildId: Snowflake, channelId: Snowflake): Boolean =
        getOrCreateJoinedChannelState(guildId).value?.id == channelId

    private suspend fun performJoin(guildId: Snowflake, channelId: Snowflake, audioProvider: AudioProvider) {
        val guild = client.getGuildById(guildId).awaitFirst()
        val channel = guild.getChannelById(channelId).awaitFirst()
        if (channel !is VoiceChannel) {
            logger.error("Channel $channelId (${channel.name}) is not a voice channel")
            return
        }
        val connection = withTimeoutOrNull(5.seconds) {
            channel.join { it.setProvider(audioProvider) }.awaitFirst()
        }
        if (connection == null) {
            logger.error("Timed out when trying to join channel $channelId in guild $guildId")
            return
        }
        connectionsByGuild[guildId] = connection
        getOrCreateJoinedChannelState(guildId).value = channel
        logger.info("Connected to voice channel $channelId in guild $guildId")
    }

    suspend fun leave(guildId: Snowflake) {
        activityMonitor.notify()
        ensureVoiceDisconnectedFrom(guildId)
    }

    private suspend fun ensureVoiceDisconnectedFrom(guildId: Snowflake) {
        try {
            val connection = connectionsByGuild.remove(guildId) ?: return
            if (connection.isConnected) {
                connection.disconnect()
                    .awaitVoidWithTimeout(message = "Timed out when disconnecting voice from guild $guildId")
                logger.info("Disconnected voice from guild $guildId")
            } else {
                logger.info("Cleaned up voice connection from guild $guildId (disconnected externally)")
            }
        } finally {
            joinedChannelsByGuild[guildId]?.value = null
        }
    }

    suspend fun close() {
        client.logout().awaitVoidWithTimeout(message = "Didn't get logout event, shutting down anyway...")
        shutdown()
    }

    private fun shutdown() {
        connected = false
        joinedChannelsByGuild.values.forEach { it.value = null}
        joinedChannelsByGuild.clear()
        connectionsByGuild.clear()
        scope.cancel()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(DiscordSession::class.java)
    }
}
