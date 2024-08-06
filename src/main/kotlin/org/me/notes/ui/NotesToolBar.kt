package org.me.notes.ui

import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.ComponentInlayAlignment
import com.intellij.openapi.editor.ComponentInlayRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayProperties
import com.intellij.openapi.editor.addComponentInlay
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.observable.util.whenDisposed
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiManager
import com.intellij.ui.JBColor
import com.intellij.ui.codeFloatingToolbar.CodeFloatingToolbar
import java.awt.Color
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.VK_SHIFT

@Suppress("UnstableApiUsage")
class NotesToolBar(val editor: Editor, val project: Project) : Disposable {
    companion object {
        val activeInlay = Key.create<Inlay<ComponentInlayRenderer<NotesInlayPanel>>>("activeInlay")
    }

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
                myInlay = createInlay()
            }
        }
    }

    private fun createInlay(): Inlay<HintRenderer>? {
        return editor.inlayModel.addAfterLineEndElement(
            editor.selectionModel.selectionEnd,
            false,
            HintRenderer("Press %s to leave note".format(KeymapUtil.getKeyText(VK_SHIFT)))
        )
    }

    private fun createComponentInlay(): Inlay<ComponentInlayRenderer<NotesInlayPanel>>? {
        val disabled = CodeFloatingToolbar.isTemporarilyDisabled()
        CodeFloatingToolbar.temporarilyDisable()

        val component = NotesInlayPanel(editor, project)
        val inlay = editor.addComponentInlay(
            editor.caretModel.offset,
            InlayProperties().showAbove(false).showWhenFolded(true),
            component,
            ComponentInlayAlignment.STRETCH_TO_CONTENT_WIDTH
        )

        inlay?.whenDisposed {
            CodeFloatingToolbar.temporarilyDisable(disabled)
            component.dispose()
        }
        return inlay
    }

    private inner class MyKeyBoardListener : KeyAdapter() {
        override fun keyPressed(e: KeyEvent?) {
            if (e?.keyCode == VK_SHIFT) {
                myInlay?.dispose()
                if (myFile == null || !editor.selectionModel.hasSelection()) return
                val inlay = createComponentInlay()
                activeInlay.set(editor, inlay)
            }
        }

        override fun keyReleased(e: KeyEvent?) {
            myInlay?.dispose()
            if (activeInlay.get(editor) != null) return
            if (e?.keyCode == VK_SHIFT) {
                if (myFile == null || !editor.selectionModel.hasSelection()) return
                myInlay = createInlay()
            }
        }
    }

    private inner class MySelectionListener : SelectionListener {
        override fun selectionChanged(e: SelectionEvent) {
            if (!editor.selectionModel.hasSelection()) myInlay?.dispose()
        }
    }

    override fun dispose() {
        editor.contentComponent.removeKeyListener(myKeyBoardListener)
        editor.selectionModel.removeSelectionListener(mySelectionListener)
        myInlay?.dispose()
        activeInlay.get(editor)?.dispose()
        activeInlay.set(editor, null)
    }
}

fun textAttributes(): TextAttributes = with(TextAttributes()) {
    backgroundColor = JBColor(
        Color(250, 248, 253),
        Color(42, 40, 46)
    )
    this
}