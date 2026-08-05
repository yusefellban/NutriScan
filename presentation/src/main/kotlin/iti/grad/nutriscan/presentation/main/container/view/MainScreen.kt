package iti.grad.nutriscan.presentation.main.container.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import iti.grad.nutriscan.presentation.common.components.AppBottomNavBar
import iti.grad.nutriscan.presentation.common.components.AppSnackbar
import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.home.view.HomeScreen
import iti.grad.nutriscan.presentation.main.calories.view.CaloriesScreen
import iti.grad.nutriscan.presentation.saved.view.SavedScreen
import iti.grad.nutriscan.presentation.scan.camera.view.CameraScanScreen
import iti.grad.nutriscan.presentation.settings.profile.view.UserProfileScreen

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
    modifier: Modifier = Modifier,
    initialTab: BottomNavTab = BottomNavTab.HOME
) {
    var selectedTab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    var captureTrigger by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (selectedTab == BottomNavTab.SCAN) Color.Transparent else AppTheme.colors.Background,
        bottomBar = {
            AppBottomNavBar(
                selectedTab = selectedTab,
                onTabClick = { tab -> 
                    if (tab == BottomNavTab.SCAN && selectedTab == BottomNavTab.SCAN) {
                        captureTrigger++
                    } else {
                        selectedTab = tab 
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                AppSnackbar(snackbarData = data)
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        val bottomPadding = innerPadding.calculateBottomPadding()

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                BottomNavTab.HOME -> {
                    HomeScreen(
                        bottomPadding = bottomPadding,
                        onNavigateToScanResult = onNavigateToProductDetail,
                        onNavigateToHistory = onNavigateToHistory,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onNavigateToNews = onNavigateToNews,
                        onNavigateToChatWithAi = onNavigateToChatWithAi,
                        onNavigateToEditProfile = onNavigateToEditProfile,
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
