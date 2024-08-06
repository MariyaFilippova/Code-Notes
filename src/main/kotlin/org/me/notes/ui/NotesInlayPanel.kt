package org.me.notes.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import org.jetbrains.annotations.NonNls
import org.me.notes.NotesBundle
import org.me.notes.actions.NoteClose
import org.me.notes.actions.NoteSave
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import kotlin.math.max
import kotlin.math.min

class NotesInlayPanel(val editor: Editor, val project: Project) : JPanel(), Disposable, DataProvider {
    companion object {
        const val ACTION_PLACE = "notes.inlay.panel"
    }

    private val myPanel = BorderLayoutPanel()
    private val myTextArea = object : EditorTextField(project, FileTypes.PLAIN_TEXT) {

        override fun createEditor(): EditorEx {
            return (super.createEditor() as EditorImpl).apply {
                isOneLineMode = false
                setBorder(null)
                scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
            }
        }

        override fun getPreferredSize(): Dimension {
            val preferredSize = super.getPreferredSize()
            return Dimension(
                max(preferredSize.width, JBUIScale.scale(300)),
                min(preferredSize.height, JBUIScale.scale(200))
            )
        }
    }

    private val myBackground = editor.colorsScheme.defaultBackground

    init {
        border = JBEmptyBorder(20)
        background = myBackground

        myPanel.apply {
            border = BorderFactory.createCompoundBorder(
                RoundedLineBorder(JBColor.GRAY, 15, 1),
                JBUI.Borders.empty(10)
            )
            isOpaque = false
            background = myBackground
        }

        myTextArea.apply {
            addSettingsProvider {
                it.settings.isUseSoftWraps = true
                it.settings.isPaintSoftWraps = false
            }
            setFontInheritedFromLAF(true)
            setPlaceholder(NotesBundle.message("notes.inlay.leave.your.note"))
            background = myBackground
            border = JBUI.Borders.empty()
            isOpaque = false
        }

        val group = DefaultActionGroup(NoteSave(myTextArea, editor, project), NoteClose(myTextArea, editor))
        val button = ActionToolbarImpl(ACTION_PLACE, group, true)
            .apply { targetComponent = this }
            .component.apply {
                border = JBUI.Borders.empty()
                background = myBackground
                border = null
                isOpaque = false
                size = Dimension(10, 10)
            }

        myPanel.addToCenter(myTextArea)
        myPanel.addToRight(button)

        add(myPanel)
        layout = NotesLayoutManager(editor, editor.caretModel.offset)
    }

    override fun dispose() {
        removeAll()
    }

    override fun getData(e: @NonNls String) = when (e) {
        PlatformDataKeys.EDITOR.name -> myTextArea.editor
        CommonDataKeys.HOST_EDITOR.name -> myTextArea.editor
        PlatformCoreDataKeys.FILE_EDITOR.name -> TextEditorProvider.getInstance().getTextEditor(myTextArea.editor!!)
        else -> null
    }
}