package org.me.notes.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorTextField
import org.me.notes.Note
import org.me.notes.NotesStorage
import org.me.notes.ui.NotesToolBar.Companion.activeInlay
import org.me.notes.ui.NotesToolWindowPanel.Companion.pinIcon
import org.me.notes.ui.textAttributes
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Supplier

class NoteSave(val component: EditorTextField, val editor: Editor, val project: Project) :
    DumbAwareAction(Supplier { "Save Note" }, pinIcon) {

    init {
        registerCustomShortcutSet(
            ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_ENTER).shortcutSet, component
        )
    }

    override fun actionPerformed(e: AnActionEvent) {
        saveNote()
        val inlay = activeInlay.get(editor) ?: return
        Disposer.dispose(inlay)
        activeInlay.set(editor, null)
    }

    private fun saveNote() {
        val file = PsiManager.getInstance(project).findFile(editor.virtualFile)
        if (file == null || component.text.isEmpty()) return

        val selectedCode = editor.selectionModel.selectedText ?: throw Exception("empty selected code")
        val highlighter = editor.markupModel.addRangeHighlighter(
            editor.selectionModel.selectionStart,
            editor.selectionModel.selectionEnd, 0, textAttributes(), HighlighterTargetArea.EXACT_RANGE
        )

        val formatter = DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy")
        val currentTime = LocalDateTime.now().format(formatter)
        val note = Note(component.text, selectedCode, project, editor.virtualFile, highlighter, currentTime)

        NotesStorage.getInstance(project).state.addNote(editor.virtualFile, note)

        project.messageBus.syncPublisher(NotesStorage.NotesChangedListener.NOTES_CHANGED_TOPIC).notesChanged()
    }
}

class NoteClose(component: EditorTextField, val editor: Editor) : DumbAwareAction(Supplier { "Close" }) {

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