package se.dorne.discordbot.services

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.guild.GuildDeleteEvent
import discord4j.core.event.domain.guild.GuildEvent
import discord4j.core.event.domain.guild.GuildUpdateEvent
import discord4j.rest.util.Snowflake
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import se.dorne.discordbot.utils.fetchGuilds
import se.dorne.discordbot.utils.watch
import java.util.concurrent.ConcurrentHashMap

/**
 * Watches updates to the list of guilds for the logged in user.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GuildWatcher(
        private val client: GatewayDiscordClient
) {
    private val mutableGuildsState: MutableStateFlow<List<Guild>> = MutableStateFlow(emptyList())
    val guildsState: StateFlow<List<Guild>> = mutableGuildsState

    private val channelWatchers: MutableMap<Snowflake, ChannelWatcher> = ConcurrentHashMap()

    fun startIn(scope: CoroutineScope) {
        scope.launch {
            watchGuildUpdates()
        }
    }

    private suspend fun watchGuildUpdates() {
        coroutineScope {
            val initialGuilds = client.fetchGuilds()
            client.watch<GuildEvent>()
                    .onEach { event -> updateChannelWatchers(event) }
                    .scan(initialGuilds) { set, event -> set.updatedBy(event) }
                    .collect { guilds ->
                        // we have to sort here so that the set is different on rename and we publish a new state
                        mutableGuildsState.value = guilds.sortedBy { it.name }
                    }
        }
    }

    private fun Set<Guild>.updatedBy(event: GuildEvent): Set<Guild> = when (event) {
        is GuildCreateEvent -> this + event.guild
        is GuildUpdateEvent -> this + event.current
        is GuildDeleteEvent -> this - (event.guild.orElse(first { it.id == event.guildId }))
        else -> this
    }

    private fun CoroutineScope.updateChannelWatchers(event: GuildEvent) {
        when (event) {
            is GuildCreateEvent -> {
                val guildId = event.guild.id
                val watcher = channelWatchers.computeIfAbsent(guildId) { ChannelWatcher(guildId, client) }
                watcher.startIn(this)
            }
            is GuildDeleteEvent -> {
                val watcher = channelWatchers[event.guildId]
                watcher?.stop()
                channelWatchers.remove(event.guildId)
            }
        }
    }

    fun watchChannels(guildId: Snowflake): Flow<List<VoiceChannel>> {
        // sometimes a subscription can start before the guilds are initialized (e.g. on reconnect)
        // in this case, we pre-create the channel watcher and only start it when we have the guild
        val watcher = channelWatchers.computeIfAbsent(guildId) { ChannelWatcher(guildId, client) }
        return watcher.channelsState
    }
}
