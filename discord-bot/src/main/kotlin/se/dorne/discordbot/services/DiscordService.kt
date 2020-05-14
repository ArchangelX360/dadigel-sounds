package se.dorne.discordbot.services

import discord4j.core.DiscordClient
import discord4j.rest.util.Snowflake
import discord4j.voice.VoiceConnection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import se.dorne.discordbot.configurations.DiscordConfiguration
import se.dorne.discordbot.models.ChannelResult
import se.dorne.discordbot.models.GuildResult
import se.dorne.discordbot.models.toResult
import se.dorne.discordbot.utils.awaitVoidWithTimeout
import kotlin.time.ExperimentalTime

@Service
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class DiscordService(
        @Autowired discordConfiguration: DiscordConfiguration,
        @Autowired val audioService: AudioService
) {
    private val discord = DiscordClient.create(discordConfiguration.bot.token)

    private val sessionMutex = Mutex()
    private var session: DiscordSession? = null

    internal val connections: MutableMap<Snowflake, VoiceConnection> = mutableMapOf()

    private suspend fun getSession(): DiscordSession {
        sessionMutex.withLock {
            if (session?.connected != true) {
                logger.info("No connected session, (re-)connecting to Discord...")
                session = discord.newSession()
            }
            return session ?: error("failed to login to Discord")
        }
    }

    fun listGuilds(): Flow<List<GuildResult>> = flow {
        val session = getSession()
        emitAll(session.watchGuilds().map { guilds -> guilds.map { it.toResult() } })
    }

    fun listChannels(guildId: Snowflake): Flow<List<ChannelResult>> = flow {
        val session = getSession()
        emitAll(session.watchChannels(guildId).map { channels -> channels.map { it.toResult() } })
    }

    suspend fun join(guildId: Snowflake, channelId: Snowflake) {
        val audioProvider = audioService.registerGuild(guildId)
        val voiceConnection = getSession().join(guildId, channelId, audioProvider)
        connections[guildId] = voiceConnection
    }

    suspend fun leave(guildId: Snowflake) {
        val connection = connections[guildId] ?: error("No channel to leave in guild $guildId")
        connections.remove(guildId)
        audioService.unregisterGuild(guildId)
        connection.disconnect().awaitVoidWithTimeout(message = "Timed out when disconnecting voice from guild $guildId")
    }

    // FIXME use ChannelResult instead of boolean to provide the current channel name to UI and allow to leave via channel ID
    // FIXME then rename this function!
    @OptIn(ExperimentalTime::class)
    fun isConnected(guildId: Snowflake): Flow<Boolean> = flow {
        val session = getSession()
        emitAll(session.watchedJoinedChannel().map { it != null })
    }

    fun play(guildId: Snowflake, soundFilepath: String): Boolean {
        audioService.play(guildId, soundFilepath)
        return true // FIXME: stream the state of the player to the client instead (e.g. currently playing, then done)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(DiscordService::class.java)
    }
}

