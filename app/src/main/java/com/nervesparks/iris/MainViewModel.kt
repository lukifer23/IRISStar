package com.nervesparks.iris

import android.content.Context
import android.llama.cpp.LLamaAndroid
import android.net.Uri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nervesparks.iris.data.repository.ModelRepository
import com.nervesparks.iris.data.repository.SettingsRepository
import com.nervesparks.iris.data.repository.ModelConfiguration
import com.nervesparks.iris.data.repository.ThinkingTokenSettings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import java.io.File
import java.util.Locale
import java.util.UUID

import android.app.Application
import com.nervesparks.iris.data.ChatRepository
import com.nervesparks.iris.data.db.Chat
import com.nervesparks.iris.data.db.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import kotlinx.coroutines.flow.first

@HiltViewModel
class MainViewModel @Inject constructor(
    private val llamaAndroid: LLamaAndroid,
    private val modelRepository: ModelRepository,
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository,
    private val huggingFaceApiService: com.nervesparks.iris.data.HuggingFaceApiService,
    application: Application
) : AndroidViewModel(application) {

    val chats = chatRepository.observeChats()

    fun renameChat(chat: Chat, title: String) {
        viewModelScope.launch {
            chatRepository.renameChat(chat, title)
        }
    }

    fun deleteChat(chat: Chat) {
        viewModelScope.launch {
            chatRepository.deleteChat(chat)
        }
    }
    
    private var currentChat: com.nervesparks.iris.data.db.Chat? = null
    companion object {
//        @JvmStatic
//        private val NanosPerSecond = 1_000_000_000.0
    }

    // Load existing chat
    fun loadChat(chatId: Long) {
        if (chatId <= 0) return
        viewModelScope.launch {
            val chat = chatRepository.observeChat(chatId).first()
            val msgs = chatRepository.loadMessages(chatId)
            messages = msgs.sortedBy { it.index }.map { m ->
                mapOf("role" to m.role, "content" to m.content)
            }
            currentChat = chat
            first = false
        }
    }

    // Thinking token settings - moved to top to ensure initialization
    var showThinkingTokens by mutableStateOf(true)
    var thinkingTokenStyle by mutableStateOf("COLLAPSIBLE") // COLLAPSIBLE, ALWAYS_VISIBLE, HIDDEN

    var downloadableModels by mutableStateOf<List<Downloadable>>(emptyList())
        private set

    private val _defaultModelName = mutableStateOf("")
    val defaultModelName: State<String> = _defaultModelName

    init {
        viewModelScope.launch {
            loadDefaultModelName()
            loadModelSettings()
            loadThinkingTokenSettings()
        }
    }

    private suspend fun loadDefaultModelName() {
        try {
            _defaultModelName.value = settingsRepository.getDefaultModelName()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error loading default model name, using empty string", e)
            _defaultModelName.value = ""
        }
    }

    fun setDefaultModelName(modelName: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultModelName(modelName)
        }
        _defaultModelName.value = modelName
    }

    lateinit var selectedModel: String
    private val tag: String? = this::class.simpleName

    var messages by mutableStateOf(

            listOf<Map<String, String>>(),
        )
        private set
    var showModal by mutableStateOf(true)
    var showDownloadInfoModal by mutableStateOf(false)
    var showModelSelection by mutableStateOf(false)
    var selectedModelForSwitch by mutableStateOf("")
    var user_thread by mutableStateOf(0f)
    var topP by mutableStateOf(0.9f)
    var topK by mutableStateOf(40)
    var temp by mutableStateOf(0.7f)

    private var first by mutableStateOf(
        true
    )
    var userSpecifiedThreads by mutableIntStateOf(2)
    var message by mutableStateOf("")
        private set

    var userGivenModel by mutableStateOf("")
    var SearchedName by mutableStateOf("")

    private var textToSpeech:TextToSpeech? = null

    var textForTextToSpeech = ""
    var stateForTextToSpeech by mutableStateOf(true)
        private set

    var eot_str = ""

    // Performance monitoring variables
    var tps by mutableStateOf(0.0) // Tokens per second
    var ttft by mutableStateOf(0L) // Time to first token (milliseconds)
    var latency by mutableStateOf(0L) // Average latency per token (milliseconds)
    var memoryUsage by mutableStateOf(0L) // Memory usage in MB
    var contextLimit by mutableStateOf(0) // Current context length
    var maxContextLimit by mutableStateOf(0) // Maximum context limit
    var isGenerating by mutableStateOf(false) // Whether currently generating
    var generationStartTime by mutableStateOf(0L) // Start time of generation
    var tokensGenerated by mutableStateOf(0) // Number of tokens generated in current session
    var totalGenerationTime by mutableStateOf(0L) // Total generation time in milliseconds

    // Performance monitoring functions
    fun startGeneration() {
        isGenerating = true
        generationStartTime = System.currentTimeMillis()
        tokensGenerated = 0
        totalGenerationTime = 0L
        ttft = 0L
        tps = 0.0
        latency = 0L
    }

    fun endGeneration() {
        isGenerating = false
        val endTime = System.currentTimeMillis()
        totalGenerationTime = endTime - generationStartTime
        
        // Calculate TPS if we have tokens and time
        if (tokensGenerated > 0 && totalGenerationTime > 0) {
            tps = (tokensGenerated * 1000.0) / totalGenerationTime
        }
        
        // Calculate average latency
        if (tokensGenerated > 0) {
            latency = totalGenerationTime / tokensGenerated
        persistChat()
        }
    }

    fun updateTokenCount(count: Int) {
        tokensGenerated = count
        if (ttft == 0L && count > 0) {
            // First token received
            ttft = System.currentTimeMillis() - generationStartTime
        }
        
        // Update TPS in real-time
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - generationStartTime
        if (elapsedTime > 0) {
            tps = (count * 1000.0) / elapsedTime
        }
    }

    fun updateMemoryUsage(usageMB: Long) {
        memoryUsage = usageMB
    }
    
    fun getCurrentMemoryUsage(): Long {
        return memoryUsage
    }
    
    suspend fun refreshMemoryUsage() {
        try {
            val usage = llamaAndroid.getMemoryUsage()
            updateMemoryUsage(usage)
        } catch (e: Exception) {
            Log.e(tag, "Error getting memory usage: ${e.message}")
            updateMemoryUsage(0)
        }
    }

    fun updateContextLimit(current: Int, max: Int) {
        contextLimit = current
        maxContextLimit = max
    }

    fun resetPerformanceMetrics() {
        tps = 0.0
        ttft = 0L
        latency = 0L
        tokensGenerated = 0
        totalGenerationTime = 0L
        isGenerating = false
    }

    // Model configuration variables
    var modelTemperature by mutableStateOf(0.7f)
    var modelTopP by mutableStateOf(0.9f)
    var modelTopK by mutableStateOf(40)
    var modelMaxTokens by mutableStateOf(2048)
    var modelContextLength by mutableStateOf(32768) // Increased for Qwen3 support
    var modelSystemPrompt by mutableStateOf("You are a helpful AI assistant.")
    var modelChatFormat by mutableStateOf("QWEN3")
    var modelThreadCount by mutableStateOf(4)
    var showModelSettings by mutableStateOf(false)

    // Model configuration functions
    fun updateModelSettings(
        temperature: Float = modelTemperature,
        topP: Float = modelTopP,
        topK: Int = modelTopK,
        maxTokens: Int = modelMaxTokens,
        contextLength: Int = modelContextLength,
        systemPrompt: String = modelSystemPrompt,
        chatFormat: String = modelChatFormat,
        threadCount: Int = modelThreadCount
    ) {
        modelTemperature = temperature
        modelTopP = topP
        modelTopK = topK
        modelMaxTokens = maxTokens
        modelContextLength = contextLength
        modelSystemPrompt = systemPrompt
        modelChatFormat = chatFormat
        modelThreadCount = threadCount
        viewModelScope.launch {
            val config = ModelConfiguration(
                temperature = temperature,
                topP = topP,
                topK = topK,
                threadCount = threadCount,
                contextLength = contextLength,
                systemPrompt = systemPrompt
            )
            modelRepository.saveModelConfiguration(defaultModelName.value, config)
        }
    }

    private suspend fun loadModelSettings() {
        try {
            Log.d("MainViewModel", "Loading model settings...")
            val config = modelRepository.getModelConfiguration(defaultModelName.value)
            modelTemperature = config.temperature
            modelTopP = config.topP
            modelTopK = config.topK
            modelThreadCount = config.threadCount
            modelContextLength = config.contextLength
            modelSystemPrompt = config.systemPrompt
            // Defaults for values not stored in repository
            modelMaxTokens = 2048
            modelChatFormat = "QWEN3"
            Log.d("MainViewModel", "Model settings loaded successfully")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error in loadModelSettings", e)
            modelTemperature = 0.7f
            modelTopP = 0.9f
            modelTopK = 40
            modelMaxTokens = 2048
            modelContextLength = 32768
            modelSystemPrompt = "You are a helpful AI assistant."
            modelChatFormat = "QWEN3"
            modelThreadCount = 4
        }
    }

    fun showModelSettings() {
        showModelSettings = true
    }

    fun hideModelSettings() {
        showModelSettings = false
    }

    // Thinking token settings
    fun updateShowThinkingTokens(show: Boolean) {
        showThinkingTokens = show
        viewModelScope.launch {
            settingsRepository.saveThinkingTokenSettings(
                ThinkingTokenSettings(
                    showThinkingTokens = show,
                    thinkingTokenStyle = thinkingTokenStyle
                )
            )
        }
    }

    fun updateThinkingTokenStyle(style: String) {
        thinkingTokenStyle = style
        viewModelScope.launch {
            settingsRepository.saveThinkingTokenSettings(
                ThinkingTokenSettings(
                    showThinkingTokens = showThinkingTokens,
                    thinkingTokenStyle = style
                )
            )
        }
    }

    private suspend fun loadThinkingTokenSettings() {
        try {
            val settings = settingsRepository.getThinkingTokenSettings()
            showThinkingTokens = settings.showThinkingTokens
            thinkingTokenStyle = settings.thinkingTokenStyle
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error in loadThinkingTokenSettings", e)
            showThinkingTokens = true
            thinkingTokenStyle = "COLLAPSIBLE"
        }
    }


    var refresh by mutableStateOf(false)

    fun loadExistingModels(directory: File) {
        viewModelScope.launch {
            val availableModels = modelRepository.getAvailableModels(directory)

            if (availableModels.isNotEmpty()) {
                val firstModel = availableModels.first()
                val destinationPath = File(directory, firstModel["destination"].toString())
                currentDownloadable = Downloadable(
                    firstModel["name"].toString(),
                    Uri.parse(firstModel["source"].toString()),
                    destinationPath
                )

                if (defaultModelName.value.isNotEmpty()) {
                    val defaultModel = availableModels.find { model -> model["name"] == defaultModelName.value }
                    if (defaultModel != null) {
                        val defaultPath = File(directory, defaultModel["destination"].toString())
                        currentDownloadable = Downloadable(
                            defaultModel["name"].toString(),
                            Uri.parse(defaultModel["source"].toString()),
                            defaultPath
                        )
                    }
                }

                showModal = false
                showModelSelection = true
            } else {
                showModal = true
                showModelSelection = false
            }
        }
    }

    // New function to switch between models
    fun switchModel(modelName: String, directory: File) {
        viewModelScope.launch {
            val models = modelRepository.getAvailableModels(directory)
            val model = models.find { it["name"] == modelName }
            if (model != null) {
                val destinationPath = File(directory, model["destination"].toString())
                if (destinationPath.exists()) {
                    try {
                        llamaAndroid.unload()
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Error unloading model", e)
                    }

                    currentDownloadable = Downloadable(
                        model["name"].toString(),
                        Uri.parse(model["source"].toString()),
                        destinationPath
                    )

                    load(destinationPath.path, userThreads = user_thread.toInt())
                    setDefaultModelName(modelName)
                    Log.i("MainViewModel", "Switched to model: $modelName")
                }
            }
        }
    }

    // New function to get available models
    fun getAvailableModels(directory: File): List<Map<String, String>> {
        return runBlocking { modelRepository.getAvailableModels(directory) }
    }

    // Model selection UI functions
    fun showModelSelectionDialog() {
        showModelSelection = true
    }

    fun hideModelSelectionDialog() {
        showModelSelection = false
        selectedModelForSwitch = ""
    }

    fun selectModelForSwitch(modelName: String) {
        selectedModelForSwitch = modelName
    }

    fun confirmModelSwitch(directory: File) {
        if (selectedModelForSwitch.isNotEmpty()) {
            switchModel(selectedModelForSwitch, directory)
            hideModelSelectionDialog()
        }
    }



    fun textToSpeech(context: Context) {
        if (!getIsSending()) {
            // If TTS is already initialized, stop it first
            textToSpeech?.stop()

            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.let { txtToSpeech ->
                        txtToSpeech.language = Locale.US
                        txtToSpeech.setSpeechRate(1.0f)

                        // Add a unique utterance ID for tracking
                        val utteranceId = UUID.randomUUID().toString()

                        txtToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onDone(utteranceId: String?) {
                                // Reset state when speech is complete
                                CoroutineScope(Dispatchers.Main).launch {
                                    stateForTextToSpeech = true
                                }
                            }

                            override fun onError(utteranceId: String?) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    stateForTextToSpeech = true
                                }
                            }

                            override fun onStart(utteranceId: String?) {
                                // Update state to indicate speech is playing
                                CoroutineScope(Dispatchers.Main).launch {
                                    stateForTextToSpeech = false
                                }
                            }
                        })

                        txtToSpeech.speak(
                            textForTextToSpeech,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            utteranceId
                        )
                    }
                }
            }
        }
    }



    fun stopTextToSpeech() {
        textToSpeech?.apply {
            stop()  // Stops current speech
            shutdown()  // Releases the resources
        }
        textToSpeech = null

        // Reset state to allow restarting
        stateForTextToSpeech = true
    }



    var toggler by mutableStateOf(false)
    var showAlert by mutableStateOf(false)
    var switchModal by mutableStateOf(false)
    var currentDownloadable: Downloadable? by mutableStateOf(null)

    override fun onCleared() {
        textToSpeech?.shutdown()
        super.onCleared()

        viewModelScope.launch {
            try {

                llamaAndroid.unload()

            } catch (exc: IllegalStateException) {
                addMessage("error", exc.message ?: "")
            }
        }
    }

    fun send() {
        val reserveTokens = 256
        val userMessage = removeExtraWhiteSpaces(message)
        message = ""

        // Add to messages console.
        if (userMessage != "" && userMessage != " ") {
            if(first){
                addMessage("system", "This is a conversation between User and Iris, a friendly chatbot. Iris is helpful, kind, honest, good at writing, and never fails to answer any requests immediately and with precision. When responding, Iris should use <think> tags to show its reasoning process before providing the final answer.")
                addMessage("user", "Hi")
                addMessage("assistant", "How may I help You?")
                first = false
            }

            addMessage("user", userMessage)
            persistChat()

            // Start performance monitoring
            startGeneration()

            viewModelScope.launch {
                try {
                    var workingMessages = messages.toMutableList()
                    var prompt: String
                    var promptTokens: Int
                    val reserve = reserveTokens

                    // Trim history until it fits within the context window
                    while (true) {
                        prompt = com.nervesparks.iris.llm.TemplateRegistry.render(
                            modelChatFormat,
                            workingMessages,
                            modelSystemPrompt,
                            includeThinkingTags = true
                        )
                        promptTokens = llamaAndroid.countTokens(prompt)
                        if (promptTokens <= modelContextLength - reserve || workingMessages.size <= 1) {
                            break
                        }
                        val removeIdx = workingMessages.indexOfFirst { it["role"] != "system" }
                        if (removeIdx >= 0) {
                            workingMessages = workingMessages.drop(removeIdx + 1).toMutableList()
                        } else {
                            break
                        }
                    }

                    if (workingMessages.size != messages.size) {
                        messages = workingMessages
                    }

                    contextLimit = promptTokens
                    maxContextLimit = modelContextLength

                    var generatedTokens = 0
                    llamaAndroid.send(prompt)
                        .catch {
                            Log.e(tag, "send() failed", it)
                            addMessage("error", it.message ?: "")
                        }
                        .collect { response ->
                            generatedTokens++
                            updateTokenCount(generatedTokens)
                            contextLimit = promptTokens + generatedTokens

                            if (getIsMarked()) {
                                addMessage("codeBlock", response)
                            } else {
                                addMessage("assistant", response)
                            }
                        }
                } finally {
                    endGeneration()
                    if (!getIsCompleteEOT()) {
                        trimEOT()
                    }
                }
            

            }
        }
    }

//    fun bench(pp: Int, tg: Int, pl: Int, nr: Int = 1) {
//        viewModelScope.launch {
//            try {
//                val start = System.nanoTime()
//                val warmupResult = llamaAndroid.bench(pp, tg, pl, nr)
//                val end = System.nanoTime()
//
//                messages += warmupResult
//
//                val warmup = (end - start).toDouble() / NanosPerSecond
//                messages += "Warm up time: $warmup seconds, please wait..."
//
//                if (warmup > 5.0) {
//                    messages += "Warm up took too long, aborting benchmark"
//                    return@launch
//                }
//
//                messages += llamaAndroid.bench(512, 128, 1, 3)
//            } catch (exc: IllegalStateException) {
//                Log.e(tag, "bench() failed", exc)
//                messages += exc.message!!
//            }
//        }
//    }

    suspend fun unload(){
        llamaAndroid.unload()
    }

    var tokensList = mutableListOf<String>() // Store emitted tokens
    var benchmarkStartTime: Long = 0L // Track the benchmark start time
    var tokensPerSecondsFinal: Double by mutableStateOf(0.0) // Track tokens per second and trigger UI updates
    var isBenchmarkingComplete by mutableStateOf(false) // Flag to track if benchmarking is complete

    fun myCustomBenchmark() {
        viewModelScope.launch {
            try {
                tokensList.clear() // Reset the token list before benchmarking
                benchmarkStartTime = System.currentTimeMillis() // Record the start time
                isBenchmarkingComplete = false // Reset benchmarking flag

                // Launch a coroutine to update the tokens per second every second
                launch {
                    while (!isBenchmarkingComplete) {
                        delay(1000L) // Delay 1 second
                        val elapsedTime = System.currentTimeMillis() - benchmarkStartTime
                        if (elapsedTime > 0) {
                            tokensPerSecondsFinal = tokensList.size.toDouble() / (elapsedTime / 1000.0)
                        }
                    }
                }

                llamaAndroid.myCustomBenchmark()
                    .collect { emittedString ->
                        if (emittedString != null) {
                            tokensList.add(emittedString) // Add each token to the list
                            Log.d(tag, "Token collected: $emittedString")
                        }
                    }
            } catch (exc: IllegalStateException) {
                Log.e(tag, "myCustomBenchmark() failed", exc)
            } catch (exc: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e(tag, "myCustomBenchmark() timed out", exc)
            } catch (exc: Exception) {
                Log.e(tag, "Unexpected error during myCustomBenchmark()", exc)
            } finally {
                // Benchmark complete, log the final tokens per second value
                val elapsedTime = System.currentTimeMillis() - benchmarkStartTime
                val finalTokensPerSecond = if (elapsedTime > 0) {
                    tokensList.size.toDouble() / (elapsedTime / 1000.0)
                } else {
                    0.0
                }
                Log.d(tag, "Benchmark complete. Tokens/sec: $finalTokensPerSecond")

                // Update the final tokens per second and stop updating the value
                tokensPerSecondsFinal = finalTokensPerSecond
                isBenchmarkingComplete = true // Mark benchmarking as complete
            }
        }
    }





    var loadedModelName = mutableStateOf("");

    fun load(pathToModel: String, userThreads: Int)  {
        viewModelScope.launch {
            try{
                llamaAndroid.unload()
            } catch (exc: IllegalStateException){
                Log.e(tag, "load() failed", exc)
            }
            try {
                var modelName = pathToModel.split("/")
                loadedModelName.value = modelName.last()
                showModal = false
                showAlert = true
                
                // Use model settings instead of default parameters
                llamaAndroid.load(
                    pathToModel, 
                    userThreads = modelThreadCount, 
                    topK = modelTopK, 
                    topP = modelTopP, 
                    temp = modelTemperature
                )
                showAlert = false

            } catch (exc: IllegalStateException) {
                Log.e(tag, "load() failed", exc)
//                addMessage("error", exc.message ?: "")
            }
            showModal = false
            showAlert = false
            eot_str = llamaAndroid.send_eot_str()
        }
    }
    
    /**
     * Load a model by file path - wrapper for the load method
     */
    fun loadModel(modelPath: String) {
        load(modelPath, modelThreadCount)
    }
    
    /**
     * Load a model by name from the external files directory
     */
    fun loadModelByName(modelName: String, directory: File) {
        val modelFile = File(directory, modelName)
        if (modelFile.exists()) {
            loadModel(modelFile.absolutePath)
        } else {
            Log.e(tag, "Model file not found: ${modelFile.absolutePath}")
        }
    }
    private fun addMessage(role: String, content: String) {
        val newMessage = mapOf("role" to role, "content" to content)

        messages = if (messages.isNotEmpty() && messages.last()["role"] == role) {
            val lastMessageContent = messages.last()["content"] ?: ""
            val updatedContent = "$lastMessageContent$content"
            val updatedLastMessage = messages.last() + ("content" to updatedContent)
            messages.toMutableList().apply {
                set(messages.lastIndex, updatedLastMessage)
            }
        } else {
            messages + listOf(newMessage)
        }
    }

    private fun trimEOT() {
        if (messages.isEmpty()) return
        val lastMessageContent = messages.last()["content"] ?: ""
        // Only slice if the content is longer than the EOT string
        if (lastMessageContent.length < eot_str.length) return

        val updatedContent = lastMessageContent.slice(0..(lastMessageContent.length-eot_str.length))
        val updatedLastMessage = messages.last() + ("content" to updatedContent)
        messages = messages.toMutableList().apply {
            set(messages.lastIndex, updatedLastMessage)
        }
        messages.last()["content"]?.let { Log.e(tag, it) }
    }

    private fun removeExtraWhiteSpaces(input: String): String {
        // Replace multiple white spaces with a single space
        return input.replace("\\s+".toRegex(), " ")
    }

    private fun persistChat() {
        val title = messages.firstOrNull { it["role"] == "user" }?.get("content")?.take(64) ?: "Chat"
        val baseChat = currentChat?.copy(title = title, updated = System.currentTimeMillis())
            ?: Chat(title = title)

        viewModelScope.launch(Dispatchers.IO) {
            val id = chatRepository.saveChatWithMessages(
                baseChat,
                messages.mapIndexed { idx, m ->
                    Message(
                        chatId = baseChat.id,
                        role = m["role"] ?: "assistant",
                        content = m["content"] ?: "",
                        index = idx
                    )
                }
            )
            if (currentChat == null || currentChat?.id != id) {
                currentChat = baseChat.copy(id = id)
            }
        }
    }

    private fun parseTemplateJson(chatData: List<Map<String, String>> ):String{
        var chatStr = ""
        for (data in chatData){
            val role = data["role"]
            val content = data["content"]
            if (role != "log"){
                chatStr += "$role \n$content \n"
            }

        }
        return chatStr
    }
    fun updateMessage(newMessage: String) {
        message = newMessage
    }

    fun clear() {
        messages = listOf(

        )
        first = true
    }

    fun log(message: String) {
//        addMessage("log", message)
    }

    fun getIsSending(): Boolean {
        return llamaAndroid.getIsSending()
    }

    private fun getIsMarked(): Boolean {
        return llamaAndroid.getIsMarked()
    }

    fun getIsCompleteEOT(): Boolean{
        return llamaAndroid.getIsCompleteEOT()
    }

    fun stop() {
        llamaAndroid.stopTextGeneration()
    }

    // Add missing methods for compilation fixes
    fun searchModels(query: String): SearchResponse {
        // For now, return a placeholder response
        // TODO: Implement proper async search with coroutines
        return SearchResponse(
            success = false,
            data = null,
            error = "Search functionality not yet implemented"
        )
    }

    fun getModelDetails(modelId: String): ModelDetailsResponse {
        // For now, return a placeholder response
        // TODO: Implement proper async model details with coroutines
        return ModelDetailsResponse(
            success = false,
            data = null,
            error = "Model details functionality not yet implemented"
        )
    }

    fun setTestHuggingFaceToken() {
        // TODO: Implement test token setting
        Log.d(tag, "setTestHuggingFaceToken called - not yet implemented")
    }
}

// Add data class for search response with proper structure
data class SearchResponse(
    val success: Boolean,
    val data: List<ModelSearchResult>?,
    val error: String?
)

data class ModelSearchResult(
    val id: String,
    val name: String,
    val description: String?,
    val downloads: Int,
    val likes: Int,
    val tags: List<String>
)

data class ModelDetailsResponse(
    val success: Boolean,
    val data: List<ModelDetailResult>?,
    val error: String?
)

data class ModelDetailResult(
    val id: String,
    val name: String,
    val description: String?,
    val downloads: Int,
    val likes: Int,
    val tags: List<String>,
    val siblings: List<ModelFile>
)

data class ModelFile(
    val filename: String,
    val size: Long?,
    val quantType: String?
)

fun sentThreadsValue(){

}