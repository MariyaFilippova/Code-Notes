package org.me.notes.ui

import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.keymap.KeymapUtil
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
    private val mySelectionListener = MySelectionListener()

    private var myInlay: Inlay<HintRenderer>? = null

    init {
        editor.addEditorMouseListener(myMouseListener)
        editor.contentComponent.addKeyListener(myKeyBoardListener)
        editor.selectionModel.addSelectionListener(mySelectionListener)
    }

    private inner class MyMouseListener : EditorMouseListener {
        override fun mouseReleased(event: EditorMouseEvent) {
            myInlay?.dispose()
            if (editor.selectionModel.hasSelection()) {
                myInlay = editor.inlayModel.addAfterLineEndElement(
                    editor.selectionModel.selectionEnd,
                    false,
                    HintRenderer("Press %s to leave note".format(KeymapUtil.getKeyText(VK_SHIFT)))
                )
            }
        }
    }

    private inner class MyKeyBoardListener : KeyAdapter() {
        override fun keyPressed(e: KeyEvent?) {
            myInlay?.dispose()
            if (e?.keyCode == VK_SHIFT) {
                if (myFile == null || !editor.selectionModel.hasSelection()) return
                myHint.showHint()
            }
        }

        override fun keyReleased(e: KeyEvent?) {
            myInlay?.dispose()
            if (e?.keyCode == VK_SHIFT) {
                if (myFile == null || !editor.selectionModel.hasSelection()) return
                myInlay = editor.inlayModel.addAfterLineEndElement(
                    editor.selectionModel.selectionEnd,
                    false,
                    HintRenderer("Press shift to leave note")
                )
            }
        }
    }

    private inner class MySelectionListener : SelectionListener {
        override fun selectionChanged(e: SelectionEvent) {
            if (!editor.selectionModel.hasSelection()) myInlay?.dispose()
        }
    }

    override fun dispose() {
        editor.contentComponent.addKeyListener(myKeyBoardListener)
        editor.selectionModel.removeSelectionListener(mySelectionListener)
        myHint.dispose()
        myInlay?.dispose()
    }
}

fun textAttributes(): TextAttributes = with(TextAttributes()) {
    backgroundColor = JBColor(
        Color(250, 248, 253),
        Color(42, 40, 46)
    )
    this
}