package com.aiconsilium.app.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * OpenAI, DeepSeek и Mistral используют одну и ту же схему запроса/ответа
 * Chat Completions ({"model", "messages":[{"role","content"}]} →
 * {"choices":[{"message":{"content"}}]}) — поэтому один набор DTO и один
 * Retrofit-интерфейс переиспользуются для всех троих. Разница между ними —
 * только base URL и путь запроса, которые заданы в [AiProviderClientFactory].
 *
 * ВАЖНО: у самого OpenAI это исторический (но полностью поддерживаемый ещё
 * долго) эндпоинт — с 2026 года OpenAI продвигает новый /v1/responses как
 * рекомендуемый для новых проектов. Мы сознательно остаёмся на Chat
 * Completions, потому что именно эту схему целиком повторяют DeepSeek
 * и Mistral, что и даёт возможность держать один клиент на троих.
 */

@Serializable
data class OpenAiCompatibleRequest(
    val model: String,
    val messages: List<OpenAiCompatibleMessage>
)

@Serializable
data class OpenAiCompatibleMessage(val role: String, val content: String)

@Serializable
data class OpenAiCompatibleResponse(
    val choices: List<OpenAiCompatibleChoice> = emptyList()
) {
    fun extractText(): String = choices.firstOrNull()?.message?.content.orEmpty()
}

@Serializable
data class OpenAiCompatibleChoice(val message: OpenAiCompatibleMessage)

/** Форма тела ошибки: {"error": {"message": "...", "type": "..."}} — общая для всех троих. */
@Serializable
data class OpenAiCompatibleErrorBody(val error: OpenAiCompatibleErrorDetail? = null)

@Serializable
data class OpenAiCompatibleErrorDetail(val message: String? = null, val type: String? = null)

interface OpenAiCompatibleApi {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") bearerToken: String,
        @Body request: OpenAiCompatibleRequest
    ): OpenAiCompatibleResponse
}
