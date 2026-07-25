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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import iti.grad.nutriscan.presentation.common.components.AppBottomNavBar
import iti.grad.nutriscan.presentation.common.components.AppSnackbar
import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.home.view.HomeScreen
import iti.grad.nutriscan.presentation.main.calories.view.CaloriesScreen
import iti.grad.nutriscan.presentation.saved.view.SavedScreen
import iti.grad.nutriscan.presentation.scan.camera.view.CameraScanScreen
import iti.grad.nutriscan.presentation.settings.profile.view.UserProfileScreen

@Composable
fun MainScreen(
    onNavigateToScanResult: (String) -> Unit,
    onNavigateToScanProcessing: (String) -> Unit,
    onNavigateToProductDetail: (ProductUiModel) -> Unit,
    onNavigateToNews: () -> Unit,
    onNavigateToChatWithAi: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToFamilyMemberDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToExercises: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: BottomNavTab = BottomNavTab.HOME
) {
    var selectedTab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (selectedTab == BottomNavTab.SCAN) Color.Transparent else AppTheme.colors.Background,
        bottomBar = {
            AppBottomNavBar(
                selectedTab = selectedTab,
                onTabClick = { tab -> selectedTab = tab }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                AppSnackbar(message = data.visuals.message)
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
                        onNavigateToScanResult = onNavigateToScanResult,
                        onNavigateToHistory = onNavigateToHistory,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onNavigateToNews = onNavigateToNews,
                        onNavigateToChatWithAi = onNavigateToChatWithAi,
                    )
                }
                BottomNavTab.CALORIES -> {
                    CaloriesScreen(
                        bottomPadding = bottomPadding,
                        snackbarHostState = snackbarHostState,
                        onNavigateToProductDetail = onNavigateToProductDetail,
                        onNavigateToExercises = onNavigateToExercises,
                        onNavigateToSavedProducts = { selectedTab = BottomNavTab.SAVED }
                    )
                }
                BottomNavTab.SCAN -> {
                    CameraScanScreen(
                        bottomPadding = bottomPadding,
                        snackbarHostState = snackbarHostState,
                        onNavigateToProcessing = onNavigateToScanProcessing,
                        onNavigateToProductDetail = onNavigateToProductDetail,
                    )
                }
                BottomNavTab.SAVED -> {
                    SavedScreen(
                        bottomPadding = bottomPadding,
                        snackbarHostState = snackbarHostState,
                        onNavigateToProductDetail = onNavigateToProductDetail
                    )
                }
                BottomNavTab.PROFILE -> {
                    UserProfileScreen(
                        bottomPadding = bottomPadding,
                        onNavigateToScanHistory = onNavigateToHistory,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToEditProfile = onNavigateToEditProfile,
                    )
                }
            }
        }
    }
}
