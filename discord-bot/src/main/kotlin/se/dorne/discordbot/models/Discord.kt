package se.dorne.discordbot.models

import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.channel.VoiceChannel

class GuildResponse(
        val id: String,
        val name: String
) {
    override fun equals(other: Any?): Boolean = other is GuildResponse && this.id == other.id

    override fun hashCode(): Int = id.hashCode()
}

data class ChannelResponse(val id: String, val name: String)

fun List<Guild>.toGuildResponses() = map { it.toResponse() }

fun Guild.toResponse() = GuildResponse(id.asString(), name)

fun List<VoiceChannel>.toChannelResponses() = map { it.toResponse() }

fun VoiceChannel.toResponse() = ChannelResponse(id.asString(), name)

data class BotStatus(
        val state: BotState,
        val joinedChannel: ChannelResponse?,
        val playingTrack: TrackInfo?
)

enum class BotState {
    // We don't need to distinguish true offline from "logged in but not joined"
    // The Discord login of the bot is not a concern of the frontend
    OFFLINE,
    JOINED_IDLE,
    PLAYING
}

data class TrackInfo(
    val id: String,
    val title: String
)
