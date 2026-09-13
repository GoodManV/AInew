package com.aiconsilium.app.domain

import com.aiconsilium.app.data.model.AIProvider
import com.aiconsilium.app.data.model.ModelAnswer
import com.aiconsilium.app.data.remote.AiProviderClient

/** События одного "созыва консилиума", по которым UI обновляется по мере готовности. */
sealed interface ConsiliumProgress {
    /** Одна из опрошенных моделей прислала ответ (успешный или нет). */
    data class AnswerUpdated(val answer: ModelAnswer) : ConsiliumProgress

    /** Арбитр закончил (или провалил) синтез финального ответа. */
    data class SynthesisReady(val answer: ModelAnswer) : ConsiliumProgress
}

/**
 * "Модуль арбитража": строит промпт для модели-арбитра из сырых ответов
 * остальных моделей и просит её свести их в один согласованный результат.
 */
object ConsensusEngine {

    suspend fun synthesize(
        originalPrompt: String,
        answers: List<ModelAnswer>,
        arbiterClient: AiProviderClient,
        arbiterProvider: AIProvider
    ): ModelAnswer {
        val successfulAnswers = answers.filterIsInstance<ModelAnswer.Success>()

        if (successfulAnswers.isEmpty()) {
            return ModelAnswer.Failure(
                arbiterProvider,
                "Ни одна из опрошенных моделей не ответила успешно — синтезировать нечего."
            )
        }

        val synthesisPrompt = buildSynthesisPrompt(originalPrompt, successfulAnswers)
        val startedAt = System.currentTimeMillis()
        val result = arbiterClient.sendPrompt(synthesisPrompt)
        val elapsed = System.currentTimeMillis() - startedAt

        return result.fold(
            onSuccess = { text -> ModelAnswer.Success(arbiterProvider, text, elapsed) },
            onFailure = { e -> ModelAnswer.Failure(arbiterProvider, e.message ?: "Ошибка арбитра") }
        )
    }

    /**
     * Промпт для арбитра: явно перечисляет мнения по именам моделей, просит
     * выявить противоречия, объединить факты и не выдавать сам процесс
     * "консилиума" за содержание ответа пользователю.
     */
    private fun buildSynthesisPrompt(originalPrompt: String, answers: List<ModelAnswer.Success>): String {
        val opinionsBlock = answers.joinToString("\n\n") { answer ->
            "### Ответ модели «${answer.provider.displayName}»\n${answer.text}"
        }
        return """
            Ты выступаешь модератором консилиума нескольких ИИ-моделей.
            Ниже — исходный запрос пользователя и ответы, которые независимо друг
            от друга дали на него несколько разных языковых моделей.

            Исходный запрос пользователя:
            "$originalPrompt"

            Ответы моделей:
            $opinionsBlock

            Твоя задача:
            1. Сравни ответы: в чём они согласны, а в чём расходятся.
            2. Если есть фактические противоречия — укажи их и, где можешь, определи
               более достоверный вариант.
            3. Объедини лучшее из всех ответов в один связный, точный и хорошо
               структурированный финальный ответ на исходный запрос пользователя.
            4. Не упоминай сам факт "консилиума" или "нескольких моделей" в финальном
               ответе — отвечай пользователю по существу, как будто это твой
               собственный полноценный ответ.
            5. Если модели существенно разошлись во мнениях по важному пункту,
               кратко отметь это как предостережение в конце ответа.
        """.trimIndent()
    }
}
