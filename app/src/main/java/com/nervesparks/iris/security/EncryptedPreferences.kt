package com.nervesparks.iris.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nervesparks.iris.data.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PHASE 2.2: Encrypted Preferences - Secure storage for sensitive data
 * Provides encrypted storage for API keys, tokens, and other sensitive data
 */
@Singleton
class EncryptedPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREFS_FILE_NAME = "iris_encrypted_prefs"
        private const val MASTER_KEY_ALIAS = "iris_master_key"
    }

    private val masterKey = MasterKey.Builder(context, MASTER_KEY_ALIAS)
        .setKeyGenParameterSpec(
            KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Secure storage for sensitive data
    fun getEncryptedString(key: String, defaultValue: String = ""): String {
        return encryptedPrefs.getString(key, defaultValue) ?: defaultValue
    }

    fun putEncryptedString(key: String, value: String) {
        encryptedPrefs.edit().putString(key, value).apply()
    }

    fun removeEncryptedString(key: String) {
        encryptedPrefs.edit().remove(key).apply()
    }

    // Clear all encrypted data
    fun clearAllEncryptedData() {
        encryptedPrefs.edit().clear().apply()
    }

    // Check if encrypted data exists
    fun hasEncryptedData(key: String): Boolean {
        return encryptedPrefs.contains(key)
    }

    // Get all encrypted keys (for debugging only)
    fun getAllEncryptedKeys(): Set<String> {
        return encryptedPrefs.all.keys
    }
}
