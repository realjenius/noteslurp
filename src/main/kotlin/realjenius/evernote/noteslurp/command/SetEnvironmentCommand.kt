package realjenius.evernote.noteslurp.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.choice
import realjenius.evernote.noteslurp.Config
import realjenius.evernote.noteslurp.EnvConfig
import realjenius.evernote.noteslurp.TokenType
import realjenius.evernote.noteslurp.evernote.Evernote
import realjenius.evernote.noteslurp.io.info
import java.nio.file.Path
import kotlin.io.path.exists

class SetEnvironmentCommand :
  CliktCommand(name = "set-env", help = "Initialize Evernote authentication for an environment") {

  val environment by option(help = "The name of the environment to connect").choice(*Evernote.serviceKeys()).prompt(
    text = "Please enter the environment (${Evernote.serviceKeys().joinToString(", ")})"
  )
  val devToken by option(help = "Provide the developer token for connecting").prompt(
    hideInput = true,
    text = "Please enter your developer token for the given environment"
  )
  val tokenFile by option(help = "Provide the name of the file to store the token").prompt(
    text = "Please enter the path to the file for storing the token (relative to config dir). Leave blank for storing in config file"
  )

  override fun run() {
    val cfg = currentContext.loadConfig()
    val tokenType: TokenType
    val token : String
    if (tokenFile.isNotBlank()) {
      val tokenFilePath = Path.of(currentContext.configDir(), tokenFile)
      EnvConfig.storeToken(tokenFilePath, devToken)
      tokenType = TokenType.Isolated
      token = tokenFile
    } else {
      tokenType = TokenType.Plain
      token = devToken
    }

    cfg.withEnvConfig(environment, EnvConfig(token, tokenType)).save(currentContext.configDir())
    info("\nConfiguration created successfully for environment $environment.")
  }
}