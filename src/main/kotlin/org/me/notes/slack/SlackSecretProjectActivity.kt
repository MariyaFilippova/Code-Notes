package org.me.notes.slack

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class SlackSecretProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        SlackCredentialsHolder.getInstance().setClientSecrets()
    }
}