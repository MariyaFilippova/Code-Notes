package org.me.notes.notes

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
    companion object {
        const val LABEL_LENGTH = 10
    }

    override fun isLeaf() = true

    fun getRepresentableText(): String {
        if (text.length > LABEL_LENGTH) {
            return text.substring(0, LABEL_LENGTH) + "..."
        }
        return text
    }

    suspend fun getSlackMessage(): String {
        val line = rangeMarker?.let {
            if (it.isValid) {
                return@let readAction {
                    ":" + virtualFile.findDocument()?.getLineNumber(it.startOffset)
                }
            }
            return@let ""
        } ?: ""
        val path = virtualFile.path.substringAfterLast("/") + line
        return ":star: _${time.substringBefore(" ")}_\n" +
                "${prepareText()}\n" +
                "<jetbrains://idea/navigate/reference?project=${project.name}&path=$path|`$path`>\n" +
                "```${prepareCode()}```"
    }

    private fun prepareText(): String {
        return text.trim().replace(">", "&gt;").split("\n").joinToString("\n") { ">$it" }
    }

    fun prepareCode(): String {
        val minCommonIndent =
            code.lines()
                .filter(String::isNotBlank)
                .minOfOrNull { line ->
                    line.indexOfFirst { !it.isWhitespace() }.let { if (it == -1) line.length else it }
                }
                ?: 0
        return code.lines().joinToString("\n") {
            line -> if (line.length > minCommonIndent) line.substring(minCommonIndent) else line
        }
    }
}

class File(val virtualFile: VirtualFile) : DefaultMutableTreeNode() {
    override fun isLeaf() = false
}