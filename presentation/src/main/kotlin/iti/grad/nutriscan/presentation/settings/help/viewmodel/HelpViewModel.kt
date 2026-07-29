package iti.grad.nutriscan.presentation.settings.help.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.presentation.settings.help.state.HelpEffect
import iti.grad.nutriscan.presentation.settings.help.state.HelpEvent
import iti.grad.nutriscan.presentation.settings.help.state.HelpState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SUPPORT_EMAIL = "ahmedtayseer424@gmail.com"

@HiltViewModel
class HelpViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(HelpState())
    val state: StateFlow<HelpState> = _state.asStateFlow()

    private val _effect = Channel<HelpEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: HelpEvent) {
        when (event) {
            HelpEvent.BackClicked -> navigate(HelpEffect.NavigateBack)
            is HelpEvent.FaqItemClicked -> toggleFaq(event.id)
            HelpEvent.ContactSupportClicked -> navigate(
                HelpEffect.OpenEmail(recipient = SUPPORT_EMAIL, subject = "", body = "", isFeedback = false)
            )
            HelpEvent.SendFeedbackClicked -> _state.update { it.copy(showFeedbackDialog = true) }
            is HelpEvent.FeedbackTextChanged -> _state.update { it.copy(feedbackText = event.text) }
            HelpEvent.FeedbackSubmitClicked -> submitFeedback()
            HelpEvent.FeedbackDismissed -> _state.update {
                it.copy(showFeedbackDialog = false, feedbackText = "")
            }
        }
    }

    private fun toggleFaq(id: Int) {
        _state.update {
            it.copy(expandedFaqId = if (it.expandedFaqId == id) null else id)
        }
    }

    private fun submitFeedback() {
        val body = _state.value.feedbackText
        _state.update { it.copy(showFeedbackDialog = false, feedbackText = "") }
        navigate(HelpEffect.OpenEmail(recipient = SUPPORT_EMAIL, subject = "", body = body, isFeedback = true))
    }

    private fun navigate(effect: HelpEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
