package com.aiconsilium.app.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Шифрование строк ключом, который физически никогда не покидает
 * Android Keystore (AES-256-GCM, аппаратно поддержанный на большинстве устройств).
 *
 * Почему не androidx.security:security-crypto/EncryptedSharedPreferences:
 * начиная с версии 1.1.0-alpha07 (апрель 2025) Google пометила
 * EncryptedSharedPreferences как deprecated из-за проблем производительности
 * (синхронное шифрование на потоке вызова) и надёжности ключей на части
 * прошивок OEM. Официально рекомендованная замена — Jetpack DataStore
 * + шифрование через сам Keystore/Tink. Здесь сделан тот же принцип напрямую
 * через javax.crypto и AndroidKeyStore, без лишней зависимости от Tink —
 * для объёма данных этого приложения (несколько коротких строк-ключей)
 * этого вполне достаточно. Хранилище на основе DataStore — в SecureSettingsStore.kt.
 *
 * Формат сохраняемой строки: Base64( IV(12 байт) || шифротекст+тег GCM ).
 */
object KeystoreCipher {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "ai_consilium_master_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    /** Возвращает пустую строку для пустого ввода — не шифруем то, чего нет. */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + cipherBytes, Base64.NO_WRAP)
    }

    /**
     * При любой проблеме (повреждённые данные, инвалидированный ключ после
     * сброса блокировки экрана на некоторых прошивках и т.п.) тихо возвращает
     * пустую строку вместо падения приложения — пользователь просто увидит
     * пустое поле API-ключа и сможет ввести его заново.
     */
    fun decrypt(storedValue: String): String {
        if (storedValue.isEmpty()) return ""
        return try {
            val combined = Base64.decode(storedValue, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
            val cipherBytes = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            }
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
