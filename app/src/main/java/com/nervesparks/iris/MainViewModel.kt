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
import timber.log.Timber
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import java.io.File
import java.util.Locale
import java.util.UUID

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import com.nervesparks.iris.data.repository.ChatRepository
import com.nervesparks.iris.data.DocumentRepository
import com.nervesparks.iris.data.db.Chat
import com.nervesparks.iris.data.db.Message
import com.nervesparks.iris.viewmodel.ChatViewModel
import com.nervesparks.iris.viewmodel.ModelViewModel
import com.nervesparks.iris.viewmodel.SearchViewModel
import com.nervesparks.iris.viewmodel.VoiceViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.nervesparks.iris.data.WebSearchService
import com.nervesparks.iris.data.AndroidSearchService
import com.nervesparks.iris.data.search.SearchResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.nervesparks.iris.llm.EmbeddingService
import com.nervesparks.iris.llm.performDocumentIndexing
import com.nervesparks.iris.data.exceptions.ValidationException
import com.nervesparks.iris.data.exceptions.ErrorHandler

@HiltViewModel
class MainViewModel @Inject constructor(
    private val llamaAndroid: LLamaAndroid,
    private val modelLoader: com.nervesparks.iris.llm.ModelLoader,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val chatRepository: ChatRepository,
    private val modelRepository: com.nervesparks.iris.data.repository.ModelRepository,
    private val huggingFaceApiService: com.nervesparks.iris.data.HuggingFaceApiService,
    private val documentRepository: DocumentRepository,
    private val embeddingService: EmbeddingService,
    private val webSearchService: WebSearchService,
    private val androidSearchService: AndroidSearchService,
    private val searchViewModel: com.nervesparks.iris.viewmodel.SearchViewModel,
    private val voiceViewModel: com.nervesparks.iris.viewmodel.VoiceViewModel,
    private val modelViewModel: com.nervesparks.iris.viewmodel.ModelViewModel,
    private val chatViewModel: com.nervesparks.iris.viewmodel.ChatViewModel,
    application: Application
) : AndroidViewModel(application) {

    // Chat operations
    val chats = chatRepository.observeChats()
    val chatStats = chatRepository.observeChatStats()

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

    var isDocumentIndexing by mutableStateOf(false)
        private set

    var documentIndexingError by mutableStateOf<String?>(null)
        private set

    var documentIndexingSuccess by mutableStateOf<String?>(null)
        private set

    fun indexDocument(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isDocumentIndexing = true
                documentIndexingError = null
                documentIndexingSuccess = null
            }

            try {
                performDocumentIndexing(text, embeddingService, documentRepository)
                withContext(Dispatchers.Main) {
                    documentIndexingSuccess = "Document indexed successfully"
                }
            } catch (e: ValidationException) {
                Timber.e(e, "Validation error while indexing document")
                withContext(Dispatchers.Main) {
                    documentIndexingError = e.message ?: "Failed to index document"
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to index document")
                withContext(Dispatchers.Main) {
                    documentIndexingError = e.message ?: "Failed to index document"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isDocumentIndexing = false
                }
            }
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
    // Ensure native stripping respects UI + model capability
    private fun applyThinkStripGate() {
        try {
            LLamaAndroid.instance().let {
                val enableStrip = !(supportsReasoning && showThinkingTokens)
                it.set_strip_think(enableStrip)
            }
        } catch (_: Exception) {}
    }

    // Flag indicating if the currently loaded model supports reasoning tokens
    var supportsReasoning by mutableStateOf(false)

    var downloadableModels by mutableStateOf<List<Downloadable>>(emptyList())
        private set

    private val _defaultModelName = mutableStateOf("")
    val defaultModelName: State<String> = _defaultModelName

    // Model configuration variables - must be initialized BEFORE init {}
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

    private var _modelChatFormat = mutableStateOf("CHATML")
    var modelChatFormat: String
        get() = _modelChatFormat.value
        set(value) { _modelChatFormat.value = value }

    private var _modelThreadCount = mutableStateOf(4)
    var modelThreadCount: Int
        get() = _modelThreadCount.value
        set(value) { _modelThreadCount.value = value }

    // GPU layers to offload (-1 = Auto)
    private var _modelGpuLayers = mutableStateOf(-1)
    var modelGpuLayers: Int
        get() = _modelGpuLayers.value
        set(value) { _modelGpuLayers.value = value }

    init {
        loadDefaultModelName()
        loadModelSettings() // Re-enabled model settings loading
        loadThinkingTokenSettings()

        // Set a default Hugging Face token if none exists
        if (userPreferencesRepository.huggingFaceToken.isEmpty()) {
            setTestHuggingFaceToken()
        }

        // Detect hardware capabilities at startup - moved to after library loads
        // detectHardwareCapabilities()



        viewModelScope.launch {
            try {
                val models = modelRepository.refreshAvailableModels()
                availableModelMetadata = models
                Timber.d("Loaded ${models.size} models from repository (defaults + API)")
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing available models from API; repository will provide curated defaults")
                availableModelMetadata = emptyList()
            }
        }
    }
    private fun loadDefaultModelName() {
        viewModelScope.launch {
            userPreferencesRepository.defaultModelNameFlow
                .catch { e ->
                    Timber.tag("MainViewModel").e(e, "Error loading default model name, using empty string")
                    _defaultModelName.value = ""
                }
                .collectLatest { name ->
                    _defaultModelName.value = name
                }
        }
    }

    fun setDefaultModelName(modelName: String) {
        _defaultModelName.value = modelName
        viewModelScope.launch {
            userPreferencesRepository.setDefaultModelName(modelName)
        }
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
    var user_thread by mutableStateOf(4f) // Default to 4 threads
    var topP by mutableStateOf(0.9f)
    var topK by mutableStateOf(40)
    var temp by mutableStateOf(0.7f)

    private var availableModelMetadata: List<Map<String, String>> = emptyList()

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

    // Hardware acceleration state variables - Initialize with CPU defaults
    var availableBackends by mutableStateOf("CPU")
    var currentBackend by mutableStateOf("CPU")
    var optimalBackend by mutableStateOf("CPU")
    var gpuInfo by mutableStateOf("CPU Only - Use Settings to detect hardware")
    var isAdrenoGpu by mutableStateOf(false)
    var showBackendSelection by mutableStateOf(false)
    var backendError by mutableStateOf<String?>(null)

    // Web search services are injected via DI

    fun performWebSearch(query: String, summarize: Boolean = true) {
        // Add the search query as a user message
        viewModelScope.launch {
            pruneForNewTokens(llamaAndroid.countTokens("Search the web for: $query"))
        }
        addMessage("user", "Search the web for: $query")

        // Use SearchViewModel from UI layer for proper separation of concerns
        searchViewModel.performWebSearch(query, summarize = true)

        // Monitor search results and add to chat when complete (optimized)
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 300 // 30 seconds max wait

            while (searchViewModel.isSearching && attempts < maxAttempts) {
                kotlinx.coroutines.delay(100)
                attempts++
            }

            if (searchViewModel.searchResults.isNotEmpty()) {
                // Use the optimized summary method for better performance
                val summary = searchViewModel.summarizeSearchResults()
                addMessage("assistant", summary)
            } else if (searchViewModel.searchError != null) {
                addMessage("assistant", "Search failed: ${searchViewModel.searchError}")
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
                            includeThinkingTags = supportsReasoning
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
                    addMessage(
                        "system",
                        "⚠️ Earlier messages were removed to stay within the model's context limit."
                    )
                }

                contextLimit = llamaAndroid.countTokens(finalPrompt)
                maxContextLimit = modelContextLength

                var generatedTokens = 0
                llamaAndroid.send(finalPrompt)
                    .catch {
                        Timber.e(it, "processWebSearch() failed")
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
                Timber.e(e, "Error in processWebSearch")
                addMessage("error", "Failed to process web search: ${e.message}")
                endGeneration()
            }
        }
    }

    fun startVoiceRecognition(context: Context) {
        // Use VoiceViewModel from UI layer for proper separation of concerns
        voiceViewModel.startVoiceRecognition(context)
    }

    // Legacy voice recognition function
    private fun legacyStartVoiceRecognition() {
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
            pruneForNewTokens(llamaAndroid.countTokens(prompt))
            addMessage("user", prompt)
            send()
        }
    }

    fun handleToolCall(toolCall: com.nervesparks.iris.data.ToolCall) {
        Timber.d("Handling tool call: $toolCall")
        
        viewModelScope.launch {
            try {
                when (toolCall.name) {
                    "web_search", "brave_search" -> {
                        val query = toolCall.args["query"] as? String
                        if (query != null) {
                            Timber.d("Executing web search for: $query")
                            
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
                Timber.e(e, "Error handling tool call")
                addMessage("assistant", "❌ Tool execution error: ${e.message}")
            }
        }
    }

    fun sendImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer
                    .process(image)
                    .addOnSuccessListener { visionText ->
                        val extractedText = visionText.text
                        if (extractedText.isNotBlank()) {
                            // Index the document for retrieval and summarize for the chat
                            indexDocument(extractedText)
                            summarizeDocument(extractedText)
                        } else {
                            addMessage("assistant", "❌ No text found in image")
                        }
                    }
                    .addOnFailureListener { e ->
                        Timber.e(e, "OCR failed")
                        addMessage("assistant", "❌ OCR failed: ${e.message}")
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error processing image")
                addMessage("assistant", "❌ Unable to process image: ${e.message}")
            }
        }
    }

    var isCodeMode by mutableStateOf(false)
        private set

    fun toggleCodeMode() {
        isCodeMode = !isCodeMode
    }

    fun sendCode(code: String) {
        val prompt = "Analyze the following code:\n\n```\n$code\n```"
        viewModelScope.launch {
            pruneForNewTokens(llamaAndroid.countTokens(prompt))
        }
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
                            includeThinkingTags = supportsReasoning
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
                    addMessage(
                        "system",
                        "⚠️ Earlier messages were removed to stay within the model's context limit."
                    )
                }

                contextLimit = llamaAndroid.countTokens(finalPrompt)
                maxContextLimit = modelContextLength

                var generatedTokens = 0
                llamaAndroid.send(finalPrompt)
                    .catch {
                        Timber.e(it, "processCodeAnalysis() failed")
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
                Timber.e(e, "Error in processCodeAnalysis")
                addMessage("error", "Failed to analyze code: ${e.message}")
                endGeneration()
            }
        }
    }

    fun translate(text: String, targetLanguage: String) {
        val prompt = "Translate the following text to $targetLanguage:\n\n$text"
        viewModelScope.launch {
            pruneForNewTokens(llamaAndroid.countTokens(prompt))
        }
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
                            includeThinkingTags = supportsReasoning
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
                    addMessage(
                        "system",
                        "⚠️ Earlier messages were removed to stay within the model's context limit."
                    )
                }

                contextLimit = llamaAndroid.countTokens(finalPrompt)
                maxContextLimit = modelContextLength

                var generatedTokens = 0
                llamaAndroid.send(finalPrompt)
                    .catch {
                        Timber.e(it, "processTranslation() failed")
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
                Timber.e(e, "Error in processTranslation")
                addMessage("error", "Failed to translate: ${e.message}")
                endGeneration()
            }
        }
    }

    private fun stripThinking(input: String): String {
        // Remove <think>...</think> segments and any stray tags for display-only purposes
        var out = input
        out = out.replace(Regex("(?is)<think>.*?</think>"), "")
        out = out.replace("<think>", "").replace("</think>", "")
        return out
    }

    suspend fun quantizeModel(model: String, quantizeType: String): Int {
        val inputFile = File(getApplication<Application>().getExternalFilesDir(null), model)
        val outputFile = File(
            getApplication<Application>().getExternalFilesDir(null),
            "${model.substringBeforeLast(".")}-$quantizeType.gguf"
        )
        return llamaAndroid.quantize(inputFile.absolutePath, outputFile.absolutePath, quantizeType)
    }

    private var template by mutableStateOf("")

    fun updateTemplate(template: String) {
        this.template = template
    }

    // User-defined prompt templates
    var templates = mutableStateListOf<Template>()
        private set

    suspend fun addTemplate(template: Template): Boolean {
        return try {
            templates.add(template)
            userPreferencesRepository.saveTemplates(templates)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun editTemplate(updated: Template): Boolean {
        val index = templates.indexOfFirst { it.id == updated.id }
        return if (index != -1) {
            try {
                templates[index] = updated
                userPreferencesRepository.saveTemplates(templates)
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    suspend fun deleteTemplate(template: Template): Boolean {
        val removed = templates.removeAll { it.id == template.id }
        return if (removed) {
            try {
                userPreferencesRepository.saveTemplates(templates)
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
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

    // GPU offload reporting (N/N)
    var offloadedLayers by mutableStateOf(-1)
    var totalLayers by mutableStateOf(-1)

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
                Timber.e(e, "Error updating memory usage")
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
            Timber.e("Error getting memory usage: ${e.message}")
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

    var showModelSettings by mutableStateOf(false)

    // Model configuration functions
    fun updateModelSettings(
        temperature: Float,
        topP: Float,
        topK: Int,
        maxTokens: Int,
        contextLength: Int,
        systemPrompt: String,
        chatFormat: String,
        threadCount: Int,
        gpuLayers: Int
    ) {
        // Use ModelViewModel from UI layer for proper separation of concerns
        modelViewModel.updateModelSettings(
            temperature, topP, topK, maxTokens, contextLength,
            systemPrompt, chatFormat, threadCount, gpuLayers
        )
    }

    // Legacy model settings function
    private fun legacyUpdateModelSettings(
        temperature: Float = modelTemperature,
        topP: Float = modelTopP,
        topK: Int = modelTopK,
        maxTokens: Int = modelMaxTokens,
        contextLength: Int = modelContextLength,
        systemPrompt: String = modelSystemPrompt,
        chatFormat: String = modelChatFormat,
        threadCount: Int = modelThreadCount,
        gpuLayers: Int = -1
    ) {
        modelTemperature = temperature
        modelTopP = topP
        modelTopK = topK
        modelMaxTokens = maxTokens
        modelContextLength = contextLength
        modelSystemPrompt = systemPrompt
        modelChatFormat = chatFormat
        modelThreadCount = threadCount
        // only set if provided
        if (gpuLayers != -2) {
            try {
                val v = gpuLayers
                // store in a backing pref via repository if available later in file
            } catch (_: Exception) {}
        }

        // Save to preferences
        viewModelScope.launch {
            userPreferencesRepository.setModelTemperature(temperature)
            userPreferencesRepository.setModelTopP(topP)
            userPreferencesRepository.setModelTopK(topK)
            userPreferencesRepository.setModelMaxTokens(maxTokens)
            userPreferencesRepository.setModelContextLength(contextLength)
            userPreferencesRepository.setModelSystemPrompt(systemPrompt)
            userPreferencesRepository.setModelChatFormat(chatFormat)
            userPreferencesRepository.setModelThreadCount(threadCount)
            userPreferencesRepository.setModelGpuLayers(gpuLayers)
        }
    }

    fun loadModelSettings() {
        // Model settings are now handled by ModelViewModel
        // This function kept for backward compatibility
    }

    // Legacy load model settings function
    private fun legacyLoadModelSettings() {
        viewModelScope.launch {
            try {
                Timber.tag("MainViewModel").d("Loading model settings...")

                // Try to load each setting with individual try-catch blocks
                try {
                    modelTemperature = userPreferencesRepository.getModelTemperature()
                    Timber.tag("MainViewModel").d("Loaded temperature: $modelTemperature")
                } catch (e: Exception) {
                    Timber.tag("MainViewModel").e(e, "Error loading temperature, using default")
                    modelTemperature = 0.7f
                }

                try {
                    modelTopP = userPreferencesRepository.getModelTopP()
                    Timber.tag("MainViewModel").d("Loaded topP: $modelTopP")
                } catch (e: Exception) {
                    Timber.tag("MainViewModel").e(e, "Error loading topP, using default")
                    modelTopP = 0.9f
                }

                try {
                    modelTopK = userPreferencesRepository.getModelTopK()
                    Timber.tag("MainViewModel").d("Loaded topK: $modelTopK")
                } catch (e: Exception) {
                    Timber.tag("MainViewModel").e(e, "Error loading topK, using default")
                    modelTopK = 40
                }

                try {
                    modelMaxTokens = userPreferencesRepository.getModelMaxTokens()
                    Timber.tag("MainViewModel").d("Loaded maxTokens: $modelMaxTokens")
                } catch (e: Exception) {
                    Timber.tag("MainViewModel").e(e, "Error loading maxTokens, using default")
                    modelMaxTokens = 2048
                }

                try {
                    modelContextLength = userPreferencesRepository.getModelContextLength()
                    Timber.tag("MainViewModel").d("Loaded contextLength: $modelContextLength")
                } catch (e: Exception) {
                    Timber.tag("MainViewModel").e(e, "Error loading contextLength, using default")
                    modelContextLength = 32768  // Increased for Qwen3 support
                }
                maxContextLimit = modelContextLength

                try {
                    modelSystemPrompt = userPreferencesRepository.getModelSystemPrompt()
                    Timber.tag("MainViewModel").d("Loaded systemPrompt: $modelSystemPrompt")
                } catch (e: Exception) {
                    Timber.tag("MainViewModel").e(e, "Error loading systemPrompt, using default")
                    modelSystemPrompt = "You are a helpful AI assistant."
                }

                try {
                    modelChatFormat = userPreferencesRepository.getModelChatFormat()
                    Timber.tag("MainViewModel").d("Loaded chatFormat: $modelChatFormat")
                } catch (e: Exception) {
                    Timber.tag("MainViewModel").e(e, "Error loading chatFormat, using default")
                    modelChatFormat = "CHATML"
                }

                try {
                    modelThreadCount = userPreferencesRepository.getModelThreadCount()
                    Timber.tag("MainViewModel").d("Loaded threadCount: $modelThreadCount")
                } catch (e: Exception) {
                    Timber.tag("MainViewModel").e(e, "Error loading threadCount, using default")
                    modelThreadCount = 4
                }

                try {
                    modelGpuLayers = userPreferencesRepository.getModelGpuLayers()
                    Timber.tag("MainViewModel").d("Loaded gpuLayers: $modelGpuLayers")
                } catch (e: Exception) {
                    Timber.tag("MainViewModel").e(e, "Error loading gpuLayers, using default")
                    modelGpuLayers = -1
                }

                Timber.tag("MainViewModel").d("Model settings loaded successfully")
            } catch (e: Exception) {
                Timber.tag("MainViewModel").e(e, "Error in loadModelSettings")
            }
        }
    }

    fun showModelSettings() {
        showModelSettings = true
    }

    fun hideModelSettings() {
        showModelSettings = false
    }

    fun updateShowThinkingTokens(show: Boolean) {
        // Use ChatViewModel from UI layer for proper separation of concerns
        chatViewModel.updateShowThinkingTokens(show)
    }

    fun updateThinkingTokenStyle(style: String) {
        // Use ChatViewModel from UI layer for proper separation of concerns
        chatViewModel.updateThinkingTokenStyle(style)
    }

    private fun loadThinkingTokenSettings() {
        viewModelScope.launch {
            try {
                Timber.tag("MainViewModel").d("Loading thinking token settings...")

                try {
                    showThinkingTokens = userPreferencesRepository.getShowThinkingTokens()
                    Timber.tag("MainViewModel").d("Loaded showThinkingTokens: $showThinkingTokens")
                } catch (e: Exception) {
                    Timber.tag("MainViewModel").e(e, "Error loading showThinkingTokens, using default")
                    showThinkingTokens = true
                }

                try {
                    thinkingTokenStyle = userPreferencesRepository.getThinkingTokenStyle()
                    Timber.tag("MainViewModel").d("Loaded thinkingTokenStyle: $thinkingTokenStyle")
                } catch (e: Exception) {
                    Timber.tag("MainViewModel").e(e, "Error loading thinkingTokenStyle, using default")
                    thinkingTokenStyle = "COLLAPSIBLE"
                }

                Timber.tag("MainViewModel").d("Thinking token settings loaded successfully")
            } catch (e: Exception) {
                Timber.tag("MainViewModel").e(e, "Error in loadThinkingTokenSettings")
            }
        }
    }



    var refresh by mutableStateOf(false)

    /**
     * Force cleanup of resources to help with memory management
     */
    fun cleanupResources() {
        viewModelScope.launch {
            try {
                // Force garbage collection
                System.gc()
                System.runFinalization()

                Timber.tag("MainViewModel").d("Resource cleanup completed")
            } catch (e: Exception) {
                Timber.tag("MainViewModel").e(e, "Error during resource cleanup")
            }
        }
    }

    fun loadExistingModels(directory: File) {
        Timber.d("loadExistingModels called with directory: ${directory.absolutePath}")
        Timber.d("Directory exists: ${directory.exists()}")
        Timber.d("Directory is readable: ${directory.canRead()}")

        viewModelScope.launch {
            try {
                val installedModels = withContext(Dispatchers.IO) {
                    modelRepository.getAvailableModels(directory)
                }

                Timber.d("Repository returned ${installedModels.size} installed models")

                availableModelMetadata = mergeModelMetadata(availableModelMetadata, installedModels)

                if (installedModels.isNotEmpty()) {
                    Timber.d("Models available, setting up UI for model selection")

                    val firstModel = installedModels.first()
                    val destinationPath = File(directory, firstModel["destination"].toString())
                    currentDownloadable = Downloadable(
                        firstModel["name"].toString(),
                        Uri.parse(firstModel["source"].toString()),
                        destinationPath
                    )

                    if (defaultModelName.value.isNotEmpty()) {
                        val defaultModel = installedModels.find { model -> model["name"] == defaultModelName.value }
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
                    Timber.d("Set showModal=false, showModelSelection=true")
                } else {
                    Timber.d("No models available, showing download modal")
                    showModal = true
                    showModelSelection = false
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading existing models")
            }
        }
    }

    private fun mergeModelMetadata(
        existing: List<Map<String, String>>,
        updates: List<Map<String, String>>
    ): List<Map<String, String>> {
        if (updates.isEmpty()) {
            return existing
        }

        val updateKeys = updates.mapNotNull { it["name"] }.toSet()
        val mergedUpdates = updates.map { update ->
            val existingEntry = existing.firstOrNull { it["name"] == update["name"] }
            existingEntry?.toMutableMap()?.apply { putAll(update) } ?: update
        }
        val retained = existing.filter { it["name"] !in updateKeys }
        return mergedUpdates + retained
    }

    // New function to switch between models
    fun switchModel(modelName: String, directory: File) {
        val model = availableModelMetadata.find { it["name"] == modelName }
        if (model == null) {
            val msg = "Model not found: $modelName"
            Timber.tag("MainViewModel").e(msg)
            modelSwitchError = msg
            modelSwitchMessage = null
            return
        }

        val destinationPath = File(directory, model["destination"].toString())
        if (!destinationPath.exists()) {
            val msg = "Model file not found: ${destinationPath.path}"
            Timber.tag("MainViewModel").e(msg)
            modelSwitchError = msg
            modelSwitchMessage = null
            return
        }

        Timber.d("=== SWITCH MODEL REASONING DEBUG ===")
        Timber.d("Switching to model: $modelName")
        Timber.d("Model configuration: $model")

        val reasoningSupport = model["supportsReasoning"]
        Timber.d("Raw reasoning support value: $reasoningSupport")

        supportsReasoning = reasoningSupport == "true"
        Timber.d("Final supportsReasoning: $supportsReasoning")
        Timber.d("=== END SWITCH MODEL DEBUG ===")

        currentDownloadable = Downloadable(
            model["name"].toString(),
            Uri.parse(model["source"].toString()),
            destinationPath
        )

        applyThinkStripGate()

        viewModelScope.launch {
            isModelSwitching = true
            modelSwitchError = null
            modelSwitchMessage = null

            try {
                val unloadResult = withContext(Dispatchers.IO) { modelLoader.unloadModel() }
                unloadResult.fold(
                    onSuccess = {
                        Timber.tag("MainViewModel").d("Model unloaded successfully")

                        val backend = "cpu"
                        val backendSet = try {
                            llamaAndroid.setBackend(backend)
                        } catch (backendException: Exception) {
                            Timber.tag("MainViewModel").e(backendException, "Error setting backend to $backend")
                            false
                        }

                        if (!backendSet) {
                            Timber.tag("MainViewModel").w("Failed to set backend to $backend, attempting CPU fallback")
                            try {
                                llamaAndroid.setBackend("cpu")
                            } catch (cpuFallbackException: Exception) {
                                Timber.tag("MainViewModel").e(cpuFallbackException, "CPU fallback failed")
                            }
                        }

                        val loadResult = withContext(Dispatchers.IO) {
                            modelLoader.loadModel(
                                modelPath = destinationPath.path,
                                threadCount = modelThreadCount,
                                backend = backend,
                                temperature = modelTemperature,
                                topP = modelTopP,
                                topK = modelTopK,
                                gpuLayers = modelGpuLayers
                            )
                        }

                        loadResult.fold(
                            onSuccess = {
                                loadedModelName.value = destinationPath.name

                                val validationPassed = validateModelSwitch()
                                if (validationPassed) {
                                    setDefaultModelName(modelName)
                                    modelSwitchError = null
                                    modelSwitchMessage = "Switched to model: $modelName"
                                    Timber.tag("MainViewModel").i("Switched to model: $modelName")
                                } else {
                                    val error = "Model validation failed after loading $modelName"
                                    modelSwitchError = error
                                    modelSwitchMessage = null
                                    Timber.tag("MainViewModel").e(error)
                                    withContext(Dispatchers.IO) { modelLoader.unloadModel() }
                                    loadedModelName.value = ""
                                }
                            },
                            onFailure = { loadError ->
                                val message = loadError.message ?: "Failed to load model: $modelName"
                                modelSwitchError = message
                                modelSwitchMessage = null
                                loadedModelName.value = ""
                                Timber.tag("MainViewModel").e(loadError, "Error loading model $modelName")
                            }
                        )
                    },
                    onFailure = { unloadError ->
                        val message = unloadError.message ?: "Failed to unload current model"
                        modelSwitchError = message
                        modelSwitchMessage = null
                        Timber.tag("MainViewModel").e(unloadError, "Error unloading model before switching")
                    }
                )
            } catch (e: Exception) {
                val message = e.message ?: "Unexpected error switching model"
                modelSwitchError = message
                modelSwitchMessage = null
                Timber.tag("MainViewModel").e(e, "Unexpected error during model switch")
            } finally {
                isModelSwitching = false
            }
        }
    }

    private suspend fun validateModelSwitch(
        maxAttempts: Int = 5,
        delayMillis: Long = 300L
    ): Boolean {
        repeat(maxAttempts) { attempt ->
            val modelHandle = llamaAndroid.getModel()
            val contextHandle = llamaAndroid.getContext()
            val batchHandle = llamaAndroid.getBatch()
            val samplerHandle = llamaAndroid.getSampler()

            val valid = modelHandle != 0L && contextHandle != 0L && batchHandle != 0L && samplerHandle != 0L
            if (valid) {
                Timber.tag("MainViewModel").d("Model validation succeeded on attempt ${attempt + 1}")
                return true
            }

            Timber.tag("MainViewModel").w(
                "Model validation attempt ${attempt + 1} failed: model=$modelHandle context=$contextHandle batch=$batchHandle sampler=$samplerHandle"
            )
            delay(delayMillis)
        }

        Timber.tag("MainViewModel").e("Model validation failed after $maxAttempts attempts")
        return false
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



    fun textToSpeech(context: Context, text: String) {
        // Use VoiceViewModel from UI layer for proper separation of concerns
        voiceViewModel.textToSpeech(context, text)
    }

    // Legacy text-to-speech function
    private fun legacyTextToSpeech(context: Context) {
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
    var isModelSwitching by mutableStateOf(false)
        private set
    var modelSwitchError by mutableStateOf<String?>(null)
        private set
    var modelSwitchMessage by mutableStateOf<String?>(null)
        private set
    var currentDownloadable: Downloadable? by mutableStateOf(null)

    override fun onCleared() {
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
        super.onCleared()

        viewModelScope.launch {
            try {
                val unloadResult = modelLoader.unloadModel()
                unloadResult.fold(
                    onSuccess = {
                        Timber.tag("MainViewModel").d("Model unloaded on clear")
                    },
                    onFailure = { e ->
                        Timber.tag("MainViewModel").e(e, "Error unloading model on clear")
                    }
                )
            } catch (exc: IllegalStateException) {
                addMessage("error", exc.message ?: "")
            } finally {
                try {
                    llamaAndroid.shutdown()
                } catch (e: Exception) {
                    Timber.tag("MainViewModel").e(e, "Error shutting down llamaAndroid")
                }
            }
        }
    }

    private suspend fun pruneForNewTokens(additionalTokens: Int) {
        if (contextLimit + additionalTokens <= modelContextLength) return

        var workingMessages = messages.toMutableList()
        var currentContextLimit = calculateContextLimit(workingMessages)

        // If we have room, no need to prune
        if (currentContextLimit + additionalTokens <= modelContextLength) {
            return
        }

        var wasPruned = false

        // Strategy 1: Remove oldest non-system messages first
        while (currentContextLimit + additionalTokens > modelContextLength && workingMessages.size > 1) {
            val oldestNonSystemIndex = workingMessages.indexOfFirst { it["role"] != "system" }
            if (oldestNonSystemIndex >= 0) {
                workingMessages.removeAt(oldestNonSystemIndex)
                currentContextLimit = calculateContextLimit(workingMessages)
                wasPruned = true
            } else {
                break
            }
        }

        // Strategy 2: If still over limit, summarize the conversation
        if (currentContextLimit + additionalTokens > modelContextLength && workingMessages.size > 2) {
            val summary = generateConversationSummary(workingMessages)
            if (summary != null) {
                // Replace old messages with summary
                val systemMessage = workingMessages.firstOrNull { it["role"] == "system" }
                val lastFewMessages = workingMessages.takeLast(3) // Keep last 3 messages

                @Suppress("TYPE_MISMATCH")
                workingMessages = mutableListOf<Map<String, Any>>().apply {
                    systemMessage?.let { add(it) }
                    add(mapOf<String, Any>(
                        "role" to "assistant",
                        "content" to "Previous conversation summary: $summary",
                        "timestamp" to System.currentTimeMillis()
                    ))
                    addAll(lastFewMessages)
                }

                // Convert to proper types for template rendering
                val stringMessages = workingMessages.map { msg ->
                    mapOf(
                        "role" to msg["role"].toString(),
                        "content" to msg["content"].toString()
                    )
                }

                currentContextLimit = calculateContextLimitWithStringMessages(workingMessages, stringMessages)
                wasPruned = true
            }
        }

        if (wasPruned) {
            messages = workingMessages
            contextLimit = currentContextLimit
            addMessage(
                "system",
                "Earlier messages were summarized to stay within the model's context limit."
            )
        }
    }

    private suspend fun calculateContextLimit(messages: List<Map<String, Any>>): Int {
        return llamaAndroid.countTokens(buildPromptForContext(messages))
    }

    private suspend fun calculateContextLimitWithStringMessages(messages: List<Map<String, Any>>, stringMessages: List<Map<String, String>>): Int {
        return if (template.isNotBlank()) {
            val jinjava = com.hubspot.jinjava.Jinjava()
            val context = mapOf("messages" to messages)
            llamaAndroid.countTokens(jinjava.render(template, context))
        } else {
            llamaAndroid.countTokens(com.nervesparks.iris.llm.TemplateRegistry.render(
                modelChatFormat,
                stringMessages,
                modelSystemPrompt,
                includeThinkingTags = supportsReasoning
            ))
        }
    }

    private fun buildPromptForContext(messages: List<Map<String, Any>>): String {
        return if (template.isNotBlank()) {
            val jinjava = com.hubspot.jinjava.Jinjava()
            val context = mapOf("messages" to messages)
            jinjava.render(template, context)
        } else {
            // Convert Map<String, Any> to Map<String, String> for TemplateRegistry
            val stringMessages: List<Map<String, String>> = messages.map { msg ->
                mapOf(
                    "role" to msg["role"].toString(),
                    "content" to msg["content"].toString()
                )
            }
            com.nervesparks.iris.llm.TemplateRegistry.render(
                modelChatFormat,
                stringMessages,
                modelSystemPrompt,
                includeThinkingTags = supportsReasoning
            )
        }
    }

    private suspend fun generateConversationSummary(messages: List<Map<String, Any>>): String? {
        try {
            // Extract key points from the conversation
            val userMessages = messages.filter { it["role"] == "user" }
            val assistantMessages = messages.filter { it["role"] == "assistant" }

            if (userMessages.size < 2 && assistantMessages.size < 2) return null

            // Simple summary based on message count and key topics
            val summary = StringBuilder()
            summary.append("Previous conversation involved ${userMessages.size} user queries and ${assistantMessages.size} responses. ")

            // Extract key topics (very basic - could be enhanced with NLP)
            val allContent = messages.joinToString(" ") { it["content"].toString() }
            val words = allContent.split(" ").take(20) // Take first 20 words as key topics
            summary.append("Key topics discussed: ${words.joinToString(", ")}.")

            return summary.toString()
        } catch (e: Exception) {
            Timber.tag("MainViewModel").e(e, "Error generating conversation summary")
            return null
        }
    }


    fun send() {
        Timber.d("Send button clicked")

        // Check if model is loaded before proceeding
        if (!isModelLoaded()) {
            Timber.w("Cannot send message: No model is loaded")
            // Show user feedback that no model is loaded
            addMessage("error", "No model is loaded. Please load a model from the Models screen first.")
            return
        }

        val reserveTokens = 256
        val userMessage = removeExtraWhiteSpaces(message)
        message = ""

        if (isCodeMode) {
            sendCode(userMessage)
            return
        }

        // Add to messages console.
        if (userMessage.isNotBlank()) {
            Timber.d("User message is not blank: $userMessage")
            if(first){
                addMessage("system", "This is a conversation between User and Iris, a friendly chatbot. Iris is helpful, kind, honest, good at writing, and never fails to answer any requests immediately and with precision. When responding, Iris should use <think> tags to show its reasoning process before providing the final answer.")
                addMessage("user", "Hi")
                addMessage("assistant", "How may I help You?")
                first = false
            }

            viewModelScope.launch {
                pruneForNewTokens(llamaAndroid.countTokens(userMessage))
            }
            addMessage("user", userMessage)
            persistChat()

            // Start performance monitoring
            startGeneration()

            viewModelScope.launch {
                try {
                    val userEmbedding = embeddingService.embed(userMessage)
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
                                includeThinkingTags = supportsReasoning
                            )
                        }
                        // Count on stripped template if thinking is disabled
                        val countPrompt = if (supportsReasoning && showThinkingTokens) prompt else stripThinking(prompt)
                        val promptTokens = llamaAndroid.countTokens(countPrompt)
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
                        addMessage(
                            "system",
                            "⚠️ Earlier messages were removed to stay within the model's context limit."
                        )
                    }

                    val countPrompt2 = if (supportsReasoning && showThinkingTokens) prompt else stripThinking(prompt)
                    contextLimit = llamaAndroid.countTokens(countPrompt2)
                    maxContextLimit = modelContextLength

                    var generatedTokens = 0
                    val inferenceStartTime = System.currentTimeMillis()

                    llamaAndroid.send(prompt)
                        .catch {
                            Timber.e(it, "send() failed")
                            addMessage("error", it.message ?: "")
                        }
                        .collect {
                            generatedTokens++
                            val currentTime = System.currentTimeMillis()
                            val inferenceTime = currentTime - inferenceStartTime

                            // Record performance metrics through ModelViewModel for proper separation of concerns
                            modelViewModel.recordInferencePerformance(
                                tokensGenerated = 1,
                                inferenceTime = inferenceTime,
                                memoryUsage = getMemoryUsage()
                            )

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
                                        val display = if (showThinkingTokens && supportsReasoning) it else stripThinking(it)
                                        addMessage("assistant", display)
                                    }
                                } catch (e: org.json.JSONException) {
                                    val display = if (showThinkingTokens && supportsReasoning) it else stripThinking(it)
                                    addMessage("assistant", display)
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
            Timber.d("User message is blank")
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
//                Timber.e(exc, "bench() failed")
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
    
    // Comparative benchmark results
    var comparativeBenchmarkResults by mutableStateOf<Map<String, Any>?>(null)
    var isComparativeBenchmarkRunning by mutableStateOf(false)
    var selectedBenchmarkModel by mutableStateOf("")
    var showBenchmarkModelSelection by mutableStateOf(false)

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
                            Timber.d("Token collected: $emittedString")
                        }
                    }
            } catch (exc: IllegalStateException) {
                Timber.e(exc, "myCustomBenchmark() failed")
            } catch (exc: kotlinx.coroutines.TimeoutCancellationException) {
                Timber.e(exc, "myCustomBenchmark() timed out")
            } catch (exc: Exception) {
                Timber.e(exc, "Unexpected error during myCustomBenchmark()")
            } finally {
                // Benchmark complete, log the final tokens per second value
                val elapsedTime = System.currentTimeMillis() - benchmarkStartTime
                val finalTokensPerSecond = if (elapsedTime > 0) {
                    tokensList.size.toDouble() / (elapsedTime / 1000.0)
                } else {
                    0.0
                }
                Timber.d("Benchmark complete. Tokens/sec: $finalTokensPerSecond")

                // Update the final tokens per second and stop updating the value
                tokensPerSecondsFinal = finalTokensPerSecond
                isBenchmarkingComplete = true // Mark benchmarking as complete
            }
        }
    }
    
    fun runComparativeBenchmark() {
        viewModelScope.launch {
            try {
                Timber.d("Starting comparative benchmark...")
                isComparativeBenchmarkRunning = true
                comparativeBenchmarkResults = null
                
                                    // Use actual model and context handles for real benchmarking
                    Timber.d("Calling native benchmark function with real model...")
                    val modelHandle = llamaAndroid.getModel()
                    val contextHandle = llamaAndroid.getContext()
                    val batchHandle = llamaAndroid.getBatch()
                    val samplerHandle = llamaAndroid.getSampler()

                    Timber.d("Model handle: $modelHandle, Context handle: $contextHandle")

                    // Verify that all handles are valid before proceeding
                    if (modelHandle == 0L || contextHandle == 0L || batchHandle == 0L || samplerHandle == 0L) {
                        Timber.e("Model not loaded. Skipping comparative benchmark.")
                        comparativeBenchmarkResults = mapOf("error" to "Model not loaded.")
                        return@launch
                    }

                    // Ensure we're on the correct thread where the library is loaded
                    val resultsJson = try {
                        withContext(llamaAndroid.runLoop) {
                            llamaAndroid.runComparativeBenchmark(modelHandle, contextHandle, batchHandle, samplerHandle)
                        }
                    } catch (e: IllegalStateException) {
                        Timber.e(e, "Comparative benchmark failed")
                        comparativeBenchmarkResults = mapOf("error" to "Benchmark failed: ${e.message}")
                        return@launch
                    } catch (e: Exception) {
                        Timber.e(e, "Comparative benchmark failed")
                        comparativeBenchmarkResults = mapOf("error" to "Benchmark failed: ${e.message}")
                        return@launch
                    }
                Timber.d("Native benchmark returned: $resultsJson")
                
                // Parse the JSON results
                try {
                    val jsonObject = org.json.JSONObject(resultsJson)
                    val results = mutableMapOf<String, Any>()
                    
                    // Parse CPU results
                    val cpuObj = jsonObject.getJSONObject("cpu")
                    results["cpu_tokens_per_sec"] = cpuObj.getDouble("tokens_per_sec")
                    results["cpu_duration_ms"] = cpuObj.getInt("duration_ms")
                    results["cpu_tokens_generated"] = cpuObj.getInt("tokens_generated")
                    
                    // Parse GPU results
                    val gpuObj = jsonObject.getJSONObject("gpu")
                    results["gpu_available"] = gpuObj.getBoolean("available")
                    
                    if (results["gpu_available"] as Boolean) {
                        results["gpu_tokens_per_sec"] = gpuObj.getDouble("tokens_per_sec")
                        results["gpu_duration_ms"] = gpuObj.getInt("duration_ms")
                        results["gpu_tokens_generated"] = gpuObj.getInt("tokens_generated")
                        
                        // Calculate speedup
                        val speedup = jsonObject.getDouble("speedup")
                        results["speedup"] = speedup
                        results["speedup_percentage"] = ((speedup - 1.0) * 100.0)
                    } else {
                        results["gpu_error"] = gpuObj.getString("error")
                    }
                    
                    comparativeBenchmarkResults = results
                    Timber.d("Comparative benchmark completed: $results")
                    Timber.d("Setting comparativeBenchmarkResults to: $comparativeBenchmarkResults")
                    
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing benchmark results")
                    comparativeBenchmarkResults = mapOf("error" to "Failed to parse results: ${e.message}")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Comparative benchmark failed")
                comparativeBenchmarkResults = mapOf("error" to "Benchmark failed: ${e.message}")
            } finally {
                isComparativeBenchmarkRunning = false
            }
        }
    }
    
    fun showBenchmarkModelSelection() {
        showBenchmarkModelSelection = true
    }
    
    fun hideBenchmarkModelSelection() {
        showBenchmarkModelSelection = false
        selectedBenchmarkModel = ""
    }
    
    fun runBenchmarkWithModel(modelName: String, directory: File) {
        viewModelScope.launch {
            isComparativeBenchmarkRunning = true

            // Save the currently loaded model so we can restore it later
            val previousModelName = loadedModelName.value
            val previousModelPath = availableModelMetadata
                .find { it["name"] == previousModelName }
                ?.let { File(directory, it["destination"].toString()).path }

            try {
                Timber.d("Starting benchmark with model: $modelName")

                // Attempt to load the user selected model
                val loaded = loadModelByName(modelName, directory)
                if (!loaded) {
                    comparativeBenchmarkResults = mapOf("error" to "Failed to load model: $modelName")
                    return@launch
                }
                
                // Wait for the model to fully load and get valid handles
                var attempts = 0
                var modelHandle = 0L
                var contextHandle = 0L
                var batchHandle = 0L
                var samplerHandle = 0L
                
                while (attempts < 10 && (modelHandle == 0L || contextHandle == 0L || batchHandle == 0L || samplerHandle == 0L)) {
                    delay(500) // Wait 500ms between attempts
                    modelHandle = llamaAndroid.getModel()
                    contextHandle = llamaAndroid.getContext()
                    batchHandle = llamaAndroid.getBatch()
                    samplerHandle = llamaAndroid.getSampler()
                    attempts++
                    Timber.d("Attempt $attempts: Model=$modelHandle, Context=$contextHandle, Batch=$batchHandle, Sampler=$samplerHandle")
                }
                
                if (modelHandle == 0L || contextHandle == 0L || batchHandle == 0L || samplerHandle == 0L) {
                    Timber.e("Model handles still invalid after $attempts attempts")
                    comparativeBenchmarkResults = mapOf("error" to "Model not properly loaded after $attempts attempts")
                    return@launch
                }

                // Run the benchmark and capture any errors
                try {
                    runComparativeBenchmark()
                } catch (benchExc: Exception) {
                    Timber.e(benchExc, "Benchmark with model failed")
                    comparativeBenchmarkResults = mapOf("error" to "Benchmark failed: ${benchExc.message}")
                }

            } finally {
                // Restore the previously loaded model
                if (previousModelPath != null) {
                    try {
                        val backend = if (currentBackend.equals("OpenCL", true)) "opencl" else "cpu"
                        load(previousModelPath, userThreads = modelThreadCount, backend = backend)
                    } catch (restoreExc: Exception) {
                        Timber.e(restoreExc, "Failed to reload previous model")
                    }
                }
                isComparativeBenchmarkRunning = false
                hideBenchmarkModelSelection()
            }
        }
    }

    var loadedModelName = mutableStateOf("");

    fun load(pathToModel: String, userThreads: Int, backend: String = "cpu") {
        // Check if native library is loaded first
        if (!llamaAndroid.isNativeLibraryLoaded()) {
            Timber.e("Cannot load model: Native library not loaded")
            return
        }

        // Unload any existing model first, then set the backend and load the new model
        viewModelScope.launch {
            try {
                Timber.d("Unloading existing model before loading new one")
                try {
                    val hasModel = llamaAndroid.getModel() != 0L
                    Timber.d("Current model pointer: ${llamaAndroid.getModel()}, hasModel: $hasModel")
                    if (hasModel) {
                        llamaAndroid.unload()
                        Timber.d("Successfully unloaded existing model")
                    }
                    loadedModelName.value = ""
                } catch (e: Exception) {
                    Timber.w("Error unloading existing model: ${e.message}")
                }

                Timber.d("Setting backend to: $backend")
                try {
                    // For Vulkan backend, do additional validation since it can fail during model loading
                    var actualBackend = backend.lowercase()
                    if (backend.lowercase() == "vulkan") {
                        Timber.d("Validating Vulkan backend...")
                        val vulkanSuccess = llamaAndroid.setBackend("vulkan")
                        Timber.d("Vulkan backend set result: $vulkanSuccess")

                        if (vulkanSuccess) {
                            // Try to validate Vulkan by checking if we can get backend info without crashing
                            try {
                                val gpuInfo = llamaAndroid.getGpuInfo()
                                Timber.d("Vulkan validation - GPU info: $gpuInfo")
                                // If we get here without crashing, Vulkan should be usable
                            } catch (e: Exception) {
                                Timber.w("Vulkan validation failed, falling back to CPU: ${e.message}")
                                actualBackend = "cpu"
                                llamaAndroid.setBackend("cpu")
                            }
                        } else {
                            Timber.w("Vulkan backend not supported, using CPU")
                            actualBackend = "cpu"
                        }
                    } else {
                        // For CPU and other backends, use normal logic
                        val backendSuccess = llamaAndroid.setBackend(actualBackend)
                        Timber.d("Backend set result for $actualBackend: $backendSuccess")
                        if (!backendSuccess) {
                            Timber.e("Failed to set backend to $actualBackend, falling back to CPU")
                            actualBackend = "cpu"
                            llamaAndroid.setBackend("cpu")
                        }
                    }

                    Timber.d("Final backend selection: $actualBackend")
                } catch (e: Exception) {
                    Timber.e("Exception setting backend: ${e.message}")
                    try {
                        llamaAndroid.setBackend("cpu")
                        Timber.d("Fallback to CPU backend successful")
                    } catch (e2: Exception) {
                        Timber.e("Failed to fallback to CPU backend: ${e2.message}")
                    }
                }

                Timber.d("Using initial backend=$backend for validation")

                // Basic model file validation before loading
                val modelFile = File(pathToModel)
                if (!modelFile.exists()) {
                    Timber.e("Model file does not exist: $pathToModel")
                    return@launch
                }

                val modelSizeMB = modelFile.length() / (1024 * 1024)
                Timber.d("Model file size: ${modelSizeMB}MB")

                // Validate model file is readable and not corrupted
                try {
                    val fileSize = modelFile.length()
                    if (fileSize < 1024) { // Minimum reasonable model size
                        Timber.e("Model file too small (${fileSize} bytes), likely corrupted")
                        return@launch
                    }

                    // Try to read first few bytes to ensure file is accessible
                    modelFile.inputStream().use { stream ->
                        val buffer = ByteArray(1024)
                        val bytesRead = stream.read(buffer)
                        if (bytesRead < 4) {
                            Timber.e("Cannot read model file header")
                            return@launch
                        }
                        // Check for GGUF magic number (basic validation)
                        val magic = String(buffer, 0, 4, Charsets.US_ASCII)
                        if (magic != "GGUF") {
                            Timber.w("File doesn't appear to be a valid GGUF model (magic: $magic)")
                            // Continue anyway as some models might have different headers
                        }
                    }
                    Timber.d("Model file validation passed")
                } catch (e: Exception) {
                    Timber.e("Error validating model file: ${e.message}")
                    return@launch
                }

                // Check available memory (rough estimate)
                val runtime = Runtime.getRuntime()
                val maxMemoryMB = runtime.maxMemory() / (1024 * 1024)
                val freeMemoryMB = (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()) / (1024 * 1024)

                Timber.d("Memory: max=${maxMemoryMB}MB, free=${freeMemoryMB}MB")

                // Let llama.cpp handle memory management - other Android LLM apps work
                // Only reject models that are truly impossible (e.g., larger than device RAM)
                val deviceRamGB = try {
                    val activityManager = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    val memoryInfo = ActivityManager.MemoryInfo()
                    activityManager.getMemoryInfo(memoryInfo)
                    memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0) // Convert to GB
                } catch (e: Exception) {
                    Timber.w("Could not detect device RAM: ${e.message}")
                    8.0 // Assume 8GB if detection fails
                }

                Timber.i("Device RAM: ${deviceRamGB}GB, Model size: ${modelSizeMB}MB")

                // Only reject if model is larger than device RAM (accounting for OS usage)
                val maxModelSizeMB = (deviceRamGB * 1024 * 0.7).toInt() // 70% of RAM

                if (modelSizeMB > maxModelSizeMB) {
                    Timber.e("Model too large: ${modelSizeMB}MB exceeds device capacity (${maxModelSizeMB}MB)")
                    Timber.i("This model requires more RAM than your device has available")
                    return@launch
                }

                // Allow llama.cpp to manage memory - it uses native allocation
                Timber.i("Allowing model load - llama.cpp will manage memory allocation")

                // Check if model is too large for available memory - force CPU if needed
                // With largeHeap=true, we can use more memory, so be less conservative
                val shouldUseCpu = when {
                    modelSizeMB > maxMemoryMB * 1.2 -> { // Model > 1.2x max heap is too big
                        Timber.w("Model size (${modelSizeMB}MB) greatly exceeds max heap (${maxMemoryMB}MB) - forcing CPU backend")
                        true
                    }
                    modelSizeMB > maxMemoryMB * 0.9 -> { // Model > 0.9x max heap may cause issues
                        Timber.w("Model size (${modelSizeMB}MB) approaches heap limit (${maxMemoryMB}MB) - consider smaller model")
                        // Still try but warn heavily
                        false
                    }
                    else -> {
                        Timber.d("Model size (${modelSizeMB}MB) within heap limits (${maxMemoryMB}MB)")
                        false
                    }
                }

                // Override backend selection based on memory constraints
                val finalBackend = if (shouldUseCpu && backend.lowercase() != "cpu") {
                    Timber.d("Switching from $backend to CPU due to memory constraints")
                    "cpu"
                } else {
                    backend
                }

                // Update gpuLayers for final backend choice
                val effectiveGpuLayers = when (finalBackend.lowercase()) {
                    "cpu" -> 0  // No GPU layers for CPU backend
                    else -> modelGpuLayers  // Use configured value for GPU backends
                }

                Timber.d("Final backend selection: $finalBackend (gpuLayers=$effectiveGpuLayers)")

                // If we switched backends due to memory constraints, actually set the new backend
                if (finalBackend != backend) {
                    try {
                        val switchSuccess = llamaAndroid.setBackend(finalBackend.lowercase())
                        Timber.d("Backend switch to $finalBackend: $switchSuccess")
                    } catch (e: Exception) {
                        Timber.e("Failed to switch to final backend $finalBackend: ${e.message}")
                    }
                }

                // For large models, use more conservative parameters to avoid memory issues
                val conservativeParams = if (modelSizeMB > maxMemoryMB * 0.5) {
                    Timber.d("Using conservative parameters for large model")
                    // Reduce thread count and use smaller defaults
                    mapOf(
                        "threadCount" to minOf(userThreads, 2), // Limit threads for large models
                        "temperature" to modelTemperature,
                        "topP" to modelTopP,
                        "topK" to modelTopK,
                        "gpuLayers" to effectiveGpuLayers
                    )
                } else {
                    mapOf(
                        "threadCount" to userThreads,
                        "temperature" to modelTemperature,
                        "topP" to modelTopP,
                        "topK" to modelTopK,
                        "gpuLayers" to effectiveGpuLayers
                    )
                }

                // Use conservative context length for large models
                val contextLength = if (modelSizeMB > maxMemoryMB * 0.4) {
                    1024 // Smaller context for large models
                } else {
                    2048 // Default context length
                }

                Timber.d("Using context length: $contextLength for model size ${modelSizeMB}MB")

                // Use centralized ModelLoader for consistent error handling and reporting
                val result = modelLoader.loadModel(
                    modelPath = pathToModel,
                    threadCount = conservativeParams["threadCount"] as Int,
                    backend = finalBackend,
                    temperature = conservativeParams["temperature"] as Float,
                    topP = conservativeParams["topP"] as Float,
                    topK = conservativeParams["topK"] as Int,
                    gpuLayers = conservativeParams["gpuLayers"] as Int,
                    contextLength = contextLength
                )

                result.fold(
                    onSuccess = { sessionId ->
                        Timber.tag("MainViewModel").d("Model loaded: $pathToModel")
                        loadedModelName.value = File(pathToModel).name
                    },
                    onFailure = { e ->
                        Timber.tag("MainViewModel").e(e, "Error loading model")
                        loadedModelName.value = "" // Reset on failure
                    }
                )
            } catch (e: Exception) {
                Timber.tag("MainViewModel").e(e, "Error in load function")
                loadedModelName.value = "" // Reset on failure
            }
        }
    }

    // Legacy load function - DEPRECATED: Use modelViewModel.load() directly
    private fun legacyLoad(pathToModel: String, userThreads: Int, backend: String = "cpu") {
        viewModelScope.launch {
            try{
                llamaAndroid.unload()
            } catch (exc: IllegalStateException){
                Timber.e(exc, "load() failed")
            }
                            try {
                // Set desired backend prior to loading model/context
                try {
                    val requested = backend.lowercase()
                    val success = llamaAndroid.setBackend(requested)
                    if (success) {
                        currentBackend = if (requested == "vulkan") "Vulkan" else if (requested == "opencl") "OpenCL" else "CPU"
                        Timber.d("Backend set OK: $currentBackend")
                    } else {
                        currentBackend = "CPU"
                        Timber.e("Backend set failed for '$requested', falling back to CPU")
                    }
                } catch (be: Exception) {
                    currentBackend = "CPU"
                    Timber.e("Exception when setting backend to $backend: ${be.message}")
                }
                
                var modelName = pathToModel.split("/")
                loadedModelName.value = modelName.last()
                
                // Add reasoning support detection here
                Timber.d("=== REASONING SUPPORT DEBUG ===")
                Timber.d("Loading model: ${loadedModelName.value}")
                Timber.d("All models count: ${availableModelMetadata.size}")
                Timber.d("All model names: ${availableModelMetadata.map { it["name"] }}")
                
                val foundModel = availableModelMetadata.find { it["name"] == loadedModelName.value }
                Timber.d("Found model in availableModelMetadata: $foundModel")
                
                val reasoningSupport = foundModel?.get("supportsReasoning")
                Timber.d("Raw reasoning support value: $reasoningSupport")
                
                // Fallback logic: if model not found, check if it's a known reasoning model
                supportsReasoning = if (reasoningSupport == "true") {
                    true
                } else if (foundModel == null) {
                    // Fallback: check if model name contains reasoning-related keywords
                    val reasoningKeywords = listOf("reasoning", "think", "qwen", "nemotron", "openreasoning")
                    val hasReasoningKeyword = reasoningKeywords.any { keyword ->
                        loadedModelName.value.lowercase().contains(keyword.lowercase())
                    }
                    Timber.d("Model not found in availableModelMetadata, checking keywords. Has reasoning keyword: $hasReasoningKeyword")
                    hasReasoningKeyword
                } else {
                    false
                }
                
                // Remove forced reasoning; trust metadata / keyword fallback only
                
                Timber.d("Final supportsReasoning: $supportsReasoning")
                Timber.d("=== END REASONING DEBUG ===")
                
                showModal = false
                showAlert = true
                
                Timber.d("Loading model with settings: backend=$currentBackend, threads=$modelThreadCount, ngl=${modelGpuLayers}, topK=$modelTopK, topP=$modelTopP, temp=$modelTemperature")
                // Ensure native library is loaded before first JNI call
                if (!llamaAndroid.isNativeLibraryLoaded()) {
                    Timber.w("Native lib not yet loaded; attempting sync load")
                    llamaAndroid.ensureLibraryLoaded()
                }
                
                // Use model settings instead of default parameters
                llamaAndroid.load(
                    pathToModel,
                    userThreads = modelThreadCount,
                    topK = modelTopK,
                    topP = modelTopP,
                    temp = modelTemperature,
                    gpuLayers = modelGpuLayers
                )
                
                Timber.d("Model loaded successfully: ${loadedModelName.value}")
                showAlert = false
                applyThinkStripGate()
                // Keep native token logs quiet in normal operation
                try { llamaAndroid.setVerboseTokens(false) } catch (_: Exception) {}
                // Fetch offload counts for display
                try {
                    val counts = llamaAndroid.getOffloadCounts()
                    if (counts.size == 2) {
                        offloadedLayers = counts[0]
                        totalLayers = counts[1]
                        Timber.d("Offload counts: ${counts[0]}/${counts[1]}")
                    }
                } catch (_: Exception) {}
                // Export runtime diagnostics
                try {
                    val diag = llamaAndroid.exportDiag()
                    Timber.d("Runtime diag: $diag")
                } catch (_: Exception) {}

            } catch (exc: IllegalStateException) {
                Timber.e(exc, "load() failed")
                // Since OpenCL is disabled, just report the error
                addMessage("error", "Failed to load model: ${exc.message}")
            } catch (exc: Exception) {
                Timber.e(exc, "load() failed with exception")
                addMessage("error", "Failed to load model: ${exc.message}")
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
        val modelName = modelPath.substringAfterLast("/")
        Timber.d("=== REASONING SUPPORT DEBUG ===")
        Timber.d("Loading model: $modelName")
        Timber.d("All models count: ${availableModelMetadata.size}")
        Timber.d("All model names: ${availableModelMetadata.map { it["name"] }}")
        
        val foundModel = availableModelMetadata.find { it["name"] == modelName }
        Timber.d("Found model in availableModelMetadata: $foundModel")
        
        val reasoningSupport = foundModel?.get("supportsReasoning")
        Timber.d("Raw reasoning support value: $reasoningSupport")
        
        // Fallback logic: if model not found, check if it's a known reasoning model
        supportsReasoning = if (reasoningSupport == "true") {
            true
        } else if (foundModel == null) {
            // Fallback: check if model name contains reasoning-related keywords
            val reasoningKeywords = listOf("reasoning", "think", "qwen", "nemotron", "openreasoning")
            val hasReasoningKeyword = reasoningKeywords.any { keyword ->
                modelName.lowercase().contains(keyword.lowercase())
            }
            Timber.d("Model not found in availableModelMetadata, checking keywords. Has reasoning keyword: $hasReasoningKeyword")
            hasReasoningKeyword
        } else {
            false
        }
        Timber.d("Final supportsReasoning: $supportsReasoning")
        
        // Set chat template from model definition
        val chatTemplate = foundModel?.get("chatTemplate")
        if (chatTemplate != null) {
            Timber.d("Setting chat template from model definition: $chatTemplate")
            modelChatFormat = chatTemplate
        } else {
            Timber.d("No chat template found in model definition, using default: $modelChatFormat")
        }
        
        Timber.d("=== END REASONING DEBUG ===")
        
        // Prefer Vulkan, then OpenCL, else CPU
        val backend = when {
            availableBackends.contains("Vulkan") -> "vulkan"
            availableBackends.contains("OpenCL") -> "opencl"
            else -> "cpu"
        }
        load(modelPath, modelThreadCount, backend = backend)
    }
    
    /**
     * Load a model by name from the external files directory
     */
    fun loadModelByName(modelName: String, directory: File): Boolean {
        Timber.d("Loading model by name: $modelName from directory: ${directory.absolutePath}")
        // Hint native where to scan for ggml backends (directory that contains libggml-*.so)
        try {
            val nativeLibDir = getApplication<Application>().applicationInfo.nativeLibraryDir
            LLamaAndroid.instance().setBackendSearchDir(nativeLibDir)
            Timber.d("Set backend search dir to $nativeLibDir")
        } catch (e: Exception) {
            Timber.w("Failed to set backend search dir: ${e.message}")
        }
        Timber.d("=== REASONING SUPPORT DEBUG ===")
        Timber.d("All models count: ${availableModelMetadata.size}")
        Timber.d("All model names: ${availableModelMetadata.map { it["name"] }}")

        val foundModel = availableModelMetadata.find { it["name"] == modelName }
        Timber.d("Found model in availableModelMetadata: $foundModel")

        val reasoningSupport = foundModel?.get("supportsReasoning")
        Timber.d("Raw reasoning support value: $reasoningSupport")

        // Fallback logic: if model not found, check if it's a known reasoning model
        supportsReasoning = if (reasoningSupport == "true") {
            true
        } else if (foundModel == null) {
            val reasoningKeywords = listOf("reasoning", "think", "qwen", "nemotron", "openreasoning")
            val hasReasoningKeyword = reasoningKeywords.any { keyword ->
                modelName.lowercase().contains(keyword.lowercase())
            }
            Timber.d("Model not found in availableModelMetadata, checking keywords. Has reasoning keyword: $hasReasoningKeyword")
            hasReasoningKeyword
        } else {
            false
        }
        Timber.d("Final supportsReasoning: $supportsReasoning")

        // Set chat template from model definition
        val chatTemplate = foundModel?.get("chatTemplate")
        if (chatTemplate != null) {
            Timber.d("Setting chat template from model definition: $chatTemplate")
            modelChatFormat = chatTemplate
        } else {
            Timber.d("No chat template found in model definition, using default: $modelChatFormat")
        }

        Timber.d("=== END REASONING DEBUG ===")

        // Remove forced reasoning; trust metadata / keyword fallback only

        return try {
            val model = availableModelMetadata.find { it["name"] == modelName }
            if (model != null) {
                val destinationPath = File(directory, model["destination"].toString())
                if (destinationPath.exists()) {
                    // Use the currently selected backend (support Vulkan/OpenCL/CPU)
                    val backend = when {
                        currentBackend.equals("Vulkan", true) -> "vulkan"
                        currentBackend.equals("OpenCL", true) -> "opencl"
                        else -> "cpu"
                    }
                    load(destinationPath.path, userThreads = modelThreadCount, backend = backend)
                    true
                } else {
                    val msg = "Model file not found: ${destinationPath.path}"
                    Timber.e(msg)
                    addMessage("error", msg)
                    false
                }
            } else {
                val msg = "Model not found: $modelName"
                Timber.e(msg)
                addMessage("error", msg)
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading model by name: ${e.message}")
            addMessage("error", "Failed to load model: ${e.message}")
            false
        }
    }

    // Set the current backend (CPU or GPU)
    fun selectBackend(backend: String) {
        viewModelScope.launch {
            try {
                val success = llamaAndroid.setBackend(backend.lowercase())
                if (success) {
                    currentBackend = backend
                    backendError = null
                    Timber.d("Backend changed to: $backend")

                    // If a model is already loaded, recreate with the new backend
                    if (llamaAndroid.getModel() != 0L) {
                        Timber.d("Recreating model/context for backend=$backend")
                        try { llamaAndroid.unload() } catch (_: Exception) {}
                        // Ensure backend search dir remains set
                        try {
                            val nativeLibDir = getApplication<Application>().applicationInfo.nativeLibraryDir
                            LLamaAndroid.instance().setBackendSearchDir(nativeLibDir)
                        } catch (_: Exception) {}
                        // Reload previously selected model name if any
                        val name = loadedModelName.value
                        if (name.isNotBlank()) {
                            val extDir = getApplication<Application>().getExternalFilesDir(null) ?: return@launch
                            loadModelByName(name, extDir)
                        }
                    }
                } else {
                    backendError = "Failed to switch backend to $backend"
                    currentBackend = "CPU"
                    Timber.e("Backend switch failed, reverted to CPU")
                }
            } catch (e: Exception) {
                Timber.e("Failed to set backend to $backend: ${e.message}")
                backendError = "Failed to switch backend to $backend: ${e.message}"
                currentBackend = "CPU"
                try {
                    llamaAndroid.setBackend("cpu")
                } catch (_: Exception) {}
            }
        }
    }

    // Hardware acceleration detection - Called manually from settings
    fun detectHardwareCapabilities() {
        viewModelScope.launch {
            try {
                Timber.d("Starting hardware detection...")

                // Force initialization of native library by using the dispatcher
                try {
                    Timber.d("Forcing native library initialization...")
                    withContext(llamaAndroid.runLoop) {
                        Timber.d("Native library initialization triggered")
                    }
                } catch (e: Exception) {
                    Timber.e("Error during native library initialization: ${e.message}")
                }

                // Check if native library is loaded before calling native functions
                if (!llamaAndroid.isNativeLibraryLoaded()) {
                    Timber.w("Native library not loaded, attempting synchronous load...")
                    if (!llamaAndroid.ensureLibraryLoaded()) {
                        Timber.w("Synchronous library load failed, using CPU-only mode")
                        availableBackends = "CPU"
                        currentBackend = "CPU"
                        optimalBackend = "CPU"
                        gpuInfo = "CPU Only - Native library not loaded"
                        isAdrenoGpu = false
                        backendError = "Native library not available"
                        return@launch
                    }
                }

                // Try to detect real hardware capabilities
                val rawBackends = llamaAndroid.getAvailableBackends().split(",").map { it.trim() }
                // Filter out Vulkan to avoid crashes - force CPU-only for stability
                val backends = rawBackends.filter { it.lowercase() != "vulkan" }
                availableBackends = backends.joinToString(",")
                optimalBackend = "cpu" // Force CPU as optimal to avoid Vulkan issues
                gpuInfo = llamaAndroid.getGpuInfo()
                isAdrenoGpu = llamaAndroid.isAdrenoGpu()
                currentBackend = optimalBackend
                backendError = null

                Timber.d("Hardware detection: Available backends: $availableBackends")
                Timber.d("Hardware detection: Optimal backend: $optimalBackend")
                Timber.d("Hardware detection: GPU info: $gpuInfo")
                Timber.d("Hardware detection: Is Adreno GPU: $isAdrenoGpu")
            } catch (e: Exception) {
                Timber.e("Error detecting hardware capabilities: ${e.message}")
                // Fallback to CPU-only
                availableBackends = "CPU"
                currentBackend = "CPU"
                optimalBackend = "CPU"
                gpuInfo = "CPU Only - Detection failed"
                isAdrenoGpu = false
                backendError = "Hardware detection failed: ${e.message}"
            }
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
        messages.last()["content"]?.let { Timber.e(it) }
    }

    private fun removeExtraWhiteSpaces(input: String): String {
        // Replace multiple white spaces with a single space
        return input.replace("\\s+".toRegex(), " ")
    }

    fun persistChat() {
        // Don't persist empty chats (no user messages)
        val hasUserMessages = messages.any { it["role"] == "user" && it["content"]?.isNotBlank() == true }
        if (!hasUserMessages) {
            Timber.d("Not persisting chat: no user messages found")
            return
        }

        val title = messages.firstOrNull { it["role"] == "user" }?.get("content")?.take(64) ?: "Chat"
        val baseChat = currentChat?.copy(title = title, updated = System.currentTimeMillis())
            ?: Chat(title = title)

        viewModelScope.launch(Dispatchers.IO) {
            try {
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
                Timber.d("Chat persisted successfully with id: $id")
            } catch (e: Exception) {
                Timber.e(e, "Error persisting chat")
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
            val token = userPreferencesRepository.huggingFaceToken
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
            Timber.e(e, "Error searching models")
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
            val token = userPreferencesRepository.huggingFaceToken
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
            Timber.e(e, "Error getting model details")
            ModelDetailsResponse(
                success = false,
                data = null,
                error = "Failed to get model details: ${e.message}"
            )
        }
    }

    fun setTestHuggingFaceToken() {
        // SECURITY: Removed hardcoded test token
        // In production, tokens should only be set by user input
        // This function now serves as a placeholder for proper token management
        Timber.w("Test token setting is disabled for security. Please set HuggingFace token through settings.")
    }

    // Memory management functions
    fun unloadCurrentModel() {
        viewModelScope.launch {
            try {
                val result = modelLoader.unloadModel()
                result.fold(
                    onSuccess = {
                        loadedModelName.value = ""
                        selectedModel = ""
                        Timber.d("Current model unloaded successfully")
                    },
                    onFailure = { e ->
                        Timber.e(e, "Error unloading model")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error in unloadCurrentModel")
            }
        }
    }
    
    suspend fun getMemoryUsage(): Long {
        return try {
            llamaAndroid.getMemoryUsage()
        } catch (e: Exception) {
            Timber.e(e, "Error getting memory usage")
            0L
        }
    }
    
    fun isModelLoaded(): Boolean {
        return loadedModelName.value.isNotEmpty()
    }
    
    fun getLoadedModelName(): String {
        return loadedModelName.value
    }
    
    // Enhanced memory optimization
    fun optimizeMemory() {
        viewModelScope.launch {
            try {
                // Clear message history if it's too large
                if (messages.size > 100) {
                    val keepCount = 50
                    messages = messages.takeLast(keepCount)
                    Timber.d("Cleared message history, kept last $keepCount messages")
                }

                // Clean up ViewModels
                searchViewModel.cleanup()
                chatViewModel.cleanup()
                modelViewModel.cleanupResources()

                // Force garbage collection if memory usage is high
                val memoryUsage = getMemoryUsage()
                if (memoryUsage > 500 * 1024 * 1024) { // 500MB threshold
                    System.gc()
                    System.runFinalization()
                    Timber.d("Forced garbage collection due to high memory usage: ${memoryUsage / (1024 * 1024)}MB")
                }

                // Clear any temporary processing data
                clearTempData()
            } catch (e: Exception) {
                Timber.e(e, "Error optimizing memory")
                ErrorHandler.reportError(e, "Memory Optimization", ErrorHandler.ErrorSeverity.LOW, "Memory optimization completed with warnings.")
            }
        }
    }

    private fun clearTempData() {
        // Clear any temporary processing data
        tempProcessingData.clear()
        Timber.tag("MainViewModel").d("Temporary data cleared")
    }

    // Track temporary processing data for cleanup
    private val tempProcessingData = mutableMapOf<String, Any>()

    // Search state management
    var isSearching by mutableStateOf(false)
    var searchResults by mutableStateOf<List<SearchResult>>(emptyList())
    var currentSearchQuery by mutableStateOf("")
    var searchError by mutableStateOf<String?>(null)
    var searchProgress by mutableStateOf("")

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

