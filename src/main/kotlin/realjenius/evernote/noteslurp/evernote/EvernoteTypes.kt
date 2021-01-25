package realjenius.evernote.noteslurp.evernote

import com.evernote.edam.type.Data
import com.evernote.edam.type.Note
import com.evernote.edam.type.Resource
import java.net.FileNameMap
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class NotePage(val offset: Int, val hasNext: Boolean, val results: List<NoteDetails>)

data class NoteDetails(var title: String,
                       val guid: String,
                       val date: ZonedDateTime,
                       var tags: Set<String>,
                       var resources: List<NoteResource>?,
                       val content: String?) {
  constructor(it: Note, tags: Set<String>) : this(it, null, tags)

  constructor(it: Note, content: String?, tags: Set<String>) : this(
    it.title,
    it.guid,
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.created), ZoneId.of("America/Chicago")),
    tags,
    it.resources?.map { r -> NoteResource(r) },
    content
  )
}

data class NoteResource(var guid: String, var fileName: String?, var isAttachment: Boolean, var sourceUrl: String?) {
  constructor(res: Resource) : this(res.guid, res.attributes.fileName, res.attributes.isAttachment, res.attributes.sourceURL)
}

data class NoteChanges(var move: Boolean = false, var delete: Boolean = false, var titleChanged: Boolean = false, var tagsChanged: Boolean = false) {
  fun isChanged() = move || titleChanged || tagsChanged
}
