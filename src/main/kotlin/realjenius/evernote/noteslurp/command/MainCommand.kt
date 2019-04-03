package realjenius.evernote.noteslurp.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import realjenius.evernote.noteslurp.Config
import realjenius.evernote.noteslurp.io.toggleDebug

class MainCommand : CliktCommand(name = "noteslurp") {
  val debug by option(help = "Enable debug logging").flag(default = false)
  val configDir by option(help = "The configuration directory (defaults to <userhome>/.noteslurp)").default(Config.defaultDir())
  override fun run() {
    toggleDebug(debug)
  }
}