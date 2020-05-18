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
 * Allows to notify when activity happens to reset the inactive timeout.
 */
interface ActivityMonitor {
    /**
     * Notifies that activity happened to reset the inactive timeout.
     */
    suspend fun notify()
}

/**
 * Fires [onInactive] after the given [inactiveTimeout] if inactive.
 * The returned [ActivityMonitor] should be used to [notify] activity and reset the timeout.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
fun CoroutineScope.launchInactiveTimeout(inactiveTimeout: Duration, onInactive: suspend () -> Unit): ActivityMonitor {

    // channel used to reset the debounce and prevent it from firing
    val resetter = Channel<Unit>()

    launch {
        while (isActive) {
            select<Unit> {
                resetter.onReceive { }
                onTimeout(inactiveTimeout, onInactive)
            }
        }
    }

    return object : ActivityMonitor {
        override suspend fun notify() {
            resetter.send(Unit)
        }
    }
}
