package iti.grad.nutriscan.presentation.settings.profile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import iti.grad.nutriscan.presentation.common.components.ConfirmationDialog
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.profile.state.UserProfileEffect
import iti.grad.nutriscan.presentation.settings.profile.state.UserProfileEvent
import iti.grad.nutriscan.presentation.settings.profile.state.UserProfileState
import iti.grad.nutriscan.presentation.settings.profile.view.components.BmiTdeeCard
import iti.grad.nutriscan.presentation.settings.profile.view.components.FamilyMembersSection
import iti.grad.nutriscan.presentation.settings.profile.view.components.ProfileHeaderSection
import iti.grad.nutriscan.presentation.settings.profile.view.components.ProfileMenuRow
import iti.grad.nutriscan.presentation.settings.profile.viewmodel.UserProfileViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest

/**
 * User Profile Screen — shows the current user's header, their family
 * members, and quick links to Scan History / Notifications / Settings.
 *
 * Follows MVI: collects [UserProfileState] from [UserProfileViewModel],
 * dispatches [UserProfileEvent], and handles [UserProfileEffect] for
 * one-shot navigation.
 */
@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp,
    onNavigateToScanHistory: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToFamilyMemberDetail: (String) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTab: (iti.grad.nutriscan.presentation.common.model.BottomNavTab) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is UserProfileEffect.NavigateToEditProfile -> onNavigateToEditProfile()
                is UserProfileEffect.NavigateToFamilyMemberDetail ->
                    onNavigateToFamilyMemberDetail(effect.memberId)
                is UserProfileEffect.NavigateToScanHistory -> onNavigateToScanHistory()
                is UserProfileEffect.NavigateToNotifications -> onNavigateToNotifications()
                is UserProfileEffect.NavigateToSettings -> onNavigateToSettings()
                is UserProfileEffect.NavigateToTab -> onNavigateToTab(effect.tab)
            }
        }
    }

    UserProfileContent(state = state, onEvent = viewModel::onEvent, bottomPadding = bottomPadding)
}

@Composable
private fun UserProfileContent(
    state: UserProfileState,
    onEvent: (UserProfileEvent) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomPadding)
                // Must contrast with ProfileSheetBackground, or the sheet's
                // rounded top corners have nothing to show through and
                // render as sharp.
                .background(AppTheme.colors.ProfileHeaderBackground),
        ) {
            ProfileHeaderSection(
                userName = state.userName,
                avatarUrl = state.avatarUrl,
                streakDays = state.streakDays,
                onEditProfileClick = { onEvent(UserProfileEvent.EditProfileClicked) },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(AppTheme.colors.ProfileSheetBackground)
                    .verticalScroll(rememberScrollState())
                    // No end padding: FamilyMembersSection's dashed box needs
                    // to reach the screen's true trailing edge. Every other
                    // child adds its own end padding back below.
                    .padding(start = 20.dp, top = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                BmiTdeeCard(
                    bmi = state.bmi,
                    tdee = state.tdee,
                )

                FamilyMembersSection(
                    familyMembers = state.familyMembers,
                    onAddMemberClick = { onEvent(UserProfileEvent.AddMemberClicked) },
                    onMemberDetailClick = { id ->
                        onEvent(UserProfileEvent.FamilyMemberDetailClicked(id))
                    },
                    onMemberLongPress = { id ->
                        onEvent(UserProfileEvent.FamilyMemberLongPressed(id))
                    },
                )

                Column(
                    modifier = Modifier.padding(end = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ProfileMenuRow(
                        iconResId = R.drawable.hour,
                        label = stringResource(R.string.user_profile_scan_history),
                        onClick = { onEvent(UserProfileEvent.ScanHistoryClicked) },
                    )
                    ProfileMenuRow(
                        iconResId = R.drawable.bell,
                        label = stringResource(R.string.user_profile_notifications),
                        onClick = { onEvent(UserProfileEvent.NotificationsClicked) },
                    )
                    ProfileMenuRow(
                        iconResId = R.drawable.settings,
                        label = stringResource(R.string.user_profile_settings),
                        onClick = { onEvent(UserProfileEvent.SettingsClicked) },
                    )
                }

                // Bottom spacing to account for the bottom nav bar overflow
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

    state.memberPendingDeletion?.let { member ->
        iti.grad.nutriscan.presentation.common.components.DeleteWarningAlert(
            title = stringResource(R.string.user_profile_remove_member_title),
            message = stringResource(R.string.user_profile_remove_member_message, member.name),
            confirmText = stringResource(R.string.action_remove),
            cancelText = stringResource(R.string.action_cancel),
            onConfirm = { onEvent(UserProfileEvent.ConfirmRemoveMemberClicked) },
            onDismiss = { onEvent(UserProfileEvent.CancelRemoveMemberClicked) },
        )
    }

    when (val alert = state.alertState) {
        is iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.InternetError -> {
            iti.grad.nutriscan.presentation.common.components.InternetAlert(
                onRetry = { onEvent(UserProfileEvent.RetryAction) },
                onDismiss = { onEvent(UserProfileEvent.DismissAlert) }
            )
        }
        is iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.Error -> {
            iti.grad.nutriscan.presentation.common.components.ErrorAlert(
                title = stringResource(id = R.string.alert_error_title),
                message = alert.messageStr ?: alert.messageResId?.let { stringResource(id = it) } ?: "",
                onDismiss = { onEvent(UserProfileEvent.DismissAlert) }
            )
        }
        is iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.Warning -> {
            iti.grad.nutriscan.presentation.common.components.WarningAlert(
                title = stringResource(id = R.string.alert_warning_title),
                message = alert.messageStr ?: alert.messageResId?.let { stringResource(id = it) } ?: "",
                onDismiss = { onEvent(UserProfileEvent.DismissAlert) }
            )
        }
        is iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.Success -> {
            iti.grad.nutriscan.presentation.common.components.SuccessAlert(
                title = stringResource(id = R.string.alert_success_title),
                message = alert.messageStr ?: alert.messageResId?.let { stringResource(id = it) } ?: "",
                onDismiss = { onEvent(UserProfileEvent.DismissAlert) }
            )
        }
        is iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.None -> Unit
    }
}
