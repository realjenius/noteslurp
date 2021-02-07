package realjenius.evernote.noteslurp.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import mu.KLogging
import reactor.core.publisher.Mono
import realjenius.evernote.noteslurp.NoteLog
import realjenius.evernote.noteslurp.evernote.Evernote
import realjenius.evernote.noteslurp.evernote.EvernoteNoteCreator
import realjenius.evernote.noteslurp.evernote.TagStrategy
import realjenius.evernote.noteslurp.io.*
import realjenius.evernote.noteslurp.reactor.debug
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class RunCommand : CliktCommand(name = "run", help = "Slurp notes into Evernote") {
  val keepSource by option(help = "Keep the files in the source directory when complete").flag(default = false)
  val from by option(help = "The directory to synchronize with Evernote").required()
  val to by option(help = "The directory to place created attachments").defaultLazy {
    loadPath(from).parent.resolve("completed-sync").toString()
  }
  val environment by option(help = "The environment to target (defaults to targeted environment in configuration if not set)")
    .choice(*Evernote.serviceKeys())
  val dryRun by option(help = "Do not actually upload to evernote. This bypasses the note save and fakes a note GUID. Useful for debugging a configuration").flag(
    default = false
  )

  override fun run() {
    val start = System.nanoTime()
    val destination = "$to/${System.currentTimeMillis()}"
    val config = currentContext.loadConfig()
    val env = environment ?: config.currentEnvironment
    ?: throw CliktError("Either an environment must be provided, or a current environment must be set in the configuration")
    if (!config.hasEnvironment(env)) throw CliktError("The environment `$env` is not set. Run `noteslurp set-env` to configure this environment.")

    logger.debug { "Verifying Evernote Connection to '$env'" }

    info("Processing files in `$from` and transmitting to $env\n\n")

    val evernote = EvernoteNoteCreator(env, config.tokenFor(currentContext.configDir(), env), loadPath(from), dryRun, TagStrategy.forConfig(config.tags))
    val result = loadFiles(from)
      .flatMap(evernote::createNote)
      .delayUntil {
        val targetPath = loadPath(destination)
        val target = if (it.path.parent != targetPath) targetPath.resolve(loadPath(from).relativize(it.path.parent))
        else targetPath
        copyFile(it.path, target)
      }
      .delayUntil { if (!keepSource) deleteFile(it.path) else Mono.empty<Unit>() }
      .debug(logger) { "Completed File: $it" }
      .map { it.toNoteLogEntry() }
      .collectList()
      .debug(logger) { "Records: ${it.size}" }
      .map { NoteLog(ZonedDateTime.now(ZoneOffset.UTC).toString(), it) }
      .delayUntil {
        if (it.records.isNotEmpty()) {
          writeToFile(
            toJson(it),
            "process-log.json",
            destination
          )
        } else Mono.empty<Unit>()
      }
      .block()!!

    val end = System.nanoTime()
    val elapsedInSeconds = TimeUnit.NANOSECONDS.toMillis(end - start).toFloat() / 1000
    if (result.records.isEmpty()) {
      info("No records found to upload.\n")
    } else {
      info("Uploaded ${result.records.size} file(s) to Evernote in $elapsedInSeconds seconds.\n")
      logger.debug { "Full Log: $result" }
    }

  }

  companion object : KLogging()
}