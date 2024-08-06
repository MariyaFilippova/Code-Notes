package org.me.notes.settings

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.options.UiDslUnnamedConfigurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindSelected
import org.me.notes.NotesBundle
import java.awt.Color
import java.awt.Dimension
import javax.swing.JPanel

class NotesHighlightingSettings : UiDslUnnamedConfigurable.Simple() {
    @Suppress("UseJBColor")
    override fun Panel.createContent() {
        val settings = NotesHighlightingConfiguration.getInstance.state

        lateinit var enable: Cell<JBCheckBox>
        lateinit var r: Cell<JBTextField>
        lateinit var g: Cell<JBTextField>
        lateinit var b: Cell<JBTextField>
        lateinit var panel: Cell<JPanel>

        fun color() = Color(r.component.text.toInt(), g.component.text.toInt(), b.component.text.toInt())

        fun rerunDaemon() {
            for (project in ProjectManager.getInstance().openProjects) {
                DaemonCodeAnalyzer.getInstance(project).restart()
            }
        }

        panel {
            row {
                enable =
                    checkBox(NotesBundle.message("notes.settings.enable.highlighting")).bindSelected(settings::enableHighlighting)
                        .onApply { rerunDaemon() }
            }

            row {
                checkBox(NotesBundle.message("notes.settings.enable.inlays")).bindSelected(settings::enableInlay)
                    .onApply { rerunDaemon() }
            }

            row(NotesBundle.message("notes.settings.highlighting.color")) {
                r = intTextField(0..255).bindIntText(settings::r).onChanged {
                    if (it.text.isNotEmpty()) panel.component.background = color()
                }.onApply { rerunDaemon() }
                g = intTextField(0..255).bindIntText(settings::g).onChanged {
                    if (it.text.isNotEmpty()) panel.component.background = color()
                }.onApply { rerunDaemon() }
                b = intTextField(0..255).bindIntText(settings::b).onChanged {
                    if (it.text.isNotEmpty()) panel.component.background = color()
                }.onApply { rerunDaemon() }
                panel = cell(JPanel()).applyToComponent {
                    preferredSize = Dimension(20, 20)
                    background = color()
                }
                button(NotesBundle.message("notes.settings.restore.defaults")) {
                    r.applyToComponent {
                        text = defaultHighlightingColor.red.toString()
                    }
                    g.applyToComponent {
                        text = defaultHighlightingColor.green.toString()
                    }
                    b.applyToComponent {
                        text = defaultHighlightingColor.blue.toString()
                    }
                }
            }
        }
    }
}

val defaultHighlightingColor = JBColor(Color(237, 235, 251), Color(56, 59, 57))