package iti.grad.nutriscan.presentation.settings.notifications.viewmodel

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.domain.notification.usecase.SendTestNotificationUseCase
import iti.grad.nutriscan.domain.notification.usecase.SetNotificationPrefUseCase
import iti.grad.nutriscan.domain.notification.usecase.SetQuietHoursEnabledUseCase
import iti.grad.nutriscan.domain.notification.usecase.SetQuietHoursUseCase
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsEffect
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsEvent
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val setPref: SetNotificationPrefUseCase,
    private val setQuietHoursEnabled: SetQuietHoursEnabledUseCase,
    private val setQuietHours: SetQuietHoursUseCase,
    private val sendTestNotification: SendTestNotificationUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationSettingsState())
    val state: StateFlow<NotificationSettingsState> = _state.asStateFlow()

    private val _effect = Channel<NotificationSettingsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        refreshBatteryOptimizationState()
        viewModelScope.launch {
            observePrefs().collectLatest { prefs ->
                _state.update {
                    it.copy(
                        enabled = prefs.enabled,
                        quietHoursEnabled = prefs.quietHoursEnabled,
                        quietHoursStart = prefs.quietHoursStart,
                        quietHoursEnd = prefs.quietHoursEnd,
                    )
                }
            }
        }
    }

    fun onEvent(event: NotificationSettingsEvent) {
        when (event) {
            is NotificationSettingsEvent.ToggleType -> toggleType(event)
            is NotificationSettingsEvent.QuietHoursEnabledChanged -> changeQuietHoursEnabled(event)
            is NotificationSettingsEvent.QuietHoursRangeChanged -> changeQuietHoursRange(event)
            is NotificationSettingsEvent.SendTestNotificationClicked -> {
                sendTestNotification()
                emitEffect(NotificationSettingsEffect.TestNotificationSent)
            }
            is NotificationSettingsEvent.AllowBackgroundNotificationsClicked ->
                emitEffect(NotificationSettingsEffect.RequestIgnoreBatteryOptimizations)
            is NotificationSettingsEvent.BackClicked -> emitEffect(NotificationSettingsEffect.NavigateBack)
        }
    }

    /** Re-checked whenever the screen resumes (e.g. after returning from the system battery
     * settings), so the "allow background notifications" prompt disappears once granted. */
    fun refreshBatteryOptimizationState() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        _state.update { it.copy(isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName)) }
    }

    private fun toggleType(event: NotificationSettingsEvent.ToggleType) {
        _state.update { it.copy(enabled = it.enabled + (event.type to event.enabled)) }
        viewModelScope.launch { setPref(event.type, event.enabled) }
    }

    private fun changeQuietHoursEnabled(event: NotificationSettingsEvent.QuietHoursEnabledChanged) {
        _state.update { it.copy(quietHoursEnabled = event.enabled) }
        viewModelScope.launch { setQuietHoursEnabled(event.enabled) }
    }

    private fun changeQuietHoursRange(event: NotificationSettingsEvent.QuietHoursRangeChanged) {
        _state.update { it.copy(quietHoursStart = event.start, quietHoursEnd = event.end) }
        viewModelScope.launch { setQuietHours(event.start, event.end) }
    }

    private fun emitEffect(effect: NotificationSettingsEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
