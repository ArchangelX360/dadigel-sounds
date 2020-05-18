package se.dorne.discordbot.services

import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.dorne.discordbot.utils.DebounceTicker
import se.dorne.discordbot.utils.awaitVoidWithTimeout
import se.dorne.discordbot.utils.launchDebounce
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

    private val guildWatcher: GuildWatcher = GuildWatcher(client)

    private val connectionsByGuild: MutableMap<Snowflake, VoiceConnection> = ConcurrentHashMap()

    private val joinedChannelsByGuild: MutableMap<Snowflake, MutableStateFlow<VoiceChannel?>> = ConcurrentHashMap()

    private val timeoutTicker: DebounceTicker

    init {
        guildWatcher.startIn(scope)

        // FIXME not sure this is really useful
        // Shuts down this session in case of disconnection
        scope.launch {
            val disconnectEvent = client.onDisconnect().awaitFirstOrNull()
            if (disconnectEvent != null) {
                logger.warn("Discord session disconnected, shutting down")
                shutdown()
            }
        }

        // TODO use this debounce ticker when playing sounds to reset the timeout
        //  => expose this property?
        // Automatically logs out when inactive for too long
        timeoutTicker = scope.launchDebounce(botInactiveTimeout) {
            logger.info("Bot inactive for $botInactiveTimeout, logging out")
            close()
        }
    }

    fun watchGuilds(): Flow<List<Guild>> = guildWatcher.guildsState

    fun watchChannels(guildId: Snowflake): Flow<List<VoiceChannel>> = guildWatcher.watchChannels(guildId)

    fun watchedJoinedChannel(guildId: Snowflake): Flow<VoiceChannel?> = getOrCreateJoinedChannelState(guildId)

    private fun getOrCreateJoinedChannelState(guildId: Snowflake) =
            joinedChannelsByGuild.computeIfAbsent(guildId) { MutableStateFlow(null) }

    suspend fun join(guildId: Snowflake, channelId: Snowflake, audioProvider: AudioProvider) {
        timeoutTicker.tick()
        if (connectionsByGuild.containsKey(guildId)) {
            leave(guildId)
        }
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
            logger.error("channel $channelId (${channel.name}) is not a voice channel")
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
    }

    suspend fun leave(guildId: Snowflake) {
        timeoutTicker.tick()
        try {
            val connection = connectionsByGuild.remove(guildId)
            if (connection == null) {
                logger.warn("Trying to leave guild $guildId with no active connection")
                return
            }
            connection.disconnect()
                .awaitVoidWithTimeout(message = "Timed out when disconnecting voice from guild $guildId")
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
