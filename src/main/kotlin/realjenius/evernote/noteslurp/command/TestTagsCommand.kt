package realjenius.evernote.noteslurp.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import realjenius.evernote.noteslurp.evernote.TagStrategy
import realjenius.evernote.noteslurp.io.info
import java.nio.file.Paths

class TestTagsCommand : CliktCommand(
  name = "test-tags",
  help = "Prints the tags that are computed off the given name for the current configuration"
) {
  val tag by option(help = "The tag name to test").multiple()
  override fun run() {
    val strategies = TagStrategy.forConfig(currentContext.loadConfig().tags)
    tag.forEach { tag ->
      info("$tag --> ${strategies.flatMap { it.findTags(Paths.get("/"), Paths.get("/$tag")) }}\n")
    }
  }
}