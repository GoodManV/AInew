// Файл верхнего уровня: здесь только объявляются плагины (через alias из
// gradle/libs.versions.toml), сама конфигурация модуля — в app/build.gradle.kts.
//
// ВАЖНО про Kotlin на AGP 9+:
// начиная с Android Gradle Plugin 9.0 (январь 2026) поддержка Kotlin встроена
// в сам AGP ("built-in Kotlin"), поэтому здесь и в app/build.gradle.kts
// сознательно НЕ подключается отдельный плагин org.jetbrains.kotlin.android —
// с AGP 9+ это приводит к ошибке сборки ("The 'org.jetbrains.kotlin.android'
// plugin is no longer required for Kotlin support since AGP 9.0").
// Компилятор Jetpack Compose и kotlinx.serialization — это отдельные
// компиляторные плагины, built-in Kotlin их не заменяет, поэтому они
// подключены как обычно.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
