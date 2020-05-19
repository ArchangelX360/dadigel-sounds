package se.dorne.discordbot.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY

internal class FileWatcherKtTest {

    data class Event(
        val kind: WatchEvent.Kind<Path>,
        val path: Path
    ) : WatchEvent<Path> {
        override fun count(): Int = 1
        override fun kind(): WatchEvent.Kind<Path> = kind
        override fun context(): Path = path
    }

    @Test
    fun updatingSet() = runBlocking {
        val bob = Paths.get("bob.txt")
        val alice = Paths.get("alice.txt")
        val carl = Paths.get("carl.txt")

        val events = flowOf(
            Event(ENTRY_CREATE, bob),
            Event(ENTRY_CREATE, alice),
            Event(ENTRY_MODIFY, bob),
            Event(ENTRY_DELETE, alice),
            Event(ENTRY_DELETE, carl)
        )
        val sets = events.updatingSet(setOf(carl)).toList()

        val expectedSets = listOf<Set<Path>>(
            setOf(carl),
            setOf(carl, bob),
            setOf(carl, bob, alice),
            setOf(carl, bob, alice),
            setOf(carl, bob),
            setOf(bob)
        )
        assertEquals(expectedSets, sets)
    }

    @OptIn(FlowPreview::class)
    @Test
    fun watchPathEvents() {
        println("File system events are slow, this test is not hanging (it does last ~30s)")
        runBlocking {
            val dir = Files.createTempDirectory("test-watch")!!
            val eventFlow = dir.watchPathEvents(ENTRY_CREATE, ENTRY_DELETE)
            val eventChannel = eventFlow.produceIn(this)

            try {
                delay(500) // allows file watcher coroutine to actually subscribe to events
                val file1 = Files.createTempFile(dir, "test", ".txt")
                val event1 = eventChannel.receive()
                assertEquals(ENTRY_CREATE, event1.kind())
                assertEquals(file1, dir.resolve(event1.context()))

                val file2 = Files.createTempFile(dir, "test", ".txt")
                val event2 = eventChannel.receive()
                assertEquals(ENTRY_CREATE, event2.kind())
                assertEquals(file2, dir.resolve(event2.context()))

                file1.toFile().delete()
                val event3 = eventChannel.receive()
                assertEquals(ENTRY_DELETE, event3.kind())
                assertEquals(file1, dir.resolve(event3.context()))
            } finally {
                eventChannel.cancel()
            }
        }
    }
}
