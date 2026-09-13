package com.aiconsilium.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aiconsilium.app.data.local.SecureSettingsStore
import com.aiconsilium.app.data.model.AIProvider
import com.aiconsilium.app.data.model.ConsiliumResult
import com.aiconsilium.app.data.model.ModelAnswer
import com.aiconsilium.app.data.model.ProviderConfig
import com.aiconsilium.app.data.repository.ConsiliumRepository
import com.aiconsilium.app.domain.ConsiliumProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsStore: SecureSettingsStore,
    private val repository: ConsiliumRepository = ConsiliumRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Настройки читаются асинхронно: на первом кадре экран показывает
        // дефолтное состояние (все провайдеры выключены), а через мгновение
        // подставляются сохранённые ключи/модели/арбитр из DataStore.
        viewModelScope.launch {
            val configs = settingsStore.loadProviderConfigs()
            val arbiter = settingsStore.loadArbiterProvider()
            _uiState.update { it.copy(providerConfigs = configs, arbiterProvider = arbiter) }
        }
    }

    fun onPromptChanged(text: String) {
        _uiState.update { it.copy(promptText = text) }
    }

    fun onProviderToggled(provider: AIProvider, isEnabled: Boolean) {
        updateAndPersistProvider(provider) { it.copy(isEnabled = isEnabled) }
    }

    fun onApiKeyChanged(provider: AIProvider, apiKey: String) {
        updateAndPersistProvider(provider) { it.copy(apiKey = apiKey) }
    }

    fun onModelIdChanged(provider: AIProvider, modelId: String) {
        updateAndPersistProvider(provider) { it.copy(modelId = modelId.ifBlank { provider.defaultModel }) }
    }

    fun onArbiterSelected(provider: AIProvider) {
        _uiState.update { it.copy(arbiterProvider = provider) }
        viewModelScope.launch { settingsStore.saveArbiterProvider(provider) }
    }

    private inline fun updateAndPersistProvider(
        provider: AIProvider,
        transform: (ProviderConfig) -> ProviderConfig
    ) {
        _uiState.update { state ->
            state.copy(providerConfigs = state.providerConfigs.map { config ->
                if (config.provider == provider) transform(config) else config
            })
        }
        val toSave = _uiState.value.providerConfigs.find { it.provider == provider } ?: return
        viewModelScope.launch { settingsStore.saveProviderConfig(toSave) }
    }

    fun submitPrompt() {
        val state = _uiState.value
        val arbiterConfig = state.arbiterConfig
        if (!state.canSubmit || arbiterConfig == null) return

        val prompt = state.promptText
        val enabledProviders = state.enabledProviders

        _uiState.update { it.copy(isSubmitting = true, promptText = "") }

        viewModelScope.launch {
            // "Живая" карточка результата, которую по ходу дела наполняем ответами
            // по мере их поступления из repository.runConsilium(...).
            var liveResult = ConsiliumResult(
                prompt = prompt,
                answers = enabledProviders.map { ModelAnswer.Loading(it.provider) },
                arbiterProvider = arbiterConfig.provider
            )
            _uiState.update { it.copy(history = it.history + liveResult) }

            repository.runConsilium(
                prompt = prompt,
                providerConfigs = enabledProviders,
                arbiterConfig = arbiterConfig
            ).collect { progress ->
                liveResult = when (progress) {
                    is ConsiliumProgress.AnswerUpdated -> liveResult.copy(
                        answers = liveResult.answers.map { existing ->
                            if (existing.provider == progress.answer.provider) progress.answer else existing
                        }
                    )
                    is ConsiliumProgress.SynthesisReady -> liveResult.copy(finalSynthesis = progress.answer)
                }
                val updatedResult = liveResult
                _uiState.update { state ->
                    state.copy(history = state.history.map { if (it.id == updatedResult.id) updatedResult else it })
                }
            }

            _uiState.update { it.copy(isSubmitting = false) }
        }
    }
}

/** Простая фабрика: MainViewModel требует Context (через SecureSettingsStore), поэтому нужен свой Factory. */
class MainViewModelFactory(private val settingsStore: SecureSettingsStore) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MainViewModel::class.java)) {
            "MainViewModelFactory умеет создавать только MainViewModel"
        }
        return MainViewModel(settingsStore) as T
    }
}
