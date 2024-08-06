package org.me.notes.ui

import com.intellij.codeInsight.navigation.openFileWithPsiElement
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.OpenFileAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.fileTypes.FileTypes.PLAIN_TEXT
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
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
import org.me.notes.notes.File
import org.me.notes.notes.Note
import org.me.notes.slack.createIcon
import org.me.notes.slack.postNotesIntoSlackBot
import org.me.notes.storage.NotesStorage
import java.awt.Component
import java.awt.Font
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION

@Suppress("UnstableApiUsage")
class NotesToolWindowPanel(private val project: Project) : BorderLayoutPanel(), Disposable {
    companion object {
        val pinIcon = createIcon("/icons/pin.svg")
        val slackIcon = createIcon("/icons/slack.svg")
    }

    private var myNotesTree: Tree = Tree()
    var myTreeModel: DefaultTreeModel

    private val mySyncSlackButton = JButton("Sync with slack", slackIcon)
    private val mySpinner = JBLabel(SpinningProgressIcon()).apply { isVisible = false }

    private val myNotesCodeTextArea: EditorTextField = object : EditorTextField(project, PLAIN_TEXT) {
        override fun createEditor(): EditorEx {
            return super.createEditor().apply {
                isEmbeddedIntoDialogWrapper = true
                setBorder(null)
                isOneLineMode = false
            }
        }
    }

    private val myTreeSelectionListener = object : TreeSelectionListener {
        override fun valueChanged(e: TreeSelectionEvent?) {
            val node = myNotesTree.getSelectedNodes(Note::class.java, null).firstOrNull()
            if (node == null) {
                myNotesCodeTextArea.isVisible = false
                return
            }
            navigateToCode(node)
            showNoteText(node)
        }
    }

    init {
        myTreeModel = DefaultTreeModel(buildNotesTree())
        myNotesTree = Tree(myTreeModel).apply { isRootVisible = false }

        myNotesTree.selectionModel.selectionMode = SINGLE_TREE_SELECTION
        myNotesTree.addTreeSelectionListener(myTreeSelectionListener)

        myNotesTree.cellRenderer = MyCellRenderer()

        PopupHandler.installPopupMenu(myNotesTree, "popup@NotesMenu", "notes.tree")

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
                    val pane = parent.parent as JBScrollPane
                    pane.border = JBUI.Borders.empty()
                    pane.horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
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
        NotesStorage.getInstance(project).notes.forEach {
            val p = File(it.key)
            root.add(p)
            it.value.forEach { note ->
                p.add(note)
            }
        }
        return root
    }

    override fun dispose() {
        myNotesTree.selectionModel.removeTreeSelectionListener(myTreeSelectionListener)
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
            hasFocus: Boolean,
        ): Component {
            val label = JBLabel()
            label.apply {
                font = Font(Font.MONOSPACED, Font.PLAIN, font.size)
            }
            when (value) {
                is File -> return label.apply {
                    text = value.virtualFile.name
                    icon = getFileIcon(value.virtualFile.name)
                }

                is Note -> return label.apply {
                    text = value.getRepresentableText()
                    icon = pinIcon
                }
            }
            return label
        }
    }
}

fun getModel(project: Project): DefaultTreeModel? {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Notes") ?: return null
    if (!toolWindow.isVisible) return null

    toolWindow.contentManager.contents.forEach {
        val panel = it.component as NotesToolWindowPanel? ?: return@forEach
        return panel.myTreeModel
    }
    return null
}