package realjenius.evernote.noteslurp.command

import com.github.ajalt.clikt.core.CliktCommand
import realjenius.evernote.noteslurp.Config
import realjenius.evernote.noteslurp.io.info

class ResetCommand : CliktCommand(name = "reset", help = "Remove all configuration settings") {
  override fun run() {
    Config.delete(currentContext.configDir())
    info("Configuration deleted.")
  }
}