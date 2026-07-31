package iti.grad.nutriscan.presentation.saved.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.components.EmptyStateWidget
import iti.grad.nutriscan.presentation.common.components.HeroHeaderTitle
import iti.grad.nutriscan.presentation.common.components.OfflineStateWidget
import iti.grad.nutriscan.presentation.common.components.SectionHeroHeader
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.saved.state.SavedEffect
import iti.grad.nutriscan.presentation.saved.state.SavedEvent
import iti.grad.nutriscan.presentation.saved.state.SavedState
import iti.grad.nutriscan.presentation.saved.view.components.SavedProductGrid
import iti.grad.nutriscan.presentation.common.components.PullToRefreshShimmerBox
import iti.grad.nutriscan.presentation.saved.view.components.SavedProductShimmerGrid
import iti.grad.nutriscan.presentation.saved.view.components.SavedSearchBar
import iti.grad.nutriscan.presentation.saved.viewmodel.SavedViewModel
import kotlinx.coroutines.launch

@Composable
fun SavedScreen(
    viewModel: SavedViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp,
    snackbarHostState: SnackbarHostState,
    onNavigateToProductDetail: (ProductUiModel) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val addedTemplate = stringResource(id = R.string.food_log_added_snackbar)
    val addErrorMessage = stringResource(id = R.string.food_log_add_error)
    val snackbarScope = rememberCoroutineScope()

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SavedEffect.NavigateToProductDetail -> onNavigateToProductDetail(effect.product)
                is SavedEffect.ShowAddedToFoodLogSnackbar -> {
                    // Launched on its own scope so showing the snackbar (which suspends until
                    // dismissed) never stalls this loop from handling the next effect — e.g. a
                    // bottom-nav tap right after a swipe must navigate immediately, not wait.
                    snackbarScope.launch {
                        snackbarHostState.showSnackbar(
                            message = String.format(addedTemplate, effect.productName),
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                is SavedEffect.ShowAddErrorSnackbar -> {
                    snackbarScope.launch {
                        snackbarHostState.showSnackbar(
                            message = addErrorMessage,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    SavedScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        bottomPadding = bottomPadding,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun SavedScreenContent(
    state: SavedState,
    onEvent: (SavedEvent) -> Unit,
    bottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(AppTheme.colors.ProfileHeaderBackground),
    ) {
        SectionHeroHeader {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 56.dp, bottom = 16.dp),
            ) {
                HeroHeaderTitle(text = stringResource(R.string.saved_screen_title))
                // Pushes the search bar down to sit just above the item list below.
                Spacer(modifier = Modifier.weight(1f))
                SavedSearchBar(
                    query = state.searchQuery,
                    onQueryChange = { onEvent(SavedEvent.SearchQueryChanged(it)) },
                    textColor = Color.White,
                    placeholderColor = Color.White.copy(alpha = 0.7f),
                    borderColor = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(AppTheme.colors.Background),
        ) {
            val shimmerGrid: @Composable () -> Unit = {
                SavedProductShimmerGrid(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = bottomPadding + 4.dp),
                )
            }

            PullToRefreshShimmerBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(SavedEvent.Refreshed) },
                shimmer = shimmerGrid,
                modifier = Modifier.fillMaxSize(),
            ) {
            when {
                state.isLoading -> shimmerGrid()
                state.error != null && state.products.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        OfflineStateWidget(onRetry = { onEvent(SavedEvent.RetryLoad) })
                    }
                }
                state.filteredProducts.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateWidget(
                            message = stringResource(id = R.string.saved_empty_state)
                        )
                    }
                }
                else -> {
                    SavedProductGrid(
                        products = state.filteredProducts,
                        onProductClick = { product -> onEvent(SavedEvent.ProductClicked(product)) },
                        onSwipeToAdd = { productId -> onEvent(SavedEvent.SwipeToAddTriggered(productId)) },
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = bottomPadding + 4.dp),
                    )
                }
            }
            }
        }
    }
}
