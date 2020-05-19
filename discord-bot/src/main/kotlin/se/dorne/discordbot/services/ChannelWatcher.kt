package se.dorne.discordbot.services

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.rest.util.Snowflake
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import se.dorne.discordbot.utils.watchVoiceChannelsForGuild

fun CoroutineScope.watchChannelsForGuild(guildId: Snowflake, client: GatewayDiscordClient): ChannelWatcher {
    return ChannelWatcher(guildId, client).also { it.startIn(this) }
}

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
            client.watchVoiceChannelsForGuild(guildId)
                .map { channels -> channels.sortedBy { it.name } }
                .collect { mutableChannelsState.value = it }
        }
        logger.info("Started channel watcher for guild $guildId")
    }

    fun stop() {
        watchJob?.cancel()
        logger.info("Stopped channel watcher for guild $guildId")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ChannelWatcher::class.java)
    }
}
