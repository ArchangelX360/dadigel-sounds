package se.dorne.discordbot.controllers

import discord4j.rest.util.Snowflake
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import se.dorne.discordbot.models.BotStatus
import se.dorne.discordbot.models.ChannelResponse
import se.dorne.discordbot.models.GuildResponse
import se.dorne.discordbot.services.DiscordService
import kotlin.time.ExperimentalTime

@RestController
@CrossOrigin
class DiscordController(
        @Autowired val discordService: DiscordService
) {

    @OptIn(ExperimentalTime::class)
    @GetMapping("/guilds")
    fun listGuilds(): Flow<List<GuildResponse>> = discordService.listGuilds()

    @OptIn(ExperimentalTime::class)
    @GetMapping("/guilds/{guildId}/channels")
    fun listChannels(@PathVariable guildId: String): Flow<List<ChannelResponse>> =
            discordService.listChannels(Snowflake.of(guildId))

    @OptIn(ExperimentalTime::class)
    @GetMapping("/guilds/{guildId}/status")
    fun botStatus(@PathVariable guildId: String): Flow<BotStatus> = discordService.botStatus(Snowflake.of(guildId))

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
    ): Boolean {
        discordService.play(Snowflake.of(guildId), soundIdentifier)
        return true
    }
}
