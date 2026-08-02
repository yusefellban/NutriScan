package iti.grad.nutriscan.presentation.notification_history.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.notification.model.NotificationHistoryItem
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.usecase.ClearNotificationHistoryUseCase
import iti.grad.nutriscan.domain.notification.usecase.DeleteNotificationUseCase
import iti.grad.nutriscan.domain.notification.usecase.GetNotificationHistoryUseCase
import iti.grad.nutriscan.domain.notification.usecase.MarkNotificationReadUseCase
import iti.grad.nutriscan.presentation.common.model.UiText
import iti.grad.nutriscan.presentation.notification_history.state.NotificationHistoryEffect
import iti.grad.nutriscan.presentation.notification_history.state.NotificationHistoryEvent
import iti.grad.nutriscan.presentation.notification_history.state.NotificationHistoryItemUi
import iti.grad.nutriscan.presentation.notification_history.state.NotificationHistoryState
import iti.grad.presentation.R
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class NotificationHistoryViewModel @Inject constructor(
    private val getNotificationHistory: GetNotificationHistoryUseCase,
    private val deleteNotification: DeleteNotificationUseCase,
    private val clearNotificationHistory: ClearNotificationHistoryUseCase,
    private val markNotificationRead: MarkNotificationReadUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationHistoryState())
    val state: StateFlow<NotificationHistoryState> = _state.asStateFlow()

    private val _effect = Channel<NotificationHistoryEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeHistory()
    }

    fun onEvent(event: NotificationHistoryEvent) {
        when (event) {
            is NotificationHistoryEvent.LoadHistory -> observeHistory()
            is NotificationHistoryEvent.DeleteNotification -> handleDelete(event.id)
            is NotificationHistoryEvent.ClearAll -> handleClearAll()
            is NotificationHistoryEvent.NotificationClicked -> handleNotificationClicked(event.id)
            is NotificationHistoryEvent.BackClicked -> emitEffect(NotificationHistoryEffect.NavigateBack)
            is NotificationHistoryEvent.NavigateToSettingsClicked -> emitEffect(NotificationHistoryEffect.NavigateToSettings)
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            getNotificationHistory().collect { items ->
                val uiItems = items.map { it.toUi() }
                _state.update {
                    it.copy(
                        isLoading = false,
                        notifications = uiItems.toImmutableList(),
                        isEmpty = uiItems.isEmpty(),
                    )
                }
            }
        }
    }

    private fun handleDelete(id: Long) {
        viewModelScope.launch {
            deleteNotification(id)
            emitEffect(NotificationHistoryEffect.ShowUndoSnackbar(R.string.notification_history_deleted))
        }
    }

    private fun handleClearAll() {
        viewModelScope.launch {
            clearNotificationHistory()
            emitEffect(NotificationHistoryEffect.ShowUndoSnackbar(R.string.notification_history_all_cleared))
        }
    }

    private fun handleNotificationClicked(id: Long) {
        viewModelScope.launch {
            markNotificationRead(id)
            // If the notification needs to navigate somewhere specific based on type,
            // we would emit an effect here. For now, just mark it as read.
        }
    }

    private fun emitEffect(effect: NotificationHistoryEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun NotificationHistoryItem.toUi(): NotificationHistoryItemUi {
        return NotificationHistoryItemUi(
            id = id,
            title = title,
            body = body,
            type = type,
            relativeTime = formatRelativeTime(timestamp),
            isRead = isRead,
        )
    }

    private fun formatRelativeTime(timestampMillis: Long): UiText {
        val now = ZonedDateTime.now()
        val itemTime = Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault())
        
        val minutes = ChronoUnit.MINUTES.between(itemTime, now)
        val hours = ChronoUnit.HOURS.between(itemTime, now)
        val days = ChronoUnit.DAYS.between(itemTime.toLocalDate(), now.toLocalDate())

        return when {
            minutes < 1 -> UiText.StringResource(R.string.notification_history_time_just_now)
            minutes < 60 -> UiText.StringResource(R.string.notification_history_time_minutes_ago, minutes.toInt())
            hours < 24 && days == 0L -> UiText.StringResource(R.string.notification_history_time_hours_ago, hours.toInt())
            else -> UiText.StringResource(R.string.notification_history_time_days_ago, days.toInt())
        }
    }
}
