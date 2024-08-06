package org.me.notes.ui

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.ui.HintHint
import com.intellij.ui.LightweightHint
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import org.me.notes.Note
import org.me.notes.NotesStorage
import org.me.notes.editor.NotesIconRenderer
import java.awt.BorderLayout
import java.awt.Point
import javax.swing.JButton
import javax.swing.JPanel

class NotesHint(private val editor: Editor, private val project: Project, private val note: Note? = null) : Disposable {
    private val myFile = PsiManager.getInstance(project).findFile(editor.virtualFile)

    private val myTextArea = JBTextArea(2, 2).apply {
        lineWrap = true
    }

    private val myHint = LightweightHint(getPanel()).apply {
        setForceShowAsPopup(true)
        setResizable(true)
        setFocusRequestor(myTextArea)
    }

    private fun getPanel(): JPanel {
        val centerPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(4)
            preferredSize = JBUI.size(400, 100)
        }
        if (note != null) myTextArea.text = note.text
        centerPanel.add(myTextArea, BorderLayout.CENTER)
        val button = JButton("Add Note").apply {
            addActionListener {
                if (note == null) {
                    saveNote()
                }
                else {
                    editNote()
                }
            }
        }
        val buttonPanel = JPanel(BorderLayout()).apply {
            add(button)
            border = null
        }
        centerPanel.add(buttonPanel, BorderLayout.SOUTH)
        return centerPanel
    }

    private fun saveNote() {
        if (myFile == null || myTextArea.text.isEmpty()) return

        val selectedCode = editor.selectionModel.selectedText ?: throw Exception("empty selected code")
        val highlighter = editor.markupModel.addRangeHighlighter(
            editor.selectionModel.selectionStart,
            editor.selectionModel.selectionEnd, 0, textAttributes(), HighlighterTargetArea.EXACT_RANGE
        )

        val note = Note(myTextArea.text, selectedCode, project, editor.virtualFile, highlighter)

        NotesStorage.getInstance(project).state.addNote(editor.virtualFile, note)
        highlighter.gutterIconRenderer = NotesIconRenderer(editor, note)

        myTextArea.text = ""
        myHint.hide()
        project.messageBus.syncPublisher(NotesStorage.NotesChangedListener.NOTES_CHANGED_TOPIC).notesChanged()
    }

    private fun editNote() {
        if (note == null) return
        note.text = myTextArea.text
        myTextArea.text = ""
        myHint.hide()
        project.messageBus.syncPublisher(NotesStorage.NotesChangedListener.NOTES_CHANGED_TOPIC).notesChanged()
    }

    override fun dispose() {
        myTextArea.text = ""
        myHint.hide()
    }

    fun showHint() {
        val point = editor.offsetToXY(editor.selectionModel.selectionEnd)
        val hintInfo = HintHint(editor, Point())
        HintManagerImpl.getInstanceImpl().showGutterHint(
            myHint,
            editor,
            editor.yToVisualLine(point.y),
            editor.offsetToXY(editor.selectionModel.selectionEnd).x,
            HintManager.HIDE_BY_ESCAPE,
            0,
            false,
            hintInfo
        )
    }
}