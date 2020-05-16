package se.dorne.discordbot.services

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import se.dorne.discordbot.configurations.SoundMapping
import se.dorne.discordbot.configurations.SoundsConfiguration
import se.dorne.discordbot.models.SoundResponse
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import kotlin.time.ExperimentalTime

@Service
@OptIn(ExperimentalCoroutinesApi::class)
class SoundService(@Autowired val soundsConfiguration: SoundsConfiguration) {
    private val scope = CoroutineScope(Job() + CoroutineName("sounds-watcher"))

    private val mutableFilenames: MutableStateFlow<Set<String>> = MutableStateFlow(listFiles(soundsConfiguration.folder))

    val filenames: StateFlow<Set<String>> = mutableFilenames

    final val fileWatcher = FileSystems.getDefault().newWatchService()

    private var watchJob: Job? = null

    val soundFolder = File(soundsConfiguration.folder)
            .toPath()
            .register(fileWatcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE)

    init {
        startIn(scope)
    }


    fun getSounds(): Flow<Set<SoundResponse>> {
        return filenames.map { sds ->
            sds.map { it.toSoundResponse(soundsConfiguration.mappings) }.toSet()
        }
    }

    fun String.toSoundResponse(mappings: List<SoundMapping>): SoundResponse {
        val mapping = soundsConfiguration.mappings.find { it.filename == this }
        return SoundResponse(mapping?.filename ?: this, mapping?.displayName)
    }

    private fun listFiles(folder: String): Set<String> {
        return File(folder)
                .listFiles { _, name -> name.endsWith(".mp3") }
                ?.map { it.name }?.toSet() ?: emptySet()
    }

    @OptIn(ExperimentalTime::class)
    private final fun startIn(scope: CoroutineScope) {
        watchJob = scope.launch {
            withContext(Dispatchers.IO) {
                logger.info("Starting file watcher on ${soundsConfiguration.folder}")
                while (true) {
                    val watchKey = fileWatcher.take()

                    for (event in watchKey.pollEvents()) {
                        if (event.kind().type() == Path::class.java) {
                            @Suppress("UNCHECKED_CAST")
                            mutableFilenames.value = mutableFilenames.value.updatedBy(event as WatchEvent<Path>)
                        }
                    }

                    if (!watchKey.reset()) {
                        watchKey.cancel()
                        fileWatcher.close()
                        break
                    }
                }

                soundFolder.cancel()
            }
        }
    }

    private fun Set<String>.updatedBy(e: WatchEvent<Path>): Set<String> {
        when (e.kind().name()) {
            StandardWatchEventKinds.ENTRY_CREATE.name() -> {
                val filename = e.context().fileName.toString()
                logger.info("file created $filename")
                return this.plus(filename)
            }
            StandardWatchEventKinds.ENTRY_DELETE.name() -> {
                val filename = e.context().fileName.toString()
                logger.info("file deleted $filename")
                return this.minus(filename)
            }
            else -> {
                logger.warn("ignored event $e")
                return this
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(SoundService::class.java)
    }
}
