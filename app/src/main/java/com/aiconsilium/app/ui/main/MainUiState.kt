package com.aiconsilium.app.ui.main

import com.aiconsilium.app.data.model.AIProvider
import com.aiconsilium.app.data.model.ConsiliumResult
import com.aiconsilium.app.data.model.ProviderConfig

/**
 * Единое состояние главного экрана. ViewModel хранит один экземпляр в
 * StateFlow, Compose только читает его и отрисовывает — никакой логики
 * здесь, кроме простых производных полей ниже.
 */
data class MainUiState(
    val providerConfigs: List<ProviderConfig> = AIProvider.entries.map { ProviderConfig(it) },
    val arbiterProvider: AIProvider = AIProvider.ANTHROPIC,
    val promptText: String = "",
    val isSubmitting: Boolean = false,
    val history: List<ConsiliumResult> = emptyList()
) {
    /** Провайдеры, отмеченные для опроса и с непустым API-ключом. */
    val enabledProviders: List<ProviderConfig>
        get() = providerConfigs.filter { it.isEnabled && it.apiKey.isNotBlank() }

    /** Конфигурация текущего арбитра (для чтения его API-ключа/модели). */
    val arbiterConfig: ProviderConfig?
        get() = providerConfigs.find { it.provider == arbiterProvider }

    /** Хватает ли настроек, чтобы кнопка "Спросить" вообще была активна. */
    val canSubmit: Boolean
        get() = promptText.isNotBlank() &&
            !isSubmitting &&
            enabledProviders.isNotEmpty() &&
            arbiterConfig?.apiKey?.isNotBlank() == true
}
