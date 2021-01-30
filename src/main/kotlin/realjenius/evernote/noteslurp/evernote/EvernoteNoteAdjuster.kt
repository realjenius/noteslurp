package realjenius.evernote.noteslurp.evernote

import com.evernote.edam.notestore.NoteFilter
import com.evernote.edam.type.Note
import com.github.ajalt.clikt.core.CliktError
import mu.KLogging
import realjenius.evernote.noteslurp.evernote.Evernote.findNotebook
import realjenius.evernote.noteslurp.evernote.Evernote.tags
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class EvernoteNoteAdjuster(service: String,
                           token: String,
                           private val from: String?,
                           to: String,
                           private val tags: List<TagStrategy>) {

  private val noteStore = Evernote.connect(service, token)

  private val sourceNotebook = from
    ?.let { noteStore.findNotebook(it) ?: throw CliktError("Source notebook '$from' could not be found'")} ?:
  noteStore.defaultNotebook

  private val targetNotebook = noteStore.findNotebook(to) ?: throw CliktError("Target notebook '$to' could not be found.")

  val filter = NoteFilter().apply {
    notebookGuid = sourceNotebook.guid
  }

  fun walkNotes(preview: Boolean = false, callback: (NoteDetails) -> NoteChanges) {
    logger.info { "Default Notebook: ${noteStore.defaultNotebook.name}" }

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
        val details = NoteDetails(it, if (preview) noteStore.getNoteContent(it.guid) else null, noteStore.tags(it))
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


  private fun mapTags(tagList: Iterable<String>, keepIfUnmapped: Boolean) = tagList.flatMap { tag ->
    tags.flatMap { it.findTags(TagContext(textContent = tag)) }
      .ifEmpty { if(keepIfUnmapped) listOf(tag) else emptyList() }
  }.toList()

  companion object : KLogging()
}

