package realjenius.evernote.noteslurp.evernote

import com.evernote.edam.notestore.NoteFilter
import com.github.ajalt.clikt.core.CliktError
import mu.KLogging
import realjenius.evernote.noteslurp.evernote.Evernote.allNotes
import realjenius.evernote.noteslurp.evernote.Evernote.findNotebook

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

    noteStore.allNotes(filter, preview) { note, details ->
      // TODO make zone pluggable.
      val changes = callback(details)
      if (changes.delete) noteStore.deleteNote(note.guid)
      else if (changes.isChanged()) {
        note.notebookGuid = if (changes.move) {
          targetNotebook.guid
        } else sourceNotebook.guid
        if (changes.titleChanged) note.title = details.title
        if (changes.tagsChanged) note.tagNames = details.tags.toList()
        noteStore.updateNote(note)
      }
      changes.move || changes.delete
    }
  }

  fun updateTags(note: NoteDetails, addTags: List<String> = emptyList(), removeTags: List<String> = emptyList(), keepIfUnmapped: Boolean) : Boolean {
    return if (addTags.isNotEmpty() || removeTags.isNotEmpty()) {
      val oldTags = note.tags
      note.tags = note.tags + mapTags(addTags, keepIfUnmapped) - mapTags(removeTags, keepIfUnmapped)
      oldTags != note.tags
    } else false
  }


  private fun mapTags(tagList: Iterable<String>, keepIfUnmapped: Boolean) = tagList.flatMap { tag ->
    tags.flatMap { it.findTags(TagContext(textContent = tag)) }
      .ifEmpty { if(keepIfUnmapped) listOf(tag) else emptyList() }
  }.toList()

  companion object : KLogging()
}