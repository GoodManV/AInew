package com.aiconsilium.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aiconsilium.app.data.model.AiProvider
import com.aiconsilium.app.data.model.ProviderConfig

@Composable
fun ControlPanel(
    state: MainUiState,
    onProviderToggled: (AiProvider, Boolean) -> Unit,
    onApiKeyChanged: (AiProvider, String) -> Unit,
    onModelIdChanged: (AiProvider, String) -> Unit,
    onArbiterSelected: (AiProvider) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Конфигурация моделей",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Отметьте модели для опроса и укажите их API-ключи.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(state.providerConfigs, key = { it.provider.name }) { config ->
            ProviderConfigRow(
                config = config,
                onToggled = { onProviderToggled(config.provider, it) },
                onApiKeyChanged = { onApiKeyChanged(config.provider, it) },
                onModelIdChanged = { onModelIdChanged(config.provider, it) }
            )
        }

        item {
            HorizontalDivider()
            Column {
                Text(
                    text = "Модель-арбитр",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Получит все ответы и сформирует итоговый согласованный ответ.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ArbiterDropdown(
                    providerConfigs = state.providerConfigs,
                    selected = state.arbiterProvider,
                    onSelected = onArbiterSelected
                )
            }
        }
    }
}

@Composable
private fun ProviderConfigRow(
    config: ProviderConfig,
    onToggled: (Boolean) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onModelIdChanged: (String) -> Unit
) {
    var isKeyVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Switch(
                checked = config.isEnabled,
                onCheckedChange = onToggled
            )
            Text(
                text = config.provider.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        OutlinedTextField(
            value = config.apiKey,
            onValueChange = onApiKeyChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API-ключ") },
            placeholder = { Text(config.provider.apiKeyHint) },
            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                    Icon(
                        imageVector = if (isKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            },
            singleLine = true
        )

        OutlinedTextField(
            value = config.modelId,
            onValueChange = onModelIdChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Модель") },
            placeholder = { Text(config.provider.defaultModel) },
            singleLine = true
        )
    }
}

@Composable
private fun ArbiterDropdown(
    providerConfigs: List<ProviderConfig>,
    selected: AiProvider,
    onSelected: (AiProvider) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Арбитр: ${selected.displayName}")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            providerConfigs.forEach { config ->
                DropdownMenuItem(
                    text = { Text(config.provider.displayName) },
                    onClick = {
                        onSelected(config.provider)
                        expanded = false
                    }
                )
            }
        }
    }
}
