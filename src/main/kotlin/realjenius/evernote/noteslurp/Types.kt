package realjenius.evernote.noteslurp

import com.github.ajalt.clikt.core.CliktError
import realjenius.evernote.noteslurp.io.*
import java.nio.file.Path
import java.time.Instant

class LoadedFile(val name: String, val mime: String, val path: Path, val data: ByteArray)

data class SavedNote(val guid: String, val path: Path) {
  fun toNoteLogEntry() = NoteLogEntry(guid, path.fileName.toString())
}

data class NoteLogEntry(val guid: String, val name: String)

data class NoteLog(val timestamp: String, val records: List<NoteLogEntry>)

data class Config(val currentEnvironment: String?, val environments: Map<String, EnvConfig>, val tags: TagConfig) {
  val version = 1

  fun tokenFor(env: String) = environments[env]?.token ?: throw CliktError(
    "Unable to find configuration for environment `$currentEnvironment`. " +
        "Please reinitialize your configuration"
  )

  fun hasEnvironment(env: String) = environments.containsKey(env)

  fun withEnvConfig(env: String, config: EnvConfig) =
    copy(currentEnvironment = env, environments = this.environments.plus(env to config))

  fun withTags(tags: TagConfig) = copy(tags = tags)

  fun save(home: String) = writeFile(configPath(home), toJson(this).toByteArray(Charsets.UTF_8))

  companion object {

    fun initial() = Config(null, emptyMap(), TagConfig(false, emptyList()))

    fun load(home: String): Config = readFile(configPath(home))?.let {
      fromJson<Config>(String(it, Charsets.UTF_8))
    } ?: Config.initial()

    fun delete(home: String) = deleteFile(configPath(home)).block()

    fun defaultDir() = "${System.getProperty("user.home")}/.noteslurp"

    private fun configPath(home: String) = loadPath(home).resolve("config.json")
  }
}

data class EnvConfig(val token: String, val created: Instant = Instant.now())

data class TagConfig(val folderTags: Boolean, val keywords: List<KeywordTag>) {
  val version = 1
  fun withFolderSetting(folderTags: Boolean) = copy(folderTags = folderTags)

  fun withKeyword(tag: KeywordTag) = copy(keywords = keywords
    .filterNot { it.matching(tag) }
    .plus(tag))

  fun plusKeywords(keys: List<KeywordTag>) = withKeywords(this.keywords.filterNot { existing ->
    keys.any { it.matching(existing) }
  } + keys)

  fun withKeywords(keys: List<KeywordTag>) = copy(keywords = keys)
}

enum class KeywordType { Text, Regex }

data class KeywordTag(val mapping: String, val target: String, val type: KeywordType) {
  fun matching(other: KeywordTag) = mapping == other.mapping && type == other.type
}