package org.me.notes

import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import javax.swing.tree.DefaultMutableTreeNode
import kotlin.text.isWhitespace

class Note(
    var text: String,
    val code: String,
    val project: Project,
    val virtualFile: VirtualFile,
    var rangeMarker: RangeMarker?,
    val time: String
) : DefaultMutableTreeNode() {

    override fun isLeaf() = true

    suspend fun getMessage(): String {
        val line = rangeMarker?.let {
            if (it.isValid) {
                return@let readAction {
                    ":" + virtualFile.findDocument()?.getLineNumber(it.startOffset)
                }
            }
            return@let ""
        } ?: ""
        return ":star: _${time.substringBefore(" ")}_\n" +
                "${prepareText()}\n" +
                "<http://localhost:3001?file=${virtualFile.path}&line=$line|`${virtualFile.path}$line`>\n" +
                "```${prepareCode()}```"
    }

    private fun prepareText(): String {
        return text.trim().replace(">", "&gt;").split("\n").joinToString("\n") { ">$it" }
    }

    private fun prepareCode(): String {
        val minCommonIndent =
            code.lines().filter(String::isNotBlank)
                .minOfOrNull { line -> line.indexOfFirst { !it.isWhitespace() }.let { if (it == -1) line.length else it } }
                ?: 0
        return code.lines().joinToString("\n") { line -> line.substring(minCommonIndent) }
    }
}

class File(val virtualFile: VirtualFile) : DefaultMutableTreeNode() {
    override fun isLeaf() = false
}