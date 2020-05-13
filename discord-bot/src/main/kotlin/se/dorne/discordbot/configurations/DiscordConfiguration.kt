package se.dorne.discordbot.configurations

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "discord")
class DiscordConfiguration {
    val bot = Bot()
}

class Bot {
    lateinit var token: String
}
