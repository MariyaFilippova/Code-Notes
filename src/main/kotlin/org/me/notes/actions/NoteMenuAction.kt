package org.me.notes.actions

import com.intellij.ide.ClipboardSynchronizer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys.CONTEXT_COMPONENT
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.treeStructure.Tree
import org.me.notes.notes.File
import org.me.notes.notes.Note
import org.me.notes.storage.NotesStorage
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

    override fun update(e: AnActionEvent) {
        val node = getSelectedNode(e)
        if (node is File) e.presentation.isVisible = false
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}

class DeleteNoteAction : DumbAwareAction(), Toggleable {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val node = getSelectedNode(e) ?: return

        when (node) {
            is File -> NotesStorage.getInstance(project).deleteNotesInFile(node.virtualFile, project)
            is Note -> NotesStorage.getInstance(project).deleteNote(node, project)
        }
    }

    override fun update(e: AnActionEvent) {
        val node = getSelectedNode(e)
        if (node is File) e.presentation.text = "Delete File Notes"
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}

private fun getSelectedNode(e: AnActionEvent): DefaultMutableTreeNode? {
    val tree = e.dataContext.getData(CONTEXT_COMPONENT) as Tree? ?: return null
    return tree.getSelectedNodes(DefaultMutableTreeNode::class.java, null).firstOrNull()
}