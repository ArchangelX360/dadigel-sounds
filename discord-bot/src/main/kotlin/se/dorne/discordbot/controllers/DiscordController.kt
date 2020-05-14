package se.dorne.discordbot.controllers

import discord4j.rest.util.Snowflake
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import se.dorne.discordbot.models.ChannelResult
import se.dorne.discordbot.models.GuildResult
import se.dorne.discordbot.services.DiscordService
import kotlin.time.ExperimentalTime

@RestController
@CrossOrigin
class DiscordController(
        @Autowired val discordService: DiscordService
) {

    @OptIn(ExperimentalTime::class)
    @GetMapping("/guilds")
    fun listGuild(): Flow<List<GuildResult>> = discordService.listGuilds()

    @OptIn(ExperimentalTime::class)
    @GetMapping("/guilds/{guildId}/channels")
    fun listGuild(@PathVariable guildId: String): Flow<List<ChannelResult>> =
            discordService.listChannels(Snowflake.of(guildId))

    @OptIn(ExperimentalTime::class)
    @GetMapping("/guilds/{guildId}/isConnected")
    fun isConnected(@PathVariable guildId: String): Flow<Boolean> =
            discordService.isConnected(Snowflake.of(guildId))

    @GetMapping("/guilds/{guildId}/channels/{channelId}/join")
    suspend fun join(@PathVariable guildId: String, @PathVariable channelId: String): Boolean {
        discordService.join(Snowflake.of(guildId), Snowflake.of(channelId))
        return true
    }

    @GetMapping("/guilds/{guildId}/leave")
    suspend fun leave(@PathVariable guildId: String): Boolean {
        discordService.leave(Snowflake.of(guildId))
        return true
    }

    @GetMapping("/guilds/{guildId}/play")
    suspend fun play(
            @PathVariable guildId: String,
            @RequestParam soundIdentifier: String
    ): Boolean = discordService.play(Snowflake.of(guildId), soundIdentifier)
}
