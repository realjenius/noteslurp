package realjenius.evernote.noteslurp.command

import com.github.ajalt.clikt.core.CliktCommand

class ListTagsCommand : CliktCommand(name = "list-tags", help = "List the current tag configurations") {
    override fun run() {
        listTags()
    }
}
