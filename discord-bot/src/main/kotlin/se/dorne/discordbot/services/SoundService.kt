package se.dorne.discordbot.services

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import se.dorne.discordbot.configurations.SoundsConfiguration
import se.dorne.discordbot.models.SoundResponse
import se.dorne.discordbot.utils.updatingSet
import se.dorne.discordbot.utils.watchPathEvents
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import kotlin.streams.asSequence

@Service
@OptIn(ExperimentalCoroutinesApi::class)
class SoundService(@Autowired val soundsConfiguration: SoundsConfiguration): DisposableBean {

    private val scope = CoroutineScope(Job() + CoroutineName("sounds-watcher"))

    private val soundFiles: MutableStateFlow<Set<Path>> = MutableStateFlow(emptySet())

    init {
        watchFiles()
    }

    fun getSounds(): Flow<Set<SoundResponse>> = soundFiles.map { sounds ->
        sounds.map { it.toSoundResponse() }.toSet()
    }

    private fun Path.toSoundResponse(): SoundResponse {
        val filename = fileName.toString()
        val mapping = soundsConfiguration.mappings.find { it.filename == filename }
        return SoundResponse(filename, mapping?.displayName)
    }

    private final fun watchFiles() {
        scope.launch {
            val path = Paths.get(soundsConfiguration.folder)
            if (!path.toFile().exists()) {
                logger.error("could not find folder ${soundsConfiguration.folder}")
                return@launch
            }
            logger.info("Starting file watcher on $path")
            watchFolder(path) {
                it.hasSupportedExtension()
            }.collect { files ->
                soundFiles.value = files
            }
        }
    }

    private fun watchFolder(path: Path, predicate: (Path) -> Boolean): Flow<Set<Path>> =
        path.watchPathEvents(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE)
            .onEach { logFileEvent(it) }
            .filter { predicate(it.context()) }
            .updatingSet(listFiles(path, predicate))

    private fun listFiles(folder: Path, predicate: (Path) -> Boolean): Set<Path> = Files.list(folder)
        .asSequence()
        .filter(predicate)
        .onEach { logger.info("Found file $it") }
        .toSet()

    private fun Path.hasSupportedExtension(): Boolean {
        val fileNameStr = this.fileName.toString()
        return soundsConfiguration.supportedExtensions.any { fileNameStr.endsWith(it) }
    }

    private fun logFileEvent(e: WatchEvent<Path>) {
        val path = e.context()
        when (e.kind()) {
            StandardWatchEventKinds.ENTRY_CREATE -> {
                if (path.hasSupportedExtension()) {
                    logger.info("File created $path")
                } else {
                    logger.warn("Ignoring creation of $path")
                }
            }
            StandardWatchEventKinds.ENTRY_DELETE -> {
                return if (path.hasSupportedExtension()) {
                    logger.info("File deleted $path")
                } else {
                    logger.warn("Ignoring deletion of $path")
                }
            }
            else -> logger.warn("Ignored event $e")
        }
    }

    override fun destroy() {
        logger.info("Closing SoundService CoroutineScope")
        scope.cancel()
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SoundService::class.java)
    }
}
