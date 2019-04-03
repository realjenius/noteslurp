@file:JvmName("NoteSlurp")
package realjenius.evernote.noteslurp

import com.github.ajalt.clikt.core.subcommands
import mu.KotlinLogging
import realjenius.evernote.noteslurp.command.*

private val logger = KotlinLogging.logger("main")

fun main(args: Array<String>) {
  try {
    MainCommand().subcommands(
      AddTagsCommand(),
      ExportTagsCommand(),
      ImportTagsCommand(),
      ListTagsCommand(),
      RemoveTagsCommand(),
      ResetCommand(),
      RunCommand(),
      SetEnvironmentCommand(),
      TestTagsCommand()
    ).main(args)
  } catch (ex: Throwable) {
    System.err.println("Fatal: An exception has occurred: ${ex::class.simpleName}: ${ex.message}\n\tRun with '--debug' to learn more.")
    logger.debug(ex) { "Failure executing NoteSlurp - Exception follows" }
    System.exit(1)
  }

}
