package iti.grad.nutriscan.presentation.news.home.view

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.common.components.AppBackButton
import iti.grad.nutriscan.presentation.common.components.BackButtonSurface
import iti.grad.nutriscan.presentation.common.components.OfflineStateWidget
import iti.grad.nutriscan.presentation.common.components.shimmerEffect
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.news.home.state.NewsHomeEffect
import iti.grad.nutriscan.presentation.news.home.state.NewsHomeEvent
import iti.grad.nutriscan.presentation.news.home.state.NewsHomeState
import iti.grad.nutriscan.presentation.news.home.view.components.BreakingNewsPager
import iti.grad.nutriscan.presentation.news.home.view.components.BreakingNewsPagerShimmer
import iti.grad.nutriscan.presentation.news.home.viewmodel.NewsHomeViewModel
import iti.grad.nutriscan.presentation.news.state.NewsUiArticle
import iti.grad.nutriscan.presentation.news.view.components.NewsArticleCard
import iti.grad.nutriscan.presentation.news.view.components.NewsArticleShimmerCard
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun NewsHomeScreen(
    viewModel: NewsHomeViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToDiscover: () -> Unit = {},
    onNavigateToDetail: (NewsUiArticle) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NewsHomeEffect.NavigateBack -> onNavigateBack()
                is NewsHomeEffect.NavigateToDiscover -> onNavigateToDiscover()
                is NewsHomeEffect.NavigateToDetail -> onNavigateToDetail(effect.article)
            }
        }
    }

    NewsHomeContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
private fun NewsHomeContent(
    state: NewsHomeState,
    onEvent: (NewsHomeEvent) -> Unit,
) {
    Scaffold(containerColor = AppTheme.colors.Background) { innerPadding ->
        if (state.errorMessageResId != null && state.breakingArticles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppBackButton(
                        onClick = { onEvent(NewsHomeEvent.BackClicked) },
                        surface = BackButtonSurface.OnLight,
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = stringResource(id = R.string.news_home_search_content_description),
                        tint = AppTheme.colors.NewsScreenTitle,
                        modifier = Modifier
                            .size(26.dp)
                            .clickable { onEvent(NewsHomeEvent.SearchClicked) },
                    )
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    OfflineStateWidget(onRetry = { onEvent(NewsHomeEvent.RetryClicked) })
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // ── Header row: Back button (left) + Search icon (right) ──
                item(key = "header") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppBackButton(
                            onClick = { onEvent(NewsHomeEvent.BackClicked) },
                            surface = BackButtonSurface.OnLight,
                        )

                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = stringResource(id = R.string.news_home_search_content_description),
                            tint = AppTheme.colors.NewsScreenTitle,
                            modifier = Modifier
                                .size(26.dp)
                                .clickable { onEvent(NewsHomeEvent.SearchClicked) },
                        )
                    }
                }

                // ── Breaking News section ──
                item(key = "breaking_title") {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(id = R.string.news_home_breaking_news),
                        style = AppTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.SectionSubtitle,
                        modifier = Modifier.padding(horizontal = 22.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item(key = "breaking_pager") {
                    when {
                        state.isLoading -> BreakingNewsPagerShimmer(
                            modifier = Modifier.padding(start = 22.dp),
                        )
                        else -> BreakingNewsPager(
                            articles = state.breakingArticles,
                            onArticleClicked = { onEvent(NewsHomeEvent.BreakingArticleClicked(it)) },
                            modifier = Modifier.padding(start = 22.dp),
                        )
                    }
                }

                // ── Recommendation section ──
                item(key = "recommendation_title") {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(id = R.string.news_home_recommendation),
                        style = AppTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.SectionSubtitle,
                        modifier = Modifier.padding(horizontal = 22.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (state.isLoading) {
                    items(4) {
                        NewsArticleShimmerCard(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
                        )
                    }
                } else if (state.errorMessageResId == null) {
                    items(
                        items = state.recommendationArticles,
                        key = { it.url },
                    ) { article ->
                        NewsArticleCard(
                            article = article,
                            onClick = { onEvent(NewsHomeEvent.RecommendationArticleClicked(article)) },
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
