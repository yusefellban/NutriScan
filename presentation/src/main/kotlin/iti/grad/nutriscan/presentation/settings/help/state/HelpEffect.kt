package iti.grad.nutriscan.presentation.settings.help.state

sealed interface HelpEffect {
    data object NavigateBack : HelpEffect
    data class OpenEmail(val recipient: String) : HelpEffect
}
