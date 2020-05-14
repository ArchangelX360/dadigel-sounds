package se.dorne.discordbot.models

import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.channel.VoiceChannel

class GuildResult(
        val id: String,
        val name: String
) {
    override fun equals(other: Any?): Boolean = other is GuildResult && this.id == other.id

    override fun hashCode(): Int = id.hashCode()
}

data class ChannelResult(val id: String, val name: String)

fun List<Guild>.toGuildResults() = map { it.toResult() }

fun Guild.toResult() = GuildResult(id.asString(), name)

fun List<VoiceChannel>.toChannelResults() = map { it.toResult() }

fun VoiceChannel.toResult() = ChannelResult(id.asString(), name)

data class BotStatus(
    val state: BotState,
    val joinedChannel: ChannelResult?,
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
