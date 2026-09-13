package com.aiconsilium.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aiconsilium.app.data.model.AIProvider
import com.aiconsilium.app.data.model.ProviderConfig

/**
 * Левая панель планшетного layout'а: выбор моделей для опроса, их API-ключи
 * и выбор модели-арбитра. Отдельной кнопки "Сохранить" нет — каждое
 * изменение сразу летит во ViewModel и сохраняется в зашифрованные настройки.
 */
@Composable
fun ControlPanel(
    state: MainUiState,
    onProviderToggled: (AIProvider, Boolean) -> Unit,
    onApiKeyChanged: (AIProvider, String) -> Unit,
    onModelIdChanged: (AIProvider, String) -> Unit,
    onArbiterSelected: (AIProvider) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Участники консилиума", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Отметьте модели для опроса и укажите их API-ключи.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(state.providerConfigs, key = { it.provider.name }) { config ->
            ProviderConfigRow(
                config = config,
                onToggled = { onProviderToggled(config.provider, it) },
                onApiKeyChanged = { onApiKeyChanged(config.provider, it) },
                onModelIdChanged = { onModelIdChanged(config.provider, it) }
            )
        }

        item { HorizontalDivider() }

        item {
            Column {
                Text("Модель-арбитр", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Получит все ответы и сформирует итоговый согласованный ответ.",
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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = config.isEnabled, onCheckedChange = onToggled)
            Text(config.provider.displayName, style = MaterialTheme.typography.bodyLarge)
        }

        OutlinedTextField(
            value = config.apiKey,
            onValueChange = onApiKeyChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API-ключ") },
            placeholder = { Text(config.provider.apiKeyHint) },
            singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace),
            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                    Icon(
                        imageVector = if (isKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (isKeyVisible) "Скрыть ключ" else "Показать ключ"
                    )
                }
            }
        )

        OutlinedTextField(
            value = config.modelId,
            onValueChange = onModelIdChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Модель") },
            singleLine = true,
            supportingText = { Text("По умолчанию: ${config.provider.defaultModel}") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArbiterDropdown(
    providerConfigs: List<ProviderConfig>,
    selected: AIProvider,
    onSelected: (AIProvider) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            label = { Text("Арбитр") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
