package com.nervesparks.iris.data

import android.content.Context
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

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

    private object PreferenceKeys {
        val defaultModelName = stringPreferencesKey(KEY_DEFAULT_MODEL_NAME)
        val modelTemperature = floatPreferencesKey(KEY_MODEL_TEMPERATURE)
        val modelTopP = floatPreferencesKey(KEY_MODEL_TOP_P)
        val modelTopK = intPreferencesKey(KEY_MODEL_TOP_K)
        val modelMaxTokens = intPreferencesKey(KEY_MODEL_MAX_TOKENS)
        val modelContextLength = intPreferencesKey(KEY_MODEL_CONTEXT_LENGTH)
        val modelSystemPrompt = stringPreferencesKey(KEY_MODEL_SYSTEM_PROMPT)
        val modelChatFormat = stringPreferencesKey(KEY_MODEL_CHAT_FORMAT)
        val modelThreadCount = intPreferencesKey(KEY_MODEL_THREAD_COUNT)
        val modelGpuLayers = intPreferencesKey(KEY_MODEL_GPU_LAYERS)
        val cachedModels = stringPreferencesKey(KEY_CACHED_MODELS)
        val showThinkingTokens = booleanPreferencesKey(KEY_SHOW_THINKING_TOKENS)
        val thinkingTokenStyle = stringPreferencesKey(KEY_THINKING_TOKEN_STYLE)
        val templates = stringPreferencesKey(KEY_TEMPLATES)
        val uiTheme = stringPreferencesKey(KEY_UI_THEME)
        val uiFontSize = floatPreferencesKey(KEY_UI_FONT_SIZE)
        val uiEnableAnimations = booleanPreferencesKey(KEY_UI_ENABLE_ANIMATIONS)
        val uiEnableHapticFeedback = booleanPreferencesKey(KEY_UI_ENABLE_HAPTIC_FEEDBACK)
        val perfEnableMemoryOptimization = booleanPreferencesKey(KEY_PERF_ENABLE_MEMORY_OPTIMIZATION)
        val perfEnableBackgroundProcessing = booleanPreferencesKey(KEY_PERF_ENABLE_BACKGROUND_PROCESSING)
        val securityBiometricEnabled = booleanPreferencesKey(KEY_SECURITY_BIOMETRIC_ENABLED)
    }

    private fun <T> preferenceFlow(key: Preferences.Key<T>, default: T): Flow<T> {
        return dataStore.data
            .map { prefs -> prefs[key] ?: default }
            .distinctUntilChanged()
    }

    private suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    // region Preference accessors
    open val defaultModelNameFlow: Flow<String> =
        preferenceFlow(PreferenceKeys.defaultModelName, "")

    open suspend fun getDefaultModelName(): String = defaultModelNameFlow.first()

    open suspend fun setDefaultModelName(modelName: String) {
        setPreference(PreferenceKeys.defaultModelName, modelName)
    }

    open val modelTemperatureFlow: Flow<Float> =
        preferenceFlow(PreferenceKeys.modelTemperature, 0.7f)

    open suspend fun getModelTemperature(): Float = modelTemperatureFlow.first()

    open suspend fun setModelTemperature(temperature: Float) {
        setPreference(PreferenceKeys.modelTemperature, temperature)
    }

    open val modelTopPFlow: Flow<Float> =
        preferenceFlow(PreferenceKeys.modelTopP, 0.9f)

    open suspend fun getModelTopP(): Float = modelTopPFlow.first()

    open suspend fun setModelTopP(value: Float) {
        setPreference(PreferenceKeys.modelTopP, value)
    }

    open val modelTopKFlow: Flow<Int> =
        preferenceFlow(PreferenceKeys.modelTopK, 40)

    open suspend fun getModelTopK(): Int = modelTopKFlow.first()

    open suspend fun setModelTopK(value: Int) {
        setPreference(PreferenceKeys.modelTopK, value)
    }

    open val modelMaxTokensFlow: Flow<Int> =
        preferenceFlow(PreferenceKeys.modelMaxTokens, 2048)

    open suspend fun getModelMaxTokens(): Int = modelMaxTokensFlow.first()

    open suspend fun setModelMaxTokens(value: Int) {
        setPreference(PreferenceKeys.modelMaxTokens, value)
    }

    open val modelContextLengthFlow: Flow<Int> =
        preferenceFlow(PreferenceKeys.modelContextLength, 4096)

    open suspend fun getModelContextLength(): Int = modelContextLengthFlow.first()

    open suspend fun setModelContextLength(value: Int) {
        setPreference(PreferenceKeys.modelContextLength, value)
    }

    open val modelSystemPromptFlow: Flow<String> =
        preferenceFlow(PreferenceKeys.modelSystemPrompt, "You are a helpful AI assistant.")

    open suspend fun getModelSystemPrompt(): String = modelSystemPromptFlow.first()

    open suspend fun setModelSystemPrompt(value: String) {
        setPreference(PreferenceKeys.modelSystemPrompt, value)
    }

    open val modelChatFormatFlow: Flow<String> =
        preferenceFlow(PreferenceKeys.modelChatFormat, "CHATML")

    open suspend fun getModelChatFormat(): String = modelChatFormatFlow.first()

    open suspend fun setModelChatFormat(value: String) {
        setPreference(PreferenceKeys.modelChatFormat, value)
    }

    open val modelThreadCountFlow: Flow<Int> =
        preferenceFlow(PreferenceKeys.modelThreadCount, 4)

    open suspend fun getModelThreadCount(): Int = modelThreadCountFlow.first()

    open suspend fun setModelThreadCount(value: Int) {
        setPreference(PreferenceKeys.modelThreadCount, value)
    }

    open val modelGpuLayersFlow: Flow<Int> =
        preferenceFlow(PreferenceKeys.modelGpuLayers, -1)

    open suspend fun getModelGpuLayers(): Int = modelGpuLayersFlow.first()

    open suspend fun setModelGpuLayers(value: Int) {
        setPreference(PreferenceKeys.modelGpuLayers, value)
    }

    open val cachedModelsFlow: Flow<String> =
        preferenceFlow(PreferenceKeys.cachedModels, "")

    open suspend fun getCachedModels(): String = cachedModelsFlow.first()

    open suspend fun setCachedModels(json: String) {
        setPreference(PreferenceKeys.cachedModels, json)
    }

    open val showThinkingTokensFlow: Flow<Boolean> =
        preferenceFlow(PreferenceKeys.showThinkingTokens, true)

    open suspend fun getShowThinkingTokens(): Boolean = showThinkingTokensFlow.first()

    open suspend fun setShowThinkingTokens(value: Boolean) {
        setPreference(PreferenceKeys.showThinkingTokens, value)
    }

    open val thinkingTokenStyleFlow: Flow<String> =
        preferenceFlow(PreferenceKeys.thinkingTokenStyle, "COLLAPSIBLE")

    open suspend fun getThinkingTokenStyle(): String = thinkingTokenStyleFlow.first()

    open suspend fun setThinkingTokenStyle(value: String) {
        setPreference(PreferenceKeys.thinkingTokenStyle, value)
    }

    private val templatesJsonFlow: Flow<String> =
        preferenceFlow(PreferenceKeys.templates, "[]")

    open val templatesFlow: Flow<List<Template>> = templatesJsonFlow
        .map { json -> parseTemplates(json) }
        .distinctUntilChanged()

    open suspend fun getTemplates(): List<Template> = templatesFlow.first()

    open suspend fun saveTemplates(templates: List<Template>) {
        val array = JSONArray()
        templates.forEach { template ->
            val obj = JSONObject()
            obj.put("id", template.id)
            obj.put("name", template.name)
            obj.put("content", template.content)
            array.put(obj)
        }
        setPreference(PreferenceKeys.templates, array.toString())
    }

    open val uiThemeFlow: Flow<String> =
        preferenceFlow(PreferenceKeys.uiTheme, "DARK")

    open suspend fun getUITheme(): String = uiThemeFlow.first()

    open suspend fun setUITheme(value: String) {
        setPreference(PreferenceKeys.uiTheme, value)
    }

    open val uiFontSizeFlow: Flow<Float> =
        preferenceFlow(PreferenceKeys.uiFontSize, 1.0f)

    open suspend fun getUIFontSize(): Float = uiFontSizeFlow.first()

    open suspend fun setUIFontSize(value: Float) {
        setPreference(PreferenceKeys.uiFontSize, value)
    }

    open val uiEnableAnimationsFlow: Flow<Boolean> =
        preferenceFlow(PreferenceKeys.uiEnableAnimations, true)

    open suspend fun getUIEnableAnimations(): Boolean = uiEnableAnimationsFlow.first()

    open suspend fun setUIEnableAnimations(value: Boolean) {
        setPreference(PreferenceKeys.uiEnableAnimations, value)
    }

    open val uiEnableHapticFeedbackFlow: Flow<Boolean> =
        preferenceFlow(PreferenceKeys.uiEnableHapticFeedback, true)

    open suspend fun getUIEnableHapticFeedback(): Boolean =
        uiEnableHapticFeedbackFlow.first()

    open suspend fun setUIEnableHapticFeedback(value: Boolean) {
        setPreference(PreferenceKeys.uiEnableHapticFeedback, value)
    }

    open val perfEnableMemoryOptimizationFlow: Flow<Boolean> =
        preferenceFlow(PreferenceKeys.perfEnableMemoryOptimization, true)

    open suspend fun getPerfEnableMemoryOptimization(): Boolean =
        perfEnableMemoryOptimizationFlow.first()

    open suspend fun setPerfEnableMemoryOptimization(value: Boolean) {
        setPreference(PreferenceKeys.perfEnableMemoryOptimization, value)
    }

    open val perfEnableBackgroundProcessingFlow: Flow<Boolean> =
        preferenceFlow(PreferenceKeys.perfEnableBackgroundProcessing, true)

    open suspend fun getPerfEnableBackgroundProcessing(): Boolean =
        perfEnableBackgroundProcessingFlow.first()

    open suspend fun setPerfEnableBackgroundProcessing(value: Boolean) {
        setPreference(PreferenceKeys.perfEnableBackgroundProcessing, value)
    }

    open val securityBiometricEnabledFlow: Flow<Boolean> =
        preferenceFlow(PreferenceKeys.securityBiometricEnabled, false)

    open suspend fun getSecurityBiometricEnabled(): Boolean =
        securityBiometricEnabledFlow.first()

    open suspend fun setSecurityBiometricEnabled(value: Boolean) {
        setPreference(PreferenceKeys.securityBiometricEnabled, value)
    }
    // endregion

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

    open fun hasHuggingFaceCredentials(): Boolean {
        return huggingFaceToken.isNotEmpty() || huggingFaceUsername.isNotEmpty()
    }

    open suspend fun getModelConfiguration(modelName: String): ModelConfiguration {
        val prefix = "${KEY_MODEL_CONFIG_PREFIX}${modelName}_"
        val prefs = dataStore.data.first()
        return ModelConfiguration(
            temperature = prefs[floatPreferencesKey(prefix + "temperature")] ?: 0.7f,
            topP = prefs[floatPreferencesKey(prefix + "top_p")] ?: 0.9f,
            topK = prefs[intPreferencesKey(prefix + "top_k")] ?: 40,
            threadCount = prefs[intPreferencesKey(prefix + "thread_count")] ?: 2,
            contextLength = prefs[intPreferencesKey(prefix + "context_length")] ?: 4096,
            systemPrompt = prefs[stringPreferencesKey(prefix + "system_prompt")] ?: "",
            gpuLayers = prefs[intPreferencesKey(prefix + "gpu_layers")] ?: -1
        )
    }

    open suspend fun saveModelConfiguration(modelName: String, config: ModelConfiguration) {
        val prefix = "${KEY_MODEL_CONFIG_PREFIX}${modelName}_"
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

    open suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    open suspend fun exportConfiguration(): String {
        val prefs = dataStore.data.first()
        val jsonObject = JSONObject()
        jsonObject.put("defaultModelName", prefs[PreferenceKeys.defaultModelName] ?: "")
        jsonObject.put("huggingFaceToken", huggingFaceToken)
        jsonObject.put("huggingFaceUsername", huggingFaceUsername)
        jsonObject.put("modelTemperature", prefs[PreferenceKeys.modelTemperature] ?: 0.7f)
        jsonObject.put("modelTopP", prefs[PreferenceKeys.modelTopP] ?: 0.9f)
        jsonObject.put("modelTopK", prefs[PreferenceKeys.modelTopK] ?: 40)
        jsonObject.put("modelMaxTokens", prefs[PreferenceKeys.modelMaxTokens] ?: 2048)
        jsonObject.put("modelContextLength", prefs[PreferenceKeys.modelContextLength] ?: 4096)
        jsonObject.put("modelSystemPrompt", prefs[PreferenceKeys.modelSystemPrompt] ?: "You are a helpful AI assistant.")
        jsonObject.put("modelChatFormat", prefs[PreferenceKeys.modelChatFormat] ?: "CHATML")
        jsonObject.put("modelThreadCount", prefs[PreferenceKeys.modelThreadCount] ?: 4)
        jsonObject.put("showThinkingTokens", prefs[PreferenceKeys.showThinkingTokens] ?: true)
        jsonObject.put("thinkingTokenStyle", prefs[PreferenceKeys.thinkingTokenStyle] ?: "COLLAPSIBLE")
        jsonObject.put("uiTheme", prefs[PreferenceKeys.uiTheme] ?: "DARK")
        jsonObject.put("uiFontSize", prefs[PreferenceKeys.uiFontSize] ?: 1.0f)
        jsonObject.put("uiEnableAnimations", prefs[PreferenceKeys.uiEnableAnimations] ?: true)
        jsonObject.put("uiEnableHapticFeedback", prefs[PreferenceKeys.uiEnableHapticFeedback] ?: true)
        jsonObject.put("perfEnableMemoryOptimization", prefs[PreferenceKeys.perfEnableMemoryOptimization] ?: true)
        jsonObject.put("perfEnableBackgroundProcessing", prefs[PreferenceKeys.perfEnableBackgroundProcessing] ?: true)
        return jsonObject.toString()
    }

    open suspend fun importConfiguration(jsonString: String): Boolean {
        return try {
            val json = JSONObject(jsonString)
            if (json.has("defaultModelName")) setDefaultModelName(json.getString("defaultModelName"))
            if (json.has("huggingFaceToken")) huggingFaceToken = json.getString("huggingFaceToken")
            if (json.has("huggingFaceUsername")) huggingFaceUsername = json.getString("huggingFaceUsername")
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
            Timber.tag("UserPreferencesRepository").e(e, "Error importing configuration")
            false
        }
    }

    private fun parseTemplates(json: String): List<Template> {
        return try {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val obj = array.getJSONObject(index)
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
}
