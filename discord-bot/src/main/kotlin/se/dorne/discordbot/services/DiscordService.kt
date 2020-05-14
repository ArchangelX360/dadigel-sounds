package se.dorne.discordbot.services

import discord4j.core.DiscordClient
import discord4j.rest.util.Snowflake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import se.dorne.discordbot.configurations.DiscordConfiguration
import se.dorne.discordbot.models.BotState
import se.dorne.discordbot.models.BotStatus
import se.dorne.discordbot.models.ChannelResult
import se.dorne.discordbot.models.GuildResult
import se.dorne.discordbot.models.TrackInfo
import se.dorne.discordbot.models.toChannelResults
import se.dorne.discordbot.models.toGuildResults
import se.dorne.discordbot.models.toResult
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
    fun listGuilds(): Flow<List<GuildResult>> = session.onStart { ensureConnected() }
        .flatMapLatest {
            it?.watchGuilds()?.map { guilds -> guilds.toGuildResults() } ?: MutableStateFlow(emptyList())
        }

    fun listChannels(guildId: Snowflake): Flow<List<ChannelResult>> = session.onStart { ensureConnected() }
        .flatMapLatest {
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

    @OptIn(ExperimentalTime::class)
    fun botStatus(guildId: Snowflake): Flow<BotStatus> {
        val playingTrack = audioService.watchPlayingTrack(guildId)
        val joinedChannel = session.onStart { ensureConnected() }
            .flatMapLatest { it?.watchedJoinedChannel(guildId) ?: emptyFlow() }
            .map { it?.toResult() }
        return joinedChannel.combine(playingTrack) { channel, track ->
            computeBotStatus(channel, track)
        }
    }

    private fun computeBotStatus(channel: ChannelResult?, playingTrackInfo: TrackInfo?): BotStatus {
        val state = when {
            playingTrackInfo != null -> BotState.PLAYING
            channel != null -> BotState.JOINED_IDLE
            else -> BotState.OFFLINE
        }
        return BotStatus(
            state = state,
            joinedChannel = channel,
            playingTrack = playingTrackInfo
        )
    }

    fun play(guildId: Snowflake, soundFilepath: String): Boolean {
        audioService.play(guildId, soundFilepath)
        return true // FIXME: stream the state of the player to the client instead (e.g. currently playing, then done)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(DiscordService::class.java)
    }
}

