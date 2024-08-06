package org.me.notes.ui

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.ui.HintHint
import com.intellij.ui.JBColor
import com.intellij.ui.LightweightHint
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import org.me.notes.Note
import org.me.notes.NotesPersistentState
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Point
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.VK_SHIFT
import javax.swing.JButton
import javax.swing.JPanel

class NotesToolBar(val editor: Editor, private val project: Project) : Disposable {
    companion object {
        private val logger = Logger.getInstance(NotesToolBar::class.java)
    }

    private val myFile = PsiManager.getInstance(project).findFile(editor.virtualFile)

    private val myMouseListener = MyMouseListener()
    private val myKeyBoardListener = MyKeyBoardListener()
    private var myMouseReleased = false

    private val myTextArea = JBTextArea(2, 2).apply {
        lineWrap = true
    }

    private val myHint = LightweightHint(getPanel()).apply {
        setForceShowAsPopup(true)
        setResizable(true)
        setFocusRequestor(myTextArea)
    }

    init {
        editor.addEditorMouseListener(myMouseListener)
        editor.contentComponent.addKeyListener(myKeyBoardListener)
    }

    private fun getPanel(): JPanel {
        val centerPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(4)
            preferredSize = JBUI.size(400, 100)
        }
        centerPanel.add(myTextArea, BorderLayout.CENTER)
        val button = JButton("Add Note").apply {
            addActionListener { saveNote() }
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

        NotesPersistentState.getInstance(project).state.addNote(editor.virtualFile, note)

        myTextArea.text = ""
        myHint.hide()
        project.messageBus.syncPublisher(NotesPersistentState.NotesChangedListener.NOTES_CHANGED_TOPIC).notesChanged()
    }

    override fun dispose() {
        editor.removeEditorMouseListener(myMouseListener)
        editor.contentComponent.addKeyListener(myKeyBoardListener)
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

    private inner class MyMouseListener : EditorMouseListener {
        override fun mouseReleased(event: EditorMouseEvent) {
            myMouseReleased = true
        }

        override fun mouseClicked(event: EditorMouseEvent) {
            myMouseReleased = false
        }
    }

    private inner class MyKeyBoardListener : KeyAdapter() {
        override fun keyPressed(e: KeyEvent?) {
            if (e?.keyCode == VK_SHIFT && myMouseReleased) {
                if (myFile == null || !editor.selectionModel.hasSelection()) return
                showHint()
            }
        }
    }
}

fun textAttributes(): TextAttributes = with(TextAttributes()) {
    backgroundColor = JBColor(Color(250, 240, 255), JBColor.LIGHT_GRAY)
    this
}