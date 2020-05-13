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
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBufferFactory
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import discord4j.voice.AudioProvider
import org.springframework.stereotype.Service
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean


@Service
class AudioService {
    fun play(provider: CustomAudioProvider, soundFilepath: String) {
        if (provider.player.playingTrack != null) {
            provider.player.stopTrack()
        }

        // FIXME: handle .mp3 resources
        provider.audioManager.loadItem(soundFilepath, provider.scheduler)
        // FIXME: wrap scheduler events in a return
    }
}

class CustomAudioProvider
    : AudioProvider(
        ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize())
) {
    internal val audioManager by lazy {
        DefaultAudioPlayerManager().apply {
            // This is an optimization strategy that Discord4J can utilize. It is not important to understand
            configuration.frameBufferFactory = AudioFrameBufferFactory { bufferDuration: Int, format: AudioDataFormat?, stopping: AtomicBoolean? -> NonAllocatingAudioFrameBuffer(bufferDuration, format, stopping) }
            // Allow playerManager to parse remote sources like YouTube links
            AudioSourceManagers.registerRemoteSources(this)
        }
    }
    internal val player by lazy {
        audioManager.createPlayer()
    }
    internal val scheduler by lazy {
        TrackScheduler(player)
    }

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

class TrackScheduler(private val player: AudioPlayer) : AudioLoadResultHandler {
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
