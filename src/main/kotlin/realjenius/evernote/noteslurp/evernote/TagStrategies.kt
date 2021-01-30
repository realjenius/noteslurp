package realjenius.evernote.noteslurp.evernote

import realjenius.evernote.noteslurp.KeywordTag
import realjenius.evernote.noteslurp.KeywordType
import realjenius.evernote.noteslurp.TagConfig
import java.nio.file.Path
import java.nio.file.Paths

data class TagContext(val rootDir: Path = Path.of("/"), val filePath: Path = Path.of("/"), val textContent: String = "")

interface TagStrategy {
  fun findTags(ctx: TagContext): Set<String>

  companion object {
    fun forConfig(config: TagConfig): List<TagStrategy> {
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

  override fun findTags(ctx: TagContext): Set<String> = matchers
    .map { it.findTag(ctx.textContent) }
    .filter { it != null }
    .filterNotNull().toSet()
}

private interface KeywordMatcher {
  fun findTag(input: String): String?
}

private class RegexMatcher(search: String, private val target: String) : KeywordMatcher {
  private val regex: Regex = Regex(search)

  override fun findTag(input: String): String? {
    val matcher = regex.toPattern().matcher(input)
    val groups = matcher.groupCount()
    return if (matcher.find()) {
      if (groups == 0) target
      else (0 until groups).fold(target) { acc, it ->
        acc.replace(oldValue = "{$it}", newValue = matcher.group(it + 1))
      }
    } else null
  }
}

private class TextMatcher(private val search: String, private val target: String) : KeywordMatcher {
  override fun findTag(input: String) : String? {
    val at = input.indexOf(string = search, ignoreCase = true)
    return if (at >= 0) {
      val startsClean = at == 0 || isAllowedBoundary(input[at - 1])
      val endsClean = at + search.length == input.length || isAllowedBoundary(input[at + search.length + 1])
      if (startsClean && endsClean) target else null
    } else null
  }

  private fun isAllowedBoundary(char: Char) = !BOUNDARY_REGEX.matches("$char")

  companion object {
    private val BOUNDARY_REGEX = Regex("[a-zA-Z0-9]")
  }
}

class FolderStrategy(private val keywords: KeywordStrategy?) : TagStrategy {
  override fun findTags(ctx: TagContext) =
    findTagNext(ctx.rootDir, ctx.filePath).let {
      if (keywords != null) it.flatMap { tag -> keywords.findTags(ctx.copy(textContent = tag)) }
      else it
    }.toSet()

  private fun findTagNext(rootDir: Path, path: Path): MutableList<String> =
    if (path.parent == rootDir || path.parent == null) arrayListOf()
    else findTagNext(rootDir, path.parent).apply { add(path.parent.fileName.toString()) }
}
