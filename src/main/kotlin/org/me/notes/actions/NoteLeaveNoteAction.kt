package org.me.notes.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.ComponentInlayAlignment
import com.intellij.openapi.editor.ComponentInlayRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayProperties
import com.intellij.openapi.editor.addComponentInlay
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.ui.codeFloatingToolbar.CodeFloatingToolbar
import org.me.notes.editor.NotesHighlightingPass.Companion.myActiveSuggestionInlay
import org.me.notes.ui.NotesInlayPanel

@Suppress("UnstableApiUsage")
class NoteLeaveNoteAction : DumbAwareAction() {
    companion object {
        val activeInlay = Key.create<Inlay<ComponentInlayRenderer<NotesInlayPanel>>>("activeInlay")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val project = e.getData(PlatformDataKeys.PROJECT) ?: return

        if (!editor.selectionModel.hasSelection()) return

        myActiveSuggestionInlay.get(editor)?.dispose()
        createComponentInlay(editor, project)
    }

    private fun createComponentInlay(editor: Editor, project: com.intellij.openapi.project.Project) {
        val disabled = CodeFloatingToolbar.isTemporarilyDisabled()
        CodeFloatingToolbar.temporarilyDisable()

        val component = NotesInlayPanel(editor, project, disabled)
        val inlay = editor.addComponentInlay(
            editor.caretModel.offset,
            InlayProperties().showAbove(false).showWhenFolded(true),
            component,
            ComponentInlayAlignment.STRETCH_TO_CONTENT_WIDTH
        )?.also {
            Disposer.register(it, component)
        }

        activeInlay.set(editor, inlay)
    }
}