package iti.grad.nutriscan.presentation.profile_setup.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.common.components.AppBackButton
import iti.grad.nutriscan.presentation.common.components.AppSnackbar
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.profile_setup.state.ProfileSetupPagerEffect
import iti.grad.nutriscan.presentation.profile_setup.state.ProfileSetupPagerEvent
import iti.grad.nutriscan.presentation.profile_setup.view.components.DateOfBirthPlaceholder
import iti.grad.nutriscan.presentation.profile_setup.view.components.GenderSelectionPage
import iti.grad.nutriscan.presentation.profile_setup.view.components.HealthProfileContent
import iti.grad.nutriscan.presentation.profile_setup.view.components.HeightSelectionPlaceholder
import iti.grad.nutriscan.presentation.profile_setup.view.components.ProfileSetupPageIndicator
import iti.grad.nutriscan.presentation.profile_setup.view.components.ProgressNextButton
import iti.grad.nutriscan.presentation.profile_setup.view.components.WeightSelectionPlaceholder
import iti.grad.nutriscan.presentation.profile_setup.viewmodel.ProfileSetupPagerViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest

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
                    snackbarHostState.showSnackbar(message = message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                AppSnackbar(message = data.visuals.message)
            }
        },
        containerColor = AppTheme.colors.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        onEvent = viewModel::onEvent
                    )
                    1 -> DateOfBirthPlaceholder()
                    2 -> HeightSelectionPlaceholder()
                    3 -> WeightSelectionPlaceholder()
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
                    iconTint = AppTheme.colors.Primary,
                    borderColor = AppTheme.colors.Primary,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 24.dp, top = 24.dp)
                        .align(Alignment.TopStart)
                )
            }

            // Page indicator — overlaid top-center (only on pages 0–3)
            if (!isHealthProfilePage) {
                ProfileSetupPageIndicator(
                    currentPage = state.currentPage,
                    pageCount = state.pageCount - 1,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 80.dp)
                        .align(Alignment.TopCenter)
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
