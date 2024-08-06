package org.me.notes.storage

import com.intellij.openapi.components.BaseState
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.XCollection
import com.intellij.util.xmlb.annotations.XMap

class NotesState : BaseState() {
    @get:XMap()
    var notes by map<String, NoteListState>()

    fun changeNotesState() {
        incrementModificationCount()
    }
}

class NoteListState : BaseState() {
    @get:OptionTag("notes")
    @get:XCollection(style = XCollection.Style.v2)
    val notes by list<NoteState>()
}

class NoteState @JvmOverloads constructor(
    text: String? = null,
    code: String? = null,
    time: String? = null,
    start: Int = -1,
    end: Int = -1,
) : BaseState() {
    @get:OptionTag("text")
    var text by string()

    @get:OptionTag("code")
    var code by string()

    @get:OptionTag("time")
    var time by string()

    @get:OptionTag("start")
    var start by property(-1)

    @get:OptionTag("end")
    var end by property(-1)

    init {
        this.text = text
        this.code = code
        this.time = time
        this.start = start
        this.end = end
    }
}