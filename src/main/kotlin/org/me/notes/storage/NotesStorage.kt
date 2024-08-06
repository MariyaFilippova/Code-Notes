package org.me.notes.storage

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.intellij.refactoring.suggested.range
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.me.notes.editor.NotesToolBar.Companion.activeInlay
import org.me.notes.notes.Note
import java.nio.file.Paths

@Service(Service.Level.PROJECT)
@State(name = "NotesStorage", storages = [Storage("notes.xml")])
class NotesStorage(val project: Project) : SimplePersistentStateComponent<NotesState>(NotesState()) {

    companion object {
        fun getInstance(project: Project): NotesStorage = project.service()

        val logger = Logger.getInstance(NotesStorage::class.java)
    }

    val notes: MutableMap<VirtualFile, List<Note>> = mutableMapOf()

    override fun loadState(state: NotesState) {
        super.loadState(state)
        CoroutineScope(Dispatchers.Default).launch {
            state.notes.forEach { entry ->
                val virtualFile = VfsUtil.findFile(Paths.get(entry.key), true)
                val notesInFile = entry.value.notes

                if (virtualFile == null) {
                    logger.debug("Cannot find ${entry.key} file")
                    return@forEach
                }
                notes[virtualFile] =
                    notesInFile.mapNotNull { noteState -> readAction { createNote(noteState, virtualFile) } }
            }
        }
    }

    private fun createNote(noteState: NoteState, virtualFile: VirtualFile): Note? {
        val project = ProjectManager.getInstance().openProjects.first()

        val document = virtualFile.findDocument()
        if (document == null) {
            logger.debug("Cannot find document for $virtualFile")
        }

        val start = noteState.start

        if (noteState.start > noteState.end) logger.debug("Invalid range marker for note ${noteState.text}")

        val rangeMarker = let {
            if (document == null || document.textLength < noteState.end || noteState.start == -1 || noteState.end == -1) return@let null
            else document.createRangeMarker(start, noteState.end)
        }

        if (noteState.text == null || noteState.code == null || noteState.time == null) {
            logger.debug("Cannot deserialize Note from NoteState: $noteState")
            return null
        }
        return Note(noteState.text!!, noteState.code!!, project, virtualFile, rangeMarker, noteState.time!!)
    }

    fun addNote(note: Note, editor: Editor) {
        val virtualFile = note.virtualFile
        val path = virtualFile.path

        notes.compute(virtualFile) { _, v ->
            v?.plus(note) ?: listOf(note)
        }
        project.messageBus.syncPublisher(NotesChangedListener.NOTES_CHANGED_TOPIC).notesChanged()

        state.notes.compute(path) { _, v ->
            val start = note.rangeMarker?.startOffset ?: -1
            val end = note.rangeMarker?.endOffset ?: -1

            val noteState = NoteState(note.text, note.code, note.time, start, end)

            if (v != null) {
                v.notes.add(noteState)
                return@compute v
            }
            else {
                return@compute NoteListState().apply { notes.add(noteState) }
            }
        }

        logger.debug("note was added: ${note.text}")
        state.changeNotesState()

        // clean ui
        val inlay = activeInlay.get(editor) ?: return
        Disposer.dispose(inlay)
        activeInlay.set(editor, null)
    }

    fun deleteNote(note: Note, project: Project) {
        val virtualFile = note.virtualFile
        val path = virtualFile.path

        notes.computeIfPresent(virtualFile) { _, n -> n.minus(note) }

        if (notes[virtualFile]?.isEmpty() == true) notes.remove(virtualFile)

        project.messageBus.syncPublisher(NotesChangedListener.NOTES_CHANGED_TOPIC).notesChanged()

        val notesListState = state.notes[path] ?: return
        val noteState = notesListState.notes.find { it.code == note.code } ?: return

        notesListState.notes.remove(noteState)

        if (notesListState.notes.isEmpty()) {
            state.notes.remove(path)
        }

        // clean ui
        val fileEditor = FileEditorManagerEx.getInstanceEx(project).selectedEditor ?: return
        val editor = EditorUtil.getEditorEx(fileEditor) ?: return

        val rangeMarker = note.rangeMarker
        if (rangeMarker == null || !rangeMarker.isValid) return

        editor.markupModel.allHighlighters.forEach {
            if (it.range == rangeMarker.range) it.dispose()
        }
        editor.inlayModel.getAfterLineEndElementsInRange(rangeMarker.startOffset, rangeMarker.endOffset).forEach {
            it.dispose()
        }

        logger.debug("note was deleted: ${note.text}")
        state.changeNotesState()
    }

    fun deleteNotesInFile(file: VirtualFile, project: Project) {
        notes[file]?.forEach { note -> deleteNote(note, project) }
        project.messageBus.syncPublisher(NotesChangedListener.NOTES_CHANGED_TOPIC).notesChanged()

        state.notes.remove(file.path)

        logger.debug("notes were deleted: ${file.path}")
        state.changeNotesState()
    }
}