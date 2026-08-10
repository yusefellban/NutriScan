package iti.grad.nutriscan.domain.nutrigpt.manager

sealed interface VoiceState {
    data object Idle : VoiceState
    data class Listening(val rmsdB: Float = 0f) : VoiceState
    data class PartialResult(val text: String) : VoiceState
    data class FinalResult(val text: String) : VoiceState
    data class Error(val message: String) : VoiceState
    data object DoneSpeaking : VoiceState
}
