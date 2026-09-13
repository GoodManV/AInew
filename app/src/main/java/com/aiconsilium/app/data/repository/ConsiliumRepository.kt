package com.aiconsilium.app.data.repository

import com.aiconsilium.app.data.model.ModelAnswer
import com.aiconsilium.app.data.model.ProviderConfig
import com.aiconsilium.app.data.remote.AiProviderClientFactory
import com.aiconsilium.app.domain.ConsensusEngine
import com.aiconsilium.app.domain.ConsiliumProgress
import kotlinx.coroutines.async
import kotlinx.coroutines.channelFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Точка входа для "созыва консилиума": параллельно опрашивает все включённые
 * модели, затем просит арбитра синтезировать финальный ответ.
 *
 * Прогресс отдаётся через Flow, а не единым результатом в конце: так UI может
 * показать ответ каждой модели сразу по готовности, не дожидаясь остальных —
 * это и есть требование "прозрачности работы" из ТЗ.
 */
class ConsiliumRepository {

    fun runConsilium(
        prompt: String,
        providerConfigs: List<ProviderConfig>,
        arbiterConfig: ProviderConfig
    ): Flow<ConsiliumProgress> = channelFlow {
        // 1. Запускаем запрос к каждой включённой модели ПАРАЛЛЕЛЬНО.
        //    async (не launch), потому что каждой корутине нужно вернуть
        //    итоговый ModelAnswer — он ещё понадобится ниже для арбитра.
        val deferredAnswers = providerConfigs.map { config ->
            async {
                val client = AiProviderClientFactory.create(config)
                val startedAt = System.currentTimeMillis()

                // Два уровня тайм-аута с разным смыслом:
                //  - OkHttp readTimeout (см. NetworkModule) — сетевой уровень,
                //    ловит зависшее TCP-соединение;
                //  - withTimeoutOrNull здесь — уровень приложения, даёт
                //    предсказуемое время ожидания и дружелюбное сообщение
                //    вместо сырого исключения сокета.
                val result = withTimeoutOrNull(PER_MODEL_TIMEOUT_MS) { client.sendPrompt(prompt) }
                    ?: Result.failure(IllegalStateException("Модель не ответила за ${PER_MODEL_TIMEOUT_MS / 1000} сек."))

                val elapsed = System.currentTimeMillis() - startedAt
                val answer = result.fold(
                    onSuccess = { text -> ModelAnswer.Success(config.provider, text, elapsed) },
                    onFailure = { e -> ModelAnswer.Failure(config.provider, e.message ?: "Ошибка") }
                )

                // Отправляем в UI сразу, как только ЭТА модель ответила — остальные
                // при этом продолжают работать параллельно, никто никого не блокирует.
                send(ConsiliumProgress.AnswerUpdated(answer))
                answer
            }
        }

        // 2. Дожидаемся всех ответов (успешных или нет) — ошибка одной модели
        //    не отменяет соседей, так как исключения уже превращены в
        //    ModelAnswer.Failure внутри блока выше, а не выброшены наружу.
        val allAnswers = deferredAnswers.map { it.await() }

        // 3. Просим арбитра свести ответы воедино.
        val arbiterClient = AiProviderClientFactory.create(arbiterConfig)
        val synthesis = ConsensusEngine.synthesize(
            originalPrompt = prompt,
            answers = allAnswers,
            arbiterClient = arbiterClient,
            arbiterProvider = arbiterConfig.provider
        )
        send(ConsiliumProgress.SynthesisReady(synthesis))

        // Специально НЕ вызываем awaitClose здесь: это требование callbackFlow
        // (для оборачивания callback-based API), а не обычного channelFlow.
        // Как только этот блок завершится — а все дочерние корутины (async
        // выше) уже awaited — Flow корректно и сразу завершится сам.
    }

    private companion object {
        const val PER_MODEL_TIMEOUT_MS = 60_000L
    }
}
