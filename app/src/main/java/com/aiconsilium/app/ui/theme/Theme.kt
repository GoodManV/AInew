package com.aiconsilium.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aiconsilium.app.data.model.AIProvider

/*
 * Палитра "Зал заседаний": чернильно-синий фон + приглушённое золото-латунь
 * для акцентов арбитра. Осознанно НЕ берём дефолтную Material-фиолетовую
 * схему и не используем связку "тёплый кремовый фон + терракотовый акцент" —
 * это, наравне со связкой "чёрный фон + один кислотный акцент", один из
 * самых узнаваемых штампов авто-сгенерированного дизайна.
 */

private val InkBackground = Color(0xFF12141C)
private val InkSurface = Color(0xFF1B1E2A)
private val InkSurfaceVariant = Color(0xFF262B3B)
private val Parchment = Color(0xFFEDEAE2)
private val MutedParchment = Color(0xFFA7ABBA)
private val Brass = Color(0xFFC6A15B)
private val BrassDark = Color(0xFF8A6A2F)
private val SlateBlue = Color(0xFF7C93B8)
private val ErrorRedDark = Color(0xFFCF6679)

private val PaperBackground = Color(0xFFF4F5F7)
private val PaperSurface = Color(0xFFFFFFFF)
private val PaperSurfaceVariant = Color(0xFFE4E6EB)
private val InkText = Color(0xFF1B1E2A)
private val MutedInkText = Color(0xFF585C6B)
private val ErrorRedLight = Color(0xFFB3261E)

private val DarkColors = darkColorScheme(
    primary = Brass,
    onPrimary = Color(0xFF1B1E2A),
    secondary = SlateBlue,
    onSecondary = Color(0xFF12141C),
    background = InkBackground,
    onBackground = Parchment,
    surface = InkSurface,
    onSurface = Parchment,
    surfaceVariant = InkSurfaceVariant,
    onSurfaceVariant = MutedParchment,
    error = ErrorRedDark,
    onError = Color(0xFF12141C)
)

private val LightColors = lightColorScheme(
    primary = BrassDark,
    onPrimary = Color.White,
    secondary = SlateBlue,
    onSecondary = Color.White,
    background = PaperBackground,
    onBackground = InkText,
    surface = PaperSurface,
    onSurface = InkText,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = MutedInkText,
    error = ErrorRedLight,
    onError = Color.White
)

/**
 * Фирменные акценты провайдеров — используются ТОЛЬКО как тонкая цветовая
 * маркировка (например, левая полоска карточки ответа), не как заливка фона.
 */
fun providerAccentColor(provider: AIProvider): Color = when (provider) {
    AIProvider.ANTHROPIC -> Color(0xFFC77B5B)
    AIProvider.OPENAI -> Color(0xFF3FA796)
    AIProvider.GEMINI -> Color(0xFF6C8EBF)
    AIProvider.DEEPSEEK -> Color(0xFF8B6DAE)
    AIProvider.MISTRAL -> Color(0xFFD98C3D)
}

private val AppTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 14.sp)
)

@Composable
fun AiConsiliumTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
