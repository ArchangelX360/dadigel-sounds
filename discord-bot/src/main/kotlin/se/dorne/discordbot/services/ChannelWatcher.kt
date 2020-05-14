package se.dorne.discordbot.services

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.core.event.domain.channel.ChannelEvent
import discord4j.core.event.domain.channel.VoiceChannelCreateEvent
import discord4j.core.event.domain.channel.VoiceChannelDeleteEvent
import discord4j.core.event.domain.channel.VoiceChannelUpdateEvent
import discord4j.rest.util.Snowflake
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import se.dorne.discordbot.utils.fetchVoiceChannels
import se.dorne.discordbot.utils.watch

/**
 * Watches updates to the list of channels in a given guild.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChannelWatcher(
        private val guildId: Snowflake,
        private val client: GatewayDiscordClient
) {
    // this has to be a sorted list (not a set) so that renames are considered changes
    private val mutableChannelsState: MutableStateFlow<List<VoiceChannel>> = MutableStateFlow(emptyList())

    val channelsState: StateFlow<List<VoiceChannel>> = mutableChannelsState

    private var watchJob: Job? = null

    fun startIn(scope: CoroutineScope) {
        watchJob = scope.launch {
            val initialChannels = client.fetchVoiceChannels(guildId)
            client.watch<ChannelEvent>()
                    .scan(initialChannels) { set, event -> set.updatedBy(event) }
                    .collect { channels ->
                        val sortedChannelsList = channels.sortedBy { it.name }
                        mutableChannelsState.value = sortedChannelsList
                    }
        }
    }

    private fun Set<VoiceChannel>.updatedBy(event: ChannelEvent): Set<VoiceChannel> = when (event) {
        is VoiceChannelCreateEvent -> if (event.channel.guildId == guildId) this + event.channel else this
        is VoiceChannelUpdateEvent -> if (event.current.guildId == guildId) this + event.current else this
        is VoiceChannelDeleteEvent -> if (event.channel.guildId == guildId) this - event.channel else this
        else -> this
    }

    fun stop() {
        watchJob?.cancel()
    }
}
