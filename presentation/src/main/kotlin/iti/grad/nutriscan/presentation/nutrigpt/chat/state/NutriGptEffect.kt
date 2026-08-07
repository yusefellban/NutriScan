package iti.grad.nutriscan.presentation.nutrigpt.chat.state

import iti.grad.nutriscan.presentation.common.model.UiText

sealed interface NutriGptEffect {
    data class ShowError(val message: UiText) : NutriGptEffect
    data object NavigateBack : NutriGptEffect
}
