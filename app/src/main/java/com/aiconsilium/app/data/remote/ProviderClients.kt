package com.aiconsilium.app.data.remote

import com.aiconsilium.app.data.model.AIProvider
import com.aiconsilium.app.data.model.ProviderConfig
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException

/**
 * Единый контракт для остального приложения: "дай текст запроса — получи текст
 * ответа или понятную причину неудачи". Скрывает протокольные различия между
 * провайдерами (заголовки, форма URL, форма JSON) за одной сигнатурой.
 */
interface AiProviderClient {
    suspend fun sendPrompt(prompt: String): Result<String>
}

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Общая обёртка вызова API: превращает любое исключение в Result.failure
 * с читаемым сообщением, но НЕ глотает CancellationException — если корутину
 * отменили (например, пользователь ушёл с экрана), отмена обязана дойти
 * до вызывающего кода, иначе структурированная конкурентность корутин
 * работает некорректно.
 */
private suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (c: CancellationException) {
    throw c
} catch (t: Exception) {
    Result.failure(IllegalStateException(describeFailure(t), t))
}

private fun describeFailure(t: Throwable): String = when (t) {
    is HttpException -> {
        val code = t.code()
        val detail = t.response()?.errorBody()?.string()?.let(::extractErrorMessage)
        if (detail.isNullOrBlank()) "HTTP $code" else "HTTP $code: $detail"
    }
    is IOException -> "Сеть недоступна или истёк тайм-аут (${t::class.simpleName})"
    else -> t.message ?: t::class.simpleName ?: "Неизвестная ошибка"
}

/**
 * У Anthropic, OpenAI-совместимых провайдеров и Gemini тело ошибки в разных
 * вариантах, но всегда сводится к {"error": {"message": "..."}} —
 * этого достаточно, чтобы не парсить каждый формат отдельным типом.
 */
private fun extractErrorMessage(body: String): String? = runCatching {
    errorJson.parseToJsonElement(body).jsonObject["error"]
        ?.jsonObject?.get("message")?.jsonPrimitive?.content
}.getOrNull()

// ---------------------------------------------------------------------------
// Anthropic
// ---------------------------------------------------------------------------

class AnthropicProviderClient(
    private val api: AnthropicApi,
    private val apiKey: String,
    private val model: String
) : AiProviderClient {
    override suspend fun sendPrompt(prompt: String): Result<String> = safeApiCall {
        val response = api.createMessage(
            apiKey = apiKey,
            request = AnthropicRequest(model = model, messages = listOf(AnthropicMessage("user", prompt)))
        )
        response.extractText().ifBlank { error("Модель вернула пустой ответ") }
    }
}

// ---------------------------------------------------------------------------
// OpenAI-совместимые: OpenAI, DeepSeek, Mistral
// ---------------------------------------------------------------------------

class OpenAiCompatibleProviderClient(
    private val api: OpenAiCompatibleApi,
    private val apiKey: String,
    private val model: String
) : AiProviderClient {
    override suspend fun sendPrompt(prompt: String): Result<String> = safeApiCall {
        val response = api.createChatCompletion(
            bearerToken = "Bearer $apiKey",
            request = OpenAiCompatibleRequest(model = model, messages = listOf(OpenAiCompatibleMessage("user", prompt)))
        )
        response.extractText().ifBlank { error("Модель вернула пустой ответ") }
    }
}

// ---------------------------------------------------------------------------
// Gemini
// ---------------------------------------------------------------------------

class GeminiProviderClient(
    private val api: GeminiApi,
    private val apiKey: String,
    private val model: String
) : AiProviderClient {
    override suspend fun sendPrompt(prompt: String): Result<String> = safeApiCall {
        val response = api.generateContent(
            model = model,
            apiKey = apiKey,
            request = GeminiRequest(contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))))
        )
        response.extractText().ifBlank { error("Модель вернула пустой ответ") }
    }
}

// ---------------------------------------------------------------------------
// Фабрика
// ---------------------------------------------------------------------------

/**
 * Собирает готовый [AiProviderClient] из настроек провайдера. Base URL каждого
 * провайдера прописан здесь единожды — включая ту часть пути ("/v1/" и т.п.),
 * которая у разных OpenAI-совместимых провайдеров отличается, поэтому сам
 * Retrofit-интерфейс [OpenAiCompatibleApi] остаётся одинаковым для всех троих.
 */
object AiProviderClientFactory {
    fun create(config: ProviderConfig): AiProviderClient = when (config.provider) {
        AIProvider.ANTHROPIC -> AnthropicProviderClient(
            api = NetworkModule.retrofitFor("https://api.anthropic.com/").create(AnthropicApi::class.java),
            apiKey = config.apiKey,
            model = config.modelId
        )
        AIProvider.OPENAI -> OpenAiCompatibleProviderClient(
            api = NetworkModule.retrofitFor("https://api.openai.com/v1/").create(OpenAiCompatibleApi::class.java),
            apiKey = config.apiKey,
            model = config.modelId
        )
        AIProvider.DEEPSEEK -> OpenAiCompatibleProviderClient(
            api = NetworkModule.retrofitFor("https://api.deepseek.com/").create(OpenAiCompatibleApi::class.java),
            apiKey = config.apiKey,
            model = config.modelId
        )
        AIProvider.MISTRAL -> OpenAiCompatibleProviderClient(
            api = NetworkModule.retrofitFor("https://api.mistral.ai/v1/").create(OpenAiCompatibleApi::class.java),
            apiKey = config.apiKey,
            model = config.modelId
        )
        AIProvider.GEMINI -> GeminiProviderClient(
            api = NetworkModule.retrofitFor("https://generativelanguage.googleapis.com/").create(GeminiApi::class.java),
            apiKey = config.apiKey,
            model = config.modelId
        )
    }
}
