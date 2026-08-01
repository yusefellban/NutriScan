package iti.grad.nutriscan.presentation.news.view


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import iti.grad.nutriscan.presentation.common.components.SearchNotFoundEmptyStateWidget
import iti.grad.nutriscan.presentation.common.components.AppBackButton
import iti.grad.nutriscan.presentation.common.components.OfflineStateWidget
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.news.state.NewsEffect
import iti.grad.nutriscan.presentation.news.state.NewsEvent
import iti.grad.nutriscan.presentation.news.state.NewsState
import iti.grad.nutriscan.presentation.news.state.NewsUiArticle
import iti.grad.nutriscan.presentation.news.view.components.NewsArticleCard
import iti.grad.nutriscan.presentation.news.view.components.NewsArticleShimmerCard
import iti.grad.nutriscan.presentation.news.view.components.NewsSearchBar
import iti.grad.nutriscan.presentation.news.view.components.NewsTopicChipRow
import iti.grad.nutriscan.presentation.news.view.components.NewsTopicChipShimmerRow
import iti.grad.nutriscan.presentation.news.viewmodel.NewsViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest
import java.time.Duration
import java.time.Instant

@Composable
fun NewsScreen(
    viewModel: NewsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (NewsUiArticle) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NewsEffect.NavigateBack -> onNavigateBack()
                is NewsEffect.NavigateToDetail -> onNavigateToDetail(effect.article)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NewsContent(state = state, onEvent = viewModel::onEvent)
    }
}

@Composable
private fun NewsContent(
    state: NewsState,
    onEvent: (NewsEvent) -> Unit,
) {
    Scaffold(containerColor = AppTheme.colors.Background) { innerPadding ->
        if (state.errorMessageResId != null && state.articles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                ) {
                    AppBackButton(
                        onClick = { onEvent(NewsEvent.BackClicked) },
                        iconTint = AppTheme.colors.NewsCategoryLabel,
                        borderColor = AppTheme.colors.NewsCategoryLabel,
                    )
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    OfflineStateWidget(onRetry = { onEvent(NewsEvent.RetryClicked) })
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 22.dp)
            ) {
                // Header: Back button + Title in same row
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppBackButton(
                        onClick = { onEvent(NewsEvent.BackClicked) },
                        iconTint = AppTheme.colors.NewsCategoryLabel,
                        borderColor = AppTheme.colors.NewsCategoryLabel,
                    )
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text(
                            text = stringResource(id = R.string.news_screen_title),
                            style = AppTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.NewsScreenTitle,
                        )
                        Text(
                            text = stringResource(id = R.string.news_screen_subtitle),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.NewsSourceText,
                        )
                    }
                }
                
                // Search Bar
                Spacer(modifier = Modifier.height(20.dp))
                NewsSearchBar(
                    query = state.searchQuery,
                    onQueryChange = { onEvent(NewsEvent.SearchQueryChanged(it)) },
                    onFilterClick = { onEvent(NewsEvent.FilterClicked) },
                )
                
                // Chips Row
                Spacer(modifier = Modifier.height(16.dp))
                if (state.isLoading && state.chips.isEmpty()) {
                    NewsTopicChipShimmerRow()
                } else {
                    NewsTopicChipRow(
                        chips = state.chips,
                        selectedChipIds = state.selectedChipIds,
                        onChipClicked = { onEvent(NewsEvent.ChipClicked(it)) },
                    )
                }
                
                // Articles List
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        state.isLoading -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            userScrollEnabled = false,
                        ) {
                            items(5) { 
                                NewsArticleShimmerCard()
                            }
                        }
                        state.errorMessageResId != null -> OfflineStateWidget(
                            onRetry = { onEvent(NewsEvent.RetryClicked) },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    state.articles.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.searchQuery.isNotEmpty()) {
                                SearchNotFoundEmptyStateWidget(
                                    showButton = false
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.news_empty_state),
                                    style = AppTheme.typography.bodyMedium,
                                    color = AppTheme.colors.TextSecondary,
                                )
                            }
                        }
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items = state.articles, key = { it.url }) { article ->
                            NewsArticleCard(
                                article = article,
                                onClick = { onEvent(NewsEvent.ArticleClicked(article)) },
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

