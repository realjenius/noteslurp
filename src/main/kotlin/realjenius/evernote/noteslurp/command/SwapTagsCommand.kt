package realjenius.evernote.noteslurp.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import mu.KLogging
import realjenius.evernote.noteslurp.evernote.Evernote
import realjenius.evernote.noteslurp.evernote.EvernoteTagManager

class SwapTagsCommand : CliktCommand(name = "swap-tags", help = "Swap tags on notes in batch") {
  val sourceTag by option(help = "The source tag name").required()
  val targetTag by option(help = "The target tag name").required()
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
      tags.swapTag(sourceTag, targetTag)
    } catch (ex: Quit) {
      return
    }
  }

  companion object : KLogging()
}