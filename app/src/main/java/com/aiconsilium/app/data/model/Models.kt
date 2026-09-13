package com.aiconsilium.app.data.model

import java.util.UUID

/**
 * Провайдер LLM, поддерживаемый приложением.
 *
 * defaultModel — актуален на момент написания (сентябрь 2026), но провайдеры
 * обновляют/переименовывают модели часто (например, DeepSeek полностью вывела
 * из обращения "deepseek-chat"/"deepseek-reasoner" 24 июля 2026 в пользу
 * "deepseek-v4-flash"/"deepseek-v4-pro"). Поэтому modelId в [ProviderConfig] —
 * обычное текстовое поле, редактируемое прямо в приложении, а не жёстко
 * зашитая константа: если провайдер в очередной раз переименует модель,
 * не понадобится новая сборка приложения.
 */
enum class AIProvider(
    val displayName: String,
    val defaultModel: String,
    val apiKeyHint: String
) {
    ANTHROPIC(
        displayName = "Anthropic Claude",
        defaultModel = "claude-opus-5",
        apiKeyHint = "sk-ant-…"
    ),
    OPENAI(
        displayName = "OpenAI GPT",
        defaultModel = "gpt-6-astra",
        apiKeyHint = "sk-…"
    ),
    GEMINI(
        displayName = "Google Gemini",
        // Алиас "-latest" сам следует за текущей флагманской моделью Google —
        // это надёжнее, чем зашивать версионное имя вроде gemini-3-pro-preview.
        defaultModel = "gemini-pro-latest",
        apiKeyHint = "AIza…"
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        defaultModel = "deepseek-v4-flash",
        apiKeyHint = "sk-…"
    ),
    MISTRAL(
        displayName = "Mistral AI",
        defaultModel = "mistral-large-latest",
        apiKeyHint = "…"
    )
}

/** Настройки одного провайдера, задаются пользователем в панели управления. */
data class ProviderConfig(
    val provider: AIProvider,
    val isEnabled: Boolean = false,
    val apiKey: String = "",
    val modelId: String = provider.defaultModel
)

/** Состояние ответа одной модели на запрос пользователя. */
sealed interface ModelAnswer {
    val provider: AIProvider

    data class Loading(override val provider: AIProvider) : ModelAnswer

    data class Success(
        override val provider: AIProvider,
        val text: String,
        val latencyMs: Long
    ) : ModelAnswer

    data class Failure(
        override val provider: AIProvider,
        val message: String
    ) : ModelAnswer
}

/**
 * Один "созыв консилиума": запрос пользователя, сырые ответы всех опрошенных
 * моделей и финальный синтез арбитра.
 *
 * finalSynthesis == null означает "арбитр ещё не закончил" — отдельного
 * состояния Loading для него нет, оно не нужно.
 */
data class ConsiliumResult(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val answers: List<ModelAnswer>,
    val arbiterProvider: AIProvider,
    val finalSynthesis: ModelAnswer? = null
)
