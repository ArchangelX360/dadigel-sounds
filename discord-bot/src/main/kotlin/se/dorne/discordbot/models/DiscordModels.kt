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

fun Guild.toResult() = GuildResult(id.asString(), name)

fun VoiceChannel.toResult() = ChannelResult(id.asString(), name)
