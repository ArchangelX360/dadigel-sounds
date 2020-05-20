package se.dorne.discordbot.services

import discord4j.core.DiscordClient
import discord4j.rest.util.Snowflake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import se.dorne.discordbot.configurations.DiscordConfiguration
import se.dorne.discordbot.models.*
import kotlin.time.ExperimentalTime

@Service
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class DiscordService(
        @Autowired discordConfiguration: DiscordConfiguration,
        @Autowired val audioService: AudioService
) : DisposableBean {
    private val discord = DiscordClient.create(discordConfiguration.bot.token)

    private val sessionMutex = Mutex()

    private var session: MutableStateFlow<DiscordSession?> = MutableStateFlow(null)

    private suspend fun getSession(): DiscordSession {
        sessionMutex.withLock {
            if (session.value?.connected != true) {
                logger.info("No connected session, (re-)connecting to Discord...")
                session.value = discord.newSession()
                logger.info("Connected to Discord")
            }
            return session.value ?: error("failed to login to Discord")
        }
    }

    private suspend fun ensureConnected() {
        getSession()
    }

    // Some things to note here:
    //  1. ensureConnected() makes sure we have a session at the subscription start and we can listen to guilds
    //  2. the session flow ensures that we get the events of the new session if deco/reco happens
    //  3. if the bot auto-disconnects, any active subscription on this flow won't get new events until someone else
    //  subscribes or if join/leave are used.
    //  We could end this flow on disconnect (and thus stop subscribers), but this is inferior because it would
    //  prevent #2 without solving #3.
    //  Another option would be to auto-reconnect as long as there is a subscriber, but this would be a waste if
    //  someone just leaves his browser open forever.
    fun listGuilds(): Flow<List<GuildResponse>> = session.onStart { ensureConnected() }
            .flatMapLatest {
                it?.watchGuilds()?.map { guilds -> guilds.toGuildResponses() } ?: MutableStateFlow(emptyList())
            }

    fun listChannels(guildId: Snowflake): Flow<List<ChannelResponse>> = session.onStart { ensureConnected() }
            .flatMapLatest {
                it?.watchChannels(guildId)?.map { channels -> channels.toChannelResponses() } ?: emptyFlow()
            }

    suspend fun join(guildId: Snowflake, channelId: Snowflake) {
        val audioProvider = audioService.registerGuild(guildId)
        getSession().join(guildId, channelId, audioProvider)
        // We don't unregister in case of failure to join, because we don't know the reason of the failure.
        // It is likely that join() times out if the bot is already in the channel, and we want audio in this case.
    }

    suspend fun leave(guildId: Snowflake) {
        audioService.unregisterGuild(guildId)
        getSession().leave(guildId)
    }

    @OptIn(ExperimentalTime::class)
    fun botStatus(guildId: Snowflake): Flow<BotStatus> {
        val playingTrack = audioService.watchPlayingTrack(guildId)
        val joinedChannel = session.onStart { ensureConnected() }
                .flatMapLatest { it?.watchedJoinedChannel(guildId) ?: emptyFlow() }
                .map { it?.toResponse() }
        return joinedChannel.combine(playingTrack) { channel, track -> BotStatus(channel, track) }
    }

    suspend fun play(guildId: Snowflake, soundFilepath: String) {
        getSession().activityMonitor.notify()
        audioService.play(guildId, soundFilepath)
    }

    override fun destroy() {
        runBlocking {
            logger.info("Closing Discord session")
            session.value?.close()
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(DiscordService::class.java)
    }
}

