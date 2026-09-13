package com.aiconsilium.app.data.repository

import com.aiconsilium.app.data.model.ModelAnswer
import com.aiconsilium.app.data.model.ProviderConfig
import com.aiconsilium.app.data.remote.AiProviderClientFactory
import com.aiconsilium.app.domain.ConsensusEngine
import com.aiconsilium.app.domain.ConsiliumProgress
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withTimeoutOrNull

class ConsiliumRepository {

    fun runConsilium(
        prompt: String,
        providerConfigs: List<ProviderConfig>,
        arbiterConfig: ProviderConfig
    ): Flow<ConsiliumProgress> = channelFlow {
        coroutineScope {
            val deferredAnswers = providerConfigs.map { config ->
                async {
                    val client = AiProviderClientFactory.create(config)
                    val startedAt = System.currentTimeMillis()

                    val result = withTimeoutOrNull(PER_MODEL_TIMEOUT_MS) {
                        client.sendPrompt(prompt)
                    } ?: Result.failure(IllegalStateException("Модель не ответила за ${PER_MODEL_TIMEOUT_MS / 1000} сек."))

                    val elapsed = System.currentTimeMillis() - startedAt
                    val answer = result.fold(
                        onSuccess = { text -> ModelAnswer.Success(config.provider, text, elapsed) },
                        onFailure = { e -> ModelAnswer.Failure(config.provider, e.message ?: "Ошибка") }
                    )

                    send(ConsiliumProgress.AnswerUpdated(answer))
                    answer
                }
            }

            val allAnswers = deferredAnswers.awaitAll()

            val arbiterClient = AiProviderClientFactory.create(arbiterConfig)
            val synthesis = ConsensusEngine.synthesize(
                originalPrompt = prompt,
                answers = allAnswers,
                arbiterClient = arbiterClient,
                arbiterProvider = arbiterConfig.provider
            )
            send(ConsiliumProgress.SynthesisReady(synthesis))
        }
    }

    private companion object {
        const val PER_MODEL_TIMEOUT_MS = 60_000L
    }
}
