package org.me.notes.ui

import com.intellij.codeInsight.navigation.openFileWithPsiElement
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.OpenFileAction
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.fileTypes.FileTypes.PLAIN_TEXT
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorTextField
import com.intellij.ui.PopupHandler
import com.intellij.ui.SpinningProgressIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import org.me.notes.File
import org.me.notes.Note
import org.me.notes.NotesStorage
import org.me.notes.slack.SLACK_ICON
import org.me.notes.slack.createIcon
import org.me.notes.slack.postNotesIntoSlackBot
import java.awt.Component
import java.awt.Font
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION

class NotesToolWindowPanel(private val project: Project) : BorderLayoutPanel() {
    companion object {
        val pinIcon = createIcon("/icons/pin.svg")
        const val LABEL_LENGTH = 10
    }

    private var myNotesTree: Tree = Tree()
    private var myTreeModel: DefaultTreeModel

    private val mySyncSlackButton = JButton("Sync with slack", createIcon(SLACK_ICON))

    @Suppress("UnstableApiUsage")
    private val mySpinner = JBLabel(SpinningProgressIcon()).apply {
        isVisible = false
    }

    private val myNotesCodeTextArea: EditorTextField = object : EditorTextField(project, PLAIN_TEXT) {
        override fun createEditor(): EditorEx {
            val editor = super.createEditor()
            editor.isEmbeddedIntoDialogWrapper = true
            editor.setBorder(null)
            return editor
        }

        override fun isOneLineMode(): Boolean {
            return false
        }
    }

    init {
        myTreeModel = DefaultTreeModel(buildNotesTree())
        myNotesTree = Tree(myTreeModel)
        myNotesTree.apply {
            isRootVisible = false
        }
        PopupHandler.installPopupMenu(myNotesTree, "popup@NotesMenu", ActionPlaces.BOOKMARKS_VIEW_POPUP)
        myNotesTree.selectionModel.selectionMode = SINGLE_TREE_SELECTION
        myNotesTree.addTreeSelectionListener { _ ->
            val node = myNotesTree.getSelectedNodes(DefaultMutableTreeNode::class.java, null).firstOrNull()
            if (node == null) {
                myNotesCodeTextArea.isVisible = false
                return@addTreeSelectionListener
            }
            if (node is File) {
                myNotesCodeTextArea.isVisible = false
                return@addTreeSelectionListener
            }
            val note = node as Note
            navigateToCode(note)
            showNoteText(note)
        }
        myNotesTree.cellRenderer = MyCellRenderer()

        mySyncSlackButton.apply {
            addActionListener {
                mySpinner.isVisible = true
                postNotesIntoSlackBot(project) {
                    mySpinner.isVisible = false
                }
            }
        }

        myNotesCodeTextArea.apply {
            isVisible = false
            font = Font(Font.MONOSPACED, Font.PLAIN, 12)
            border = null
            isFocusable = false
        }

        add(getPanel())
    }

    fun getPanel(): JPanel {
        return panel {
            indent {
                row {
                    cell(mySyncSlackButton).align(Align.FILL)
                    cell(mySpinner)
                }
            }
            row {
                scrollCell(myNotesTree).align(AlignX.FILL).resizableColumn().applyToComponent {
                    (parent.parent as JBScrollPane).border = JBUI.Borders.empty()
                    (parent.parent as JBScrollPane).horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                }
            }
            indent {
                row {
                    scrollCell(myNotesCodeTextArea).align(Align.FILL).applyToComponent {
                        (parent.parent as JBScrollPane).border = JBUI.Borders.empty()
                    }
                }.resizableRow()
            }
        }
    }

    private fun showNoteText(note: Note) {
        myNotesCodeTextArea.isVisible = true
        myNotesCodeTextArea.fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(note.virtualFile.name)

        val commentedNote = note.text.lines().joinToString("\n") { "// $it" }
        val preparedText = note.prepareCode()

        myNotesCodeTextArea.document = EditorFactory.getInstance().createDocument("$commentedNote\n\n$preparedText")
        myNotesCodeTextArea.document.setReadOnly(true)
    }

    private fun navigateToCode(note: Note) {
        if (note.rangeMarker == null || !note.rangeMarker!!.isValid) return
        OpenFileAction.openFile(note.virtualFile, note.project)
        val psiFile = PsiManager.getInstance(note.project).findFile(note.virtualFile) ?: return
        val element = psiFile.findElementAt(note.rangeMarker!!.startOffset) ?: return
        openFileWithPsiElement(element, searchForOpen = true, requestFocus = true)
    }

    private fun buildNotesTree(): DefaultMutableTreeNode {
        val root = DefaultMutableTreeNode()
        val notes = NotesStorage.getInstance(project).state.notes
        notes.forEach {
            val p = File(it.key)
            root.add(p)
            it.value.forEach { note ->
                p.add(note)
            }
        }
        return root
    }

    private class MyCellRenderer : DefaultTreeCellRenderer() {
        private fun getFileIcon(fileName: String): Icon {
            val fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(fileName)
            return fileType.icon ?: AllIcons.FileTypes.Unknown
        }

        override fun getTreeCellRendererComponent(
            tree: JTree?,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ): Component {
            if (value is File) {
                return JBLabel().apply {
                    text = value.virtualFile.name
                    font = Font(Font.MONOSPACED, Font.PLAIN, font.size)
                    icon = getFileIcon(value.virtualFile.name)
                }
            } else if (value is Note) {
                val label = JBLabel().apply {
                    text = value.getRepresentableText()
                    icon = pinIcon
                    font = Font(Font.MONOSPACED, Font.PLAIN, font.size)
                    border = JBUI.Borders.empty(4)
                }
                return label
            }
            return JBLabel()
        }
    }
}
