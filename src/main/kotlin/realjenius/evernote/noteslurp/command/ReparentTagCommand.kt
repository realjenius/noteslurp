package realjenius.evernote.noteslurp.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import mu.KLogging
import realjenius.evernote.noteslurp.evernote.Evernote
import realjenius.evernote.noteslurp.evernote.EvernoteTagManager

class ReparentTagCommand : CliktCommand(name = "reparent-tag", help = "Change parent tag of another tag in Evernote") {
  val tag by option(help = "The source tag name").required()
  val parent by option(help = "The parent tag name").required()
  val environment by option(help = "The environment to target (defaults to targeted environment in configuration if not set)")
    .choice(*Evernote.serviceKeys())

  override fun run() {
    val config = currentContext.loadConfig()
    val env = environment ?: config.currentEnvironment
    ?: throw CliktError("Either an environment must be provided, or a current environment must be set in the configuration")
    if (!config.hasEnvironment(env)) throw CliktError("The environment `$env` is not set. Run `noteslurp set-env` to configure this environment.")

    logger.debug { "Verifying Evernote Connection to '$env'" }

    val tags = EvernoteTagManager(env, config.tokenFor(currentContext.configDir(), env))
    try {
      tags.reparentTag(tag, parent)
    } catch (ex: Quit) {
      return
    }
  }

  companion object : KLogging()
}