package realjenius.evernote.noteslurp.evernote

import com.evernote.edam.notestore.NoteFilter
import com.github.ajalt.clikt.core.CliktError
import mu.KLogging
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
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
        val details = NoteDetails(it.title, it.guid, ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.created), ZoneId.of("America/Chicago")), noteStore.getNoteTagNames(it.guid).toSet())
        val changes = callback(details)

        if (changes.delete) noteStore.deleteNote(it.guid)
        else if (changes.isChanged()) {
          it.notebookGuid = if (changes.move) {
            offset--
            targetNotebook.guid
          } else sourceNotebook.guid
          if (changes.titleChanged) it.title = details.title
          if (changes.tagsChanged) it.tagNames = details.tags.toList()
          noteStore.updateNote(it)
        }
      }
    }
  }

  fun updateTags(note: NoteDetails, addTags: List<String> = emptyList(), removeTags: List<String> = emptyList(), keepIfUnmapped: Boolean) : Boolean {
    return if (addTags.isNotEmpty() || removeTags.isNotEmpty()) {
      val oldTags = note.tags
      note.tags = note.tags.plus(mapTags(addTags, keepIfUnmapped)).minus(mapTags(removeTags, keepIfUnmapped))
      oldTags != note.tags
    } else false
  }

  private fun findNotebook(name: String) = noteStore.listNotebooks().firstOrNull { it.name.equals(name, true) }

  private fun mapTags(tagList: Iterable<String>, keepIfUnmapped: Boolean) = tagList.flatMap { tag ->
    tags.flatMap { it.findTags(Paths.get("/"), Paths.get("/$tag")) }
      .ifEmpty { if(keepIfUnmapped) listOf(tag) else emptyList() }
  }.toList()

  companion object : KLogging()
}

data class NoteDetails(var title: String, val guid: String, val date: ZonedDateTime, var tags: Set<String>)

data class NoteChanges(var move: Boolean = false, var delete: Boolean = false, var titleChanged: Boolean = false, var tagsChanged: Boolean = false) {
  fun isChanged() = move || titleChanged || tagsChanged
}
