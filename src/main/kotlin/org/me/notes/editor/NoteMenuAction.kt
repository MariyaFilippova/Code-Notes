package org.me.notes.editor

import com.intellij.ide.ClipboardSynchronizer
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys.CONTEXT_COMPONENT
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.refactoring.suggested.range
import com.intellij.ui.treeStructure.Tree
import org.me.notes.Note
import org.me.notes.NotesStorage
import org.me.notes.ui.NotesHint
import java.awt.datatransfer.StringSelection

class EditNoteAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val note = getSelectedNote(e) ?: return
        val project = e.project ?: return

        val fileEditor = FileEditorManagerEx.getInstanceEx(project).selectedEditor ?: return
        val editor = EditorUtil.getEditorEx(fileEditor) ?: return
        NotesHint(editor, note.project, note).showHint()
    }
}

class CopyCodeAction : DumbAwareAction(), Toggleable {
    override fun actionPerformed(e: AnActionEvent) {
        val note = getSelectedNote(e) ?: return
        val content = StringSelection(note.code)
        ClipboardSynchronizer.getInstance().setContent(content, content)
    }
}

class DeleteNoteAction : DumbAwareAction(), Toggleable {
    override fun actionPerformed(e: AnActionEvent) {
        val note = getSelectedNote(e) ?: return
        val project = note.project
        val virtualFile = note.virtualFile
        NotesStorage.getInstance(project).state.deleteNote(virtualFile, note)

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
        project.messageBus.syncPublisher(NotesStorage.NotesChangedListener.NOTES_CHANGED_TOPIC).notesChanged()
    }
}

private fun getSelectedNote(e: AnActionEvent) : Note? {
    val tree = e.dataContext.getData(CONTEXT_COMPONENT) as Tree? ?: return null
    val notes = tree.getSelectedNodes(Note::class.java, null)
    if (notes.isEmpty()) return null
    return notes.first()
}