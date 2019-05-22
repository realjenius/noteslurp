package realjenius.evernote.noteslurp.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import realjenius.evernote.noteslurp.io.info
import realjenius.evernote.noteslurp.io.loadPath
import realjenius.evernote.noteslurp.io.toJson
import realjenius.evernote.noteslurp.io.writeFile

class ExportTagsCommand : CliktCommand(name = "export-tags", help = "Export the current tag definition") {
  val to by option(help = "The file location to export the tags (JSON format)").required()
  override fun run() {
    writeFile(loadPath(to), toJson(context.loadConfig().tags).toByteArray(Charsets.UTF_8))
    info("\nTag configuration exported to $to\n")
  }
}