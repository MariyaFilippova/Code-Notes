package org.me.notes.storage

import com.intellij.util.messages.Topic

interface NotesChangedListener {
    companion object {
        val NOTES_CHANGED_TOPIC = Topic.create("notes", NotesChangedListener::class.java)
    }

    fun notesChanged()
}