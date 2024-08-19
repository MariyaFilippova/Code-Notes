package org.me.notes.editor

import com.intellij.codeHighlighting.*
import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import org.me.notes.NotesBundle
import org.me.notes.actions.NoteLeaveNoteAction.Companion.activeInlay
import org.me.notes.settings.NotesHighlightingConfiguration
import org.me.notes.settings.NotesHighlightingConfiguration.Settings
import org.me.notes.storage.NotesStorage
import java.awt.Color

class NotesHighlightingPassFactory : TextEditorHighlightingPassFactoryRegistrar, TextEditorHighlightingPassFactory,
    DumbAware {
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
    private val editor: Editor,
) : TextEditorHighlightingPass(file.project, editor.document) {
    companion object {
        val myActiveSuggestionInlay = Key.create<Inlay<HintRenderer>>("hint.notes.inlay")
    }

    override fun doCollectInformation(progress: ProgressIndicator) {}

    override fun doApplyInformationToEditor() {
        val settings = NotesHighlightingConfiguration.getInstance(file.project).state

        createSuggestionHintInlay(settings)

        NotesStorage.getInstance(file.project).notes[file.virtualFile]?.forEach { note ->
            val rangeMarker = note.rangeMarker ?: return@forEach
            val start = rangeMarker.startOffset
            val end = rangeMarker.endOffset

            val text = note.getRepresentableText()

            val existingInlay = editor.inlayModel.getAfterLineEndElementsInRange(0, document.text.length - 1)
                .filter { it.renderer is HintRenderer }
                .find { (it.renderer as HintRenderer).text == note.getRepresentableText() }

            val existingHighlighter =
                editor.markupModel.allHighlighters.find { it.startOffset == start && it.endOffset == end }

            if (!settings.enableInlay) existingInlay?.dispose()
            if (!settings.enableHighlighting) existingHighlighter?.dispose()

            if (!isValidRangeMarker(rangeMarker)) {
                existingHighlighter?.dispose()
                existingInlay?.dispose()
                return@forEach
            }

            if (existingInlay == null && settings.enableInlay) {
                editor.inlayModel.addAfterLineEndElement(rangeMarker.endOffset, true, HintRenderer(text))
            }

            if (existingHighlighter != null || !settings.enableHighlighting) return@forEach
            editor.markupModel.addRangeHighlighter(
                start,
                end,
                0,
                textAttributes(file.project),
                HighlighterTargetArea.EXACT_RANGE
            )
        }
    }

    private fun isValidRangeMarker(rangeMarker: RangeMarker): Boolean {
        val start = rangeMarker.startOffset
        val end = rangeMarker.endOffset
        if (start >= end) return false
        val textUnderRangeMarker = rangeMarker.document.getText(rangeMarker.textRange)
        return !textUnderRangeMarker.trim().isEmpty()
    }

    private fun canCreateInlay() = activeInlay.get(editor) == null && editor.selectionModel.hasSelection()

    private fun createSuggestionHintInlay(settings: Settings) {
        if (!settings.enableInlay) return

        myActiveSuggestionInlay.get(editor)?.dispose()

        if (canCreateInlay()) {
            val hintInlay = editor.inlayModel.addAfterLineEndElement(
                editor.selectionModel.selectionEnd,
                false,
                HintRenderer(
                    NotesBundle.message(
                        "notes.inlay.press.to.leave.note",
                        KeymapUtil.getFirstKeyboardShortcutText("org.me.notes.actions.NoteLeaveNote")
                    )
                )
            )
            myActiveSuggestionInlay.set(editor, hintInlay)
        }
    }
}

@Suppress("UseJBColor")
fun textAttributes(project: Project): TextAttributes = with(TextAttributes()) {
    val settings = NotesHighlightingConfiguration.getInstance(project).state
    backgroundColor = Color(settings.r.toInt(), settings.g.toInt(), settings.b.toInt())
    this
}