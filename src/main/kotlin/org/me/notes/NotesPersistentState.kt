package org.me.notes

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic

@Service(Service.Level.PROJECT)
@State(name = "NotesPersistentState", storages = [Storage("notes.xml")])
class NotesPersistentState: SimplePersistentStateComponent<NotesPersistentState.MyState>(MyState()) {
    class MyState: BaseState() {

        val notes: MutableMap<VirtualFile, List<Note>> by map()

        fun addNote(file: VirtualFile, note: Note) {
            notes.compute(file) { _, v ->
                v?.plus(note) ?: listOf(note)
            }
            incrementModificationCount()
        }

        fun deleteNote(file: VirtualFile, note: Note) {
            notes.computeIfPresent(file, { _, n -> n.minus(note) })
            if (notes[file] != null && notes[file]!!.isEmpty()) {
                notes.remove(file)
            }
            incrementModificationCount()
        }
    }

    companion object {
        fun getInstance(project: Project): NotesPersistentState = project.service()
    }

    interface NotesChangedListener {
        companion object {
            val NOTES_CHANGED_TOPIC = Topic.create("notes", NotesChangedListener::class.java)
        }

        fun notesChanged()
    }
}
