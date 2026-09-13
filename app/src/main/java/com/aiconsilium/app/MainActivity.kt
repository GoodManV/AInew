package com.aiconsilium.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiconsilium.app.ui.main.MainScreen
import com.aiconsilium.app.ui.main.MainViewModel
import com.aiconsilium.app.ui.main.MainViewModelFactory
import com.aiconsilium.app.ui.theme.AiConsiliumTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AiConsiliumApp

        setContent {
            AiConsiliumTheme {
                val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(app.settingsStore))
                MainScreen(viewModel)
            }
        }
    }
}
