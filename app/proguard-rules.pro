# kotlinx.serialization: сохраняем сгенерированные сериализаторы наших DTO.
# Без этих правил R8 в релизной сборке может удалить/переименовать классы,
# нужные сериализатору для (де)сериализации JSON моделей провайдеров.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.aiconsilium.app.**$$serializer { *; }
-keepclassmembers class com.aiconsilium.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.aiconsilium.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp / Retrofit / Coroutines публикуют собственные consumer-правила,
# но платформенные предупреждения безопасно отключить явно.
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn kotlinx.coroutines.**
