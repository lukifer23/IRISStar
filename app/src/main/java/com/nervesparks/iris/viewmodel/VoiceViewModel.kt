package com.nervesparks.iris.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import timber.log.Timber
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nervesparks.iris.data.exceptions.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * PHASE 1.4: VoiceViewModel - Extracted from MainViewModel
 * Handles speech recognition and text-to-speech functionality
 */
@HiltViewModel
class VoiceViewModel @Inject constructor() : ViewModel() {

    private val tag = "VoiceViewModel"

    // Speech recognition state
    var isListening by mutableStateOf(false)
    var speechRecognitionResult by mutableStateOf("")
    var speechRecognitionError by mutableStateOf<String?>(null)

    // Text-to-speech state
    var isSpeaking by mutableStateOf(false)
    var ttsError by mutableStateOf<String?>(null)

    // Speech recognizer and TTS instances
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Timber.tag(tag).d("Ready for speech")
            isListening = true
        }

        override fun onBeginningOfSpeech() {
            Timber.tag(tag).d("Beginning of speech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // RMS changed - can be used for visual feedback
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            Timber.tag(tag).d("Buffer received")
        }

        override fun onEndOfSpeech() {
            Timber.tag(tag).d("End of speech")
            isListening = false
        }

        override fun onError(error: Int) {
            val errorMessage = getErrorMessage(error)
            Timber.tag(tag).e("Speech recognition error: $errorMessage")
            speechRecognitionError = errorMessage
            isListening = false
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                speechRecognitionResult = matches[0]
                speechRecognitionError = null
                Timber.tag(tag).d("Speech recognition result: $speechRecognitionResult")
            }
            isListening = false
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                speechRecognitionResult = matches[0]
                Timber.tag(tag).d("Partial speech recognition result: $speechRecognitionResult")
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Timber.tag(tag).d("Speech recognition event: $eventType")
        }
    }

    private val ttsProgressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            Timber.tag(tag).d("TTS started: $utteranceId")
            isSpeaking = true
        }

        override fun onDone(utteranceId: String?) {
            Timber.tag(tag).d("TTS done: $utteranceId")
            isSpeaking = false
        }

        override fun onError(utteranceId: String?) {
            Timber.tag(tag).e("TTS error: $utteranceId")
            ttsError = "Text-to-speech failed"
            isSpeaking = false
        }
    }

    // Initialize TTS
    fun initializeTTS(context: Context) {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.language = Locale.getDefault()
                    textToSpeech?.setOnUtteranceProgressListener(ttsProgressListener)
                    Timber.tag(tag).d("TTS initialized successfully")
                } else {
                    Timber.tag(tag).e("TTS initialization failed")
                    ttsError = "Text-to-speech initialization failed"
                }
            }
        }
    }

    // Speech recognition functions
    fun startVoiceRecognition(context: Context) {
        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(recognitionListener)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
            speechRecognitionError = null
            Timber.tag(tag).d("Started voice recognition")

        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error starting voice recognition")
            ErrorHandler.reportError(e, "Voice Recognition", ErrorHandler.ErrorSeverity.MEDIUM, "Failed to start voice recognition. Please check microphone permissions and try again.")
            speechRecognitionError = "Failed to start voice recognition: ${e.message}"
        }
    }

    fun stopVoiceRecognition() {
        try {
            speechRecognizer?.stopListening()
            isListening = false
            Timber.tag(tag).d("Stopped voice recognition")
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error stopping voice recognition")
        }
    }

    // Text-to-speech functions
    fun textToSpeech(context: Context, text: String) {
        try {
            initializeTTS(context)

            val utteranceId = UUID.randomUUID().toString()
            val result = textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

            if (result == TextToSpeech.SUCCESS) {
                ttsError = null
                Timber.tag(tag).d("TTS started for text: ${text.take(50)}...")
            } else {
                ttsError = "Text-to-speech failed to start"
                Timber.tag(tag).e("TTS failed to start")
            }

        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error in text-to-speech")
            ErrorHandler.reportError(e, "Text-to-Speech", ErrorHandler.ErrorSeverity.MEDIUM, "Text-to-speech failed. Please check if TTS is properly initialized.")
            ttsError = "Text-to-speech error: ${e.message}"
        }
    }

    fun stopTextToSpeech() {
        try {
            textToSpeech?.stop()
            isSpeaking = false
            Timber.tag(tag).d("TTS stopped")
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error stopping TTS")
        }
    }

    // Utility functions
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            else -> "Unknown error ($errorCode)"
        }
    }

    // Cleanup
    fun shutdown() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null

            textToSpeech?.shutdown()
            textToSpeech = null

            Timber.tag(tag).d("VoiceViewModel shutdown")
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error during VoiceViewModel shutdown")
        }
    }

    override fun onCleared() {
        super.onCleared()
        shutdown()
    }
}
