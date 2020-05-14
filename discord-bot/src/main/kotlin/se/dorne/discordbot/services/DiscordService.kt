package se.dorne.discordbot.services

import discord4j.core.DiscordClient
import discord4j.rest.util.Snowflake
import discord4j.voice.VoiceConnection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import se.dorne.discordbot.configurations.DiscordConfiguration
import se.dorne.discordbot.models.*
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

    private var session: MutableStateFlow<DiscordSession?> = MutableStateFlow(null)

    private suspend fun getSession(): DiscordSession {
        sessionMutex.withLock {
            if (session.value?.connected != true) {
                logger.info("No connected session, (re-)connecting to Discord...")
                session.value = discord.newSession()
            }
            return session.value ?: error("failed to login to Discord")
        }
    }

    fun listGuilds(): Flow<List<GuildResult>> = session.flatMapLatest {
        it?.watchGuilds()?.map { guilds -> guilds.toGuildResults() } ?: emptyFlow()
    }

    fun listChannels(guildId: Snowflake): Flow<List<ChannelResult>> = session.flatMapLatest {
        it?.watchChannels(guildId)?.map { channels -> channels.toChannelResults() } ?: emptyFlow()
    }

    suspend fun join(guildId: Snowflake, channelId: Snowflake) {
        val audioProvider = audioService.registerGuild(guildId)
        getSession().join(guildId, channelId, audioProvider)
        // FIXME unregister from audio service if join() call fails
    }

    suspend fun leave(guildId: Snowflake) {
        audioService.unregisterGuild(guildId)
        getSession().leave(guildId)
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

