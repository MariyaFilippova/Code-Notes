package org.me.notes.slack

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class CodeNavigationProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        startServer(project)
    }
}