package iti.grad.nutriscan.domain.nutrigpt.manager

import kotlinx.coroutines.flow.StateFlow

interface IVoiceManager {
    val state: StateFlow<VoiceState>
    
    fun startListening(languageCode: String)
    fun stopListening()
    
    fun speak(text: String, languageCode: String)
    fun stopSpeaking()
    
    fun release()
}
