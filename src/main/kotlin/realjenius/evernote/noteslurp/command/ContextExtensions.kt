package realjenius.evernote.noteslurp.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import realjenius.evernote.noteslurp.Config
import realjenius.evernote.noteslurp.KeywordType
import realjenius.evernote.noteslurp.io.info

fun Context.loadConfig() = Config.load(this.configDir())

fun Context.configDir(): String = when {
  command is MainCommand -> (command as MainCommand).configDir
  parent != null -> parent!!.configDir()
  else -> throw CliktError("Unable to find configDir.")
}

fun CliktCommand.listTags() {
  val config = currentContext.loadConfig()
  info("Tag List:\n")
  info("-----\n")
  info("Folder Tag Mapping: ${config.tags.folderTags}\n")
  if (config.tags.keywords.isEmpty()) {
    info("Keywords: None\n")
  } else {
    info("Keywords: \n")
    config.tags.keywords.forEachIndexed { idx, it ->
      val idxStr = String.format("%02d", idx)
      info("\t$idxStr: ${it.mapping} = ${it.target}  ${if (it.type == KeywordType.Regex) "(Regex)" else ""}\n")
    }
  }
}