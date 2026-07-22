package iti.grad.nutriscan.presentation.saved.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.components.AppBottomNavBar
import iti.grad.nutriscan.presentation.common.components.EmptyStateWidget
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.saved.state.SavedEffect
import iti.grad.nutriscan.presentation.saved.state.SavedEvent
import iti.grad.nutriscan.presentation.saved.state.SavedState
import iti.grad.nutriscan.presentation.saved.view.components.SavedProductGrid
import iti.grad.nutriscan.presentation.saved.view.components.SavedSearchBar
import iti.grad.nutriscan.presentation.saved.viewmodel.SavedViewModel

@Composable
fun SavedScreen(
    viewModel: SavedViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {},
    onNavigateToScan: () -> Unit = {},
    onNavigateToCalories: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToProductDetail: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val addedTemplate = stringResource(id = R.string.food_log_added_snackbar)
    val addErrorMessage = stringResource(id = R.string.food_log_add_error)

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SavedEffect.NavigateToHome -> onNavigateToHome()
                is SavedEffect.NavigateToScan -> onNavigateToScan()
                is SavedEffect.NavigateToCalories -> onNavigateToCalories()
                is SavedEffect.NavigateToProfile -> onNavigateToProfile()
                is SavedEffect.NavigateToProductDetail -> onNavigateToProductDetail(effect.productId)
                is SavedEffect.ShowAddedToFoodLogSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = String.format(addedTemplate, effect.productName),
                        duration = SnackbarDuration.Short
                    )
                }
                is SavedEffect.ShowAddErrorSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = addErrorMessage,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AppBottomNavBar(
                selectedTab = state.selectedTab,
                onTabClick = { tab -> viewModel.onEvent(SavedEvent.BottomNavTabClicked(tab)) }
            )
        },
        containerColor = AppTheme.colors.SurfaceVariant
    ) { paddingValues ->
        SavedScreenContent(
            state = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@Composable
private fun SavedScreenContent(
    state: SavedState,
    onEvent: (SavedEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.filteredProducts.isEmpty()) {
        Column(modifier = modifier) {
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
            onProductClick = { productId -> onEvent(SavedEvent.ProductClicked(productId)) },
            onSwipeToAdd = { productId -> onEvent(SavedEvent.SwipeToAddTriggered(productId)) },
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 4.dp),
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
