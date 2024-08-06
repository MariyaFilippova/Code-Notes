package org.me.notes.editor

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
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiManager
import com.intellij.ui.JBColor
import com.intellij.ui.codeFloatingToolbar.CodeFloatingToolbar
import org.me.notes.ui.NotesInlayPanel
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
            createHintInlay()
        }
    }

    private fun createHintInlay() {
        myInlay?.dispose()

        if (!canCreateInlay()) return

        myInlay = editor.inlayModel.addAfterLineEndElement(
            editor.selectionModel.selectionEnd,
            false,
            HintRenderer("Press %s to leave note".format(KeymapUtil.getKeyText(VK_SHIFT)))
        )
    }

    private fun createComponentInlay() {
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

        activeInlay.set(editor, inlay)
    }

    private inner class MyKeyBoardListener : KeyAdapter() {
        override fun keyPressed(e: KeyEvent?) {
            if (e?.keyCode == VK_SHIFT) {
                myInlay?.dispose()

                if (!canCreateInlay()) return

                createComponentInlay()
            }
        }

        override fun keyReleased(e: KeyEvent?) {
            if (e?.keyCode == VK_SHIFT) {
                createHintInlay()
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
        val inlay = activeInlay.get(editor) ?: return
        Disposer.dispose(inlay)
        activeInlay.set(editor, null)
    }

    private fun canCreateInlay(): Boolean {
        return activeInlay.get(editor) == null && myFile != null && editor.selectionModel.hasSelection()
    }
}

fun textAttributes(): TextAttributes = with(TextAttributes()) {
    backgroundColor = JBColor(
        Color(237, 235, 251),
        Color(56, 59, 57)
    )
    this
}