package org.me.notes

import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.tree.DefaultMutableTreeNode

class Note(
    var text: String,
    val code: String,
    val project: Project,
    val virtualFile: VirtualFile,
    var rangeMarker: RangeMarker
) : DefaultMutableTreeNode() {

    override fun isLeaf() = true
}

class File(val virtualFile: VirtualFile) : DefaultMutableTreeNode() {
    override fun isLeaf() = false
}