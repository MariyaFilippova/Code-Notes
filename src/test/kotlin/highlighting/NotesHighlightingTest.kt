package highlighting

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.refactoring.suggested.range
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.me.notes.notes.Note
import org.me.notes.settings.NotesHighlightingConfiguration
import org.me.notes.storage.NotesStorage

class NotesHighlightingTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String? {
        return basePath() + "highlighting"
    }

    fun basePath() = "src/test/resources/"

    private fun doHighlighting() {
        val file = myFixture.configureByFile("Highlighting.java")
        NotesStorage.getInstance(project).notes = mutableMapOf<VirtualFile, List<Note>>(
            file.virtualFile to listOf(
                Note("note",
                    "        if (root == null) {&#10;            return;&#10;        }",
                    project,
                    file.virtualFile,
                    file.fileDocument.createRangeMarker(487, 536),
                    "12:42 05-08-2024"
                )
            )
        )
        myFixture.openFileInEditor(file.virtualFile)
        myFixture.doHighlighting()
    }

    fun testHighlightingEnabled() {
        doHighlighting()
        assert(myFixture.editor.markupModel.allHighlighters.any {
            it.range?.startOffset == 487 && it.range?.endOffset == 536
        })
    }

    fun testHighlightingDisabled() {
        val initialState = NotesHighlightingConfiguration.getInstance.state.enableHighlighting
        try {
            NotesHighlightingConfiguration.getInstance.state.enableHighlighting = false
            doHighlighting()
            assert(!myFixture.editor.markupModel.allHighlighters.any {
                it.range?.startOffset == 487 && it.range?.endOffset == 536
            })
        }
        finally {
            NotesHighlightingConfiguration.getInstance.state.enableHighlighting = initialState
        }
    }
}