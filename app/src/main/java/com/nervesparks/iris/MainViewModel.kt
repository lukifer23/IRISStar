package com.nervesparks.iris

import android.Manifest
import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.llama.cpp.LLamaAndroid
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.View
import com.nervesparks.iris.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import java.io.File
import java.util.Locale
import java.util.UUID

import android.app.Application
import android.content.Context
import com.nervesparks.iris.data.ChatRepository
import com.nervesparks.iris.data.DocumentRepository
import com.nervesparks.iris.data.db.Chat
import com.nervesparks.iris.data.db.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.nervesparks.iris.data.WebSearchService

@HiltViewModel
class MainViewModel @Inject constructor(
    private val llamaAndroid: LLamaAndroid,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val chatRepository: ChatRepository,
    private val modelRepository: com.nervesparks.iris.data.repository.ModelRepository,
    private val huggingFaceApiService: com.nervesparks.iris.data.HuggingFaceApiService,
    private val documentRepository: DocumentRepository,
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

    fun indexDocument(text: String) {
        viewModelScope.launch {
            val embedding = embedText(text)
            documentRepository.addDocument(text, embedding.toList())
        }
    }

    suspend fun embedText(text: String): FloatArray =
        withContext(Dispatchers.Default) {
            llamaAndroid.getEmbeddings(text)
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
        loadDefaultModelName()
        loadModelSettings()
        loadThinkingTokenSettings()
        
        // Set a default Hugging Face token if none exists
        if (userPreferencesRepository.getHuggingFaceToken().isEmpty()) {
            setTestHuggingFaceToken()
        }
        
        viewModelScope.launch {
            try {
                allModels = modelRepository.refreshAvailableModels()
            } catch (e: Exception) {
                Log.e(tag, "Error refreshing available models", e)
                // Use default models if API fails
                allModels = listOf(
                    mapOf(
                        "name" to "Qwen_Qwen3-0.6B-Q4_K_M.gguf",
                        "source" to "https://huggingface.co/bartowski/Qwen_Qwen3-0.6B-GGUF/resolve/main/Qwen_Qwen3-0.6B-Q4_K_M.gguf?download=true",
                        "destination" to "Qwen_Qwen3-0.6B-Q4_K_M.gguf",
                        "supportsReasoning" to "true"
                    )
                )
            }
        }
    }
    private fun loadDefaultModelName(){
        try {
            _defaultModelName.value = userPreferencesRepository.getDefaultModelName()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error loading default model name, using empty string", e)
            _defaultModelName.value = ""
        }
    }

    fun setDefaultModelName(modelName: String){
        userPreferencesRepository.setDefaultModelName(modelName)
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

    var allModels by mutableStateOf<List<Map<String, String>>>(emptyList())

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

    private var speechRecognizer: SpeechRecognizer? = null
    var isListening by mutableStateOf(false)
        private set
    var voiceError by mutableStateOf<String?>(null)

    var eot_str = ""

    // Web search service
    private val webSearchService = WebSearchService()

    fun performWebSearch(query: String, summarize: Boolean = true) {
        // Add the search query as a user message
        addMessage("user", "Search the web for: $query")
        
        viewModelScope.launch {
            try {
                // Update search state
                isSearching = true
                currentSearchQuery = query
                searchProgress = "Initializing search..."
                
                // Show search in progress
                addMessage("assistant", "🔍 Searching the web for \"$query\"...")
                
                searchProgress = "Querying search engine..."
                
                // Perform actual web search
                val searchResponse = webSearchService.searchWeb(query)
                
                searchProgress = "Processing results..."
                
                if (searchResponse.success && searchResponse.results != null) {
                    searchProgress = "Formatting results..."
                    
                    // Format and display search results
                    val formattedResults = webSearchService.formatSearchResults(searchResponse.results, query)
                    addMessage("assistant", formattedResults)
                    
                    // If summarize is true, ask the model to summarize the results
                    if (summarize && searchResponse.results.isNotEmpty()) {
                        searchProgress = "Generating summary..."
                        
                        val summaryPrompt = """
                            Based on the search results above, provide a concise summary of the key information about "$query".
                            Focus on the most important facts and recent developments.
                        """.trimIndent()
                        
                        addMessage("user", summaryPrompt)
                        processWebSearch(summaryPrompt)
                    }
                } else {
                    // Handle search error
                    val errorMessage = searchResponse.error ?: "Unknown search error"
                    addMessage("assistant", "❌ Search failed: $errorMessage\n\nPlease try rephrasing your search query.")
                }
                
            } catch (e: Exception) {
                Log.e(tag, "Error performing web search", e)
                addMessage("assistant", "❌ Search error: ${e.message}\n\nPlease try again later.")
            } finally {
                // Reset search state
                isSearching = false
                currentSearchQuery = ""
                searchProgress = ""
            }
        }
    }
    
    private fun processWebSearch(prompt: String) {
        viewModelScope.launch {
            try {
                startGeneration()
                
                var workingMessages = messages.toMutableList()
                val reserve = 256

                // Trim history until it fits within the context window
                var finalPrompt = ""
                while (true) {
                    finalPrompt = if (template.isNotBlank()) {
                        val jinjava = com.hubspot.jinjava.Jinjava()
                        val context = mapOf("messages" to workingMessages)
                        jinjava.render(template, context)
                    } else {
                        com.nervesparks.iris.llm.TemplateRegistry.render(
                            modelChatFormat,
                            workingMessages,
                            modelSystemPrompt,
                            includeThinkingTags = true
                        )
                    }
                    val promptTokens = llamaAndroid.countTokens(finalPrompt)
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

                contextLimit = llamaAndroid.countTokens(finalPrompt)
                maxContextLimit = modelContextLength

                var generatedTokens = 0
                llamaAndroid.send(finalPrompt)
                    .catch {
                        Log.e(tag, "processWebSearch() failed", it)
                        addMessage("error", it.message ?: "")
                    }
                    .collect {
                        generatedTokens++
                        updateTokenCount(generatedTokens)
                        contextLimit = llamaAndroid.countTokens(finalPrompt) + generatedTokens

                        if (getIsMarked()) {
                            addMessage("codeBlock", it)
                        } else {
                            try {
                                val json = org.json.JSONObject(it)
                                if (json.has("tool")) {
                                    val tool = json.getString("tool")
                                    val args = json.getJSONObject("args")
                                    val argsMap = mutableMapOf<String, Any>()
                                    args.keys().forEach { key ->
                                        argsMap[key] = args.get(key)
                                    }
                                    handleToolCall(com.nervesparks.iris.data.ToolCall(tool, argsMap))
                                } else {
                                    val cleanedResponse = cleanThinkingResponse(it)
                                    addMessage("assistant", cleanedResponse)
                                }
                            } catch (e: org.json.JSONException) {
                                val cleanedResponse = cleanThinkingResponse(it)
                                addMessage("assistant", cleanedResponse)
                            }
                        }
                    }
                    .also {
                        endGeneration()
                        persistChat()
                    }
            } catch (e: Exception) {
                Log.e(tag, "Error in processWebSearch", e)
                addMessage("error", "Failed to process web search: ${e.message}")
                endGeneration()
            }
        }
    }

    fun startVoiceRecognition() {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            voiceError = "Microphone permission not granted"
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            voiceError = "Speech recognition not available"
            return
        }
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        voiceError = null
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = data?.firstOrNull() ?: ""
                        updateMessage(text)
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        voiceError = "Error: $error"
                    }

                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer?.startListening(intent)
    }

    // Quick action and attachment handlers
    var lastQuickAction by mutableStateOf<String?>(null)
        private set
    var lastAttachmentAction by mutableStateOf<String?>(null)
        private set

    fun onLatestNews() {
        lastQuickAction = "latest_news"
        performWebSearch("latest news")
    }

    fun onCreateImages() {
        lastQuickAction = "create_images"
        performWebSearch("create images")
    }

    fun onCartoonStyle() {
        lastQuickAction = "cartoon_style"
        performWebSearch("cartoon style art")
    }

    fun onCameraAttachment() {
        lastAttachmentAction = "camera"
    }

    fun onPhotosAttachment() {
        lastAttachmentAction = "photos"
    }

    fun onFilesAttachment() {
        lastAttachmentAction = "files"
    }

    fun summarizeDocument(text: String) {
        viewModelScope.launch {
            val prompt = "Summarize the following text:\n\n$text"
            addMessage("user", prompt)
            send()
        }
    }

    fun handleToolCall(toolCall: com.nervesparks.iris.data.ToolCall) {
        Log.d(tag, "Handling tool call: $toolCall")
        
        viewModelScope.launch {
            try {
                when (toolCall.name) {
                    "web_search", "brave_search" -> {
                        val query = toolCall.args["query"] as? String
                        if (query != null) {
                            Log.d(tag, "Executing web search for: $query")
                            
                            // Show tool execution in progress
                            addMessage("assistant", "🔍 Executing web search for \"$query\"...")
                            
                            // Perform the search
                            val searchResponse = webSearchService.searchWeb(query)
                            
                            if (searchResponse.success && searchResponse.results != null) {
                                // Format and display results
                                val formattedResults = webSearchService.formatSearchResults(searchResponse.results, query)
                                addMessage("assistant", formattedResults)
                            } else {
                                val errorMessage = searchResponse.error ?: "Unknown search error"
                                addMessage("assistant", "❌ Web search failed: $errorMessage")
                            }
                        } else {
                            addMessage("assistant", "❌ Invalid search query provided")
                        }
                    }
                    "wolfram_alpha" -> {
                        val query = toolCall.args["query"] as? String
                        if (query != null) {
                            addMessage("assistant", "🧮 Wolfram Alpha calculation for \"$query\"\n\nThis feature is not yet implemented. Please try a different approach.")
                        } else {
                            addMessage("assistant", "❌ Invalid Wolfram Alpha query")
                        }
                    }
                    "python", "code_interpreter" -> {
                        val code = toolCall.args["code"] as? String
                        if (code != null) {
                            addMessage("assistant", "🐍 Python code execution:\n\n```python\n$code\n```\n\nThis feature is not yet implemented. Please try a different approach.")
                        } else {
                            addMessage("assistant", "❌ Invalid Python code")
                        }
                    }
                    else -> {
                        addMessage("assistant", "❌ Unknown tool: ${toolCall.name}\n\nAvailable tools: web_search, brave_search, wolfram_alpha, python")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error handling tool call", e)
                addMessage("assistant", "❌ Tool execution error: ${e.message}")
            }
        }
    }

    fun sendImage(uri: Uri) {
        Log.d(tag, "Sending image: $uri")
    }

    var isCodeMode by mutableStateOf(false)
        private set

    fun toggleCodeMode() {
        isCodeMode = !isCodeMode
    }

    fun sendCode(code: String) {
        val prompt = "Analyze the following code:\n\n```\n$code\n```"
        addMessage("user", prompt)
        // Don't call send() to avoid infinite recursion
        // Instead, directly process the code analysis
        processCodeAnalysis(prompt)
    }

    private fun processCodeAnalysis(prompt: String) {
        viewModelScope.launch {
            try {
                startGeneration()
                
                var workingMessages = messages.toMutableList()
                val reserve = 256

                // Trim history until it fits within the context window
                var finalPrompt = ""
                while (true) {
                    finalPrompt = if (template.isNotBlank()) {
                        val jinjava = com.hubspot.jinjava.Jinjava()
                        val context = mapOf("messages" to workingMessages)
                        jinjava.render(template, context)
                    } else {
                        com.nervesparks.iris.llm.TemplateRegistry.render(
                            modelChatFormat,
                            workingMessages,
                            modelSystemPrompt,
                            includeThinkingTags = true
                        )
                    }
                    val promptTokens = llamaAndroid.countTokens(finalPrompt)
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

                contextLimit = llamaAndroid.countTokens(finalPrompt)
                maxContextLimit = modelContextLength

                var generatedTokens = 0
                llamaAndroid.send(finalPrompt)
                    .catch {
                        Log.e(tag, "processCodeAnalysis() failed", it)
                        addMessage("error", it.message ?: "")
                    }
                    .collect {
                        generatedTokens++
                        updateTokenCount(generatedTokens)
                        contextLimit = llamaAndroid.countTokens(finalPrompt) + generatedTokens

                        if (getIsMarked()) {
                            addMessage("codeBlock", it)
                        } else {
                            addMessage("assistant", it)
                        }
                    }
                    .also {
                        endGeneration()
                        persistChat()
                    }
            } catch (e: Exception) {
                Log.e(tag, "Error in processCodeAnalysis", e)
                addMessage("error", "Failed to analyze code: ${e.message}")
                endGeneration()
            }
        }
    }

    fun translate(text: String, targetLanguage: String) {
        val prompt = "Translate the following text to $targetLanguage:\n\n$text"
        addMessage("user", prompt)
        processTranslation(prompt)
    }

    /**
     * Cleans the model response by removing raw thinking tags and formatting
     */
    private fun cleanThinkingResponse(response: String): String {
        // Remove raw <think> and </think> tags
        var cleaned = response.replace("<think>", "").replace("</think>", "")
        
        // If the response starts with thinking content, try to extract just the answer
        if (cleaned.contains("Let me think") || cleaned.contains("Okay, let's see")) {
            // Try to find the actual answer after thinking
            val answerPatterns = listOf(
                "So the answer is:",
                "Therefore,",
                "Answer:",
                "The answer is:",
                "Result:"
            )
            
            for (pattern in answerPatterns) {
                if (cleaned.contains(pattern)) {
                    val parts = cleaned.split(pattern)
                    if (parts.size > 1) {
                        cleaned = parts[1].trim()
                        break
                    }
                }
            }
        }
        
        return cleaned.trim()
    }

    private fun processTranslation(prompt: String) {
        viewModelScope.launch {
            try {
                startGeneration()
                
                var workingMessages = messages.toMutableList()
                val reserve = 256

                // Trim history until it fits within the context window
                var finalPrompt = ""
                while (true) {
                    finalPrompt = if (template.isNotBlank()) {
                        val jinjava = com.hubspot.jinjava.Jinjava()
                        val context = mapOf("messages" to workingMessages)
                        jinjava.render(template, context)
                    } else {
                        com.nervesparks.iris.llm.TemplateRegistry.render(
                            modelChatFormat,
                            workingMessages,
                            modelSystemPrompt,
                            includeThinkingTags = true
                        )
                    }
                    val promptTokens = llamaAndroid.countTokens(finalPrompt)
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

                contextLimit = llamaAndroid.countTokens(finalPrompt)
                maxContextLimit = modelContextLength

                var generatedTokens = 0
                llamaAndroid.send(finalPrompt)
                    .catch {
                        Log.e(tag, "processTranslation() failed", it)
                        addMessage("error", it.message ?: "")
                    }
                    .collect {
                        generatedTokens++
                        updateTokenCount(generatedTokens)
                        contextLimit = llamaAndroid.countTokens(finalPrompt) + generatedTokens

                        if (getIsMarked()) {
                            addMessage("codeBlock", it)
                        } else {
                            addMessage("assistant", it)
                        }
                    }
                    .also {
                        endGeneration()
                        persistChat()
                    }
            } catch (e: Exception) {
                Log.e(tag, "Error in processTranslation", e)
                addMessage("error", "Failed to translate: ${e.message}")
                endGeneration()
            }
        }
    }

    fun quantizeModel(model: String, quantizeType: String) {
        viewModelScope.launch {
            val inputFile = File(getApplication<Application>().getExternalFilesDir(null), model)
            val outputFile = File(getApplication<Application>().getExternalFilesDir(null), "${model.substringBeforeLast(".")}-$quantizeType.gguf")
            llamaAndroid.quantize(inputFile.absolutePath, outputFile.absolutePath, quantizeType)
        }
    }

    private var template by mutableStateOf("")

    fun updateTemplate(template: String) {
        this.template = template
    }

    // User-defined prompt templates
    var templates = mutableStateListOf<Template>()
        private set

    fun addTemplate(template: Template) {
        templates.add(template)
    }

    fun editTemplate(updated: Template) {
        val index = templates.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            templates[index] = updated
        }
    }

    fun deleteTemplate(template: Template) {
        templates.removeAll { it.id == template.id }
    }

    fun clearLastQuickAction() {
        lastQuickAction = null
    }

    fun clearLastAttachmentAction() {
        lastAttachmentAction = null
    }

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
            // Calculate time to first token
            ttft = System.currentTimeMillis() - generationStartTime
        }
        
        // Update total generation time
        totalGenerationTime = System.currentTimeMillis() - generationStartTime
        
        // Calculate TPS in real-time
        val elapsedTime = totalGenerationTime.toDouble()
        if (elapsedTime > 0) {
            tps = (count * 1000.0) / elapsedTime
        }
        
        // Calculate average latency
        if (count > 0) {
            latency = totalGenerationTime / count
        }
        
        // Update memory usage periodically
        viewModelScope.launch {
            try {
                val memoryUsage = getMemoryUsage()
                updateMemoryUsage(memoryUsage / (1024 * 1024)) // Convert to MB
            } catch (e: Exception) {
                Log.e(tag, "Error updating memory usage", e)
            }
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

    // Model configuration variables - Initialize with default values
    private var _modelTemperature = mutableStateOf(0.7f)
    var modelTemperature: Float
        get() = _modelTemperature.value
        set(value) { _modelTemperature.value = value }
    
    private var _modelTopP = mutableStateOf(0.9f)
    var modelTopP: Float
        get() = _modelTopP.value
        set(value) { _modelTopP.value = value }
    
    private var _modelTopK = mutableStateOf(40)
    var modelTopK: Int
        get() = _modelTopK.value
        set(value) { _modelTopK.value = value }
    
    private var _modelMaxTokens = mutableStateOf(2048)
    var modelMaxTokens: Int
        get() = _modelMaxTokens.value
        set(value) { _modelMaxTokens.value = value }
    
    private var _modelContextLength = mutableStateOf(32768) // Increased for Qwen3 support
    var modelContextLength: Int
        get() = _modelContextLength.value
        set(value) { _modelContextLength.value = value }
    
    private var _modelSystemPrompt = mutableStateOf("You are a helpful AI assistant.")
    var modelSystemPrompt: String
        get() = _modelSystemPrompt.value
        set(value) { _modelSystemPrompt.value = value }
    
    private var _modelChatFormat = mutableStateOf("QWEN3")
    var modelChatFormat: String
        get() = _modelChatFormat.value
        set(value) { _modelChatFormat.value = value }
    
    private var _modelThreadCount = mutableStateOf(4)
    var modelThreadCount: Int
        get() = _modelThreadCount.value
        set(value) { _modelThreadCount.value = value }
    
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
        
        // Save to preferences
        userPreferencesRepository.setModelTemperature(temperature)
        userPreferencesRepository.setModelTopP(topP)
        userPreferencesRepository.setModelTopK(topK)
        userPreferencesRepository.setModelMaxTokens(maxTokens)
        userPreferencesRepository.setModelContextLength(contextLength)
        userPreferencesRepository.setModelSystemPrompt(systemPrompt)
        userPreferencesRepository.setModelChatFormat(chatFormat)
        userPreferencesRepository.setModelThreadCount(threadCount)
    }

    fun loadModelSettings() {
        try {
            Log.d("MainViewModel", "Loading model settings...")
            
            // Try to load each setting with individual try-catch blocks
            try {
                modelTemperature = userPreferencesRepository.getModelTemperature()
                Log.d("MainViewModel", "Loaded temperature: $modelTemperature")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading temperature, using default", e)
                modelTemperature = 0.7f
            }
            
            try {
                modelTopP = userPreferencesRepository.getModelTopP()
                Log.d("MainViewModel", "Loaded topP: $modelTopP")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading topP, using default", e)
                modelTopP = 0.9f
            }
            
            try {
                modelTopK = userPreferencesRepository.getModelTopK()
                Log.d("MainViewModel", "Loaded topK: $modelTopK")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading topK, using default", e)
                modelTopK = 40
            }
            
            try {
                modelMaxTokens = userPreferencesRepository.getModelMaxTokens()
                Log.d("MainViewModel", "Loaded maxTokens: $modelMaxTokens")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading maxTokens, using default", e)
                modelMaxTokens = 2048
            }
            
            try {
                modelContextLength = userPreferencesRepository.getModelContextLength()
                Log.d("MainViewModel", "Loaded contextLength: $modelContextLength")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading contextLength, using default", e)
                modelContextLength = 32768  // Increased for Qwen3 support
            }
            
            try {
                modelSystemPrompt = userPreferencesRepository.getModelSystemPrompt()
                Log.d("MainViewModel", "Loaded systemPrompt: $modelSystemPrompt")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading systemPrompt, using default", e)
                modelSystemPrompt = "You are a helpful AI assistant."
            }
            
            try {
                modelChatFormat = userPreferencesRepository.getModelChatFormat()
                Log.d("MainViewModel", "Loaded chatFormat: $modelChatFormat")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading chatFormat, using default", e)
                modelChatFormat = "CHATML"
            }
            
            try {
                modelThreadCount = userPreferencesRepository.getModelThreadCount()
                Log.d("MainViewModel", "Loaded threadCount: $modelThreadCount")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading threadCount, using default", e)
                modelThreadCount = 4
            }
            
            Log.d("MainViewModel", "Model settings loaded successfully")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error in loadModelSettings", e)
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
        userPreferencesRepository.setShowThinkingTokens(show)
    }

    fun updateThinkingTokenStyle(style: String) {
        thinkingTokenStyle = style
        userPreferencesRepository.setThinkingTokenStyle(style)
    }

    fun loadThinkingTokenSettings() {
        try {
            Log.d("MainViewModel", "Loading thinking token settings...")
            
            try {
                showThinkingTokens = userPreferencesRepository.getShowThinkingTokens()
                Log.d("MainViewModel", "Loaded showThinkingTokens: $showThinkingTokens")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading showThinkingTokens, using default", e)
                showThinkingTokens = true
            }
            
            try {
                thinkingTokenStyle = userPreferencesRepository.getThinkingTokenStyle()
                Log.d("MainViewModel", "Loaded thinkingTokenStyle: $thinkingTokenStyle")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading thinkingTokenStyle, using default", e)
                thinkingTokenStyle = "COLLAPSIBLE"
            }
            
            Log.d("MainViewModel", "Thinking token settings loaded successfully")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error in loadThinkingTokenSettings", e)
        }
    }


    var refresh by mutableStateOf(false)

    fun loadExistingModels(directory: File) {
        // List models in the directory that end with .gguf
        directory.listFiles { file -> file.extension == "gguf" }?.forEach { file ->
            val modelName = file.name
            Log.i("This is the modelname", modelName)
            if (!allModels.any { it["name"] == modelName }) {
                allModels += mapOf(
                    "name" to modelName,
                    "source" to "local",
                    "destination" to file.name,
                    "supportsReasoning" to "false"
                )
            }
        }

        // Check if we have any models available
        val availableModels = allModels.filter { model ->
            val destinationPath = File(directory, model["destination"].toString())
            destinationPath.exists()
        }

        if (availableModels.isNotEmpty()) {
            // Set the first available model as currentDownloadable but don't auto-load
            val firstModel = availableModels.first()
            val destinationPath = File(directory, firstModel["destination"].toString())
            currentDownloadable = Downloadable(
                firstModel["name"].toString(),
                Uri.parse(firstModel["source"].toString()),
                destinationPath
            )
            
            // If we have a default model and it exists, use it
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
            
            // DON'T show download modal when models exist - show model selection instead
            showModal = false
            showModelSelection = true
        } else {
            // No models available, show download modal
            showModal = true
            showModelSelection = false
        }
    }

    // New function to switch between models
    fun switchModel(modelName: String, directory: File) {
        val model = allModels.find { it["name"] == modelName }
        if (model != null) {
            val destinationPath = File(directory, model["destination"].toString())
            if (destinationPath.exists()) {
                // Unload current model if any
                viewModelScope.launch {
                    try {
                        llamaAndroid.unload()
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Error unloading model", e)
                    }
                }
                
                // Set new model as current
                currentDownloadable = Downloadable(
                    model["name"].toString(),
                    Uri.parse(model["source"].toString()),
                    destinationPath
                )
                
                // Load the new model
                load(destinationPath.path, userThreads = user_thread.toInt())
                
                // Update default model name
                setDefaultModelName(modelName)
                
                Log.i("MainViewModel", "Switched to model: $modelName")
            }
        }
    }

    // New function to get available models
    fun getAvailableModels(directory: File): List<Map<String, String>> {
        return allModels.filter { model ->
            val destinationPath = File(directory, model["destination"].toString())
            destinationPath.exists()
        }
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
        speechRecognizer?.destroy()
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
        Log.d(tag, "Send button clicked")
        val reserveTokens = 256
        val userMessage = removeExtraWhiteSpaces(message)
        message = ""

        if (isCodeMode) {
            sendCode(userMessage)
            return
        }

        // Add to messages console.
        if (userMessage.isNotBlank()) {
            Log.d(tag, "User message is not blank: $userMessage")
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
                    val userEmbedding = embedText(userMessage).toList()
                    val similarDocs = documentRepository.topKSimilar(userEmbedding, 3)
                    val contextDocs = similarDocs.joinToString("\n") { it.text }
                    val fullMessage = if (contextDocs.isNotEmpty()) {
                        "Context: $contextDocs\n\nQuestion: $userMessage"
                    } else {
                        userMessage
                    }

                    var workingMessages = messages.toMutableList()
                    val reserve = reserveTokens

                    // Trim history until it fits within the context window
                    var prompt = ""
                    while (true) {
                        prompt = if (template.isNotBlank()) {
                            val jinjava = com.hubspot.jinjava.Jinjava()
                            val context = mapOf("messages" to workingMessages)
                            jinjava.render(template, context)
                        } else {
                            com.nervesparks.iris.llm.TemplateRegistry.render(
                                modelChatFormat,
                                workingMessages,
                                modelSystemPrompt,
                                includeThinkingTags = true
                            )
                        }
                        val promptTokens = llamaAndroid.countTokens(prompt)
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

                    contextLimit = llamaAndroid.countTokens(prompt)
                    maxContextLimit = modelContextLength

                    var generatedTokens = 0
                    llamaAndroid.send(prompt)
                        .catch {
                            Log.e(tag, "send() failed", it)
                            addMessage("error", it.message ?: "")
                        }
                        .collect {
                            generatedTokens++
                            updateTokenCount(generatedTokens)
                            contextLimit = llamaAndroid.countTokens(prompt) + generatedTokens

                            if (getIsMarked()) {
                                addMessage("codeBlock", it)
                            } else {
                                try {
                                    val json = org.json.JSONObject(it)
                                    if (json.has("tool")) {
                                        val tool = json.getString("tool")
                                        val args = json.getJSONObject("args")
                                        val argsMap = mutableMapOf<String, Any>()
                                        args.keys().forEach { key ->
                                            argsMap[key] = args.get(key)
                                        }
                                        handleToolCall(com.nervesparks.iris.data.ToolCall(tool, argsMap))
                                    } else {
                                        // Store the full response (including thinking) in conversation history
                                        addMessage("assistant", it)
                                    }
                                } catch (e: org.json.JSONException) {
                                    // Store the full response (including thinking) in conversation history
                                    addMessage("assistant", it)
                                }
                            }
                        }
                } finally {
                    endGeneration()
                    if (!getIsCompleteEOT()) {
                        trimEOT()
                    }
                }
            

            }
        } else {
            Log.d(tag, "User message is blank")
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
                
                Log.d(tag, "Loading model with settings: threads=$modelThreadCount, topK=$modelTopK, topP=$modelTopP, temp=$modelTemperature")
                
                // Use model settings instead of default parameters
                llamaAndroid.load(
                    pathToModel, 
                    userThreads = modelThreadCount, 
                    topK = modelTopK, 
                    topP = modelTopP, 
                    temp = modelTemperature
                )
                
                Log.d(tag, "Model loaded successfully: ${loadedModelName.value}")
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
        Log.d(tag, "Loading model by name: $modelName from directory: ${directory.absolutePath}")
        val modelFile = File(directory, modelName)
        if (modelFile.exists()) {
            Log.d(tag, "Model file exists at: ${modelFile.absolutePath}")
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

    fun persistChat() {
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
        // This is now a synchronous wrapper for the async search
        // The actual search should be called from a coroutine scope
        return SearchResponse(
            success = false,
            data = null,
            error = "Use searchModelsAsync() for proper async search"
        )
    }

    suspend fun searchModelsAsync(query: String): SearchResponse {
        return try {
            val token = userPreferencesRepository.getHuggingFaceToken()
            val authHeader = if (token.isNotEmpty()) "Bearer $token" else null
            
            val models = huggingFaceApiService.searchModels(query, authHeader)
            
            val searchResults = models.map { model ->
                ModelSearchResult(
                    id = model.id,
                    name = model.name,
                    description = model.tags.joinToString(", "),
                    downloads = model.downloads,
                    likes = model.likes,
                    tags = model.tags
                )
            }
            
            SearchResponse(
                success = true,
                data = searchResults,
                error = null
            )
        } catch (e: Exception) {
            Log.e(tag, "Error searching models", e)
            SearchResponse(
                success = false,
                data = null,
                error = "Search failed: ${e.message}"
            )
        }
    }

    fun getModelDetails(modelId: String): ModelDetailsResponse {
        // This is now a synchronous wrapper for the async search
        return ModelDetailsResponse(
            success = false,
            data = null,
            error = "Use getModelDetailsAsync() for proper async search"
        )
    }

    suspend fun getModelDetailsAsync(modelId: String): ModelDetailsResponse {
        return try {
            val token = userPreferencesRepository.getHuggingFaceToken()
            val authHeader = if (token.isNotEmpty()) "Bearer $token" else null
            
            val model = huggingFaceApiService.getModelDetails(modelId, authHeader)
            
            val detailResult = ModelDetailResult(
                id = model.id,
                name = model.name,
                description = model.tags.joinToString(", "),
                downloads = model.downloads,
                likes = model.likes,
                tags = model.tags,
                siblings = model.siblings.map { file ->
                    ModelFile(
                        filename = file.filename,
                        size = file.size,
                        quantType = null // Not available in current API response
                    )
                }
            )
            
            ModelDetailsResponse(
                success = true,
                data = listOf(detailResult),
                error = null
            )
        } catch (e: Exception) {
            Log.e(tag, "Error getting model details", e)
            ModelDetailsResponse(
                success = false,
                data = null,
                error = "Failed to get model details: ${e.message}"
            )
        }
    }

    fun setTestHuggingFaceToken() {
        // Set a test token for development purposes
        // In production, this should be obtained from secure storage
        val testToken = "hf_test_token_for_development"
        userPreferencesRepository.setHuggingFaceToken(testToken)
        Log.d(tag, "Test HuggingFace token set for development")
    }

    // Memory management functions
    fun unloadCurrentModel() {
        viewModelScope.launch {
            try {
                llamaAndroid.unload()
                loadedModelName.value = ""
                selectedModel = ""
                Log.d(tag, "Current model unloaded successfully")
            } catch (e: Exception) {
                Log.e(tag, "Error unloading model", e)
            }
        }
    }
    
    suspend fun getMemoryUsage(): Long {
        return try {
            llamaAndroid.getMemoryUsage()
        } catch (e: Exception) {
            Log.e(tag, "Error getting memory usage", e)
            0L
        }
    }
    
    fun isModelLoaded(): Boolean {
        return loadedModelName.value.isNotEmpty()
    }
    
    fun getLoadedModelName(): String {
        return loadedModelName.value
    }
    
    // Memory optimization
    fun optimizeMemory() {
        viewModelScope.launch {
            try {
                // Clear message history if it's too large
                if (messages.size > 100) {
                    val keepCount = 50
                    messages = messages.takeLast(keepCount)
                    Log.d(tag, "Cleared message history, kept last $keepCount messages")
                }
                
                // Force garbage collection if memory usage is high
                val memoryUsage = getMemoryUsage()
                if (memoryUsage > 500 * 1024 * 1024) { // 500MB threshold
                    System.gc()
                    Log.d(tag, "Forced garbage collection due to high memory usage: ${memoryUsage / (1024 * 1024)}MB")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error optimizing memory", e)
            }
        }
    }

    // Search state management
    var isSearching by mutableStateOf(false)
        private set
    var currentSearchQuery by mutableStateOf("")
        private set
    var searchProgress by mutableStateOf("")
        private set
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

data class Template(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val content: String
)

fun sentThreadsValue(){

}