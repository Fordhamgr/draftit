package com.flash.draft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = NoteRepository(this)

        setContent {
            DraftCanvas(
                initialElements = repo.loadElements(),
                onSave = { elements ->
                    repo.saveElements(elements)
                }
            )
        }
    }
}