package com.flash.draft

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class DraftViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = DraftDatabase.getDatabase(application).noteDao()

    // UI will observe this list automatically
    val notesFlow = dao.getAllNotes()

    fun addNote(note: NoteEntity) = viewModelScope.launch {
        dao.insertNote(note)
    }

    fun updateNote(note: NoteEntity) = viewModelScope.launch {
        dao.updateNote(note)
    }
}