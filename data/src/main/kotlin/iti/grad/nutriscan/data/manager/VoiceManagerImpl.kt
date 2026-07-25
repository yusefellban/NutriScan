package iti.grad.nutriscan.data.manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import iti.grad.nutriscan.domain.nutrigpt.manager.IVoiceManager
import iti.grad.nutriscan.domain.nutrigpt.manager.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

class VoiceManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IVoiceManager, RecognitionListener {

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    override val state: StateFlow<VoiceState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    
    private var isTtsInitialized = false

    init {
        // Initialize SpeechRecognizer on the main thread
        Handler(Looper.getMainLooper()).post {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(this@VoiceManagerImpl)
            } else {
                _state.update { VoiceState.Error("Speech recognition is not available on this device") }
            }
        }

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                textToSpeech?.setPitch(0.92f)
                textToSpeech?.setSpeechRate(0.88f)
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        _state.update { VoiceState.DoneSpeaking }
                    }
                    override fun onError(utteranceId: String?) {
                        _state.update { VoiceState.DoneSpeaking }
                    }
                })
            } else {
                _state.update { VoiceState.Error("TTS initialization failed") }
            }
        }
    }

    override fun startListening(languageCode: String) {
        Handler(Looper.getMainLooper()).post {
            if (speechRecognizer == null) {
                _state.update { VoiceState.Error("Speech recognition not initialized") }
                return@post
            }
            
            // Stop any ongoing TTS before listening
            stopSpeaking()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            }

            _state.update { VoiceState.Listening }
            speechRecognizer?.startListening(intent)
        }
    }

    override fun stopListening() {
        Handler(Looper.getMainLooper()).post {
            speechRecognizer?.stopListening()
            _state.update { VoiceState.Idle }
        }
    }

    override fun speak(text: String, languageCode: String) {
        if (!isTtsInitialized || textToSpeech == null) return
        
        val locale = Locale(languageCode)
        textToSpeech?.language = locale
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "VOICE_REPLY")
    }

    override fun stopSpeaking() {
        textToSpeech?.stop()
    }

    override fun release() {
        Handler(Looper.getMainLooper()).post {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
        
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    // --- RecognitionListener Implementation ---
    
    override fun onReadyForSpeech(params: Bundle?) {}

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No recognition result matched"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
        _state.update { VoiceState.Error(message) }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            _state.update { VoiceState.FinalResult(matches[0]) }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            _state.update { VoiceState.PartialResult(matches[0]) }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
