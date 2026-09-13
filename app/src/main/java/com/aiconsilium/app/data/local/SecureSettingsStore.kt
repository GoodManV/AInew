package com.aiconsilium.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aiconsilium.app.data.model.AIProvider
import com.aiconsilium.app.data.model.ProviderConfig
import kotlinx.coroutines.flow.first

private val Context.settingsDataStore by preferencesDataStore(name = "ai_consilium_settings")

/**
 * Хранилище настроек на Jetpack DataStore. Каждый API-ключ шифруется
 * [KeystoreCipher] перед записью и расшифровывается сразу при чтении —
 * на диске в файле DataStore лежит только шифротекст, само приложение
 * везде дальше работает с обычными строками.
 *
 * Все операции — suspend-функции: чтения и записи выполняются на фоне,
 * без блокировки UI-потока (в отличие от синхронного SharedPreferences).
 */
class SecureSettingsStore(private val context: Context) {

    suspend fun loadProviderConfigs(): List<ProviderConfig> {
        val prefs = context.settingsDataStore.data.first()
        return AIProvider.entries.map { provider ->
            ProviderConfig(
                provider = provider,
                isEnabled = prefs[booleanPreferencesKey(keyEnabled(provider))] ?: false,
                apiKey = KeystoreCipher.decrypt(prefs[stringPreferencesKey(keyApiKey(provider))] ?: ""),
                modelId = prefs[stringPreferencesKey(keyModel(provider))] ?: provider.defaultModel
            )
        }
    }

    suspend fun saveProviderConfig(config: ProviderConfig) {
        context.settingsDataStore.edit { prefs ->
            prefs[booleanPreferencesKey(keyEnabled(config.provider))] = config.isEnabled
            prefs[stringPreferencesKey(keyApiKey(config.provider))] = KeystoreCipher.encrypt(config.apiKey)
            prefs[stringPreferencesKey(keyModel(config.provider))] = config.modelId
        }
    }

    suspend fun loadArbiterProvider(): AIProvider {
        val name = context.settingsDataStore.data.first()[stringPreferencesKey(KEY_ARBITER)]
        return runCatching { AIProvider.valueOf(name ?: "") }.getOrDefault(AIProvider.ANTHROPIC)
    }

    suspend fun saveArbiterProvider(provider: AIProvider) {
        context.settingsDataStore.edit { prefs ->
            prefs[stringPreferencesKey(KEY_ARBITER)] = provider.name
        }
    }

    private fun keyEnabled(provider: AIProvider) = "enabled_${provider.name}"
    private fun keyApiKey(provider: AIProvider) = "api_key_${provider.name}"
    private fun keyModel(provider: AIProvider) = "model_${provider.name}"

    companion object {
        private const val KEY_ARBITER = "arbiter_provider"
    }
}
