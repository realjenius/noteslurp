@file:JvmName("NoteSlurp")

package realjenius.evernote.noteslurp

import com.github.ajalt.clikt.core.subcommands
import mu.KotlinLogging
import realjenius.evernote.noteslurp.command.*
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger("main")

fun main(args: Array<String>) {
  try {
    MainCommand().subcommands(
      AddTagsCommand(),
      ExportTagsCommand(),
      FileNotesCommand(),
      ImportTagsCommand(),
      ListTagsCommand(),
      RemoveTagsCommand(),
      ResetCommand(),
      RunCommand(),
      SetEnvironmentCommand(),
      TestTagsCommand(),
      SwapTagsCommand(),
      DeleteTagCommand(),
      ReparentTagCommand()
    ).main(args)
  } catch (ex: Throwable) {
    System.err.println("Fatal: An exception has occurred: ${ex::class.simpleName}: ${ex.message}\n\tRun with '--debug' to learn more.")
    logger.debug(ex) { "Failure executing NoteSlurp - Exception follows" }
    exitProcess(1)
  }

}
