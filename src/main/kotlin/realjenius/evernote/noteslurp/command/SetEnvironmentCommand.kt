package realjenius.evernote.noteslurp.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import realjenius.evernote.noteslurp.EnvConfig
import realjenius.evernote.noteslurp.evernote.Evernote
import realjenius.evernote.noteslurp.io.info

class SetEnvironmentCommand : CliktCommand(name = "set-env", help = "Initialize Evernote authentication for an environment") {

  val environment by option(help = "The name of the environment to connect").choice(*Evernote.serviceKeys()).prompt(
    text = "Please enter the environment (${Evernote.serviceKeys().joinToString(", ")})"
  )
  val devToken by option(help = "Provide the developer token for connecting").prompt(
    hideInput = true,
    text = "Please enter your developer token for the given environment"
  )

  override fun run() {
    context.loadConfig().withEnvConfig(environment, EnvConfig(devToken)).save(context.configDir())
    info("\nConfiguration created successfully for environment $environment.")
  }
}