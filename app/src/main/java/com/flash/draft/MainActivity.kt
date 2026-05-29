package com.flash.draft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Get the ViewModel and pass it to the Canvas
            val viewModel: DraftViewModel = viewModel()
            DraftCanvas(viewModel = viewModel)
        }
    }
}