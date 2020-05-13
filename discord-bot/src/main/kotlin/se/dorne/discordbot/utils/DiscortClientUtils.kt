package se.dorne.discordbot.utils

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.core.event.domain.Event
import discord4j.rest.util.Snowflake
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow

inline fun <reified E : Event> GatewayDiscordClient.watch(): Flow<E> = on(E::class.java).asFlow()

suspend fun GatewayDiscordClient.fetchGuilds(): Set<Guild> = guilds.asFlow().toSet()

suspend fun GatewayDiscordClient.fetchVoiceChannels(guildId: Snowflake): Set<VoiceChannel> =
        getGuildChannels(guildId)
                .asFlow()
                .filterIsInstance<VoiceChannel>()
                .toSet()
