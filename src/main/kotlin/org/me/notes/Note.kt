package org.me.notes

import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import javax.swing.tree.DefaultMutableTreeNode

class Note(
    var text: String,
    val code: String,
    val project: Project,
    val virtualFile: VirtualFile,
    var rangeMarker: RangeMarker,
    val time: String
) : DefaultMutableTreeNode() {

    override fun isLeaf() = true

    fun getMessage(): String {
        if (!rangeMarker.isValid) return ""
        val line = virtualFile.findDocument()?.getLineNumber(rangeMarker.startOffset)
        rangeMarker.startOffset
        return ":star: _${time.substringBefore(" ")}_\n" +
                "${prepareText()}\n" +
                "`${virtualFile.path}:$line` \n" +
                "```${code}```"
    }

    private fun prepareText(): String {
        return text.trim().replace(">", "&gt;").split("\n").joinToString("\n") { ">$it" }
    }
}

class File(val virtualFile: VirtualFile) : DefaultMutableTreeNode() {
    override fun isLeaf() = false
}