package iti.grad.nutriscan.presentation.nutrigpt.voice.state

sealed interface NutriGptVoiceEvent {
    data object ToggleLanguage : NutriGptVoiceEvent
    data class SetListeningState(val isListening: Boolean) : NutriGptVoiceEvent
    data class UpdateQuery(val text: String) : NutriGptVoiceEvent
    data class SubmitQuery(val query: String) : NutriGptVoiceEvent
    data object StopPlaying : NutriGptVoiceEvent
}
