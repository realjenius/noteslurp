package realjenius.evernote.noteslurp.io

import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import realjenius.evernote.noteslurp.LoadedFile
import realjenius.evernote.noteslurp.reactor.debug
import realjenius.evernote.noteslurp.reactor.elasticMono
import realjenius.evernote.noteslurp.reactor.schedulerMap
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

private val logger = KotlinLogging.logger { }

fun loadFiles(directory: String): Flux<LoadedFile> {
  val dirPath = loadPath(directory)
  assert(Files.isDirectory(dirPath))
  return Flux
    .fromStream { streamDir(dirPath) }
    .debug(logger) { "Path Discovered: $it" }
    .filter { Files.probeContentType(it) != null }
    .schedulerMap(Schedulers.elastic()) {
      LoadedFile(
        it.fileName.toString(),
        Files.probeContentType(it),
        it,
        Files.readAllBytes(it)
      )
    }
}

private fun streamDir(path: Path): Stream<Path> =
  Files.list(path).flatMap {
    when {
      Files.isDirectory(it) -> streamDir(it)
      Files.isRegularFile(it) -> Stream.of(it)
      else -> Stream.empty()
    }
  }

fun readFile(file: Path) = if (Files.exists(file)) Files.readAllBytes(file) else null

fun writeFile(file: Path, contents: ByteArray) {
  Files.createDirectories(file.parent)
  if (!Files.exists(file)) Files.createFile(file)
  Files.write(file, contents)
}

fun copyFile(file: Path, target: Path) = elasticMono {
  Files.createDirectories(target)
  Files.copy(file, target.resolve(file.fileName))
}

fun deleteFile(file: Path) = elasticMono { Files.delete(file) }

fun writeToFile(contents: String, name: String, directory: String) = elasticMono {
  val targetDir = loadPath(directory)
  val filePath = targetDir.resolve(name)
  Files.createDirectories(targetDir)
  Files.createFile(filePath)
  Files.newOutputStream(filePath).bufferedWriter(Charsets.UTF_8).use {
    it.write(contents)
  }
}

fun loadPath(path: String) = FileSystems.getDefault().getPath(path)