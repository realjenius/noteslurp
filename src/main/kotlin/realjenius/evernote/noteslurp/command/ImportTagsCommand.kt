package realjenius.evernote.noteslurp.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import realjenius.evernote.noteslurp.TagConfig
import realjenius.evernote.noteslurp.io.fromJson
import realjenius.evernote.noteslurp.io.info
import realjenius.evernote.noteslurp.io.loadPath
import realjenius.evernote.noteslurp.io.readFile

class ImportTagsCommand : CliktCommand(name = "import-tags", help = "Import a tags export file") {
  val from by option(help = "The file to import (JSON, UTF-8 format)").required()
  val replace by option(help = "If set to true, the import will replace the current tag set").flag(default = false)

  override fun run() {
    val config = context.loadConfig()
    var tags: TagConfig = fromJson(
      String(
        readFile(loadPath(from)) ?: throw CliktError("Unable to find $from to import"), Charsets.UTF_8
      )
    )

    val tagCount = tags.keywords.size
    if (!replace) tags = config.tags.withFolderSetting(tags.folderTags).plusKeywords(tags.keywords)
    config.withTags(tags).save(context.configDir())
    info("\n$tagCount keywords imported, ${if (replace) "replacing existing tags" else "added to existing tags"}.\n----\n\n")
    listTags()

  }
}