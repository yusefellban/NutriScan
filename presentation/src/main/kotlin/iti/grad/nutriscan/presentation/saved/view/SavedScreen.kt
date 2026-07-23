package iti.grad.nutriscan.presentation.saved.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.Dp
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.components.EmptyStateWidget
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.saved.state.SavedEffect
import iti.grad.nutriscan.presentation.saved.state.SavedEvent
import iti.grad.nutriscan.presentation.saved.state.SavedState
import iti.grad.nutriscan.presentation.saved.view.components.SavedProductGrid
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
    val snackbarHostState = remember { SnackbarHostState() }
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
        modifier = Modifier
            .fillMaxSize()
            .padding(top = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding())
    )
}

@Composable
private fun SavedScreenContent(
    state: SavedState,
    onEvent: (SavedEvent) -> Unit,
    bottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    if (state.filteredProducts.isEmpty()) {
        Column(modifier = modifier.padding(bottom = bottomPadding)) {
            SavedSearchBar(
                query = state.searchQuery,
                onQueryChange = { onEvent(SavedEvent.SearchQueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
            
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateWidget(
                    message = stringResource(id = R.string.saved_empty_state)
                )
            }
        }
    } else {
        SavedProductGrid(
            products = state.filteredProducts,
            onProductClick = { product -> onEvent(SavedEvent.ProductClicked(product)) },
            onSwipeToAdd = { productId -> onEvent(SavedEvent.SwipeToAddTriggered(productId)) },
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = bottomPadding + 4.dp),
            header = {
                SavedSearchBar(
                    query = state.searchQuery,
                    onQueryChange = { onEvent(SavedEvent.SearchQueryChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 12.dp)
                )
            },
            modifier = modifier
        )
    }
}
