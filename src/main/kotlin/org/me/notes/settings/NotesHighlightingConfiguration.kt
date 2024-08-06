package org.me.notes.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.OptionTag
import org.me.notes.settings.NotesHighlightingConfiguration.Settings

@Service(Service.Level.PROJECT)
@State(name = "NotesHighlightingSettings", storages = [Storage("notes.xml")])
class NotesHighlightingConfiguration : SimplePersistentStateComponent<Settings>(Settings()) {
    companion object {
        fun getInstance(project: Project): NotesHighlightingConfiguration = project.service()
    }

    class Settings : BaseState() {
        @get:OptionTag("ENABLE_HIGHLIGHTING")
        var enableHighlighting by property(true)

        @get:OptionTag("ENABLE_INLAY")
        var enableInlay by property(true)

        @get:OptionTag("R")
        var r by property(defaultHighlightingColor.red)

        @get:OptionTag("G")
        var g by property(defaultHighlightingColor.green)

        @get:OptionTag("B")
        var b by property(defaultHighlightingColor.blue)
    }
}