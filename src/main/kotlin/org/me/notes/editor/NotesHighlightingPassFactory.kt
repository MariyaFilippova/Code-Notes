package org.me.notes.editor

import com.intellij.codeHighlighting.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.me.notes.NotesPersistentState
import org.me.notes.ui.textAttributes

class NotesHighlightingPassFactory : TextEditorHighlightingPassFactoryRegistrar, TextEditorHighlightingPassFactory {
    override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
        registrar.registerTextEditorHighlightingPass(
            this,
            TextEditorHighlightingPassRegistrar.Anchor.AFTER,
            Pass.LAST_PASS,
            false,
            false
        )
    }

    override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass {
        return NotesHighlightingPass(file, editor)
    }
}

class NotesHighlightingPass(
    private val file: PsiFile,
    private val editor: Editor
) : TextEditorHighlightingPass(file.project, editor.document) {
    override fun doCollectInformation(progress: ProgressIndicator) {}

    override fun doApplyInformationToEditor() {
        NotesPersistentState.getInstance(file.project).state.notes[file.virtualFile]?.forEach { note ->
            if (!note.rangeHighlighter.isValid) return@forEach
            val start = note.rangeHighlighter.startOffset
            val end = note.rangeHighlighter.endOffset

            val existingHighlighter = editor.markupModel.allHighlighters.find {
                it.startOffset == start && it.endOffset == end
            }
            if (existingHighlighter != null) return@forEach

            val highlighter = editor.markupModel.addRangeHighlighter(
                start,
                end,
                0,
                textAttributes(),
                HighlighterTargetArea.EXACT_RANGE
            )
            highlighter.gutterIconRenderer = NotesIconRenderer(editor, note)
            note.rangeHighlighter = highlighter
        }
    }
}