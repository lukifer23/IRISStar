package com.nervesparks.iris.data

import android.content.Context

private const val USER_PREFERENCES_NAME = "user_preferences"
private const val KEY_DEFAULT_MODEL_NAME = "default_model_name"
private const val KEY_HUGGINGFACE_TOKEN = "huggingface_token"
private const val KEY_HUGGINGFACE_USERNAME = "huggingface_username"

// Model configuration keys
private const val KEY_MODEL_TEMPERATURE = "model_temperature"
private const val KEY_MODEL_TOP_P = "model_top_p"
private const val KEY_MODEL_TOP_K = "model_top_k"
private const val KEY_MODEL_MAX_TOKENS = "model_max_tokens"
private const val KEY_MODEL_CONTEXT_LENGTH = "model_context_length"
private const val KEY_MODEL_SYSTEM_PROMPT = "model_system_prompt"
private const val KEY_MODEL_CHAT_FORMAT = "model_chat_format"
private const val KEY_MODEL_THREAD_COUNT = "model_thread_count"

// Thinking token settings keys
private const val KEY_SHOW_THINKING_TOKENS = "show_thinking_tokens"
private const val KEY_THINKING_TOKEN_STYLE = "thinking_token_style"

// UI settings keys
private const val KEY_UI_THEME = "ui_theme"
private const val KEY_UI_FONT_SIZE = "ui_font_size"
private const val KEY_UI_ENABLE_ANIMATIONS = "ui_enable_animations"
private const val KEY_UI_ENABLE_HAPTIC_FEEDBACK = "ui_enable_haptic_feedback"

// Performance settings keys
private const val KEY_PERF_ENABLE_MEMORY_OPTIMIZATION = "perf_enable_memory_optimization"
private const val KEY_PERF_ENABLE_BACKGROUND_PROCESSING = "perf_enable_background_processing"

class UserPreferencesRepository private constructor(context: Context) {

    private val sharedPreferences =
        context.applicationContext.getSharedPreferences(USER_PREFERENCES_NAME, Context.MODE_PRIVATE)

    // Get the default model name, returns empty string if not set
    fun getDefaultModelName(): String {
        return sharedPreferences.getString(KEY_DEFAULT_MODEL_NAME, "") ?: ""
    }

    // Set the default model name
    fun setDefaultModelName(modelName: String) {
        sharedPreferences.edit().putString(KEY_DEFAULT_MODEL_NAME, modelName).apply()
    }

    // Get HuggingFace token
    fun getHuggingFaceToken(): String {
        return sharedPreferences.getString(KEY_HUGGINGFACE_TOKEN, "") ?: ""
    }

    // Set HuggingFace token
    fun setHuggingFaceToken(token: String) {
        sharedPreferences.edit().putString(KEY_HUGGINGFACE_TOKEN, token).apply()
    }

    // Get HuggingFace username
    fun getHuggingFaceUsername(): String {
        return sharedPreferences.getString(KEY_HUGGINGFACE_USERNAME, "") ?: ""
    }

    // Set HuggingFace username
    fun setHuggingFaceUsername(username: String) {
        sharedPreferences.edit().putString(KEY_HUGGINGFACE_USERNAME, username).apply()
    }

    // Check if HuggingFace credentials are set
    fun hasHuggingFaceCredentials(): Boolean {
        return getHuggingFaceToken().isNotEmpty() || getHuggingFaceUsername().isNotEmpty()
    }

    // Temporary method for testing - now disabled to prevent committing secrets
    fun setTestHuggingFaceToken() {
        // NO-OP. Obtain token from UI or secure storage.
    }

    // Model configuration methods
    fun setModelTemperature(temperature: Float) {
        sharedPreferences.edit().putFloat(KEY_MODEL_TEMPERATURE, temperature).apply()
    }

    fun getModelTemperature(): Float {
        return sharedPreferences.getFloat(KEY_MODEL_TEMPERATURE, 0.7f)
    }

    fun setModelTopP(topP: Float) {
        sharedPreferences.edit().putFloat(KEY_MODEL_TOP_P, topP).apply()
    }

    fun getModelTopP(): Float {
        return sharedPreferences.getFloat(KEY_MODEL_TOP_P, 0.9f)
    }

    fun setModelTopK(topK: Int) {
        sharedPreferences.edit().putInt(KEY_MODEL_TOP_K, topK).apply()
    }

    fun getModelTopK(): Int {
        return sharedPreferences.getInt(KEY_MODEL_TOP_K, 40)
    }

    fun setModelMaxTokens(maxTokens: Int) {
        sharedPreferences.edit().putInt(KEY_MODEL_MAX_TOKENS, maxTokens).apply()
    }

    fun getModelMaxTokens(): Int {
        return sharedPreferences.getInt(KEY_MODEL_MAX_TOKENS, 2048)
    }

    fun setModelContextLength(contextLength: Int) {
        sharedPreferences.edit().putInt(KEY_MODEL_CONTEXT_LENGTH, contextLength).apply()
    }

    fun getModelContextLength(): Int {
        return sharedPreferences.getInt(KEY_MODEL_CONTEXT_LENGTH, 4096)
    }

    fun setModelSystemPrompt(systemPrompt: String) {
        sharedPreferences.edit().putString(KEY_MODEL_SYSTEM_PROMPT, systemPrompt).apply()
    }

    fun getModelSystemPrompt(): String {
        return sharedPreferences.getString(KEY_MODEL_SYSTEM_PROMPT, "You are a helpful AI assistant.") ?: "You are a helpful AI assistant."
    }

    fun setModelChatFormat(chatFormat: String) {
        sharedPreferences.edit().putString(KEY_MODEL_CHAT_FORMAT, chatFormat).apply()
    }

    fun getModelChatFormat(): String {
        return sharedPreferences.getString(KEY_MODEL_CHAT_FORMAT, "CHATML") ?: "CHATML"
    }

    fun setModelThreadCount(threadCount: Int) {
        sharedPreferences.edit().putInt(KEY_MODEL_THREAD_COUNT, threadCount).apply()
    }

    fun getModelThreadCount(): Int {
        return sharedPreferences.getInt(KEY_MODEL_THREAD_COUNT, 4)
    }

    // Thinking token settings
    fun setShowThinkingTokens(show: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SHOW_THINKING_TOKENS, show).apply()
    }

    fun getShowThinkingTokens(): Boolean {
        return sharedPreferences.getBoolean(KEY_SHOW_THINKING_TOKENS, true)
    }

    fun setThinkingTokenStyle(style: String) {
        sharedPreferences.edit().putString(KEY_THINKING_TOKEN_STYLE, style).apply()
    }

    fun getThinkingTokenStyle(): String {
        return sharedPreferences.getString(KEY_THINKING_TOKEN_STYLE, "COLLAPSIBLE") ?: "COLLAPSIBLE"
    }

    // UI settings
    fun setUITheme(theme: String) {
        sharedPreferences.edit().putString(KEY_UI_THEME, theme).apply()
    }

    fun getUITheme(): String {
        return sharedPreferences.getString(KEY_UI_THEME, "DARK") ?: "DARK"
    }

    fun setUIFontSize(fontSize: Float) {
        sharedPreferences.edit().putFloat(KEY_UI_FONT_SIZE, fontSize).apply()
    }

    fun getUIFontSize(): Float {
        return sharedPreferences.getFloat(KEY_UI_FONT_SIZE, 1.0f)
    }

    fun setUIEnableAnimations(enable: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_UI_ENABLE_ANIMATIONS, enable).apply()
    }

    fun getUIEnableAnimations(): Boolean {
        return sharedPreferences.getBoolean(KEY_UI_ENABLE_ANIMATIONS, true)
    }

    fun setUIEnableHapticFeedback(enable: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_UI_ENABLE_HAPTIC_FEEDBACK, enable).apply()
    }

    fun getUIEnableHapticFeedback(): Boolean {
        return sharedPreferences.getBoolean(KEY_UI_ENABLE_HAPTIC_FEEDBACK, true)
    }

    // Performance settings
    fun setPerfEnableMemoryOptimization(enable: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_PERF_ENABLE_MEMORY_OPTIMIZATION, enable).apply()
    }

    fun getPerfEnableMemoryOptimization(): Boolean {
        return sharedPreferences.getBoolean(KEY_PERF_ENABLE_MEMORY_OPTIMIZATION, true)
    }

    fun setPerfEnableBackgroundProcessing(enable: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_PERF_ENABLE_BACKGROUND_PROCESSING, enable).apply()
    }

    fun getPerfEnableBackgroundProcessing(): Boolean {
        return sharedPreferences.getBoolean(KEY_PERF_ENABLE_BACKGROUND_PROCESSING, true)
    }

    // Clear all stored preferences
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesRepository(context).also { INSTANCE = it }
            }
        }
    }
} 