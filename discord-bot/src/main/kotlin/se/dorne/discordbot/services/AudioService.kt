package se.dorne.discordbot.services

import discord4j.voice.AudioProvider
import org.springframework.stereotype.Service

@Service
class AudioService {
    // TODO add methods to send sounds to an audio player and provide a corresponding audio provider
    // Refer to steps 22 and 23 here: https://github.com/Discord4J/Discord4J/wiki/Music-Bot-Tutorial
    // Maybe not Lava player in our case?
}

object NoopAudioProvider : AudioProvider() {

    override fun provide(): Boolean {
        return false
    }
}
