package com.nervesparks.iris.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.MasterKey
import com.nervesparks.iris.Template
import com.nervesparks.iris.data.repository.ModelConfiguration
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

private const val USER_PREFERENCES_NAME = "user_preferences"
private const val KEY_DEFAULT_MODEL_NAME = "default_model_name"
private const val KEY_HUGGINGFACE_TOKEN = "huggingface_token"
private const val KEY_HUGGINGFACE_USERNAME = "huggingface_username"
private const val KEY_GOOGLE_API_KEY = "google_api_key"
private const val KEY_GOOGLE_CSE_ID = "google_cse_id"
private const val KEY_MODEL_TEMPERATURE = "model_temperature"
private const val KEY_MODEL_TOP_P = "model_top_p"
private const val KEY_MODEL_TOP_K = "model_top_k"
private const val KEY_MODEL_MAX_TOKENS = "model_max_tokens"
private const val KEY_MODEL_CONTEXT_LENGTH = "model_context_length"
private const val KEY_MODEL_SYSTEM_PROMPT = "model_system_prompt"
private const val KEY_MODEL_CHAT_FORMAT = "model_chat_format"
private const val KEY_MODEL_THREAD_COUNT = "model_thread_count"
private const val KEY_MODEL_GPU_LAYERS = "model_gpu_layers"
private const val KEY_CACHED_MODELS = "cached_models"
private const val KEY_MODEL_CONFIG_PREFIX = "model_config_"
private const val KEY_SHOW_THINKING_TOKENS = "show_thinking_tokens"
private const val KEY_THINKING_TOKEN_STYLE = "thinking_token_style"
private const val KEY_TEMPLATES = "user_templates"
private const val KEY_UI_THEME = "ui_theme"
private const val KEY_UI_FONT_SIZE = "ui_font_size"
private const val KEY_UI_ENABLE_ANIMATIONS = "ui_enable_animations"
private const val KEY_UI_ENABLE_HAPTIC_FEEDBACK = "ui_enable_haptic_feedback"
private const val KEY_PERF_ENABLE_MEMORY_OPTIMIZATION = "perf_enable_memory_optimization"
private const val KEY_PERF_ENABLE_BACKGROUND_PROCESSING = "perf_enable_background_processing"
private const val KEY_SECURITY_BIOMETRIC_ENABLED = "security_biometric_enabled"

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_PREFERENCES_NAME
)

class EncryptionManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val secretKey: SecretKey

    init {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        secretKey = keyStore.getKey(masterKey.alias, null) as SecretKey
    }

    fun encrypt(text: String): String {
        if (text.isEmpty()) return ""
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(text.toByteArray())
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decrypt(data: String): String {
        if (data.isEmpty()) return ""
        val bytes = Base64.decode(data, Base64.DEFAULT)
        val iv = bytes.copyOfRange(0, 12)
        val encrypted = bytes.copyOfRange(12, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted)
    }
}

open class UserPreferencesRepository protected constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesRepository(context).also { INSTANCE = it }
            }
        }
    }

    private val dataStore = context.userPrefsDataStore
    private val encryptor = EncryptionManager(context)

    // Delegated preference helpers
    private fun stringPreference(key: String, default: String = "", encrypted: Boolean = false) =
        object : kotlin.properties.ReadWriteProperty<Any, String> {
            private val prefKey = stringPreferencesKey(key)
            override fun getValue(thisRef: Any, property: kotlin.reflect.KProperty<*>): String = runBlocking {
                val stored = dataStore.data.first()[prefKey] ?: default
                if (encrypted && stored.isNotEmpty()) encryptor.decrypt(stored) else stored
            }

            override fun setValue(thisRef: Any, property: kotlin.reflect.KProperty<*>, value: String) {
                runBlocking {
                    dataStore.edit { prefs ->
                        prefs[prefKey] = if (encrypted && value.isNotEmpty()) encryptor.encrypt(value) else value
                    }
                }
            }
        }

    private fun intPreference(key: String, default: Int = 0) =
        object : kotlin.properties.ReadWriteProperty<Any, Int> {
            private val prefKey = intPreferencesKey(key)
            override fun getValue(thisRef: Any, property: kotlin.reflect.KProperty<*>): Int = runBlocking {
                dataStore.data.first()[prefKey] ?: default
            }
            override fun setValue(thisRef: Any, property: kotlin.reflect.KProperty<*>, value: Int) {
                runBlocking { dataStore.edit { it[prefKey] = value } }
            }
        }

    private fun floatPreference(key: String, default: Float = 0f) =
        object : kotlin.properties.ReadWriteProperty<Any, Float> {
            private val prefKey = floatPreferencesKey(key)
            override fun getValue(thisRef: Any, property: kotlin.reflect.KProperty<*>): Float = runBlocking {
                dataStore.data.first()[prefKey] ?: default
            }
            override fun setValue(thisRef: Any, property: kotlin.reflect.KProperty<*>, value: Float) {
                runBlocking { dataStore.edit { it[prefKey] = value } }
            }
        }

    private fun booleanPreference(key: String, default: Boolean = false) =
        object : kotlin.properties.ReadWriteProperty<Any, Boolean> {
            private val prefKey = booleanPreferencesKey(key)
            override fun getValue(thisRef: Any, property: kotlin.reflect.KProperty<*>): Boolean = runBlocking {
                dataStore.data.first()[prefKey] ?: default
            }
            override fun setValue(thisRef: Any, property: kotlin.reflect.KProperty<*>, value: Boolean) {
                runBlocking { dataStore.edit { it[prefKey] = value } }
            }
        }

    // Preferences stored via delegates
    private var defaultModelName by stringPreference(KEY_DEFAULT_MODEL_NAME, encrypted = true)
    private var huggingFaceToken by stringPreference(KEY_HUGGINGFACE_TOKEN, encrypted = true)
    private var huggingFaceUsername by stringPreference(KEY_HUGGINGFACE_USERNAME, encrypted = true)
    private var googleApiKey by stringPreference(KEY_GOOGLE_API_KEY, encrypted = true)
    private var googleCseId by stringPreference(KEY_GOOGLE_CSE_ID, encrypted = true)
    private var modelTemperature by floatPreference(KEY_MODEL_TEMPERATURE, 0.7f)
    private var modelTopP by floatPreference(KEY_MODEL_TOP_P, 0.9f)
    private var modelTopK by intPreference(KEY_MODEL_TOP_K, 40)
    private var modelMaxTokens by intPreference(KEY_MODEL_MAX_TOKENS, 2048)
    private var modelContextLength by intPreference(KEY_MODEL_CONTEXT_LENGTH, 4096)
    private var modelSystemPrompt by stringPreference(KEY_MODEL_SYSTEM_PROMPT, "You are a helpful AI assistant.")
    private var modelChatFormat by stringPreference(KEY_MODEL_CHAT_FORMAT, "CHATML")
    private var modelThreadCount by intPreference(KEY_MODEL_THREAD_COUNT, 4)
    private var modelGpuLayers by intPreference(KEY_MODEL_GPU_LAYERS, -1)
    private var cachedModels by stringPreference(KEY_CACHED_MODELS, "")
    private var showThinkingTokens by booleanPreference(KEY_SHOW_THINKING_TOKENS, true)
    private var thinkingTokenStyle by stringPreference(KEY_THINKING_TOKEN_STYLE, "COLLAPSIBLE")
    private var templatesJson by stringPreference(KEY_TEMPLATES, "[]")
    private var uiTheme by stringPreference(KEY_UI_THEME, "DARK")
    private var uiFontSize by floatPreference(KEY_UI_FONT_SIZE, 1.0f)
    private var uiEnableAnimations by booleanPreference(KEY_UI_ENABLE_ANIMATIONS, true)
    private var uiEnableHapticFeedback by booleanPreference(KEY_UI_ENABLE_HAPTIC_FEEDBACK, true)
    private var perfEnableMemoryOptimization by booleanPreference(KEY_PERF_ENABLE_MEMORY_OPTIMIZATION, true)
    private var perfEnableBackgroundProcessing by booleanPreference(KEY_PERF_ENABLE_BACKGROUND_PROCESSING, true)
    private var securityBiometricEnabled by booleanPreference(KEY_SECURITY_BIOMETRIC_ENABLED, false)

    // Public API
    open fun getDefaultModelName(): String = defaultModelName
    open fun setDefaultModelName(modelName: String) { defaultModelName = modelName }

    open fun getHuggingFaceToken(): String = huggingFaceToken
    open fun setHuggingFaceToken(token: String) { huggingFaceToken = token }

    open fun getHuggingFaceUsername(): String = huggingFaceUsername
    open fun setHuggingFaceUsername(username: String) { huggingFaceUsername = username }

    open fun hasHuggingFaceCredentials(): Boolean {
        return getHuggingFaceToken().isNotEmpty() || getHuggingFaceUsername().isNotEmpty()
    }

    open fun setGoogleApiKey(key: String) { googleApiKey = key }
    open fun getGoogleApiKey(): String = googleApiKey
    open fun setGoogleCseId(id: String) { googleCseId = id }
    open fun getGoogleCseId(): String = googleCseId

    open fun setModelTemperature(temperature: Float) { modelTemperature = temperature }
    open fun getModelTemperature(): Float = modelTemperature
    open fun setModelTopP(topP: Float) { modelTopP = topP }
    open fun getModelTopP(): Float = modelTopP
    open fun setModelTopK(topK: Int) { modelTopK = topK }
    open fun getModelTopK(): Int = modelTopK
    open fun setModelMaxTokens(maxTokens: Int) { modelMaxTokens = maxTokens }
    open fun getModelMaxTokens(): Int = modelMaxTokens
    open fun setModelContextLength(contextLength: Int) { modelContextLength = contextLength }
    open fun getModelContextLength(): Int = modelContextLength
    open fun setModelSystemPrompt(systemPrompt: String) { modelSystemPrompt = systemPrompt }
    open fun getModelSystemPrompt(): String = modelSystemPrompt
    open fun setModelChatFormat(chatFormat: String) { modelChatFormat = chatFormat }
    open fun getModelChatFormat(): String = modelChatFormat
    open fun setModelThreadCount(threadCount: Int) { modelThreadCount = threadCount }
    open fun getModelThreadCount(): Int = modelThreadCount
    open fun setModelGpuLayers(layers: Int) { modelGpuLayers = layers }
    open fun getModelGpuLayers(): Int = modelGpuLayers

    open fun getModelConfiguration(modelName: String): ModelConfiguration = runBlocking {
        val prefix = "${KEY_MODEL_CONFIG_PREFIX}${modelName}_"
        val prefs = dataStore.data.first()
        ModelConfiguration(
            temperature = prefs[floatPreferencesKey(prefix + "temperature")] ?: 0.7f,
            topP = prefs[floatPreferencesKey(prefix + "top_p")] ?: 0.9f,
            topK = prefs[intPreferencesKey(prefix + "top_k")] ?: 40,
            threadCount = prefs[intPreferencesKey(prefix + "thread_count")] ?: 2,
            contextLength = prefs[intPreferencesKey(prefix + "context_length")] ?: 4096,
            systemPrompt = prefs[stringPreferencesKey(prefix + "system_prompt")] ?: "",
            gpuLayers = prefs[intPreferencesKey(prefix + "gpu_layers")] ?: -1
        )
    }

    open fun saveModelConfiguration(modelName: String, config: ModelConfiguration) {
        val prefix = "${KEY_MODEL_CONFIG_PREFIX}${modelName}_"
        runBlocking {
            dataStore.edit { prefs ->
                prefs[floatPreferencesKey(prefix + "temperature")] = config.temperature
                prefs[floatPreferencesKey(prefix + "top_p")] = config.topP
                prefs[intPreferencesKey(prefix + "top_k")] = config.topK
                prefs[intPreferencesKey(prefix + "thread_count")] = config.threadCount
                prefs[intPreferencesKey(prefix + "context_length")] = config.contextLength
                prefs[stringPreferencesKey(prefix + "system_prompt")] = config.systemPrompt
                prefs[intPreferencesKey(prefix + "gpu_layers")] = config.gpuLayers
            }
        }
    }

    open fun getCachedModels(): String = cachedModels
    open fun setCachedModels(json: String) { cachedModels = json }

    open fun setShowThinkingTokens(show: Boolean) { showThinkingTokens = show }
    open fun getShowThinkingTokens(): Boolean = showThinkingTokens
    open fun setThinkingTokenStyle(style: String) { thinkingTokenStyle = style }
    open fun getThinkingTokenStyle(): String = thinkingTokenStyle

    open fun setUITheme(theme: String) { uiTheme = theme }
    open fun getUITheme(): String = uiTheme
    open fun setUIFontSize(fontSize: Float) { uiFontSize = fontSize }
    open fun getUIFontSize(): Float = uiFontSize
    open fun setUIEnableAnimations(enable: Boolean) { uiEnableAnimations = enable }
    open fun getUIEnableAnimations(): Boolean = uiEnableAnimations
    open fun setUIEnableHapticFeedback(enable: Boolean) { uiEnableHapticFeedback = enable }
    open fun getUIEnableHapticFeedback(): Boolean = uiEnableHapticFeedback

    open fun setPerfEnableMemoryOptimization(enable: Boolean) { perfEnableMemoryOptimization = enable }
    open fun getPerfEnableMemoryOptimization(): Boolean = perfEnableMemoryOptimization
    open fun setPerfEnableBackgroundProcessing(enable: Boolean) { perfEnableBackgroundProcessing = enable }
    open fun getPerfEnableBackgroundProcessing(): Boolean = perfEnableBackgroundProcessing

    open fun setSecurityBiometricEnabled(enabled: Boolean) { securityBiometricEnabled = enabled }
    open fun getSecurityBiometricEnabled(): Boolean = securityBiometricEnabled

    open fun getTemplates(): List<Template> {
        val json = templatesJson
        return try {
            val array = JSONArray(json)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                Template(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    content = obj.getString("content")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    open fun saveTemplates(templates: List<Template>) {
        val array = JSONArray()
        templates.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            obj.put("content", t.content)
            array.put(obj)
        }
        templatesJson = array.toString()
    }

    open fun clearAll() {
        runBlocking { dataStore.edit { it.clear() } }
    }

    open fun exportConfiguration(): String {
        val jsonObject = JSONObject()
        jsonObject.put("defaultModelName", getDefaultModelName())
        jsonObject.put("huggingFaceToken", getHuggingFaceToken())
        jsonObject.put("huggingFaceUsername", getHuggingFaceUsername())
        jsonObject.put("modelTemperature", getModelTemperature())
        jsonObject.put("modelTopP", getModelTopP())
        jsonObject.put("modelTopK", getModelTopK())
        jsonObject.put("modelMaxTokens", getModelMaxTokens())
        jsonObject.put("modelContextLength", getModelContextLength())
        jsonObject.put("modelSystemPrompt", getModelSystemPrompt())
        jsonObject.put("modelChatFormat", getModelChatFormat())
        jsonObject.put("modelThreadCount", getModelThreadCount())
        jsonObject.put("showThinkingTokens", getShowThinkingTokens())
        jsonObject.put("thinkingTokenStyle", getThinkingTokenStyle())
        jsonObject.put("uiTheme", getUITheme())
        jsonObject.put("uiFontSize", getUIFontSize())
        jsonObject.put("uiEnableAnimations", getUIEnableAnimations())
        jsonObject.put("uiEnableHapticFeedback", getUIEnableHapticFeedback())
        jsonObject.put("perfEnableMemoryOptimization", getPerfEnableMemoryOptimization())
        jsonObject.put("perfEnableBackgroundProcessing", getPerfEnableBackgroundProcessing())
        return jsonObject.toString()
    }

    open fun importConfiguration(jsonString: String): Boolean {
        return try {
            val json = JSONObject(jsonString)
            if (json.has("defaultModelName")) setDefaultModelName(json.getString("defaultModelName"))
            if (json.has("huggingFaceToken")) setHuggingFaceToken(json.getString("huggingFaceToken"))
            if (json.has("huggingFaceUsername")) setHuggingFaceUsername(json.getString("huggingFaceUsername"))
            if (json.has("modelTemperature")) setModelTemperature(json.getDouble("modelTemperature").toFloat())
            if (json.has("modelTopP")) setModelTopP(json.getDouble("modelTopP").toFloat())
            if (json.has("modelTopK")) setModelTopK(json.getInt("modelTopK"))
            if (json.has("modelMaxTokens")) setModelMaxTokens(json.getInt("modelMaxTokens"))
            if (json.has("modelContextLength")) setModelContextLength(json.getInt("modelContextLength"))
            if (json.has("modelSystemPrompt")) setModelSystemPrompt(json.getString("modelSystemPrompt"))
            if (json.has("modelChatFormat")) setModelChatFormat(json.getString("modelChatFormat"))
            if (json.has("modelThreadCount")) setModelThreadCount(json.getInt("modelThreadCount"))
            if (json.has("showThinkingTokens")) setShowThinkingTokens(json.getBoolean("showThinkingTokens"))
            if (json.has("thinkingTokenStyle")) setThinkingTokenStyle(json.getString("thinkingTokenStyle"))
            if (json.has("uiTheme")) setUITheme(json.getString("uiTheme"))
            if (json.has("uiFontSize")) setUIFontSize(json.getDouble("uiFontSize").toFloat())
            if (json.has("uiEnableAnimations")) setUIEnableAnimations(json.getBoolean("uiEnableAnimations"))
            if (json.has("uiEnableHapticFeedback")) setUIEnableHapticFeedback(json.getBoolean("uiEnableHapticFeedback"))
            if (json.has("perfEnableMemoryOptimization")) setPerfEnableMemoryOptimization(json.getBoolean("perfEnableMemoryOptimization"))
            if (json.has("perfEnableBackgroundProcessing")) setPerfEnableBackgroundProcessing(json.getBoolean("perfEnableBackgroundProcessing"))
            true
        } catch (e: Exception) {
            android.util.Log.e("UserPreferencesRepository", "Error importing configuration", e)
            false
        }
    }
}
