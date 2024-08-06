package org.me.notes.editor

import com.intellij.ide.ClipboardSynchronizer
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.impl.source.tree.injected.changesHandler.range
import org.me.notes.NotesPersistentState
import org.me.notes.editor.NotesIconRenderer.Companion.NOTE
import org.me.notes.ui.NotesHint
import java.awt.datatransfer.StringSelection

class EditNoteAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.dataContext.getData(CommonDataKeys.EDITOR) ?: return
        val note = editor.getUserData(NOTE) ?: return
        NotesHint(editor, note.project, note).showHint()
    }
}

class CopyCodeAction : DumbAwareAction(), Toggleable {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.dataContext.getData(CommonDataKeys.EDITOR) ?: return
        val note = editor.getUserData(NOTE) ?: return
        val code = note.rangeHighlighter.document.getText(note.rangeHighlighter.range)
        val content = StringSelection(code)
        ClipboardSynchronizer.getInstance().setContent(content, content)
    }
}

class DeleteNoteAction : DumbAwareAction(), Toggleable {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.dataContext.getData(CommonDataKeys.EDITOR) ?: return
        val note = editor.getUserData(NOTE) ?: return
        val project = note.project
        val virtualFile = note.virtualFile
        NotesPersistentState.getInstance(project).state.deleteNote(virtualFile, note)
        note.rangeHighlighter.dispose()
        project.messageBus.syncPublisher(NotesPersistentState.NotesChangedListener.NOTES_CHANGED_TOPIC)
            .notesChanged()
    }
}