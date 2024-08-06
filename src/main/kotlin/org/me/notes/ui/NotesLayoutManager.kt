package org.me.notes.ui

import com.intellij.openapi.editor.Editor
import com.intellij.util.DocumentUtil
import com.intellij.util.text.CharArrayUtil
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.LayoutManager

class NotesLayoutManager(val editor: Editor, val offset: Int) : LayoutManager {
    override fun addLayoutComponent(name: String?, comp: Component?) {}

    override fun removeLayoutComponent(comp: Component?) {}

    override fun preferredLayoutSize(parent: Container?): Dimension {
        if (parent == null) return Dimension(0, 0)

        var width = 0
        var height = 0
        parent.components?.forEach { component ->
            component.preferredSize.let {
                height += it.height
                width = maxOf(width, getX(editor, offset) + it.width)
            }
        }

        return Dimension(width, height + parent.insets.top + parent.insets.bottom)
    }

    override fun minimumLayoutSize(parent: Container?): Dimension? {
        return null
    }

    override fun layoutContainer(parent: Container?) {
        if (parent == null) return

        parent.components?.forEach {
            val ps = it.preferredSize
            it.setBounds(getX(editor, offset), parent.insets.top, ps.width, ps.height)
        }
    }

    private fun getX(editor: Editor, offset: Int): Int {
        val lineStartOffset = DocumentUtil.getLineStartOffset(offset, editor.document)
        val shiftForward = CharArrayUtil.shiftForward(editor.document.immutableCharSequence, lineStartOffset, " \t")
        return editor.offsetToXY(shiftForward).x
    }
}