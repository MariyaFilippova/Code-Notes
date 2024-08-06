package org.me.notes.editor

import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorCustomizer
import com.intellij.openapi.util.Disposer

class NotesTextEditorCustomizer : TextEditorCustomizer {
    override fun customize(textEditor: TextEditor) {
        val project = textEditor.editor.project ?: return
        val toolbar = NotesToolBar(textEditor.editor, project)
        Disposer.register(textEditor, toolbar)
    }
}