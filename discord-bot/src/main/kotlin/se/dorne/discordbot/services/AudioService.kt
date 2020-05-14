package se.dorne.discordbot.services

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private val MAX_OPUS_FRAME_SIZE = StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()

@Service
class AudioService {

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
        val provider = CustomAudioProvider(audioManager.createPlayer())
        audioProvider[guildId] = provider
        logger.info("Registered guild $guildId to play audio")
        return provider
    }

    /**
     * Unregisters the guild with the given [guildId], cleaning up resources.
     */
    fun unregisterGuild(guildId: Snowflake) {
        audioProvider.remove(guildId)
        logger.info("Unregistered guild $guildId")
    }

    /**
     * Plays the track identified by [soundFilepath] in the registered guild with the given
     * [guildId].
     */
    fun play(guildId: Snowflake, soundFilepath: String) {
        logger.info("Playing track $soundFilepath in guild $guildId")
        val provider = audioProvider[guildId] ?:
            error("Audio provider not found for guild $guildId, you must register one to enable playing tracks")
        if (provider.player.playingTrack != null) {
            provider.player.stopTrack()
        }

        // FIXME: handle .mp3 resources
        audioManager.loadItem(soundFilepath, provider.scheduler)
        // FIXME: wrap scheduler events in a return
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(AudioService::class.java)
    }
}

private class CustomAudioProvider(
    val player: AudioPlayer
) : AudioProvider(ByteBuffer.allocate(MAX_OPUS_FRAME_SIZE)) {

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
