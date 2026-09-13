plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.aiconsilium.app"
    // compileSdk 37 — минимум, требуемый Jetpack Compose 1.12 (август 2026).
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aiconsilium.app"
        minSdk = 26
        // targetSdk держим равным compileSdk — актуальные требования Google Play
        // по целевому уровню API меняются ежегодно, перед публикацией сверьтесь
        // с текущим порогом Play Console.
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Примечание: начиная с AGP 9, built-in Kotlin автоматически берёт jvmTarget
    // из compileOptions.targetCompatibility выше, поэтому отдельный блок
    // kotlin { compilerOptions { jvmTarget = ... } } здесь не нужен.

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM управляет версиями всех Compose-артефактов ниже разом
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended) // иконки "показать/скрыть API-ключ"
    implementation(libs.androidx.foundation)

    // Хранение настроек (см. data/local/SecureSettingsStore.kt)
    implementation(libs.androidx.datastore.preferences)

    // Сеть: единый Retrofit + kotlinx.serialization для всех провайдеров
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
}
