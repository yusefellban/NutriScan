package iti.grad.nutriscan.presentation.exercises.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.common.components.AppBackButton
import iti.grad.nutriscan.presentation.common.components.AppEmptyStateWidget
import iti.grad.nutriscan.presentation.common.components.ExerciseListItemCard
import iti.grad.nutriscan.presentation.common.components.ExerciseListItemShimmerCard
import iti.grad.nutriscan.presentation.common.components.OfflineStateWidget
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.exercises.state.ExercisesEffect
import iti.grad.nutriscan.presentation.exercises.state.ExercisesEvent
import iti.grad.nutriscan.presentation.exercises.view.components.ExerciseInstructionsBottomSheet
import iti.grad.nutriscan.presentation.exercises.viewmodel.ExercisesViewModel
import iti.grad.nutriscan.presentation.profile_setup.view.components.SelectableChip
import iti.grad.nutriscan.presentation.saved.view.components.SavedSearchBar
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest
import iti.grad.nutriscan.presentation.common.components.SearchNotFoundEmptyStateWidget

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.graphics.Color

import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size

@Composable
fun ExercisesScreen(
    viewModel: ExercisesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToWorkout: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                ExercisesEffect.NavigateBack -> onNavigateBack()
                is ExercisesEffect.NavigateToExerciseWorkout -> onNavigateToWorkout(effect.exerciseId)
            }
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.Background,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 22.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

        // ── Header ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AppBackButton(
                onClick = { viewModel.onEvent(ExercisesEvent.OnBackClick) },
                iconTint = AppTheme.colors.ExerciseBackButtonTint,
                borderColor = AppTheme.colors.ExerciseBackButtonTint
            )
            Text(
                text = stringResource(id = R.string.exercises_title),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.TextPrimary,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Search bar (shared component with height 48.dp to match UI height) ──
        SavedSearchBar(
            query = state.searchQuery,
            onQueryChange = { viewModel.onEvent(ExercisesEvent.OnSearchQueryChange(it)) },
            height = 48.dp
        )

        // 5- decrease slightly the vertical space between the search bar and the chips which are under it
        Spacer(modifier = Modifier.height(10.dp))

        // ── Category chips ──
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.categories, key = { it.id }) { category ->
                SelectableChip(
                    text = category.label,
                    isSelected = state.selectedCategoryId == category.id,
                    onClick = { viewModel.onEvent(ExercisesEvent.OnCategorySelected(category.id)) },
                    selectedBgColor = AppTheme.colors.ExerciseChipSelectedBg,
                    unselectedBgColor = AppTheme.colors.ExerciseChipUnselectedBg,
                    selectedBorderColor = AppTheme.colors.ExerciseChipSelectedBorder,
                    unselectedBorderColor = AppTheme.colors.ExerciseChipUnselectedBorder,
                    selectedTextColor = AppTheme.colors.ExerciseChipSelectedText,
                    unselectedTextColor = AppTheme.colors.ExerciseChipUnselectedText
                )
            }
        }

        // 5- increase slightly the vertical space beween the chips and the exercises items
        Spacer(modifier = Modifier.height(24.dp))

        // ── Exercise list ──
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(6) {
                            ExerciseListItemShimmerCard()
                        }
                    }
                }
                state.errorMessageRes != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        OfflineStateWidget(
                            onRetry = { viewModel.onEvent(ExercisesEvent.OnRetryClick) }
                        )
                    }
                }
                state.visibleExercises.isEmpty() -> {
                    Box(
                        modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.searchQuery.isNotEmpty()) {
                            SearchNotFoundEmptyStateWidget(
                                showButton = false
                            )
                        } else {
                            AppEmptyStateWidget(
                                lightImageRes = R.drawable.search_reasult_not_found_light,
                                darkImageRes = R.drawable.search_reasult_not_found_dark,
                                title = stringResource(id = R.string.exercises_empty_state),
                                subtitle = "",
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        // 5- increase slightly the vertical space beween the exercises items itself
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.visibleExercises, key = { it.id }) { exercise ->
                            ExerciseListItemCard(
                                exercise = exercise,
                                onClick = { viewModel.onEvent(ExercisesEvent.OnExerciseClick(exercise.id)) }
                            )
                        }

                        // Pagination loading indicator or retry
                        if (state.hasNextPage) {
                            item {
                                LaunchedEffect(Unit) {
                                    viewModel.onEvent(ExercisesEvent.OnLoadMore)
                                }
                                if (state.isLoadingMore) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = AppTheme.colors.Teal1000,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom spacing so content isn't hidden under bottom sheet
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
    }

    // Bottom sheet
    state.selectedExercise?.let { exercise ->
        ExerciseInstructionsBottomSheet(
            exercise = exercise,
            isExpanded = state.isInstructionsExpanded,
            onDismiss = { viewModel.onEvent(ExercisesEvent.OnDismissInstructions) },
            onReadMoreClick = { viewModel.onEvent(ExercisesEvent.OnReadMoreClick) },
            onStartWorkoutClick = { viewModel.onEvent(ExercisesEvent.OnStartWorkoutClick) }
        )
    }
}
