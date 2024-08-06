package org.me.notes.editor

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Key
import com.intellij.util.ImageLoader
import com.intellij.util.ui.JBImageIcon
import org.me.notes.Note
import javax.swing.Icon

@Suppress("UnstableApiUsage")
class NotesIconRenderer(private val editor: Editor, private val note: Note) : DumbAware, GutterIconRenderer() {
    companion object {
        private const val NOTE_ICON = "/icons/notes.svg"
        val NOTE = Key<Note>("NOTE")
    }

    override fun equals(other: Any?): Boolean {
        return note == (other as NotesIconRenderer).note
    }

    override fun hashCode(): Int {
        return note.hashCode()
    }

    override fun getIcon(): Icon = createNotesIcon() ?: AllIcons.General.Note

    override fun getAlignment(): Alignment = Alignment.RIGHT

    override fun getPopupMenuActions(): ActionGroup? {
        editor.putUserData(NOTE, note)
        return ActionUtil.getActionGroup("popup@NotesMenu")
    }

    override fun getTooltipText(): String {
        return note.text
    }

    private fun createNotesIcon(): Icon? {
        val imgURL = javaClass.getResource(NOTE_ICON) ?: return null
        val image = ImageLoader.loadFromUrl(imgURL) ?: return null
        return JBImageIcon(ImageLoader.scaleImage(image, 16, 16))
    }
}