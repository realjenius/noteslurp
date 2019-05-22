package realjenius.evernote.noteslurp.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import mu.KLogging
import realjenius.evernote.noteslurp.evernote.Evernote
import realjenius.evernote.noteslurp.evernote.EvernoteNoteAdjuster
import realjenius.evernote.noteslurp.evernote.NoteChanges
import realjenius.evernote.noteslurp.evernote.TagStrategy
import java.lang.RuntimeException

class FileNotesCommand : CliktCommand(name = "file-notes", help = "File notes, potentially adjusting tags as you go") {
  val source by option(help = "Source notebook name - defaults to default notebook if not set")
  val dest by option(help = "Destination notebook name").required()
  val environment by option(help = "The environment to target (defaults to targeted environment in configuration if not set)")
    .choice(*Evernote.serviceKeys())

  override fun run() {
    val config = context.loadConfig()
    val env = environment ?: config.currentEnvironment
    ?: throw CliktError("Either an environment must be provided, or a current environment must be set in the configuration")
    if (!config.hasEnvironment(env)) throw CliktError("The environment `$env` is not set. Run `noteslurp set-env` to configure this environment.")

    logger.debug { "Verifying Evernote Connection to '$env'" }

    val adjuster = EvernoteNoteAdjuster(env, config.tokenFor(env), source, dest, TagStrategy.forConfig(config.tags))
    adjuster.walkNotes {
      var invalidInput = true
      lateinit var result: NoteChanges
      while(invalidInput) {
        try {
          invalidInput = false
          val input = this.context.console
            .promptForLine("Note: '${it.title}' (created at: ${it.date.toLocalDate()} ${it.date.toLocalTime()}) with Tags: '${it.tags} - Action: (M/C/S/D):", false)?.toUpperCase()
          result = when (input) {
            "M" -> NoteChanges(true)
            "S" -> NoteChanges(false)
            "D" -> NoteChanges(move = false, delete = true)
            "C" -> {
              val tagChanges = this.context.console.promptForLine("Tag Changes:", false)
              val tagChangeParts = parseTagChanges(tagChanges!!)
              val move = this.context.console.promptForLine("Move: (Y/N):", false)?.toUpperCase()
              NoteChanges(move == "Y", false, tagChangeParts.first, tagChangeParts.second)
            }
            else -> {
              context.console.print("Unrecognized Input: $input. Try again.", true)
              throw InvalidInput()
            }
          }
        } catch (e: InvalidInput) {
          invalidInput = true
        }
      }
      result
    }

  }

  private fun parseTagChanges(input: String) : Pair<List<String>, List<String>> {
    val tagChanges = input.split(' ')
    val additions = tagChanges.filter { it.startsWith('+') }.map { it.substring(1) }.map { it.replace('+', ' ') }
    val subtractions = tagChanges.filter { it.startsWith('-') }.map { it.substring(1) }.map { it.replace('+', ' ') }
    return additions to subtractions
  }

  companion object : KLogging()
}

class InvalidInput : RuntimeException()