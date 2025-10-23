package com.nervesparks.iris.data

import android.content.Context
import com.nervesparks.iris.Template
import com.nervesparks.iris.data.repository.ModelConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class FakeUserPreferencesRepository(context: Context) : UserPreferencesRepository(context) {

    private val defaultModelNameState = MutableStateFlow("")
    private val modelTemperatureState = MutableStateFlow(0.7f)
    private val modelTopPState = MutableStateFlow(0.9f)
    private val modelTopKState = MutableStateFlow(40)
    private val modelMaxTokensState = MutableStateFlow(2048)
    private val modelContextLengthState = MutableStateFlow(4096)
    private val modelSystemPromptState = MutableStateFlow("You are a helpful AI assistant.")
    private val modelChatFormatState = MutableStateFlow("CHATML")
    private val modelThreadCountState = MutableStateFlow(4)
    private val modelGpuLayersState = MutableStateFlow(-1)
    private val cachedModelsState = MutableStateFlow("")
    private val showThinkingTokensState = MutableStateFlow(true)
    private val thinkingTokenStyleState = MutableStateFlow("COLLAPSIBLE")
    private val templatesState = MutableStateFlow<List<Template>>(emptyList())
    private val uiThemeState = MutableStateFlow("DARK")
    private val uiFontSizeState = MutableStateFlow(1.0f)
    private val uiEnableAnimationsState = MutableStateFlow(true)
    private val uiEnableHapticFeedbackState = MutableStateFlow(true)
    private val perfEnableMemoryOptimizationState = MutableStateFlow(true)
    private val perfEnableBackgroundProcessingState = MutableStateFlow(true)
    private val securityBiometricEnabledState = MutableStateFlow(false)
    private val appLanguageState = MutableStateFlow<String?>(null)

    private val modelConfigurations = mutableMapOf<String, ModelConfiguration>()

    // Sensitive data stored in memory for testing
    override var huggingFaceToken: String = ""
    override var huggingFaceUsername: String = ""
    override var googleApiKey: String = ""

    override val defaultModelNameFlow: Flow<String> = defaultModelNameState
    override suspend fun getDefaultModelName(): String = defaultModelNameState.value
    override suspend fun setDefaultModelName(modelName: String) {
        defaultModelNameState.value = modelName
    }

    override val modelTemperatureFlow: Flow<Float> = modelTemperatureState
    override suspend fun getModelTemperature(): Float = modelTemperatureState.value
    override suspend fun setModelTemperature(temperature: Float) {
        modelTemperatureState.value = temperature
    }

    override val modelTopPFlow: Flow<Float> = modelTopPState
    override suspend fun getModelTopP(): Float = modelTopPState.value
    override suspend fun setModelTopP(value: Float) {
        modelTopPState.value = value
    }

    override val modelTopKFlow: Flow<Int> = modelTopKState
    override suspend fun getModelTopK(): Int = modelTopKState.value
    override suspend fun setModelTopK(value: Int) {
        modelTopKState.value = value
    }

    override val modelMaxTokensFlow: Flow<Int> = modelMaxTokensState    override suspend fun getModelMaxTokens(): Int = modelMaxTokensState.value
    override suspend fun setModelMaxTokens(value: Int) {
        modelMaxTokensState.value = value
    }

    override val modelContextLengthFlow: Flow<Int> = modelContextLengthState    override suspend fun getModelContextLength(): Int = modelContextLengthState.value
    override suspend fun setModelContextLength(value: Int) {
        modelContextLengthState.value = value
    }

    override val modelSystemPromptFlow: Flow<String> = modelSystemPromptState    override suspend fun getModelSystemPrompt(): String = modelSystemPromptState.value
    override suspend fun setModelSystemPrompt(value: String) {
        modelSystemPromptState.value = value
    }

    override val modelChatFormatFlow: Flow<String> = modelChatFormatState    override suspend fun getModelChatFormat(): String = modelChatFormatState.value
    override suspend fun setModelChatFormat(value: String) {
        modelChatFormatState.value = value
    }

    override val modelThreadCountFlow: Flow<Int> = modelThreadCountState    override suspend fun getModelThreadCount(): Int = modelThreadCountState.value
    override suspend fun setModelThreadCount(value: Int) {
        modelThreadCountState.value = value
    }

    override val modelGpuLayersFlow: Flow<Int> = modelGpuLayersState    override suspend fun getModelGpuLayers(): Int = modelGpuLayersState.value
    override suspend fun setModelGpuLayers(value: Int) {
        modelGpuLayersState.value = value
    }

    override val cachedModelsFlow: Flow<String> = cachedModelsState    override suspend fun getCachedModels(): String = cachedModelsState.value
    override suspend fun setCachedModels(json: String) {
        cachedModelsState.value = json
    }

    override val showThinkingTokensFlow: Flow<Boolean> = showThinkingTokensState    override suspend fun getShowThinkingTokens(): Boolean = showThinkingTokensState.value
    override suspend fun setShowThinkingTokens(value: Boolean) {
        showThinkingTokensState.value = value
    }

    override val thinkingTokenStyleFlow: Flow<String> = thinkingTokenStyleState    override suspend fun getThinkingTokenStyle(): String = thinkingTokenStyleState.value
    override suspend fun setThinkingTokenStyle(value: String) {
        thinkingTokenStyleState.value = value
    }

    override val templatesFlow: Flow<List<Template>> = templatesState    override suspend fun getTemplates(): List<Template> = templatesState.value
    override suspend fun saveTemplates(templates: List<Template>) {
        templatesState.value = templates
    }

    override val uiThemeFlow: Flow<String> = uiThemeState    override suspend fun getUITheme(): String = uiThemeState.value
    override suspend fun setUITheme(value: String) {
        uiThemeState.value = value
    }

    override val uiFontSizeFlow: Flow<Float> = uiFontSizeState    override suspend fun getUIFontSize(): Float = uiFontSizeState.value
    override suspend fun setUIFontSize(value: Float) {
        uiFontSizeState.value = value
    }

    override val uiEnableAnimationsFlow: Flow<Boolean> = uiEnableAnimationsState    override suspend fun getUIEnableAnimations(): Boolean = uiEnableAnimationsState.value
    override suspend fun setUIEnableAnimations(value: Boolean) {
        uiEnableAnimationsState.value = value
    }

    override val uiEnableHapticFeedbackFlow: Flow<Boolean> = uiEnableHapticFeedbackState    override suspend fun getUIEnableHapticFeedback(): Boolean = uiEnableHapticFeedbackState.value
    override suspend fun setUIEnableHapticFeedback(value: Boolean) {
        uiEnableHapticFeedbackState.value = value
    }

    override val appLanguageFlow: Flow<String?> = appLanguageState    override suspend fun getAppLanguage(): String? = appLanguageState.value
    override suspend fun setAppLanguage(value: String?) {
        appLanguageState.value = value
    }

    override val perfEnableMemoryOptimizationFlow: Flow<Boolean> =
        perfEnableMemoryOptimizationState    override suspend fun getPerfEnableMemoryOptimization(): Boolean = perfEnableMemoryOptimizationState.value
    override suspend fun setPerfEnableMemoryOptimization(value: Boolean) {
        perfEnableMemoryOptimizationState.value = value
    }

    override val perfEnableBackgroundProcessingFlow: Flow<Boolean> =
        perfEnableBackgroundProcessingState    override suspend fun getPerfEnableBackgroundProcessing(): Boolean =
        perfEnableBackgroundProcessingState.value
    override suspend fun setPerfEnableBackgroundProcessing(value: Boolean) {
        perfEnableBackgroundProcessingState.value = value
    }

    override val securityBiometricEnabledFlow: Flow<Boolean> =
        securityBiometricEnabledState    override suspend fun getSecurityBiometricEnabled(): Boolean = securityBiometricEnabledState.value
    override suspend fun setSecurityBiometricEnabled(value: Boolean) {
        securityBiometricEnabledState.value = value
    }

    override suspend fun getModelConfiguration(modelName: String): ModelConfiguration {
        return modelConfigurations[modelName] ?: ModelConfiguration()
    }

    override suspend fun saveModelConfiguration(modelName: String, config: ModelConfiguration) {
        modelConfigurations[modelName] = config
    }

    override suspend fun clearAll() {
        defaultModelNameState.value = ""
        modelTemperatureState.value = 0.7f
        modelTopPState.value = 0.9f
        modelTopKState.value = 40
        modelMaxTokensState.value = 2048
        modelContextLengthState.value = 4096
        modelSystemPromptState.value = "You are a helpful AI assistant."
        modelChatFormatState.value = "CHATML"
        modelThreadCountState.value = 4
        modelGpuLayersState.value = -1
        cachedModelsState.value = ""
        showThinkingTokensState.value = true
        thinkingTokenStyleState.value = "COLLAPSIBLE"
        templatesState.value = emptyList()
        uiThemeState.value = "DARK"
        uiFontSizeState.value = 1.0f
        uiEnableAnimationsState.value = true
        uiEnableHapticFeedbackState.value = true
        perfEnableMemoryOptimizationState.value = true
        perfEnableBackgroundProcessingState.value = true
        securityBiometricEnabledState.value = false
        appLanguageState.value = null
        modelConfigurations.clear()
    }

    override suspend fun exportConfiguration(): String {
        return "{" +
            "\"defaultModelName\":\"${defaultModelNameState.value}\"," +
            "\"modelTemperature\":${modelTemperatureState.value}," +
            "\"modelTopP\":${modelTopPState.value}," +
            "\"modelTopK\":${modelTopKState.value}," +
            "\"modelMaxTokens\":${modelMaxTokensState.value}," +
            "\"modelContextLength\":${modelContextLengthState.value}," +
            "\"modelSystemPrompt\":\"${modelSystemPromptState.value}\"," +
            "\"modelChatFormat\":\"${modelChatFormatState.value}\"," +
            "\"modelThreadCount\":${modelThreadCountState.value}," +
            "\"showThinkingTokens\":${showThinkingTokensState.value}," +
            "\"thinkingTokenStyle\":\"${thinkingTokenStyleState.value}\"," +
            "\"uiTheme\":\"${uiThemeState.value}\"," +
            "\"uiFontSize\":${uiFontSizeState.value}," +
            "\"uiEnableAnimations\":${uiEnableAnimationsState.value}," +
            "\"uiEnableHapticFeedback\":${uiEnableHapticFeedbackState.value}," +
            "\"perfEnableMemoryOptimization\":${perfEnableMemoryOptimizationState.value}," +
            "\"perfEnableBackgroundProcessing\":${perfEnableBackgroundProcessingState.value}," +
            "\"appLanguage\":\"${appLanguageState.value ?: ""}\"" +
            "}"
    }

    override suspend fun importConfiguration(jsonString: String): Boolean {
        return try {
            val json = org.json.JSONObject(jsonString)
            if (json.has("defaultModelName")) setDefaultModelName(json.getString("defaultModelName"))
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
            if (json.has("appLanguage")) setAppLanguage(json.optString("appLanguage").takeIf { it.isNotBlank() })
            true
        } catch (e: Exception) {
            false
        }
    }
}
