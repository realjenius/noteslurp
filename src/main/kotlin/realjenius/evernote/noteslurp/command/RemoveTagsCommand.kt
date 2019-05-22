package realjenius.evernote.noteslurp.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import realjenius.evernote.noteslurp.io.error
import realjenius.evernote.noteslurp.io.info

class RemoveTagsCommand : CliktCommand(name = "remove-tags", help = "Remove one to many tag configurations") {
  val tagIndex by option(help = "An index for the tag to remove from the `list-tags` command").int().multiple()

  override fun run() {
    val config = context.loadConfig()
    val removalList = tagIndex.filter {
      if (it >= config.tags.keywords.size || it < 0) {
        error("Index $tagIndex is invalid. Skipping.\n")
        false
      } else true
    }.filter {
      val toRemove = context.console.promptForLine(
        "Are you sure you wish to remove '${config.tags.keywords[it].mapping}' [Y/N]: ",
        false
      )
      toRemove.equals("Y", true) ||
          toRemove.equals("TRUE", true) ||
          toRemove.equals("T", true)
    }

    val newList = config.tags.keywords.filterIndexed { idx, _ -> !removalList.contains(idx) }
    config.withTags(config.tags.withKeywords(newList)).save(context.configDir())
    info("Removed ${removalList.size} from configuration\n----\n\n")
    listTags()
  }
}