package iti.grad.nutriscan.presentation.main.container.view

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import iti.grad.nutriscan.presentation.common.components.AppBottomNavBar
import iti.grad.nutriscan.presentation.common.components.AppSnackbar
import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.home.view.HomeScreen
import iti.grad.nutriscan.presentation.main.calories.view.CaloriesScreen
import iti.grad.nutriscan.presentation.saved.view.SavedScreen
import iti.grad.nutriscan.presentation.scan.camera.view.CameraScanScreen
import iti.grad.nutriscan.presentation.scan.camera.state.ScanInputMode
import iti.grad.nutriscan.presentation.settings.profile.view.UserProfileScreen
import iti.grad.nutriscan.presentation.common.components.ActionConfirmAlert
import iti.grad.presentation.R

@Composable
fun MainScreen(
    onNavigateToScanProcessing: (String) -> Unit,
    onNavigateToProductDetail: (String) -> Unit,
    onNavigateToNews: () -> Unit,
    onNavigateToChatWithAi: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToFamilyMemberDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToExercises: () -> Unit,
    onNavigateToStepHistory: () -> Unit,
    onNavigateToCaloriesHistory: () -> Unit,
    onNavigateToAccountPendingDeletion: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialTab: BottomNavTab = BottomNavTab.HOME
) {
    var selectedTab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    var captureTrigger by remember { mutableIntStateOf(0) }
    var isScanUploadMode by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    if (showExitDialog) {
        ActionConfirmAlert(
            title = stringResource(R.string.exit_dialog_title),
            message = stringResource(R.string.exit_dialog_message),
            confirmText = stringResource(R.string.exit_dialog_confirm),
            cancelText = stringResource(R.string.action_cancel),
            onConfirm = {
                showExitDialog = false
                context.findActivity()?.moveTaskToBack(true)
            },
            onDismiss = { showExitDialog = false }
        )
    }

    BackHandler {
        if (selectedTab != BottomNavTab.HOME) {
            selectedTab = BottomNavTab.HOME
        } else {
            showExitDialog = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (selectedTab == BottomNavTab.SCAN) Color.Transparent else AppTheme.colors.Background,
        bottomBar = {
            AppBottomNavBar(
                selectedTab = selectedTab,
                isScanUploadMode = isScanUploadMode,
                onTabClick = { tab -> 
                    if (tab == BottomNavTab.SCAN && selectedTab == BottomNavTab.SCAN) {
                        captureTrigger++
                    } else {
                        if (tab != BottomNavTab.SCAN) {
                            isScanUploadMode = false
                        }
                        selectedTab = tab 
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 40.dp)
            ) { data ->
                AppSnackbar(snackbarData = data)
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        val bottomPadding = innerPadding.calculateBottomPadding()

        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(400),
                label = "bottomNavTab",
            ) { tab ->
            when (tab) {
                BottomNavTab.HOME -> {
                    HomeScreen(
                        bottomPadding = bottomPadding,
                        onNavigateToScanResult = onNavigateToProductDetail,
                        onNavigateToHistory = onNavigateToHistory,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onNavigateToNews = onNavigateToNews,
                        onNavigateToChatWithAi = onNavigateToChatWithAi,
                        onNavigateToEditProfile = onNavigateToEditProfile,
                        onNavigateToScan = { selectedTab = BottomNavTab.SCAN },
                        onNavigateToAccountPendingDeletion = onNavigateToAccountPendingDeletion,
                    )
                }
                BottomNavTab.CALORIES -> {
                    CaloriesScreen(
                        bottomPadding = bottomPadding,
                        snackbarHostState = snackbarHostState,
                        onNavigateToProductDetail = { uiModel -> onNavigateToProductDetail(uiModel.id) },
                        onNavigateToExercises = onNavigateToExercises,
                        onNavigateToStepHistory = onNavigateToStepHistory,
                        onNavigateToSavedProducts = { selectedTab = BottomNavTab.SAVED }
                    )
                }
                BottomNavTab.SCAN -> {
                    CameraScanScreen(
                        bottomPadding = bottomPadding,
                        snackbarHostState = snackbarHostState,
                        captureTrigger = captureTrigger,
                        onCenterActionUploadModeChanged = { isUpload ->
                            isScanUploadMode = isUpload
                        },
                        onNavigateToProductDetail = { uiModel -> onNavigateToProductDetail(uiModel.id) }
                    )
                }
                BottomNavTab.SAVED -> {
                    SavedScreen(
                        bottomPadding = bottomPadding,
                        snackbarHostState = snackbarHostState,
                        onNavigateToProductDetail = { uiModel -> onNavigateToProductDetail(uiModel.id) },
                        onNavigateToScan = { selectedTab = BottomNavTab.SCAN }
                    )
                }
                BottomNavTab.PROFILE -> {
                    UserProfileScreen(
                        bottomPadding = bottomPadding,
                        onNavigateToScanHistory = onNavigateToHistory,
                        onNavigateToNotificationSettings = onNavigateToNotificationSettings,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToEditProfile = onNavigateToEditProfile,
                        onNavigateToCaloriesHistory = onNavigateToCaloriesHistory,
                    )
                }
            }
            }
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
