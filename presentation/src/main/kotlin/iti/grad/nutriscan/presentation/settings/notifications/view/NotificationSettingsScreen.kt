package iti.grad.nutriscan.presentation.settings.notifications.view

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.app.view.components.SettingsSwitchRow
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsEffect
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsEvent
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsState
import iti.grad.nutriscan.presentation.settings.notifications.view.components.QuietHoursRow
import iti.grad.nutriscan.presentation.settings.notifications.viewmodel.NotificationSettingsViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val testNotificationSentMessage = stringResource(R.string.notification_settings_test_sent)

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NotificationSettingsEffect.NavigateBack -> onNavigateBack()
                is NotificationSettingsEffect.TestNotificationSent ->
                    Toast.makeText(context, testNotificationSentMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    NotificationSettingsContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun NotificationSettingsContent(
    state: NotificationSettingsState,
    onEvent: (NotificationSettingsEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.Background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.notification_settings_title),
                style = AppTheme.typography.titleLarge,
                color = AppTheme.colors.AppSettingsRowLabel,
            )
        }

        item {
            Text(
                text = stringResource(R.string.notification_settings_toggles_header),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.Gray500,
            )
        }

        items(NotificationType.entries.toList()) { type ->
            SettingsSwitchRow(
                icon = painterResource(iconFor(type)),
                label = stringResource(labelFor(type)),
                checked = state.enabled[type] ?: true,
                onCheckedChange = { checked -> onEvent(NotificationSettingsEvent.ToggleType(type, checked)) },
            )
        }

        item {
            Text(
                text = stringResource(R.string.notification_settings_quiet_hours_header),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.Gray500,
            )
        }

        item {
            QuietHoursRow(
                icon = painterResource(R.drawable.bell),
                start = state.quietHoursStart,
                end = state.quietHoursEnd,
                enabled = state.quietHoursEnabled,
                onEnabledChange = { enabled -> onEvent(NotificationSettingsEvent.QuietHoursEnabledChanged(enabled)) },
            )
        }

        item {
            AppButton(
                textResId = R.string.notification_settings_send_test,
                isLoading = false,
                onClick = { onEvent(NotificationSettingsEvent.SendTestNotificationClicked) },
            )
        }
    }
}

private fun iconFor(type: NotificationType): Int = when (type) {
    NotificationType.STEPS -> R.drawable.bell
    NotificationType.WATER -> R.drawable.bell
    NotificationType.WORKOUT -> R.drawable.bell
    NotificationType.FOOD -> R.drawable.bell
    NotificationType.NEWS -> R.drawable.bell
    NotificationType.QUOTE -> R.drawable.bell
    NotificationType.SCAN -> R.drawable.bell
    NotificationType.STREAK -> R.drawable.bell
}

private fun labelFor(type: NotificationType): Int = when (type) {
    NotificationType.STEPS -> R.string.notification_type_steps
    NotificationType.WATER -> R.string.notification_type_water
    NotificationType.WORKOUT -> R.string.notification_type_workout
    NotificationType.FOOD -> R.string.notification_type_food
    NotificationType.NEWS -> R.string.notification_type_news
    NotificationType.QUOTE -> R.string.notification_type_quote
    NotificationType.SCAN -> R.string.notification_type_scan
    NotificationType.STREAK -> R.string.notification_type_streak
}

@Preview(name = "Light", showBackground = true)
@Preview(
    name = "Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun NotificationSettingsScreenPreview() {
    AppTheme {
        NotificationSettingsContent(
            state = NotificationSettingsState(),
            onEvent = {},
        )
    }
}
