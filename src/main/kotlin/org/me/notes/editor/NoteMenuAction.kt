package org.me.notes.editor

import com.intellij.ide.ClipboardSynchronizer
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.refactoring.suggested.range
import org.me.notes.NotesStorage
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
        val content = StringSelection(note.code)
        ClipboardSynchronizer.getInstance().setContent(content, content)
    }
}

class DeleteNoteAction : DumbAwareAction(), Toggleable {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.dataContext.getData(CommonDataKeys.EDITOR) ?: return
        val note = editor.getUserData(NOTE) ?: return
        val project = note.project
        val virtualFile = note.virtualFile
        NotesStorage.getInstance(project).state.deleteNote(virtualFile, note)
        editor.markupModel.allHighlighters.forEach {
            if (it.range == note.rangeMarker.range) it.dispose()
        }
        project.messageBus.syncPublisher(NotesStorage.NotesChangedListener.NOTES_CHANGED_TOPIC)
            .notesChanged()
    }
}