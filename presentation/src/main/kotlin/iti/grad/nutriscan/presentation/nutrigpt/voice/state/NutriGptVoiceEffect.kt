package iti.grad.nutriscan.presentation.nutrigpt.voice.state

import iti.grad.nutriscan.presentation.nutrigpt.chat.state.ChatLanguage

sealed interface NutriGptVoiceEffect {
    data object NavigateBack : NutriGptVoiceEffect
    data class ShowError(val message: String) : NutriGptVoiceEffect
    data class Speak(val text: String, val language: ChatLanguage) : NutriGptVoiceEffect
    data object StopSpeaking : NutriGptVoiceEffect
}
