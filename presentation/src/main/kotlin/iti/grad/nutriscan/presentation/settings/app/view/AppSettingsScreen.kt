package iti.grad.nutriscan.presentation.settings.app.view

import android.app.Activity
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.domain.settings.model.AppLanguage
import iti.grad.nutriscan.domain.settings.model.ThemeMode
import iti.grad.nutriscan.presentation.common.components.ConfirmationDialog
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.app.state.AppSettingsEffect
import iti.grad.nutriscan.presentation.settings.app.state.AppSettingsEvent
import iti.grad.nutriscan.presentation.settings.app.state.AppSettingsState
import iti.grad.nutriscan.presentation.settings.app.view.components.AppSettingsHeader
import iti.grad.nutriscan.presentation.settings.app.view.components.LogoutButton
import iti.grad.nutriscan.presentation.settings.app.view.components.SettingsActionRow
import iti.grad.nutriscan.presentation.settings.app.view.components.SettingsToggleRow
import iti.grad.nutriscan.presentation.settings.app.viewmodel.AppSettingsViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest

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
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is AppSettingsEffect.NavigateBack -> onNavigateBack()
                is AppSettingsEffect.NavigateToEditProfile -> onNavigateToEditProfile()
                is AppSettingsEffect.NavigateToTermsAndConditions -> onNavigateToTermsAndConditions()
                is AppSettingsEffect.NavigateToHelp -> onNavigateToHelp()
                is AppSettingsEffect.NavigateToLogin -> onNavigateToLogin()
                is AppSettingsEffect.ApplyLocale -> {
                    val languageTag = if (effect.language == AppLanguage.AR) "ar" else "en"
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.wrap(LocaleList.forLanguageTags(languageTag))
                    )
                    (context as? Activity)?.recreate()
                }
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
        ConfirmationDialog(
            title = stringResource(R.string.app_settings_logout_confirm_title),
            message = stringResource(R.string.app_settings_logout_confirm_message),
            confirmLabel = stringResource(R.string.app_settings_logout),
            cancelLabel = stringResource(R.string.action_cancel),
            onConfirm = { onEvent(AppSettingsEvent.LogoutConfirmed) },
            onDismiss = { onEvent(AppSettingsEvent.LogoutDismissed) },
        )
    }
}
