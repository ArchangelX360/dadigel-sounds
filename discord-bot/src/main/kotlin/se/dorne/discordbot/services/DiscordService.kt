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
import kotlin.time.ExperimentalTime

data class Connection(val voice: VoiceConnection, val audioProvider: CustomAudioProvider)

@Service
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class DiscordService(
        @Autowired discordConfiguration: DiscordConfiguration,
        @Autowired val audioService: AudioService
) {
    private val discord = DiscordClient.create(discordConfiguration.bot.token)

    private val sessionMutex = Mutex()
    private var session: DiscordSession? = null

    internal val connections: MutableMap<Snowflake, Connection> = mutableMapOf()

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

    suspend fun join(guildId: Snowflake, channelId: Snowflake, audioProvider: CustomAudioProvider): Connection {
        // FIXME: hack to prevent channel switch to timeout
        // First "join" works, but then any subsequent "join" will not emit in the Mono returned by Discord4J
        //  - Only leaving the channel without closing the session still times out
        //  - Only closing the session without leaving the channel still times out
        try {
            leave(guildId)
        } catch (e: Exception) {
        }
        getSession().close()

        val voiceConnection = getSession().join(guildId, channelId, audioProvider)
        val connection = Connection(voiceConnection, audioProvider)
        connections[guildId] = connection
        return connection
    }

    suspend fun leave(guildId: Snowflake) {
        val connection = connections[guildId] ?: error("No channel to leave in guild $guildId")
        connection.voice.disconnect()
        connections.remove(guildId)
    }

    // FIXME use ChannelResult instead of boolean to provide the current channel name to UI and allow to leave via channel ID
    // FIXME then rename this function!
    @OptIn(ExperimentalTime::class)
    fun isConnected(guildId: Snowflake): Flow<Boolean> = flow {
        val session = getSession()
        emitAll(session.watchedJoinedChannel().map { it != null })
    }

    fun play(guildId: Snowflake, soundFilepath: String): Boolean {
        val connection = connections[guildId] ?: error("No voice connection found in guild $guildId")
        audioService.play(connection.audioProvider, soundFilepath)
        return true // FIXME: stream the state of the player to the client instead (e.g. currently playing, then done)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(DiscordService::class.java)
    }
}

