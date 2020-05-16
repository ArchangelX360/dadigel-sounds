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
import java.nio.file.*
import kotlin.time.ExperimentalTime

@Service
@OptIn(ExperimentalCoroutinesApi::class)
class SoundService(@Autowired val soundsConfiguration: SoundsConfiguration) {
    private val scope = CoroutineScope(Job() + CoroutineName("sounds-watcher"))
    private var watchJob: Job? = null

    private lateinit var soundFolder: File
    private lateinit var fileWatcher: WatchService
    private lateinit var soundFolderKey: WatchKey

    private val mutableFilenames: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    private val filenames: StateFlow<Set<String>> = mutableFilenames

    init {
        try {
            soundFolder = File(soundsConfiguration.folder)
            fileWatcher = FileSystems.getDefault().newWatchService()
            soundFolderKey = soundFolder
                    .toPath()
                    .register(fileWatcher,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE)
            mutableFilenames.value = listSoundFiles(soundFolder)
            startIn(scope)
        } catch (e: NoSuchFileException) {
            logger.error("could not find folder ${soundsConfiguration.folder}")
        }
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

    private fun listSoundFiles(folder: File): Set<String> {
        return folder
                .listFiles { _, name -> name.hasSupportedExtension() }
                ?.map {
                    logger.info("found file ${it.name}")
                    it.name
                }
                ?.toSet() ?: emptySet()
    }

    @OptIn(ExperimentalTime::class)
    private final fun startIn(scope: CoroutineScope) {
        watchJob = scope.launch {
            withContext(Dispatchers.IO) {
                logger.info("Starting file watcher on ${soundsConfiguration.folder}")
                while (true) {
                    val watchKey = fileWatcher.take()
                    watchKey.pollEvents()
                            .filterPathEvents()
                            .forEach { mutableFilenames.value = mutableFilenames.value.updatedBy(it) }

                    if (!watchKey.reset()) {
                        watchKey.cancel()
                        fileWatcher.close()
                        break
                    }
                }

                soundFolderKey.cancel()
            }
        }
    }

    private fun String.hasSupportedExtension(): Boolean {
        return soundsConfiguration.supportedExtensions.any { this.endsWith(it) }
    }

    private fun List<WatchEvent<*>>.filterPathEvents(): List<WatchEvent<Path>> =
            filter {
                it.kind().type() == Path::class.java
            }.map {
                @Suppress("UNCHECKED_CAST")
                it as WatchEvent<Path>
            }

    private fun Set<String>.updatedBy(e: WatchEvent<Path>): Set<String> {
        when (e.kind().name()) {
            StandardWatchEventKinds.ENTRY_CREATE.name() -> {
                val filename = e.context().fileName.toString()
                return if (filename.hasSupportedExtension()) {
                    logger.info("file created $filename")
                    this.minus(filename)
                } else {
                    logger.warn("ignoring creation of $filename")
                    this
                }
            }
            StandardWatchEventKinds.ENTRY_DELETE.name() -> {
                val filename = e.context().fileName.toString()
                return if (filename.hasSupportedExtension()) {
                    logger.info("file deleted $filename")
                    this.minus(filename)
                } else {
                    logger.warn("ignoring deletion of $filename")
                    this
                }
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
