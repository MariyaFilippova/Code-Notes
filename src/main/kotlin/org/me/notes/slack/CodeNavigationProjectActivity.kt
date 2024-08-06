package org.me.notes.slack

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import startServer

class CodeNavigationProjectActivity: ProjectActivity {
    override suspend fun execute(project: Project) {
        startServer(project)
    }
}