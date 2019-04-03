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
import realjenius.evernote.noteslurp.reactor.debug
import realjenius.evernote.noteslurp.io.byteArrayToHex
import realjenius.evernote.noteslurp.io.md5
import realjenius.evernote.noteslurp.reactor.schedulerMap
import java.nio.file.Path
import java.util.*

private const val CLIENT_NAME = "NoteSlurp"
private const val SCHEDULER_NAME = "evernote"

class Evernote(service: String, token: String, private val syncDir: Path, private val fakeStore: Boolean, private val tags: List<TagStrategy>, maxParallelism: Int = 1) {
  private val scheduler = Schedulers.newParallel(SCHEDULER_NAME, maxParallelism)
  private val noteStore: NoteStoreClient
  init {
    assert(validServiceKey(service))
    val auth = EvernoteAuth(EvernoteService.valueOf(service), token)
    val clients = ClientFactory(auth)
    if(!clients.createUserStoreClient().checkVersion(
        CLIENT_NAME,
        Constants.EDAM_VERSION_MAJOR,
        Constants.EDAM_VERSION_MINOR)) {
      throw RuntimeException("Minimum Client Version not met!")
    }
    noteStore = clients.createNoteStoreClient()
  }

  fun createNote(file: LoadedFile) = Mono.just(file)
    .map { it.path to fileAsNote(it) }
    .debug(logger) { "Note: ${it.first} -> \nTags:---\n${it.second.tagNames}\nContents:---\n${it.second.content}\n---" }
    .schedulerMap(scheduler) {
      SavedNote(
        if (fakeStore) UUID.randomUUID().toString() else noteStore.createNote(it.second).guid,
        it.first
      )
    }
    .debug(logger) { "Note Created${if(fakeStore) " (Pretend)" else ""}: ${it.guid}" }

  private fun fileAsNote(file: LoadedFile) = Note().apply {
    title = file.name
    tags.flatMap { it.findTags(syncDir, file.path) }
      .forEach { this.addToTagNames(it) }

    val resource = Resource().apply {
      mime = file.mime
      isActive = true
      data = Data().apply {
        size = file.data.size
        bodyHash = md5(file.data)
        body = file.data
      }
      attributes = ResourceAttributes().apply {
        fileName = file.name
        isAttachment = true
      }
    }
    addToResources(resource)
    content = NOTE_TEMPLATE
      .replace("{attachment.mime}", resource.mime)
      .replace("{attachment.hash}", byteArrayToHex(resource.data.bodyHash))
  }


  companion object : KLogging() {
    private val NOTE_TEMPLATE =  Evernote::class.java.classLoader
      .getResource("document_template.xml")
      .readText(Charsets.UTF_8)

    fun serviceKeys() = EvernoteService.values().map { it.name }.toTypedArray()
    fun validServiceKey(key: String) = EvernoteService.values().any { it.name == key }
  }
}