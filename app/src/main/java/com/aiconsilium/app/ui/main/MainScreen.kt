package com.aiconsilium.app.ui.main

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Начиная с этой ширины окна показываем полноценный планшетный двухпанельный layout. */
private val TWO_PANE_MIN_WIDTH = 840.dp
private val CONTROL_PANEL_WIDTH = 360.dp

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (maxWidth >= TWO_PANE_MIN_WIDTH) {
                TwoPaneLayout(uiState, viewModel)
            } else {
                CompactLayout(uiState, viewModel)
            }
        }
    }
}

/** Планшетный ландшафтный master-detail: слева настройки, справа рабочая область. */
@Composable
private fun TwoPaneLayout(state: MainUiState, viewModel: MainViewModel) {
    Row(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .width(CONTROL_PANEL_WIDTH)
                .fillMaxSize(),
            tonalElevation = 1.dp
        ) {
            ControlPanel(
                state = state,
                onProviderToggled = viewModel::onProviderToggled,
                onApiKeyChanged = viewModel::onApiKeyChanged,
                onModelIdChanged = viewModel::onModelIdChanged,
                onArbiterSelected = viewModel::onArbiterSelected
            )
        }
        VerticalDivider()
        WorkArea(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            state = state,
            onPromptChanged = viewModel::onPromptChanged,
            onSubmit = viewModel::submitPrompt
        )
    }
}

/** Узкий экран (телефон/портретная ориентация): та же панель настроек прячется за вкладкой. */
@Composable
private fun CompactLayout(state: MainUiState, viewModel: MainViewModel) {
    var showSettings by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = if (showSettings) 1 else 0) {
            Tab(
                selected = !showSettings,
                onClick = { showSettings = false },
                text = { Text("Консилиум") }
            )
            Tab(
                selected = showSettings,
                onClick = { showSettings = true },
                text = { Text("Настройки") }
            )
        }

        if (showSettings) {
            ControlPanel(
                state = state,
                onProviderToggled = viewModel::onProviderToggled,
                onApiKeyChanged = viewModel::onApiKeyChanged,
                onModelIdChanged = viewModel::onModelIdChanged,
                onArbiterSelected = viewModel::onArbiterSelected
            )
        } else {
            WorkArea(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = state,
                onPromptChanged = viewModel::onPromptChanged,
                onSubmit = viewModel::submitPrompt
            )
        }
    }
}
