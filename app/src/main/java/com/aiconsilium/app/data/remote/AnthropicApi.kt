package com.aiconsilium.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * DTO и Retrofit-интерфейс для Anthropic Messages API.
 * Документация: https://docs.claude.com/en/api/messages
 */

@Serializable
data class AnthropicRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int = 4096,
    val messages: List<AnthropicMessage>
)

@Serializable
data class AnthropicMessage(val role: String, val content: String)

@Serializable
data class AnthropicResponse(
    val id: String? = null,
    val content: List<AnthropicContentBlock> = emptyList(),
    @SerialName("stop_reason") val stopReason: String? = null
) {
    /** Склеивает все текстовые блоки ответа в одну строку (обычно блок один). */
    fun extractText(): String =
        content.filter { it.type == "text" }.joinToString("\n") { it.text.orEmpty() }
}

@Serializable
data class AnthropicContentBlock(val type: String, val text: String? = null)

/** Форма тела ошибки Anthropic: {"type":"error","error":{"type":"...","message":"..."}} */
@Serializable
data class AnthropicErrorBody(val error: AnthropicErrorDetail? = null)

@Serializable
data class AnthropicErrorDetail(val type: String? = null, val message: String? = null)

interface AnthropicApi {
    @Headers("content-type: application/json")
    @POST("v1/messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body request: AnthropicRequest
    ): AnthropicResponse
}
