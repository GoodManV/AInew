package com.aiconsilium.app.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Общая сетевая настройка для всех провайдеров: один переиспользуемый
 * OkHttpClient (свой пул соединений/потоков на каждый Retrofit-инстанс был бы
 * расточительным) и фабрика Retrofit под конкретный base URL.
 */
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true // провайдеры добавляют новые поля в ответы чаще, чем мы обновляем DTO
        explicitNulls = false
    }

    private val jsonMediaType = "application/json".toMediaType()

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            // BODY удобен для отладки, но пишет в logcat промпты пользователя и текст
            // ответов моделей целиком. Для сборки, которая может попасть не только
            // разработчику, стоит понизить хотя бы до BASIC.
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS) // "думающие" модели-ризонеры могут отвечать долго
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    /** Создаёт Retrofit-клиент под конкретный base URL одного провайдера. */
    fun retrofitFor(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(jsonMediaType))
            .build()
}
