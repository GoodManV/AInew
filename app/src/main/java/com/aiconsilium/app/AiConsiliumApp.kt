package com.aiconsilium.app

import android.app.Application
import com.aiconsilium.app.data.local.SecureSettingsStore

/**
 * Простой ручной DI-контейнер вместо Hilt/Koin: для приложения такого размера
 * это меньше "магии" и проще читать целиком. Если проект вырастет —
 * стоит перейти на Hilt.
 */
class AiConsiliumApp : Application() {

    lateinit var settingsStore: SecureSettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = SecureSettingsStore(applicationContext)
    }
}
