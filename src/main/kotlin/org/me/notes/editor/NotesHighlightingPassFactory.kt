package org.me.notes.editor

import com.intellij.codeHighlighting.*
import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.me.notes.NotesStorage
import org.me.notes.ui.textAttributes

class NotesHighlightingPassFactory : TextEditorHighlightingPassFactoryRegistrar, TextEditorHighlightingPassFactory, DumbAware {
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
        NotesStorage.getInstance(file.project).state.notes[file.virtualFile]?.forEach { note ->
            val rangeMarker = note.rangeMarker ?: return@forEach
            if (!rangeMarker.isValid) return@forEach
            val start = rangeMarker.startOffset
            val end = rangeMarker.endOffset

            val inlays = editor.inlayModel.getAfterLineEndElementsInRange(rangeMarker.startOffset, rangeMarker.endOffset)
            if (inlays.any { (it.renderer as? HintRenderer?)?.text == note.text }) return@forEach
            editor.inlayModel.addAfterLineEndElement(
                rangeMarker.endOffset,
                true,
                HintRenderer(note.text)
            )
            val existingHighlighter = editor.markupModel.allHighlighters.find {
                it.startOffset == start && it.endOffset == end
            }
            if (existingHighlighter != null) return@forEach

            editor.markupModel.addRangeHighlighter(
                start,
                end,
                0,
                textAttributes(),
                HighlighterTargetArea.EXACT_RANGE
            )
        }
    }
}