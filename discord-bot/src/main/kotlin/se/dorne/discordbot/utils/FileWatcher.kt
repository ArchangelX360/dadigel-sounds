package se.dorne.discordbot.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
fun Path.watchPathEvents(vararg eventTypes: WatchEvent.Kind<Path>): Flow<WatchEvent<Path>> =
    flow {
        // blocking calls are ok because of .flowOn(Dispatchers.IO)
        // withContext(IO) should not be used around emit() to ensure context preservation
        @Suppress("BlockingMethodInNonBlockingContext")
        val watchService = FileSystems.getDefault().newWatchService()
        @Suppress("BlockingMethodInNonBlockingContext")
        val sub = register(watchService, eventTypes)
        try {
            while (coroutineContext.isActive) {
                @Suppress("BlockingMethodInNonBlockingContext")
                val watchKey = watchService.take()
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
            @Suppress("BlockingMethodInNonBlockingContext")
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
