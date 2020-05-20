package se.dorne.discordbot.services

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBuffer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBufferFactory
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import discord4j.rest.util.Snowflake
import discord4j.voice.AudioProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import se.dorne.discordbot.configurations.SoundsConfiguration
import se.dorne.discordbot.models.TrackInfo
import java.nio.ByteBuffer
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private val MAX_OPUS_FRAME_SIZE = StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()

@OptIn(ExperimentalCoroutinesApi::class)
@Service
class AudioService(@Autowired private val soundsConfiguration: SoundsConfiguration) {

    private val audioManager = DefaultAudioPlayerManager().apply {
        // This is an optimization strategy that Discord4J can utilize. It is not important to understand
        configuration.frameBufferFactory = NonAllocatingBufferFactory()
        // Allow playerManager to parse remote sources like YouTube links
        AudioSourceManagers.registerRemoteSources(this)
    }

    private val audioProvider: MutableMap<Snowflake, CustomAudioProvider> = ConcurrentHashMap()

    /**
     * Registers the guild with the given [guildId], which can then receive audio through the
     * returned [AudioProvider].
     */
    fun registerGuild(guildId: Snowflake): AudioProvider {
        logger.info("Registered guild $guildId to play audio")
        return getOrCreateAudioProvider(guildId)
    }

    /**
     * Unregisters the guild with the given [guildId], cleaning up resources.
     */
    fun unregisterGuild(guildId: Snowflake) {
        audioProvider.remove(guildId)
        logger.info("Unregistered guild $guildId")
    }

    fun watchPlayingTrack(guildId: Snowflake): Flow<TrackInfo?> = getOrCreateAudioProvider(guildId).trackInfo

    private fun getOrCreateAudioProvider(guildId: Snowflake) =
            audioProvider.computeIfAbsent(guildId) { CustomAudioProvider(audioManager.createPlayer()) }

    /**
     * Plays the track identified by [soundIdentifier] in the registered guild with the given
     * [guildId].
     */
    fun play(guildId: Snowflake, soundIdentifier: String) {
        val resolvedIdentifier = soundIdentifier.resolveIdentifier()
        logger.info("Playing track $resolvedIdentifier in guild $guildId")
        val provider = audioProvider[guildId]
                ?: error("Audio provider not found for guild $guildId, you must register one to enable playing tracks")
        if (provider.player.playingTrack != null) {
            provider.player.stopTrack()
        }

        audioManager.loadItem(resolvedIdentifier, provider.scheduler)
    }

    private fun String.resolveIdentifier(): String {
        if (isLocalFileId()) {
            val rootPath = Paths.get(soundsConfiguration.folder).normalize().toAbsolutePath()
            val path = Paths.get(soundsConfiguration.folder, this).normalize().toAbsolutePath()
            if (!path.startsWith(rootPath)) {
                throw IllegalArgumentException("trying to access a path that is not '${soundsConfiguration.folder}' nor its children")
            }
            return path.toString()
        }
        return this
    }

    private fun String.isLocalFileId(): Boolean =
        !startsWith("http") && soundsConfiguration.supportedExtensions.any { this.endsWith(it) }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(AudioService::class.java)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class CustomAudioProvider(
        val player: AudioPlayer
) : AudioProvider(ByteBuffer.allocate(MAX_OPUS_FRAME_SIZE)), AudioEventListener {

    init {
        player.addListener(this)
    }

    val trackInfo: MutableStateFlow<TrackInfo?> = MutableStateFlow(null)

    val scheduler = TrackScheduler(player)

    private val frame: MutableAudioFrame = MutableAudioFrame().apply {
        setBuffer(buffer)
    }

    override fun provide(): Boolean {
        // AudioPlayer writes audio data to its AudioFrame
        val didProvide: Boolean = player.provide(frame)
        // If audio was provided, flip from write-mode to read-mode
        if (didProvide) {
            buffer.flip()
        }
        return didProvide
    }

    override fun onEvent(event: AudioEvent?) {
        when (event) {
            is TrackStartEvent -> trackInfo.value = TrackInfo(event.track.identifier, event.track.info.title)
            is TrackEndEvent -> trackInfo.value = null
        }
    }
}

private class TrackScheduler(private val player: AudioPlayer) : AudioLoadResultHandler {
    override fun trackLoaded(track: AudioTrack) {
        // LavaPlayer found an audio source for us to play
        player.playTrack(track)
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        // LavaPlayer found multiple AudioTracks from some playlist
    }

    override fun noMatches() {
        // LavaPlayer did not find any audio to extract
    }

    override fun loadFailed(exception: FriendlyException) {
        // LavaPlayer could not parse an audio source for some reason
    }
}

private class NonAllocatingBufferFactory : AudioFrameBufferFactory {
    override fun create(
            bufferDuration: Int,
            format: AudioDataFormat?,
            stopping: AtomicBoolean?
    ): AudioFrameBuffer = NonAllocatingAudioFrameBuffer(bufferDuration, format, stopping)
}
