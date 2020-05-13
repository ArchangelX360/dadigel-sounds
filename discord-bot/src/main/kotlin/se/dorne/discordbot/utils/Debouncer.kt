package se.dorne.discordbot.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Sends ticks to prevent the debouncer from firing.
 */
interface DebounceTicker {
    suspend fun tick()
}

/**
 * Fires [onDebounce] after the given [timeout] if inactive.
 * The returned [DebounceTicker] should be used to reset the timeout if necessary to prevent firing.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
fun CoroutineScope.launchDebounce(timeout: Duration, onDebounce: suspend () -> Unit): DebounceTicker {

    // channel used to reset the debounce and prevent it from firing
    val resetter = Channel<Unit>()

    launch {
        while (isActive) {
            select<Unit> {
                resetter.onReceive { }
                onTimeout(timeout, onDebounce)
            }
        }
    }

    return object : DebounceTicker {
        override suspend fun tick() {
            resetter.send(Unit)
        }
    }
}
