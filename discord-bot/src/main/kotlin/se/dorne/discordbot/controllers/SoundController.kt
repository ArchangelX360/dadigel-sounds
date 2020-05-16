package se.dorne.discordbot.controllers

import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import se.dorne.discordbot.models.SoundResponse
import se.dorne.discordbot.services.SoundService

@RestController
@CrossOrigin
class SoundController(@Autowired val soundService: SoundService) {
    @GetMapping("/sounds")
    fun listSounds(): Flow<Set<SoundResponse>> = soundService.getSounds()
}
