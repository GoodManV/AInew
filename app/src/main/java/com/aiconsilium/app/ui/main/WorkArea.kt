package com.aiconsilium.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiconsilium.app.data.model.ConsiliumResult
import com.aiconsilium.app.data.model.ModelAnswer
import com.aiconsilium.app.ui.theme.providerAccentColor

private val FEED_MAX_WIDTH = 760.dp

@Composable
fun WorkArea(
    modifier: Modifier = Modifier,
    state: MainUiState,
    onPromptChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(modifier = modifier.padding(16.dp)) {
        PromptComposer(
            promptText = state.promptText,
            canSubmit = state.canSubmit,
            isSubmitting = state.isSubmitting,
            onPromptChanged = onPromptChanged,
            onSubmit = onSubmit
        )

        Spacer(Modifier.height(16.dp))

        // weight(1f), не fillMaxSize(): в Column последнее нужно, чтобы явно
        // забрать именно ОСТАВШЕЕСЯ место под лентой, а не полную высоту
        // колонки поверх уже занятого полем ввода — иначе лента будет
        // претендовать на высоту больше доступной и обрежется.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (state.history.isEmpty()) {
                EmptyFeedHint()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = FEED_MAX_WIDTH)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(state.history.reversed(), key = { it.id }) { result ->
                        ConsiliumResultCard(result)
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptComposer(
    promptText: String,
    canSubmit: Boolean,
    isSubmitting: Boolean,
    onPromptChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = promptText,
            onValueChange = onPromptChanged,
            modifier = Modifier.weight(1f),
            label = { Text("Запрос консилиуму") },
            placeholder = { Text("Например: сравни ипотеку под 12% и 14% на 20 лет…") },
            minLines = 2,
            maxLines = 6
        )
        Spacer(Modifier.width(12.dp))
        Button(onClick = onSubmit, enabled = canSubmit) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Спросить")
            }
        }
    }
}

@Composable
private fun EmptyFeedHint() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Пока пусто", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Включите хотя бы одну модель слева, введите вопрос и нажмите «Спросить».",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConsiliumResultCard(result: ConsiliumResult) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(result.prompt, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            FinalAnswerSection(result)
            Spacer(Modifier.height(12.dp))
            RawAnswersSection(result.answers)
        }
    }
}

@Composable
private fun FinalAnswerSection(result: ConsiliumResult) {
    when (val synthesis = result.finalSynthesis) {
        null -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                "Арбитр (${result.arbiterProvider.displayName}) формирует итог…",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        is ModelAnswer.Success -> Column {
            Text(
                "Согласованный ответ · ${result.arbiterProvider.displayName}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(synthesis.text, style = MaterialTheme.typography.bodyLarge)
        }
        is ModelAnswer.Failure -> Text(
            "Не удалось получить согласованный ответ: ${synthesis.message}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        // Для finalSynthesis не создаётся отдельное состояние Loading — пока
        // арбитр работает, поле просто равно null (см. ветку null выше),
        // поэтому эта ветка недостижима, но нужна для исчерпывающего when.
        is ModelAnswer.Loading -> Unit
    }
}

@Composable
private fun RawAnswersSection(answers: List<ModelAnswer>) {
    var expanded by remember { mutableStateOf(false) }

    TextButton(onClick = { expanded = !expanded }) {
        Text(if (expanded) "Скрыть мнения моделей" else "Показать мнения всех моделей (${answers.size})")
    }

    if (expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            answers.forEach { answer -> ModelAnswerRow(answer) }
        }
    }
}

@Composable
private fun ModelAnswerRow(answer: ModelAnswer) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(providerAccentColor(answer.provider))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(answer.provider.displayName, style = MaterialTheme.typography.labelLarge)
                if (answer is ModelAnswer.Success) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "· ${answer.latencyMs} мс",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            when (answer) {
                is ModelAnswer.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Ожидаем ответ…", style = MaterialTheme.typography.bodySmall)
                }
                is ModelAnswer.Success -> Text(answer.text, style = MaterialTheme.typography.bodyMedium)
                is ModelAnswer.Failure -> Text(
                    "Ошибка: ${answer.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
