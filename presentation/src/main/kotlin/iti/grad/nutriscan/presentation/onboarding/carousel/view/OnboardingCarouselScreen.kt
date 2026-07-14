package iti.grad.nutriscan.presentation.onboarding.carousel.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import iti.grad.nutriscan.presentation.common.components.AuthActionButton
import iti.grad.nutriscan.presentation.common.theme.LexendDeca
import iti.grad.nutriscan.presentation.onboarding.carousel.state.OnboardingCarouselEffect
import iti.grad.nutriscan.presentation.onboarding.carousel.state.OnboardingCarouselEvent
import iti.grad.nutriscan.presentation.onboarding.carousel.view.components.OnboardingPageContent
import iti.grad.nutriscan.presentation.onboarding.carousel.view.components.OnboardingPageIndicator
import iti.grad.nutriscan.presentation.onboarding.carousel.viewmodel.OnboardingCarouselViewModel
import iti.grad.presentation.R

@Composable
fun OnboardingCarouselScreen(
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingCarouselViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { state.pageCount })

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is OnboardingCarouselEffect.NavigateToLogin -> onNavigateToLogin()
                is OnboardingCarouselEffect.ScrollToPage -> {
                    pagerState.animateScrollToPage(effect.page)
                }
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.onEvent(OnboardingCarouselEvent.PageChanged(page))
        }
    }

    val images = listOf(
        R.drawable.onboarding_1,
        R.drawable.onboarding_2,
        R.drawable.onboarding_3
    )

    val titles = listOf(
        R.string.onboarding_title_1,
        R.string.onboarding_title_2,
        R.string.onboarding_title_3
    )

    val descriptions = listOf(
        R.string.onboarding_desc_1,
        R.string.onboarding_desc_2,
        R.string.onboarding_desc_3
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Row: BACK / SKIP
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val showBack = state.currentPage > 0
            val showSkip = state.currentPage < state.pageCount - 1

            if (showBack) {
                Text(
                    text = stringResource(id = R.string.onboarding_back),
                    fontFamily = LexendDeca,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.clickable {
                        viewModel.onEvent(OnboardingCarouselEvent.BackClicked)
                    }
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (showSkip) {
                Text(
                    text = stringResource(id = R.string.onboarding_skip),
                    fontFamily = LexendDeca,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.clickable {
                        viewModel.onEvent(OnboardingCarouselEvent.SkipClicked)
                    }
                )
            } else if (showBack) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingPageContent(
                imageRes = images[page],
                titleRes = titles[page],
                descriptionRes = descriptions[page],
                isSelected = page == state.currentPage
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Button
        val buttonTextRes = if (state.currentPage == state.pageCount - 1) {
            R.string.onboarding_lets_start
        } else {
            R.string.onboarding_next
        }

        AuthActionButton(
            textResId = buttonTextRes,
            isLoading = false,
            onClick = {
                viewModel.onEvent(OnboardingCarouselEvent.NextClicked)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Page Indicator
        OnboardingPageIndicator(
            currentPage = state.currentPage,
            pageCount = state.pageCount,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}
