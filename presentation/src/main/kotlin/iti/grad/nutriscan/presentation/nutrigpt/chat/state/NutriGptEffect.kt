package iti.grad.nutriscan.presentation.nutrigpt.chat.state

sealed interface NutriGptEffect {
    data class ShowError(val message: String) : NutriGptEffect
    data object NavigateBack : NutriGptEffect
}
