package se.dorne.discordbot.services

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.rest.util.Snowflake
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import se.dorne.discordbot.utils.watchGuilds

fun CoroutineScope.watchGuilds(client: GatewayDiscordClient): GuildWatcher =
    GuildWatcher(client).also { it.startIn(this) }

/**
 * Watches updates to the list of guilds for the logged in user.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GuildWatcher(
        private val client: GatewayDiscordClient
) {
    private val mutableGuilds: MutableStateFlow<List<Guild>> = MutableStateFlow(emptyList())
    val guilds: StateFlow<List<Guild>> = mutableGuilds

    private val channelWatchers: MutableStateFlow<Map<Snowflake, ChannelWatcher>> = MutableStateFlow(emptyMap())

    fun startIn(scope: CoroutineScope) {
        scope.launch {
            client.watchGuilds()
                .map { gs -> gs.sortedBy { it.name } }
                .collect { mutableGuilds.value = it }
        }
        scope.launch {
            guilds.collect { updateChannelWatchers(it) }
        }
        logger.info("Started guild watcher")
    }

    private fun CoroutineScope.updateChannelWatchers(guilds: List<Guild>) {
        val guildIds = guilds.map { it.id }
        val currentWatchers = channelWatchers.value

        val watchersToStop = currentWatchers - guildIds
        watchersToStop.values.forEach { it.stop() }

        val watchersToKeep = currentWatchers.filterKeys { it in guildIds }
        val newWatchers = guildIds
            .filter { it !in watchersToKeep }
            .associateWith { id -> watchChannelsForGuild(id, client) }
        channelWatchers.value = watchersToKeep + newWatchers
    }

    fun watchChannels(guildId: Snowflake): Flow<List<VoiceChannel>> =
        channelWatchers.flatMapLatest { it[guildId]?.channelsState ?: emptyFlow<List<VoiceChannel>>() }

    companion object {
        private val logger = LoggerFactory.getLogger(GuildWatcher::class.java)
    }
}
