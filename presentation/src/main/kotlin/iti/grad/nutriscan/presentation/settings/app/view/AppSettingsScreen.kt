package iti.grad.nutriscan.presentation.settings.app.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import iti.grad.nutriscan.domain.settings.model.ThemeMode
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import iti.grad.nutriscan.presentation.settings.app.view.components.SettingsToggleRow
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.WindowInsets
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.settings.app.view.components.AppSettingsHeader
import iti.grad.nutriscan.presentation.settings.app.state.AppSettingsEffect
import androidx.compose.material.icons.filled.Language
import iti.grad.nutriscan.presentation.common.components.ConfirmationDialog
import iti.grad.nutriscan.presentation.settings.app.view.components.SettingsActionRow
import iti.grad.nutriscan.presentation.settings.app.state.AppSettingsState
import androidx.compose.foundation.layout.Arrangement
import iti.grad.nutriscan.domain.settings.model.AppLanguage
import iti.grad.nutriscan.presentation.settings.app.view.components.LogoutButton
import androidx.compose.runtime.Composable
import iti.grad.nutriscan.presentation.settings.app.viewmodel.AppSettingsViewModel
import iti.grad.nutriscan.presentation.common.components.LogoutAlert
import iti.grad.nutriscan.presentation.settings.app.state.AppSettingsEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier

@Composable
fun AppSettingsScreen(
    viewModel: AppSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToTermsAndConditions: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is AppSettingsEffect.NavigateBack -> onNavigateBack()
                is AppSettingsEffect.NavigateToEditProfile -> onNavigateToEditProfile()
                is AppSettingsEffect.NavigateToTermsAndConditions -> onNavigateToTermsAndConditions()
                is AppSettingsEffect.NavigateToHelp -> onNavigateToHelp()
                is AppSettingsEffect.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    AppSettingsContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun AppSettingsContent(
    state: AppSettingsState,
    onEvent: (AppSettingsEvent) -> Unit,
) {
    val themeOptions = listOf(
        stringResource(R.string.app_settings_theme_system),
        stringResource(R.string.app_settings_theme_dark),
        stringResource(R.string.app_settings_theme_light),
    )
    val languageOptions = listOf(
        stringResource(R.string.app_settings_lang_en),
        stringResource(R.string.app_settings_lang_ar),
    )

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
            AppSettingsHeader(onBackClick = { onEvent(AppSettingsEvent.BackClicked) })

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 35.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsActionRow(
                    icon = painterResource(R.drawable.edit_profile),
                    label = stringResource(R.string.app_settings_profile_settings),
                    onClick = { onEvent(AppSettingsEvent.ProfileSettingsClicked) },
                )

                SettingsToggleRow(
                    icon = painterResource(R.drawable.fluent_dark_theme_24_regular),
                    label = stringResource(R.string.app_settings_appearance),
                    options = themeOptions,
                    selectedIndex = state.selectedThemeMode.ordinal,
                    onOptionSelected = { index ->
                        onEvent(AppSettingsEvent.ThemeModeSelected(ThemeMode.entries[index]))
                    },
                )

                SettingsToggleRow(
                    icon = rememberVectorPainter(Icons.Filled.Language),
                    label = stringResource(R.string.app_settings_language),
                    options = languageOptions,
                    selectedIndex = state.selectedLanguage.ordinal,
                    onOptionSelected = { index ->
                        onEvent(AppSettingsEvent.LanguageSelected(AppLanguage.entries[index]))
                    },
                )

                SettingsActionRow(
                    icon = rememberVectorPainter(Icons.Filled.Description),
                    label = stringResource(R.string.app_settings_terms_and_conditions),
                    onClick = { onEvent(AppSettingsEvent.TermsAndConditionsClicked) },
                )

                SettingsActionRow(
                    icon = painterResource(R.drawable.material_symbols_help_outline),
                    label = stringResource(R.string.app_settings_help),
                    onClick = { onEvent(AppSettingsEvent.HelpClicked) },
                )

                LogoutButton(
                    label = stringResource(R.string.app_settings_logout),
                    onClick = { onEvent(AppSettingsEvent.LogoutClicked) },
                )
            }
        }
    }

    if (state.showLogoutConfirmDialog) {
        LogoutAlert(
            title = stringResource(R.string.app_settings_logout_confirm_title),
            message = stringResource(R.string.app_settings_logout_confirm_message),
            confirmText = stringResource(R.string.app_settings_logout),
            cancelText = stringResource(R.string.action_cancel),
            onConfirm = { onEvent(AppSettingsEvent.LogoutConfirmed) },
            onDismiss = { onEvent(AppSettingsEvent.LogoutDismissed) },
        )
    }
}
