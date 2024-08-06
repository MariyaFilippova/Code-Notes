package org.me.notes.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class NotesToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = NotesToolWindowPanel(project)
        val content = toolWindow.contentManager.factory.createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)
    }
}