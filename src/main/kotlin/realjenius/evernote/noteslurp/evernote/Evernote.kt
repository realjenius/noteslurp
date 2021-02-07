package realjenius.evernote.noteslurp.evernote

import com.evernote.auth.EvernoteAuth
import com.evernote.auth.EvernoteService
import com.evernote.clients.ClientFactory
import com.evernote.clients.NoteStoreClient
import com.evernote.edam.notestore.NoteFilter
import com.evernote.edam.notestore.NoteStore
import com.evernote.edam.type.Note
import com.evernote.edam.userstore.Constants
import realjenius.evernote.noteslurp.evernote.Evernote.tags


object Evernote {
  private const val CLIENT_NAME = "NoteSlurp"

  fun connect(service: String, token: String) : NoteStoreClient {
    assert(validServiceKey(service))
    val auth = EvernoteAuth(EvernoteService.valueOf(service), token)
    val clients = ClientFactory(auth)
    if (!clients.createUserStoreClient().checkVersion(
        CLIENT_NAME,
        Constants.EDAM_VERSION_MAJOR,
        Constants.EDAM_VERSION_MINOR
      )
    ) {
      throw RuntimeException("Minimum Client Version not met!")
    }
    return clients.createNoteStoreClient()
  }

  fun NoteStoreClient.tags(note: Note) = this.getNoteTagNames(note.guid).toSet()

  fun NoteStoreClient.findNotebook(name: String) = listNotebooks().firstOrNull { it.name.equals(name, true) }

  fun NoteStoreClient.allNotes(filter: NoteFilter, content: Boolean = false, callback: (Note, NoteDetails) -> Boolean) {
    var moreNotes = true
    var offset = 0
    while (moreNotes) {
      val notes = findNotes(filter, offset, 20)
      if (notes.notes.size < 20) moreNotes = false
      offset += notes.notes.size
      notes.notes.forEach {
        val details = NoteDetails(it, if (content) getNoteContent(it.guid) else null, tags(it))
        if(callback(it, details)) offset--
      }
    }
  }

  fun NoteStoreClient.hasNotes(filter: NoteFilter) = findNotes(filter, 0, 1).notes.isNotEmpty()

  fun serviceKeys() = EvernoteService.values().map { it.name }.toTypedArray()
  private fun validServiceKey(key: String) = EvernoteService.values().any { it.name == key }
}