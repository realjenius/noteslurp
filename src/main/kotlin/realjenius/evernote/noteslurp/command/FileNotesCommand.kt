package realjenius.evernote.noteslurp.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import mu.KLogging
import realjenius.evernote.noteslurp.evernote.*
import java.lang.RuntimeException

class FileNotesCommand : CliktCommand(name = "file-notes", help = "File notes, potentially adjusting tags as you go") {
  val source by option(help = "Source notebook name - defaults to default notebook if not set")
  val dest by option(help = "Destination notebook name").required()
  val environment by option(help = "The environment to target (defaults to targeted environment in configuration if not set)")
    .choice(*Evernote.serviceKeys())

  override fun run() {
    val config = currentContext.loadConfig()
    val env = environment ?: config.currentEnvironment
    ?: throw CliktError("Either an environment must be provided, or a current environment must be set in the configuration")
    if (!config.hasEnvironment(env)) throw CliktError("The environment `$env` is not set. Run `noteslurp set-env` to configure this environment.")

    logger.debug { "Verifying Evernote Connection to '$env'" }

    val adjuster = EvernoteNoteAdjuster(env, config.tokenFor(env), source, dest, TagStrategy.forConfig(config.tags))
    try {
      walk(adjuster)
    } catch (ex: Quit) {
      return
    }

  }

  private fun walk(adjuster: EvernoteNoteAdjuster) {
    adjuster.walkNotes {
      var input: String? = null
      val change = NoteChanges()
      while(input == null || input !in "MSD") {
        input = this.currentContext.console
            .promptForLine("Note: '${it.title}' (created at: ${it.date.toLocalDate()} ${it.date.toLocalTime()}) with Tags: '${it.tags} - Action: (M/S/D/C/T/Q):", false)?.toUpperCase()

        when (input) {
          "Q" -> throw Quit()
          "M" -> change.move = true
          "S" -> change.move = false
          "D" -> change.delete = true
          "C" -> {
            val tagChanges = this.currentContext.console.promptForLine("Tag Changes:", false) ?: ""
            change.tagsChanged = parseTagChanges(it, tagChanges, adjuster) || change.tagsChanged
          }
          "T" -> {
            val title = this.currentContext.console.promptForLine("New Title:", false) ?: ""
            if(title.isBlank()) {
              currentContext.console.print("Unrecognized Input: $input. Try again.", true)
              input = null
            }
            change.titleChanged = true
            it.title = title
            change.tagsChanged = adjuster.updateTags(it, listOf(it.title), emptyList(), false) || change.tagsChanged
          }
          else -> {
            currentContext.console.print("Unrecognized Input: $input. Try again.", true)
          }
        }
      }
      change
    }
  }

  private fun parseTagChanges(note: NoteDetails, input: String, adjuster: EvernoteNoteAdjuster) : Boolean {
    val tagChanges = input.split(' ')
    val additions = findChanges(tagChanges, '+')
    val subtractions = findChanges(tagChanges, '-')
    return adjuster.updateTags(note, additions, subtractions, true)
  }

  private fun findChanges(tagChanges: List<String>, prefix: Char)
      = tagChanges.filter { it.startsWith(prefix) }.map { it.substring(1) }.map { it.replace('+', ' ') }

  companion object : KLogging()
}

class Quit : RuntimeException()