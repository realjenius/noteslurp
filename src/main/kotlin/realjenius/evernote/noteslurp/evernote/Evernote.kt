package realjenius.evernote.noteslurp.evernote

import com.evernote.auth.EvernoteAuth
import com.evernote.auth.EvernoteService
import com.evernote.clients.ClientFactory
import com.evernote.clients.NoteStoreClient
import com.evernote.edam.type.Data
import com.evernote.edam.type.Note
import com.evernote.edam.type.Resource
import com.evernote.edam.type.ResourceAttributes
import com.evernote.edam.userstore.Constants
import mu.KLogging
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import realjenius.evernote.noteslurp.LoadedFile
import realjenius.evernote.noteslurp.SavedNote
import realjenius.evernote.noteslurp.io.byteArrayToHex
import realjenius.evernote.noteslurp.io.md5
import realjenius.evernote.noteslurp.reactor.debug
import realjenius.evernote.noteslurp.reactor.schedulerMap
import java.nio.file.Path
import java.util.*

private const val CLIENT_NAME = "NoteSlurp"

abstract class Evernote(service: String, token: String) {
  protected val noteStore: NoteStoreClient
  init {
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
    noteStore = clients.createNoteStoreClient()
  }

  companion object {
    fun serviceKeys() = EvernoteService.values().map { it.name }.toTypedArray()
    fun validServiceKey(key: String) = EvernoteService.values().any { it.name == key }
  }
}