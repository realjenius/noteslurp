package realjenius.evernote.noteslurp.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.pair
import realjenius.evernote.noteslurp.KeywordTag
import realjenius.evernote.noteslurp.KeywordType
import realjenius.evernote.noteslurp.io.info

class AddTagsCommand : CliktCommand(name = "add-tags", help = "Add auto-tagging configurations") {
    val keyword by option(help = "A keyword mapping to a tag").pair().multiple()
    val regexKeyword by option(help = "A keyword mapping using regular expressions (and grouping) to a tag with replacements").pair().multiple()
    val folder by option(help = "Enable sub-folder name to tag mapping").flag(default = false)

    override fun run() {
        context.loadConfig().let {
            it.withTags(
                (regexKeyword.map { kw -> KeywordTag(kw.first, kw.second, KeywordType.Regex) } +
                keyword.map { kw -> KeywordTag(kw.first, kw.second, KeywordType.Text) })
                .fold(it.tags) { acc, keyword -> acc.withKeyword(keyword) }
                    .withFolderSetting(folder)
            )
        }.save(context.configDir())

        info("\nTags added to configuration\n----\n\n")
        listTags()
    }
}