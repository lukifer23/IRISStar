package com.nervesparks.iris.data

import timber.log.Timber
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
import com.nervesparks.iris.Template
import com.nervesparks.iris.data.repository.ModelConfiguration
import com.nervesparks.iris.security.EncryptedPreferences
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

// Removed EncryptionManager - using EncryptedPreferences instead

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
    private val encryptedPrefs = EncryptedPreferences(context)

    // Delegated preference helpers
    private fun stringPreference(key: String, default: String = "") =
        object : kotlin.properties.ReadWriteProperty<Any, String> {
            private val prefKey = stringPreferencesKey(key)
            override fun getValue(thisRef: Any, property: kotlin.reflect.KProperty<*>): String = runBlocking {
                dataStore.data.first()[prefKey] ?: default
            }

            override fun setValue(thisRef: Any, property: kotlin.reflect.KProperty<*>, value: String) {
                runBlocking {
                    dataStore.edit { prefs ->
                        prefs[prefKey] = value
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

    // Preferences stored via delegates - public for external access
    var defaultModelName by stringPreference(KEY_DEFAULT_MODEL_NAME)

    // Sensitive data stored in encrypted preferences
    var huggingFaceToken: String
        get() = encryptedPrefs.getEncryptedString(KEY_HUGGINGFACE_TOKEN)
        set(value) = encryptedPrefs.putEncryptedString(KEY_HUGGINGFACE_TOKEN, value)

    var huggingFaceUsername: String
        get() = encryptedPrefs.getEncryptedString(KEY_HUGGINGFACE_USERNAME)
        set(value) = encryptedPrefs.putEncryptedString(KEY_HUGGINGFACE_USERNAME, value)

    var googleApiKey: String
        get() = encryptedPrefs.getEncryptedString(KEY_GOOGLE_API_KEY)
        set(value) = encryptedPrefs.putEncryptedString(KEY_GOOGLE_API_KEY, value)

    var googleCseId: String
        get() = encryptedPrefs.getEncryptedString(KEY_GOOGLE_CSE_ID)
        set(value) = encryptedPrefs.putEncryptedString(KEY_GOOGLE_CSE_ID, value)
    var modelTemperature by floatPreference(KEY_MODEL_TEMPERATURE, 0.7f)
    var modelTopP by floatPreference(KEY_MODEL_TOP_P, 0.9f)
    var modelTopK by intPreference(KEY_MODEL_TOP_K, 40)
    var modelMaxTokens by intPreference(KEY_MODEL_MAX_TOKENS, 2048)
    var modelContextLength by intPreference(KEY_MODEL_CONTEXT_LENGTH, 4096)
    var modelSystemPrompt by stringPreference(KEY_MODEL_SYSTEM_PROMPT, "You are a helpful AI assistant.")
    var modelChatFormat by stringPreference(KEY_MODEL_CHAT_FORMAT, "CHATML")
    var modelThreadCount by intPreference(KEY_MODEL_THREAD_COUNT, 4)
    var modelGpuLayers by intPreference(KEY_MODEL_GPU_LAYERS, -1)
    var cachedModels by stringPreference(KEY_CACHED_MODELS, "")
    var showThinkingTokens by booleanPreference(KEY_SHOW_THINKING_TOKENS, true)
    var thinkingTokenStyle by stringPreference(KEY_THINKING_TOKEN_STYLE, "COLLAPSIBLE")
    var templatesJson by stringPreference(KEY_TEMPLATES, "[]")
    var uiTheme by stringPreference(KEY_UI_THEME, "DARK")
    var uiFontSize by floatPreference(KEY_UI_FONT_SIZE, 1.0f)
    var uiEnableAnimations by booleanPreference(KEY_UI_ENABLE_ANIMATIONS, true)
    var uiEnableHapticFeedback by booleanPreference(KEY_UI_ENABLE_HAPTIC_FEEDBACK, true)
    var perfEnableMemoryOptimization by booleanPreference(KEY_PERF_ENABLE_MEMORY_OPTIMIZATION, true)
    var perfEnableBackgroundProcessing by booleanPreference(KEY_PERF_ENABLE_BACKGROUND_PROCESSING, true)
    var securityBiometricEnabled by booleanPreference(KEY_SECURITY_BIOMETRIC_ENABLED, false)

    // Public API - using delegated properties directly
    open fun hasHuggingFaceCredentials(): Boolean {
        return huggingFaceToken.isNotEmpty() || huggingFaceUsername.isNotEmpty()
    }

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

    // All other properties are accessible via delegated properties

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
        jsonObject.put("defaultModelName", defaultModelName)
        jsonObject.put("huggingFaceToken", huggingFaceToken)
        jsonObject.put("huggingFaceUsername", huggingFaceUsername)
        jsonObject.put("modelTemperature", modelTemperature)
        jsonObject.put("modelTopP", modelTopP)
        jsonObject.put("modelTopK", modelTopK)
        jsonObject.put("modelMaxTokens", modelMaxTokens)
        jsonObject.put("modelContextLength", modelContextLength)
        jsonObject.put("modelSystemPrompt", modelSystemPrompt)
        jsonObject.put("modelChatFormat", modelChatFormat)
        jsonObject.put("modelThreadCount", modelThreadCount)
        jsonObject.put("showThinkingTokens", showThinkingTokens)
        jsonObject.put("thinkingTokenStyle", thinkingTokenStyle)
        jsonObject.put("uiTheme", uiTheme)
        jsonObject.put("uiFontSize", uiFontSize)
        jsonObject.put("uiEnableAnimations", uiEnableAnimations)
        jsonObject.put("uiEnableHapticFeedback", uiEnableHapticFeedback)
        jsonObject.put("perfEnableMemoryOptimization", perfEnableMemoryOptimization)
        jsonObject.put("perfEnableBackgroundProcessing", perfEnableBackgroundProcessing)
        return jsonObject.toString()
    }

    open fun importConfiguration(jsonString: String): Boolean {
        return try {
            val json = JSONObject(jsonString)
            if (json.has("defaultModelName")) defaultModelName = json.getString("defaultModelName")
            if (json.has("huggingFaceToken")) huggingFaceToken = json.getString("huggingFaceToken")
            if (json.has("huggingFaceUsername")) huggingFaceUsername = json.getString("huggingFaceUsername")
            if (json.has("modelTemperature")) modelTemperature = json.getDouble("modelTemperature").toFloat()
            if (json.has("modelTopP")) modelTopP = json.getDouble("modelTopP").toFloat()
            if (json.has("modelTopK")) modelTopK = json.getInt("modelTopK")
            if (json.has("modelMaxTokens")) modelMaxTokens = json.getInt("modelMaxTokens")
            if (json.has("modelContextLength")) modelContextLength = json.getInt("modelContextLength")
            if (json.has("modelSystemPrompt")) modelSystemPrompt = json.getString("modelSystemPrompt")
            if (json.has("modelChatFormat")) modelChatFormat = json.getString("modelChatFormat")
            if (json.has("modelThreadCount")) modelThreadCount = json.getInt("modelThreadCount")
            if (json.has("showThinkingTokens")) showThinkingTokens = json.getBoolean("showThinkingTokens")
            if (json.has("thinkingTokenStyle")) thinkingTokenStyle = json.getString("thinkingTokenStyle")
            if (json.has("uiTheme")) uiTheme = json.getString("uiTheme")
            if (json.has("uiFontSize")) uiFontSize = json.getDouble("uiFontSize").toFloat()
            if (json.has("uiEnableAnimations")) uiEnableAnimations = json.getBoolean("uiEnableAnimations")
            if (json.has("uiEnableHapticFeedback")) uiEnableHapticFeedback = json.getBoolean("uiEnableHapticFeedback")
            if (json.has("perfEnableMemoryOptimization")) perfEnableMemoryOptimization = json.getBoolean("perfEnableMemoryOptimization")
            if (json.has("perfEnableBackgroundProcessing")) perfEnableBackgroundProcessing = json.getBoolean("perfEnableBackgroundProcessing")
            true
        } catch (e: Exception) {
            Timber.tag("UserPreferencesRepository").e(e, "Error importing configuration")
            false
        }
    }
}
