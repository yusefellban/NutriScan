package iti.grad.nutriscan.presentation.profile_setup.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import iti.grad.nutriscan.presentation.profile_setup.state.ProfileSetupPagerEffect
import androidx.compose.foundation.shape.CircleShape
import iti.grad.nutriscan.presentation.profile_setup.viewmodel.ProfileSetupPagerViewModel
import androidx.compose.runtime.LaunchedEffect
import iti.grad.nutriscan.presentation.profile_setup.view.components.ProgressNextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.foundation.layout.WindowInsets
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.profile_setup.view.components.HealthProfileContent
import iti.grad.nutriscan.presentation.profile_setup.state.ProfileSetupPagerEvent
import androidx.compose.ui.Alignment
import iti.grad.nutriscan.presentation.common.components.AppBackButton
import iti.grad.nutriscan.presentation.common.components.BackButtonSurface
import iti.grad.nutriscan.presentation.profile_setup.view.components.WeightSelectionPage
import iti.grad.nutriscan.presentation.common.components.AppSnackbar
import iti.grad.nutriscan.presentation.common.components.SnackbarType
import iti.grad.nutriscan.presentation.common.components.showAppSnackbar
import androidx.compose.ui.platform.LocalContext
import iti.grad.nutriscan.presentation.profile_setup.view.components.HeightSelectionPage
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.SnackbarHostState
import iti.grad.nutriscan.presentation.profile_setup.view.components.DateOfBirthPage
import androidx.compose.runtime.Composable
import iti.grad.nutriscan.presentation.profile_setup.view.components.GenderSelectionPage
import iti.grad.nutriscan.presentation.profile_setup.view.components.ProfileSetupPageIndicator
import androidx.compose.foundation.layout.Box
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier

@Composable
fun ProfileSetupPagerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: ProfileSetupPagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { state.pageCount })

    // Sync pager page changes back to ViewModel
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.onEvent(ProfileSetupPagerEvent.PageChanged(page))
        }
    }

    // Handle system back button (hardware or gesture)
    BackHandler {
        viewModel.onEvent(ProfileSetupPagerEvent.BackClicked)
    }

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ProfileSetupPagerEffect.ScrollToPage -> {
                    pagerState.animateScrollToPage(effect.page)
                }
                ProfileSetupPagerEffect.NavigateBack -> onNavigateBack()
                ProfileSetupPagerEffect.NavigateToHome -> onNavigateToHome()
                is ProfileSetupPagerEffect.ShowSnackbar -> {
                    val message = effect.messageStr
                        ?: effect.messageResId?.let { context.getString(it) }
                        ?: ""
                    snackbarHostState.showAppSnackbar(
                        message = message,
                        type = SnackbarType.ERROR
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                AppSnackbar(snackbarData = data)
            }
        },
        containerColor = AppTheme.colors.Background,
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Pager fills the ENTIRE screen
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> GenderSelectionPage(
                        selectedGender = state.selectedGender,
                        currentPage = state.currentPage,
                        pageCount = state.pageCount - 1,
                        onEvent = viewModel::onEvent
                    )
                    1 -> DateOfBirthPage(
                        selectedDateMillis = state.selectedDateOfBirthMillis,
                        currentPage = state.currentPage,
                        pageCount = state.pageCount - 1,
                        onEvent = viewModel::onEvent
                    )
                    2 -> HeightSelectionPage(
                        selectedHeightCm = state.selectedHeightCm,
                        currentPage = state.currentPage,
                        pageCount = state.pageCount - 1,
                        onEvent = viewModel::onEvent
                    )
                    3 -> WeightSelectionPage(
                        selectedWeightKg = state.selectedWeightKg,
                        currentPage = state.currentPage,
                        pageCount = state.pageCount - 1,
                        onEvent = viewModel::onEvent
                    )
                    4 -> HealthProfileContent(
                        state = state,
                        onEvent = viewModel::onEvent
                    )
                }
            }

            val isHealthProfilePage = state.currentPage == state.pageCount - 1

            // Back button — overlaid top-left on top of page content (hidden on last page)
            if (!isHealthProfilePage) {
                AppBackButton(
                    onClick = { viewModel.onEvent(ProfileSetupPagerEvent.BackClicked) },
                    surface = BackButtonSurface.OnLight,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 24.dp, top = 24.dp)
                        .align(Alignment.TopStart)
                )
            }

            // Next FAB — overlaid bottom-center (hidden on last page)
            if (!isHealthProfilePage) {
                ProgressNextButton(
                    currentPage = state.currentPage,
                    totalPages = state.pageCount - 1,
                    onClick = { viewModel.onEvent(ProfileSetupPagerEvent.NextClicked) },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp)
                )
            }
        }
    }
}
