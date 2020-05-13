package se.dorne.discordbot.controllers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import se.dorne.discordbot.models.Sound

@RestController
@CrossOrigin
class SoundsController {
    @GetMapping("/sounds")
    fun listSounds(): Flow<List<Sound>> =
            // TODO("implement sound asset retrieval")
            flowOf(emptyList())
}
