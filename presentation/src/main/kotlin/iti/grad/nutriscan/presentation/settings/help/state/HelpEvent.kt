package iti.grad.nutriscan.presentation.settings.help.state

sealed interface HelpEvent {
    data object BackClicked : HelpEvent
    data class FaqItemClicked(val id: Int) : HelpEvent
    data object ContactSupportClicked : HelpEvent
}
