package org.me.notes.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentManager
import org.me.notes.NotesStorage

class NotesToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        addContent(toolWindow.contentManager, project)

        project.messageBus.connect().subscribe(NotesStorage.NotesChangedListener.NOTES_CHANGED_TOPIC, object :
            NotesStorage.NotesChangedListener {
            override fun notesChanged() {
                toolWindow.contentManager.removeAllContents(true)
                addContent(toolWindow.contentManager, project)
            }
        })
    }

    private fun addContent(contentManager: ContentManager, project: Project) {
        val panel = NotesToolWindowPanel(project).getPanel()
        val content = contentManager.factory.createContent(panel, null, false)
        contentManager.addContent(content)
    }
}