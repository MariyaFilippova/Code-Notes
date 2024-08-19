package org.me.notes.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorTextField
import org.me.notes.NotesBundle
import org.me.notes.actions.NoteLeaveNoteAction.Companion.activeInlay
import org.me.notes.notes.File
import org.me.notes.notes.Note
import org.me.notes.storage.NotesStorage
import org.me.notes.ui.NotesToolWindowPanel.Companion.pinIcon
import org.me.notes.ui.getModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Supplier
import javax.swing.tree.DefaultMutableTreeNode

class NoteSave(val component: EditorTextField, val editor: Editor, val project: Project) :
    DumbAwareAction(Supplier { NotesBundle.message("notes.action.save.note") }, pinIcon) {

    init {
        registerCustomShortcutSet(
            ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_ENTER).shortcutSet, component
        )
    }

    override fun actionPerformed(e: AnActionEvent) {
        saveNote()
    }

    private fun saveNote() {
        val file = PsiManager.getInstance(project).findFile(editor.virtualFile)
        if (file == null || component.text.isEmpty()) return

        val spaces = " ".repeat(editor.caretModel.visualPosition.column)
        val selectedCode = editor.selectionModel.selectedText ?: throw Exception("empty selected code")
        val code = spaces + selectedCode

        val rangeMarker =
            editor.document.createRangeMarker(editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd)

        val formatter = DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy")
        val currentTime = LocalDateTime.now().format(formatter)
        val note = Note(component.text, code, project, editor.virtualFile, rangeMarker, currentTime)

        NotesStorage.getInstance(project).addNote(note, editor)

        val model = getModel(project) ?: return
        val root = model.root as? DefaultMutableTreeNode ?: return

        var fileNode = root.children().asSequence().find { (it as? File)?.virtualFile == file.virtualFile } as File?
        if (fileNode == null) {
            fileNode = File(file.virtualFile)
            model.insertNodeInto(fileNode, root, 0)
        }
        model.insertNodeInto(note, fileNode, 0)
        if (root.childCount == 1 && fileNode.childCount == 1) {
            model.reload()
        }
    }
}

class NoteClose(component: EditorTextField, val editor: Editor) :
    DumbAwareAction(Supplier { NotesBundle.message("notes.action.close") }) {

    init {
        registerCustomShortcutSet(
            ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_ESCAPE).shortcutSet, component
        )
        registerCustomShortcutSet(
            ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_ESCAPE).shortcutSet, editor.component
        )
    }

    override fun actionPerformed(e: AnActionEvent) {
        val inlay = activeInlay.get(editor) ?: return
        Disposer.dispose(inlay)
        activeInlay.set(editor, null)
    }
}