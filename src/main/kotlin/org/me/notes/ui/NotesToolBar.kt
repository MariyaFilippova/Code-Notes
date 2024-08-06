package org.me.notes.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.VK_SHIFT

class NotesToolBar(val editor: Editor, project: Project) : Disposable {
    private val myHint = NotesHint(editor, project)
    private val myFile = PsiManager.getInstance(project).findFile(editor.virtualFile)

    private val myMouseListener = MyMouseListener()
    private val myKeyBoardListener = MyKeyBoardListener()
    private var myMouseReleased = false

    init {
        editor.addEditorMouseListener(myMouseListener)
        editor.contentComponent.addKeyListener(myKeyBoardListener)
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
                myHint.showHint()
            }
        }
    }

    override fun dispose() {
        editor.removeEditorMouseListener(myMouseListener)
        editor.contentComponent.addKeyListener(myKeyBoardListener)
        myHint.dispose()
    }
}

fun textAttributes(): TextAttributes = with(TextAttributes()) {
    backgroundColor = JBColor(
        Color(250, 240, 255),
        Color(40, 4, 44)
    )
    this
}