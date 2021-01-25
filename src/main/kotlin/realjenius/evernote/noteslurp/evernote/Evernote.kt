package realjenius.evernote.noteslurp.evernote

import com.evernote.auth.EvernoteAuth
import com.evernote.auth.EvernoteService
import com.evernote.clients.ClientFactory
import com.evernote.clients.NoteStoreClient
import com.evernote.edam.type.Note
import com.evernote.edam.userstore.Constants



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


  fun serviceKeys() = EvernoteService.values().map { it.name }.toTypedArray()
  private fun validServiceKey(key: String) = EvernoteService.values().any { it.name == key }
}