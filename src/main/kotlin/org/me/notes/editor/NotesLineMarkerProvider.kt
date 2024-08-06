package org.me.notes.editor

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.ide.ClipboardSynchronizer
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.injected.changesHandler.range
import com.intellij.util.ImageLoader
import com.intellij.util.ui.JBImageIcon
import org.me.notes.Note
import org.me.notes.NotesPersistentState
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.ImageIcon

class NotesLineMarkerProvider : LineMarkerProvider {
    companion object {
        private const val NOTE_ICON = "/icons/notes.svg"
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val file = element.containingFile
        val notes = NotesPersistentState.getInstance(file.project).state.notes
        val notesForFile = notes[file.virtualFile] ?: return null

        notesForFile.forEach { note ->
            val range = note.rangeHighlighter.range
            val elementInRange = file.findElementAt(note.rangeHighlighter.startOffset)
            if (elementInRange != element) return@forEach
            val icon = createNotesIcon() ?: AllIcons.General.Note
            return LineMarkerInfo(element, range, icon, { note.text }, { e, _ -> clickAction(e, file, note) },
                GutterIconRenderer.Alignment.LEFT, { "Notes" })
        }
        return null
    }

    private fun createNotesIcon(): ImageIcon? {
        val imgURL = javaClass.getResource(NOTE_ICON) ?: return null
        val image = ImageLoader.loadFromUrl(imgURL) ?: return null
        return JBImageIcon(ImageLoader.scaleImage(image, 16, 16))
    }

    private fun clickAction(e: MouseEvent, file: PsiFile, note: Note) {
        val document = note.rangeHighlighter.document
        val menu = JBPopupMenu().apply {

        }
        menu.add(object : AbstractAction("Copy code") {
            override fun actionPerformed(e: ActionEvent?) {
                val code = document.getText(note.rangeHighlighter.range)
                val content = StringSelection(code)
                ClipboardSynchronizer.getInstance().setContent(content, content)
            }
        })
        menu.add(object : AbstractAction("Delete note") {
            override fun actionPerformed(e: ActionEvent?) {
                val project = file.project
                val virtualFile = note.virtualFile
                val notes = NotesPersistentState.getInstance(project).state.notes
                note.rangeHighlighter.dispose()
                notes.computeIfPresent(virtualFile, { _, n -> n.minus(note) })
                if (notes[virtualFile] != null && notes[virtualFile]!!.isEmpty()) {
                    notes.remove(virtualFile)
                }
                DaemonCodeAnalyzer.getInstance(project).restart(file)
                project.messageBus.syncPublisher(NotesPersistentState.NotesChangedListener.NOTES_CHANGED_TOPIC)
                    .notesChanged()
            }
        })
        menu.show(e.component, e.x, e.y)
    }
}