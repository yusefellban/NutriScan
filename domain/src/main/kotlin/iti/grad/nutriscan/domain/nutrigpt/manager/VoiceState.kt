package iti.grad.nutriscan.domain.nutrigpt.manager

sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data class PartialResult(val text: String) : VoiceState
    data class FinalResult(val text: String) : VoiceState
    data class Error(val message: String) : VoiceState
    data object DoneSpeaking : VoiceState
}
