package iti.grad.nutriscan.presentation.nutrigpt.chat.state

sealed interface NutriGptEvent {
    data class SendMessage(val query: String) : NutriGptEvent
    data class UpdateQuery(val text: String) : NutriGptEvent
    data class SetListeningState(val isListening: Boolean) : NutriGptEvent
    data object ToggleSources : NutriGptEvent
    data object ToggleLanguage : NutriGptEvent
}
