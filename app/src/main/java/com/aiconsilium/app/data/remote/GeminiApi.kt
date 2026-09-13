package com.aiconsilium.app.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * DTO и Retrofit-интерфейс для классического Gemini generateContent API.
 *
 * У Google с июня 2026 есть более новый универсальный Interactions API
 * (POST /v1beta2/interactions) для многошаговых агентных сценариев с
 * серверным состоянием диалога. Он не нужен для этой задачи: "Консилиум"
 * отправляет каждой модели ровно один независимый запрос без истории,
 * а generateContent Google прямо называет "остающимся полностью
 * поддерживаемым" — это ровно то, что нужно, и заметно проще Interactions API.
 */

@Serializable
data class GeminiRequest(val contents: List<GeminiContent>)

@Serializable
data class GeminiContent(val parts: List<GeminiPart>, val role: String? = null)

@Serializable
data class GeminiPart(val text: String)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList()
) {
    fun extractText(): String =
        candidates.firstOrNull()?.content?.parts?.joinToString("\n") { it.text }.orEmpty()
}

@Serializable
data class GeminiCandidate(val content: GeminiContent? = null, val finishReason: String? = null)

/** Форма тела ошибки: {"error": {"code":..., "message":"...", "status":"..."}} */
@Serializable
data class GeminiErrorBody(val error: GeminiErrorDetail? = null)

@Serializable
data class GeminiErrorDetail(val code: Int? = null, val message: String? = null, val status: String? = null)

interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
