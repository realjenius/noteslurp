package realjenius.evernote.noteslurp.evernote

import com.evernote.edam.notestore.NoteFilter
import com.evernote.edam.type.Tag
import com.github.ajalt.clikt.core.CliktError
import realjenius.evernote.noteslurp.evernote.Evernote.allNotes
import realjenius.evernote.noteslurp.evernote.Evernote.hasNotes

class EvernoteTagManager(private val service: String,
                         private val token: String) {

  fun swapTag(oldTag: String, newTag: String) {
    val evernote = Evernote.connect(service, token)
    val allTags = evernote.listTags()
    val oldTagObj = allTags.firstOrNull { it.name.equals(oldTag, true) } ?: throw CliktError("Unable to find tag: $oldTag")
    val newTagObj = allTags.firstOrNull { it.name.equals(newTag, true) } ?: Tag().let {
      it.name = newTag
      evernote.createTag(it)
    }

    evernote.allNotes(NoteFilter().apply { this.tagGuids = listOf(oldTagObj.guid) }) { note, details ->
      note.tagGuids = ((note.tagGuids - oldTagObj.guid) + newTagObj.guid).toList()
      evernote.updateNote(note)
      true
    }
  }

  fun deleteTag(name: String) {
    val evernote = Evernote.connect(service, token)
    val allTags = evernote.listTags()
    val tag = allTags.firstOrNull { it.name.equals(name, true) } ?: throw CliktError("Unable to find tag: $name")
    if (evernote.hasNotes(NoteFilter().apply { this.tagGuids = listOf(tag.guid) }))
      throw CliktError("Notes found for tag: $name")

    evernote.expungeTag(tag.guid)
  }

  fun reparentTag(name: String, parent: String) {
    val evernote = Evernote.connect(service, token)
    val allTags = evernote.listTags()
    val tagObj = allTags.firstOrNull { it.name.equals(name, true) } ?: throw CliktError("Unable to find tag: $name")
    val parentTagObj = allTags.firstOrNull { it.name.equals(parent, true) } ?: Tag().let {
      it.name = parent
      evernote.createTag(it)
    }

    evernote.updateTag(tagObj.apply {
      this.parentGuid = parentTagObj.guid
    })
  }
}