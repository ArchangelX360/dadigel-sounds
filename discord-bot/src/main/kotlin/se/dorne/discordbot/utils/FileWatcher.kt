package se.dorne.discordbot.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import kotlin.coroutines.coroutineContext

fun Flow<WatchEvent<Path>>.updatingSet(initialSet: Set<Path> = emptySet()): Flow<Set<Path>> = flow {
    var currentSet = initialSet
    emit(currentSet)
    collect { e ->
        val path: Path = e.context()
        currentSet = when (e.kind()) {
            ENTRY_CREATE -> currentSet + path
            ENTRY_DELETE -> currentSet - path
            else -> currentSet
        }
        emit(currentSet)
    }
}

/**
 * Returns a [Flow] of events of the provided [types] that happened on paths relative to this [Path].
 */
// blocking calls are ok because of .flowOn(Dispatchers.IO)
// withContext(IO) should not be used around emit() to ensure context preservation
@Suppress("BlockingMethodInNonBlockingContext")
@OptIn(ExperimentalCoroutinesApi::class)
fun Path.watchPathEvents(vararg types: WatchEvent.Kind<Path>): Flow<WatchEvent<Path>> =
    flow {
        val watchService = FileSystems.getDefault().newWatchService()
        val sub = register(watchService, types)
        try {
            while (coroutineContext.isActive) {
                val watchKey = watchService.poll()
                if (watchKey == null) {
                    delay(3000)
                    continue
                }
                watchKey.pollEvents().filterPathEvents().forEach {
                    emit(it)
                }

                if (!watchKey.reset()) {
                    watchKey.cancel()
                    break
                }
            }
        } finally {
            sub.cancel()
            watchService.close()
        }
    }.flowOn(Dispatchers.IO)

private fun List<WatchEvent<*>>.filterPathEvents(): List<WatchEvent<Path>> =
    filter {
        it.kind().type() == Path::class.java
    }.map {
        @Suppress("UNCHECKED_CAST")
        it as WatchEvent<Path>
    }
