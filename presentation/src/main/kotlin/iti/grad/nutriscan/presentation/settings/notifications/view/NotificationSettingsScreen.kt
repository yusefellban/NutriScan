package iti.grad.nutriscan.presentation.settings.notifications.view

import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.app.view.components.AppSettingsHeader
import iti.grad.nutriscan.presentation.settings.app.view.components.SettingsSwitchRow
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsEffect
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsEvent
import iti.grad.nutriscan.presentation.settings.notifications.state.NotificationSettingsState
import iti.grad.nutriscan.presentation.settings.notifications.view.components.QuietHoursRow
import iti.grad.nutriscan.presentation.settings.notifications.viewmodel.NotificationSettingsViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalTime

@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val testNotificationSentMessage = stringResource(R.string.notification_settings_test_sent)

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentViewModel = rememberUpdatedState(viewModel)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentViewModel.value.refreshBatteryOptimizationState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NotificationSettingsEffect.NavigateBack -> onNavigateBack()
                is NotificationSettingsEffect.TestNotificationSent ->
                    Toast.makeText(context, testNotificationSentMessage, Toast.LENGTH_SHORT).show()
                is NotificationSettingsEffect.RequestIgnoreBatteryOptimizations -> {
                    // Opens the system's battery-optimization list rather than firing
                    // ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS directly — that action needs the
                    // REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission, which Google Play restricts
                    // and requires a Play Console justification declaration for. This action needs
                    // no special permission; the user just finds NutriScan in the list themselves.
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
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
    val context = LocalContext.current

    val onRangeClick: () -> Unit = {
        TimePickerDialog(
            context,
            { _, startHour, startMinute ->
                val newStart = LocalTime.of(startHour, startMinute)
                TimePickerDialog(
                    context,
                    { _, endHour, endMinute ->
                        onEvent(NotificationSettingsEvent.QuietHoursRangeChanged(newStart, LocalTime.of(endHour, endMinute)))
                    },
                    state.quietHoursEnd.hour,
                    state.quietHoursEnd.minute,
                    true,
                ).show()
            },
            state.quietHoursStart.hour,
            state.quietHoursStart.minute,
            true,
        ).show()
    }

    Scaffold(
        containerColor = AppTheme.colors.Background,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AppTheme.colors.Background)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            AppSettingsHeader(
                title = stringResource(R.string.notification_settings_title),
                onBackClick = { onEvent(NotificationSettingsEvent.BackClicked) },
            )

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                NotificationCategorySection(
                    title = stringResource(R.string.notification_settings_category_activity),
                    types = listOf(
                        NotificationType.STEPS,
                        NotificationType.WATER,
                        NotificationType.WORKOUT,
                        NotificationType.FOOD,
                        NotificationType.STREAK,
                    ),
                    state = state,
                    onEvent = onEvent,
                )

                NotificationCategorySection(
                    title = stringResource(R.string.notification_settings_category_reminders),
                    types = listOf(NotificationType.SCAN, NotificationType.BREAK),
                    state = state,
                    onEvent = onEvent,
                )

                NotificationCategorySection(
                    title = stringResource(R.string.notification_settings_category_content),
                    types = listOf(NotificationType.NEWS, NotificationType.QUOTE),
                    state = state,
                    onEvent = onEvent,
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.notification_settings_quiet_hours_header),
                        style = AppTheme.typography.titleSmall,
                        color = AppTheme.colors.SectionSubtitle,
                    )
                    QuietHoursRow(
                        icon = rememberVectorPainter(Icons.Filled.NightsStay),
                        start = state.quietHoursStart,
                        end = state.quietHoursEnd,
                        enabled = state.quietHoursEnabled,
                        onEnabledChange = { enabled -> onEvent(NotificationSettingsEvent.QuietHoursEnabledChanged(enabled)) },
                        onRangeClick = onRangeClick,
                    )
                }

                AppButton(
                    textResId = R.string.notification_settings_send_test,
                    isLoading = false,
                    outlined = true,
                    onClick = { onEvent(NotificationSettingsEvent.SendTestNotificationClicked) },
                )

                if (!state.isIgnoringBatteryOptimizations) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.notification_settings_battery_header),
                            style = AppTheme.typography.titleSmall,
                            color = AppTheme.colors.SectionSubtitle,
                        )
                        Text(
                            text = stringResource(R.string.notification_settings_battery_warning),
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.MenuSectionLabel,
                        )
                        AppButton(
                            textResId = R.string.notification_settings_battery_button,
                            isLoading = false,
                            onClick = { onEvent(NotificationSettingsEvent.AllowBackgroundNotificationsClicked) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCategorySection(
    title: String,
    types: List<NotificationType>,
    state: NotificationSettingsState,
    onEvent: (NotificationSettingsEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.SectionSubtitle,
        )
        types.forEach { type ->
            SettingsSwitchRow(
                icon = rememberVectorPainter(iconFor(type)),
                label = stringResource(labelFor(type)),
                checked = state.enabled[type] ?: true,
                onCheckedChange = { checked -> onEvent(NotificationSettingsEvent.ToggleType(type, checked)) },
            )
        }
    }
}

private fun iconFor(type: NotificationType): ImageVector = when (type) {
    NotificationType.STEPS -> Icons.Filled.DirectionsWalk
    NotificationType.WATER -> Icons.Filled.WaterDrop
    NotificationType.WORKOUT -> Icons.Filled.FitnessCenter
    NotificationType.FOOD -> Icons.Filled.Restaurant
    NotificationType.NEWS -> Icons.Filled.Article
    NotificationType.QUOTE -> Icons.Filled.FormatQuote
    NotificationType.SCAN -> Icons.Filled.CameraAlt
    NotificationType.STREAK -> Icons.Filled.LocalFireDepartment
    NotificationType.BREAK -> Icons.Filled.FreeBreakfast
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
    NotificationType.BREAK -> R.string.notification_type_break
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

