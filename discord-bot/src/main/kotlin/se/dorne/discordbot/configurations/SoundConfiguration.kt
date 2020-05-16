package se.dorne.discordbot.configurations

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "sounds")
class SoundsConfiguration {
    lateinit var folder: String
    lateinit var mappings: List<SoundMapping>
    lateinit var supportedExtensions: Set<String>
}

class SoundMapping {
    lateinit var filename: String
    lateinit var displayName: String
}
