package iti.grad.nutriscan.presentation.nutrigpt.chat.state

sealed interface NutriGptEvent {
    data class SendMessage(val query: String) : NutriGptEvent
    data class UpdateQuery(val text: String) : NutriGptEvent
}
