package realjenius.evernote.noteslurp.evernote

import realjenius.evernote.noteslurp.KeywordTag
import realjenius.evernote.noteslurp.KeywordType
import realjenius.evernote.noteslurp.LoadedFile
import realjenius.evernote.noteslurp.TagConfig
import java.nio.file.Path
import java.nio.file.Paths

interface TagStrategy {
    fun findTags(rootDir: Path, filePath: Path) : Set<String>

    companion object {
        fun forConfig(config: TagConfig) : List<TagStrategy> {
            val keywords = if (config.keywords.isNotEmpty()) KeywordStrategy(config.keywords) else null
            val list = arrayListOf<TagStrategy>()
            if (config.folderTags) list.add(FolderStrategy(keywords))
            if (keywords != null) list.add(keywords)
            return list
        }
    }
}

class KeywordStrategy(keywords: List<KeywordTag>) : TagStrategy {
    private val matchers: List<KeywordMatcher> = keywords.map {
        if (it.type == KeywordType.Regex) RegexMatcher(it.mapping, it.target)
        else TextMatcher(it.mapping, it.target)
    }
    override fun findTags(rootDir: Path, filePath: Path) : Set<String> = matchers
        .map { it.findTag(filePath.fileName.toString()) }
        .filter { it != null }
        .filterNotNull().toSet()
}

private interface KeywordMatcher {
    fun findTag(input: String) : String?
}
private class RegexMatcher(search: String, private val target: String) : KeywordMatcher {
    private val regex: Regex = Regex(search)

    override fun findTag(input: String): String? {
        val matcher = regex.toPattern().matcher(input)
        val groups = matcher.groupCount()
        return if (matcher.find()) {
            if (groups == 0) target
            else (0 until groups).fold(target) { acc, it ->
                acc.replace(oldValue = "{$it}", newValue = matcher.group(it+1))
            }
        } else null
    }
}
private class TextMatcher(private val search: String, private val target: String) : KeywordMatcher {
    override fun findTag(input: String) = if(input.contains(search, true)) target else null
}

class FolderStrategy(private val keywords: KeywordStrategy?) : TagStrategy {
    override fun findTags(rootDir: Path, filePath: Path) =
        findTagNext(rootDir, filePath).let {
            if (keywords != null) it.flatMap { tag -> keywords.findTags(Paths.get("/"), Paths.get("/$tag")) }
            else it
        }.toSet()

    private fun findTagNext(rootDir: Path, path: Path) : MutableList<String> =
        if (path.parent == rootDir || path.parent == null) arrayListOf()
        else findTagNext(rootDir, path.parent).apply { add(path.parent.fileName.toString()) }
}
