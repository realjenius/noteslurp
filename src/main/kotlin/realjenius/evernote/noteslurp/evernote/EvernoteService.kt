package realjenius.evernote.noteslurp.evernote

import com.evernote.edam.notestore.NoteFilter
import com.evernote.edam.type.Note
import com.evernote.edam.type.Resource
import com.github.ajalt.clikt.core.CliktError
import realjenius.evernote.noteslurp.evernote.Evernote.findNotebook
import realjenius.evernote.noteslurp.evernote.Evernote.tags

class EvernoteService(service: String,
                      token: String,
                      private val from: String?,
                      to: String,
                      private val tags: List<TagStrategy>) {

  private val noteStore = Evernote.connect(service, token)

  private val sourceNotebook = from
    ?.let { noteStore.findNotebook(it) ?: throw CliktError("Source notebook '$from' could not be found'") } ?:
  noteStore.defaultNotebook

  private val targetNotebook = noteStore.findNotebook(to) ?: throw CliktError("Target notebook '$to' could not be found.")

  val filter = NoteFilter().apply {
    notebookGuid = sourceNotebook.guid
  }

  fun getContents(id: String) : String? {
    val note = noteStore.getNote(id, true, false, false, false)
    return note?.content
  }

  fun getResource(noteId: String, resourceId: String) : Resource? {
    val note = noteStore.getNote(noteId, true, true, true, true)
    return note?.resources?.find { it.guid == resourceId }
  }

  fun getPage(offset: Int, count: Int) : NotePage? {
    val notes = noteStore.findNotes(filter, offset, count+1)
    if (notes.notes.isEmpty()) return null
    val hasNext = notes.notes.size > count
    val page = if (hasNext) notes.notes.subList(0, notes.notes.size-1) else notes.notes
    return NotePage(offset, hasNext, page.map { NoteDetails(it, noteStore.tags(it)) })
  }

}