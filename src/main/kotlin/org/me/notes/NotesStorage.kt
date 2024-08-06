package org.me.notes

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.OptionTag
import java.nio.file.Paths

@Service(Service.Level.PROJECT)
@State(name = "NotesStorage", storages = [Storage("notes.xml")])
class NotesStorage(val project: Project) : SimplePersistentStateComponent<NotesStorage.MyState>(MyState()) {
    class MyState : BaseState() {
        @get:OptionTag(converter = NotesConverter::class)
        var notes: MutableMap<VirtualFile, List<Note>> by map()
        var messageId : String? by string()

        fun addNote(file: VirtualFile, note: Note) {
            notes.compute(file) { _, v ->
                v?.plus(note) ?: listOf(note)
            }
            incrementModificationCount()
        }

        fun deleteNote(file: VirtualFile, note: Note) {
            notes.computeIfPresent(file) { _, n -> n.minus(note) }
            if (notes[file] != null && notes[file]!!.isEmpty()) {
                notes.remove(file)
            }
            incrementModificationCount()
        }
    }

    companion object {
        fun getInstance(project: Project): NotesStorage = project.service()
    }

    interface NotesChangedListener {
        companion object {
            val NOTES_CHANGED_TOPIC = Topic.create("notes", NotesChangedListener::class.java)
        }

        fun notesChanged()
    }

    internal class NotesConverter : Converter<MutableMap<VirtualFile, List<Note>>>() {
        companion object {
            private const val NOTE_SEPARATOR = "<note_option>"
            private const val NOTES_SEPARATOR = "<note>"
            private const val VALUES_SEPARATOR = "<value>"
            private const val SEPARATOR = "<entry>"
        }

        private fun noteToString(note: Note): String {
            val start = note.rangeMarker.startOffset
            val end = note.rangeMarker.endOffset
            val file = note.virtualFile.path
            val time = note.time
            return note.text + NOTE_SEPARATOR + note.code + NOTE_SEPARATOR + time + NOTE_SEPARATOR + file + NOTE_SEPARATOR + start + NOTE_SEPARATOR + end
        }

        private fun noteFromString(value: String): Note {
            val array = value.split(NOTE_SEPARATOR)
            val file = VfsUtil.findFile(Paths.get(array[3]), true) ?: error("Cannot find ${array[0]} file")
            val project = ProjectManager.getInstance().openProjects.first()
            val start = array[4].toInt()
            val end = array[5].toInt()
            if (start > end) error("Invalid range marker for note ${array[0]}")
            val document = file.findDocument() ?: error("Cannot find document for ${file.path}")
            if (document.textLength < end) error("Invalid range marker for note ${array[0]}")
            return Note(array[0], array[1], project, file, document.createRangeMarker(start, end), array[2])
        }

        override fun toString(map: MutableMap<VirtualFile, List<Note>>): String {
            return map.entries.joinToString(SEPARATOR) {
                val file = it.key
                val notes = it.value.joinToString(separator = NOTES_SEPARATOR) { note -> noteToString(note) }
                return@joinToString file.path + VALUES_SEPARATOR + notes
            }
        }

        override fun fromString(value: String): MutableMap<VirtualFile, List<Note>> {
            val map = mutableMapOf<VirtualFile, List<Note>>()
            value.split(SEPARATOR).forEach {
                val entry = it.split(VALUES_SEPARATOR)
                val file = VfsUtil.findFile(Paths.get(entry[0]), true) ?: error("Cannot find ${entry[0]} file")
                val notes = entry[1].split(NOTES_SEPARATOR).map { stringNote -> noteFromString(stringNote) }
                map[file] = notes
            }
            return map
        }
    }
}