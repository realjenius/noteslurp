package realjenius.evernote.noteslurp.evernote

import com.evernote.edam.notestore.NoteFilter
import com.github.ajalt.clikt.core.CliktError
import mu.KLogging
import realjenius.evernote.noteslurp.io.info
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class EvernoteNoteAdjuster(service: String,
                           token: String,
                           private val from: String?,
                           private val to: String,
                           private val tags: List<TagStrategy>) : Evernote(service, token) {

  fun walkNotes(callback: (NoteDetails) -> NoteChanges) {
    logger.info { "Default Notebook: ${noteStore.defaultNotebook.name}" }

    val sourceNotebook = from
      ?.let { findNotebook(it) ?: throw CliktError("Source notebook '$from' could not be found'")} ?:
        noteStore.defaultNotebook

    val targetNotebook = findNotebook(to) ?: throw CliktError("Target notebook '$to' could not be found.")

    val filter = NoteFilter()
    filter.notebookGuid = sourceNotebook.guid
    val count = noteStore.findNoteCounts(filter, false).notebookCounts[sourceNotebook.guid]

    logger.info { "Filing $count notes from: \n\t'${sourceNotebook.name}' (${sourceNotebook.guid}) into: \n\t'${targetNotebook.name}' (${targetNotebook.guid})" }

    var moreNotes = true
    var offset = 0
    while (moreNotes) {
      val notes = noteStore.findNotes(filter, offset, 10)
      if (notes.notes.size < 10) moreNotes = false
      offset += notes.notes.size

      notes.notes.forEach {
        // TODO make zone pluggable.
        val details = NoteDetails(it.title, it.guid, ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.created), ZoneId.of("America/Chicago")), noteStore.getNoteTagNames(it.guid))
        val changes = callback(details)

        if (changes.delete) {
          noteStore.deleteNote(it.guid)
        } else {
          if (changes.addTags.isNotEmpty() || changes.removeTags.isNotEmpty()) {
            it.tagNames = details.tags.plus(mapTags(changes.addTags)).minus(mapTags(changes.removeTags))
          }
          if (changes.move) {
            it.notebookGuid = targetNotebook.guid
          } else {
            it.notebookGuid = sourceNotebook.guid
          }
          noteStore.updateNote(it)
        }
      }
    }
  }

  private fun findNotebook(name: String) = noteStore.listNotebooks().firstOrNull { it.name.equals(name, true) }

  private fun mapTags(tagList: Iterable<String>) = tagList.flatMap { tag ->
    tags.flatMap { it.findTags(Paths.get("/"), Paths.get("/$tag")) }
  }.toList()
  companion object : KLogging()
}

data class NoteDetails(val title: String, val guid: String, val date: ZonedDateTime, val tags: List<String>)

data class NoteChanges(val move: Boolean = true, val delete: Boolean = false, val addTags: List<String> = emptyList(), val removeTags: List<String> = emptyList())
