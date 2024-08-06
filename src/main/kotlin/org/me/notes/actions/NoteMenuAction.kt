package org.me.notes.actions

import com.intellij.ide.ClipboardSynchronizer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys.CONTEXT_COMPONENT
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.refactoring.suggested.range
import com.intellij.ui.treeStructure.Tree
import org.me.notes.File
import org.me.notes.Note
import org.me.notes.NotesStorage
import java.awt.datatransfer.StringSelection
import javax.swing.tree.DefaultMutableTreeNode

class CopyCodeAction : DumbAwareAction(), Toggleable {
    override fun actionPerformed(e: AnActionEvent) {
        val node = getSelectedNode(e) ?: return
        if (node is File) return
        val note = node as Note
        val content = StringSelection(note.code)
        ClipboardSynchronizer.getInstance().setContent(content, content)
    }
}

class DeleteNoteAction : DumbAwareAction(), Toggleable {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val node = getSelectedNode(e) ?: return
        if (node is File) {
            NotesStorage.getInstance(project).state.deleteNotesInFile(node.virtualFile)
            project.messageBus.syncPublisher(NotesStorage.NotesChangedListener.NOTES_CHANGED_TOPIC).notesChanged()
            return
        }
        val note = node as Note
        NotesStorage.getInstance(project).state.deleteNote(note)

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

    override fun update(e: AnActionEvent) {
        val node = getSelectedNode(e)
        if (node is File) {
            e.presentation.text = "Delete File Notes"
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}

private fun getSelectedNode(e: AnActionEvent) : DefaultMutableTreeNode? {
    val tree = e.dataContext.getData(CONTEXT_COMPONENT) as Tree? ?: return null
    val notes = tree.getSelectedNodes(DefaultMutableTreeNode::class.java, null)
    if (notes.isEmpty()) return null
    return notes.first()
}