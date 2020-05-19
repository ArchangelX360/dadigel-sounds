package se.dorne.discordbot.utils

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.channel.ChannelEvent
import discord4j.core.event.domain.channel.VoiceChannelCreateEvent
import discord4j.core.event.domain.channel.VoiceChannelDeleteEvent
import discord4j.core.event.domain.channel.VoiceChannelUpdateEvent
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.guild.GuildDeleteEvent
import discord4j.core.event.domain.guild.GuildEvent
import discord4j.core.event.domain.guild.GuildUpdateEvent
import discord4j.rest.util.Snowflake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import reactor.core.publisher.Mono
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

fun GatewayDiscordClient.watchGuilds(): Flow<Set<Guild>> = watch<GuildEvent>().updatingGuildSet { fetchGuilds() }

fun GatewayDiscordClient.watchVoiceChannelsForGuild(guildId: Snowflake): Flow<Set<VoiceChannel>> =
    watch<ChannelEvent>()
        .filterVoiceChannelEventsForGuild(guildId)
        .updatingChannelSet { fetchVoiceChannels(guildId) }

inline fun <reified E : Event> GatewayDiscordClient.watch(): Flow<E> = on(E::class.java).asFlow()

private suspend fun GatewayDiscordClient.fetchGuilds(): Set<Guild> = guilds.asFlow().toSet()

private suspend fun GatewayDiscordClient.fetchVoiceChannels(guildId: Snowflake): Set<VoiceChannel> =
    getGuildChannels(guildId).asFlow().filterIsInstance<VoiceChannel>().toSet()

private fun Flow<ChannelEvent>.filterVoiceChannelEventsForGuild(guildId: Snowflake): Flow<ChannelEvent> = filter {
    when (it) {
        is VoiceChannelCreateEvent -> it.channel.guildId == guildId
        is VoiceChannelUpdateEvent -> it.current.guildId == guildId
        is VoiceChannelDeleteEvent -> it.channel.guildId == guildId
        else -> false
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun Flow<GuildEvent>.updatingGuildSet(computeInitial: suspend () -> Set<Guild>): Flow<Set<Guild>> = flow {
    val initialGuilds = computeInitial()
    emitAll(scan(initialGuilds) { set, event -> set.updatedBy(event) })
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun Flow<ChannelEvent>.updatingChannelSet(
    computeInitial: suspend () -> Set<VoiceChannel>
): Flow<Set<VoiceChannel>> = flow {
    val initialChannels = computeInitial()
    emitAll(scan(initialChannels) { set, event -> set.updatedBy(event) })
}

private fun Set<Guild>.updatedBy(event: GuildEvent): Set<Guild> = when (event) {
    is GuildCreateEvent -> this + event.guild
    is GuildUpdateEvent -> this + event.current
    is GuildDeleteEvent -> this - (event.guild.orElse(first { it.id == event.guildId }))
    else -> this
}

private fun Set<VoiceChannel>.updatedBy(event: ChannelEvent): Set<VoiceChannel> = when (event) {
    is VoiceChannelCreateEvent -> this + event.channel
    is VoiceChannelUpdateEvent -> this + event.current
    is VoiceChannelDeleteEvent -> this - event.channel
    else -> this
}

@OptIn(ExperimentalTime::class)
suspend fun Mono<Void>.awaitVoidWithTimeout(timeout: Duration = 5.seconds, message: String) {
    withTimeoutOrNull(timeout) { awaitFirstOrNull() ?: Unit } ?: error(message)
}
