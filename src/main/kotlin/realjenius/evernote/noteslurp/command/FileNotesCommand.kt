package realjenius.evernote.noteslurp.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import mu.KLogging
import realjenius.evernote.noteslurp.command.Ansi.blue
import realjenius.evernote.noteslurp.command.Ansi.green
import realjenius.evernote.noteslurp.command.Ansi.red
import realjenius.evernote.noteslurp.evernote.*
import java.awt.Color.red
import java.lang.RuntimeException

class FileNotesCommand : CliktCommand(name = "file-notes", help = "File notes, potentially adjusting tags as you go") {
  val source by option(help = "Source notebook name - defaults to default notebook if not set")
  val dest by option(help = "Destination notebook name").required()
  val preview by option(help = "Preview note contents").flag(default = false)
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
    adjuster.walkNotes(preview) {
      val change = NoteChanges()
      while(true) {
        val summary = "Note: '${blue(it.title)}' (created at: ${it.date.toLocalDate()} ${it.date.toLocalTime()}) with Tags: '${blue("${it.tags}")}'"
        val promptText = if (preview) {
          val content = it.content?.let { c -> c.substring(0 until c.length.coerceAtMost(1000)) } ?: "<No Content>"
          TermUi.echo(summary, true)
          TermUi.echo(content, true)
          TermUi.echo("-------------", true)
          ""
        } else "$summary - "

        when (val input = TermUi.prompt("${promptText}${green("Action")}: (M - Move/S - Skip/D - Delete/C - Change/T - Title/Q - Quit)")?.toUpperCase()) {
          "Q" -> throw Quit()
          "M" -> { change.move = true; break }
          "S" -> { change.move = false; break }
          "D" -> { change.delete = true; break }
          "G" -> {
            if (it.content != null)
              change.tagsChanged = adjuster.updateTags(it, listOf(it.content), emptyList(), false) || change.tagsChanged
          }
          "C" -> {
            change.tagsChanged = parseTagChanges(it, TermUi.prompt(green("Tag Changes")) ?: "", adjuster) || change.tagsChanged
          }
          "T" -> {
            val title = this.currentContext.console.promptForLine("New Title:", false) ?: ""
            if(title.isBlank()) {
              error(input)
            } else {
              change.titleChanged = true
              it.title = title
              change.tagsChanged = adjuster.updateTags(it, listOf(it.title), emptyList(), false) || change.tagsChanged
            }
          }
          else -> {
            if (input?.startsWith("CM ") == true) {
              change.tagsChanged = parseTagChanges(it, input.substring(2), adjuster) || change.tagsChanged
              change.move = true
              break
            } else error(input ?: "")
          }
        }
      }
      change
    }
  }

  private fun error(input: String) {
    TermUi.echo(red("Unrecognized Input: $input. Try again."), true)
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