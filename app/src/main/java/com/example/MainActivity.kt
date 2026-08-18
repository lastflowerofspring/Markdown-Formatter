package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.screens.MainFormatterScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FormatterViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FormatterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle shared text or opened file on launch
        viewModel.handleIncomingIntent(intent, contentResolver)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainFormatterScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle shared text or opened file when app is already open
        viewModel.handleIncomingIntent(intent, contentResolver)
    }
}

