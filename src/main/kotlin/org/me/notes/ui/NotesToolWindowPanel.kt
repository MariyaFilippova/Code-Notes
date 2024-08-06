package org.me.notes.ui

import com.intellij.codeInsight.navigation.openFileWithPsiElement
import com.intellij.ide.actions.OpenFileAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.me.notes.File
import org.me.notes.Note
import org.me.notes.NotesStorage
import postNotesIntoSlackBot
import java.awt.Component
import java.awt.Font
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION

class NotesToolWindowPanel(private val project: Project) {
    private var myNotesTreePanel: JBScrollPane
    private var myNotesTree: Tree = Tree()
    private var myTreeModel: DefaultTreeModel
    private var mySyncSlackButton: JButton
    private var myNotesTextArea = JBTextArea().apply {
        lineWrap = true
        isVisible = false
        isEditable = false
    }

    init {
        myTreeModel = DefaultTreeModel(buildNotesTree())
        myNotesTree = Tree(myTreeModel)
        myNotesTree.isRootVisible = false
        myNotesTree.selectionModel.selectionMode = SINGLE_TREE_SELECTION
        myNotesTree.addTreeSelectionListener { _ ->
            val notes = myNotesTree.getSelectedNodes(Note::class.java, null)
            if (notes.isEmpty()) {
                myNotesTextArea.isVisible = false
                return@addTreeSelectionListener
            }
            val note = notes.first()
            navigateToCode(note)
            showNoteText(note)
        }
        myNotesTree.cellRenderer = MyCellRenderer()
        myNotesTreePanel = ScrollPaneFactory.createScrollPane(myNotesTree, true) as JBScrollPane
        mySyncSlackButton = JButton("Sync with slack").apply {
            val coroutineScope = CoroutineScope(Dispatchers.Default)
            addActionListener {
                coroutineScope.launch {
                    postNotesIntoSlackBot(project)
                }
            }
        }
    }

    fun getPanel(): JPanel {
        val containerPanel = JPanel()
        containerPanel.setLayout(BoxLayout(containerPanel, BoxLayout.Y_AXIS))
        containerPanel.add(mySyncSlackButton)
        containerPanel.add(myNotesTreePanel)
        containerPanel.add(ScrollPaneFactory.createScrollPane(myNotesTextArea, true) as JBScrollPane)
        return containerPanel
    }

    private fun showNoteText(note: Note) {
        myNotesTextArea.isVisible = true
        myNotesTextArea.text = note.text
    }

    private fun navigateToCode(note: Note) {
        OpenFileAction.openFile(note.virtualFile, note.project)
        val psiFile = PsiManager.getInstance(note.project).findFile(note.virtualFile) ?: return
        val element = psiFile.findElementAt(note.rangeMarker.startOffset) ?: return
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
                }
            } else if (value is Note) {
                val label = JBLabel().apply {
                    text = value.text
                    font = Font(Font.MONOSPACED, Font.PLAIN, font.size)
                    border = JBUI.Borders.empty(4)
                }
                return label
            }
            return JBLabel()
        }
    }
}
