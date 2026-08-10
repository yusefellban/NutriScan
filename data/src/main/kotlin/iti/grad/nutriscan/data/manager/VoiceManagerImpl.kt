package iti.grad.nutriscan.data.manager

import android.content.Context
import android.content.Intent
import android.media.AudioManager
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

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /** True while the user is physically holding the mic button. */
    @Volatile private var isHoldListening = false

    /**
     * True during the silent restart gap between two recognizer sessions.
     * While this is true we skip the Listening(0f) reset so the waveform stays smooth.
     */
    @Volatile private var isRestarting = false

    /** Language code used in the current hold session — needed for auto-restarts. */
    private var currentLanguageCode = "en-US"

    /**
     * Text confirmed by previous recognizer sessions within the same hold.
     * Prepended to every partial result so the on-screen text never blanks out
     * between invisible restarts.
     */
    private var accumulatedText = ""

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

            // Begin a fresh hold session
            isHoldListening = true
            isRestarting = false
            currentLanguageCode = languageCode
            accumulatedText = ""

            startRecognizerInternal(languageCode)
        }
    }

    /** Starts (or restarts) the underlying SpeechRecognizer — must be called on the main thread. */
    private fun startRecognizerInternal(languageCode: String) {
        if (isRestarting) {
            // Mute both streams — different OEMs route the recognizer beep to different streams.
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_RING,  AudioManager.ADJUST_MUTE, 0)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 10_000L)
            putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 10_000L)
            putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 0L)
        }
        // Only reset waveform on the first start, not on silent restarts.
        if (!isRestarting) {
            _state.update { VoiceState.Listening(0f) }
        }
        speechRecognizer?.startListening(intent)
    }

    override fun stopListening() {
        Handler(Looper.getMainLooper()).post {
            isHoldListening = false
            // stopListening() signals the recognizer to finalize what it heard → onResults() fires.
            speechRecognizer?.stopListening()
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
    
    override fun onReadyForSpeech(params: Bundle?) {
        // Recognizer is active — restore both streams if we muted them for a silent restart.
        if (isRestarting) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_RING,  AudioManager.ADJUST_UNMUTE, 0)
            isRestarting = false
        }
    }

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {
        val currentState = _state.value
        if (currentState is VoiceState.Listening) {
            _state.value = VoiceState.Listening(rmsdB)
        }
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        // While the button is held, silence/no-match errors just mean the user paused —
        // restart the recognizer so they can continue speaking.
        if (isHoldListening && (error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
        {
            isRestarting = true
            Handler(Looper.getMainLooper()).post {
                if (isHoldListening) startRecognizerInternal(currentLanguageCode)
            }
            return
        }

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
        val chunk = matches?.firstOrNull { it.isNotBlank() }?.trim().orEmpty()

        if (isHoldListening) {
            // Recognizer stopped on silence while button is still held.
            // Save this chunk, mark as restarting (mutes beep + keeps waveform), restart.
            if (chunk.isNotBlank()) {
                accumulatedText = if (accumulatedText.isBlank()) chunk
                                  else "$accumulatedText $chunk"
            }
            isRestarting = true
            Handler(Looper.getMainLooper()).post {
                if (isHoldListening) startRecognizerInternal(currentLanguageCode)
            }
        } else {
            // User released the button — combine accumulated + this final chunk.
            val finalText = buildString {
                if (accumulatedText.isNotBlank()) append(accumulatedText)
                if (chunk.isNotBlank()) {
                    if (isNotEmpty()) append(" ")
                    append(chunk)
                }
            }.trim()
            accumulatedText = ""
            if (finalText.isNotBlank()) {
                _state.update { VoiceState.FinalResult(finalText) }
            } else {
                _state.update { VoiceState.Idle }
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val currentPartial = matches?.firstOrNull { it.isNotBlank() } ?: return
        // Always prepend accumulated text so the screen never goes blank between restarts.
        val display = if (accumulatedText.isBlank()) currentPartial
                      else "$accumulatedText $currentPartial"
        _state.update { VoiceState.PartialResult(display) }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
