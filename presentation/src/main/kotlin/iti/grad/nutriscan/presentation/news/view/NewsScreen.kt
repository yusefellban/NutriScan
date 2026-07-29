package iti.grad.nutriscan.presentation.news.view

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.common.components.AppBackButton
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.news.state.NewsEffect
import iti.grad.nutriscan.presentation.news.state.NewsEvent
import iti.grad.nutriscan.presentation.news.state.NewsState
import iti.grad.nutriscan.presentation.news.view.components.NewsArticleCard
import iti.grad.nutriscan.presentation.news.view.components.NewsArticleShimmerCard
import iti.grad.nutriscan.presentation.news.view.components.NewsOfflineState
import iti.grad.nutriscan.presentation.news.view.components.NewsTopicChipRow
import iti.grad.nutriscan.presentation.news.view.components.NewsTopicChipShimmerRow
import iti.grad.nutriscan.presentation.news.viewmodel.NewsViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun NewsScreen(
    viewModel: NewsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NewsEffect.NavigateBack -> onNavigateBack()
                is NewsEffect.OpenArticle -> {
                    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(effect.url))
                }
                is NewsEffect.ShareArticle -> {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, effect.title)
                        putExtra(Intent.EXTRA_TEXT, effect.url)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                }
            }
        }
    }

    NewsContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
private fun NewsContent(
    state: NewsState,
    onEvent: (NewsEvent) -> Unit,
) {
    Scaffold(containerColor = AppTheme.colors.Background) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                AppBackButton(
                    onClick = { onEvent(NewsEvent.BackClicked) },
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                Image(
                    painter = painterResource(R.drawable.nutriscan_header),
                    contentDescription = stringResource(R.string.news_screen_title),
                    modifier = Modifier.height(20.dp).align(Alignment.Center),
                )
            }

            if (state.isLoading && state.chips.isEmpty()) {
                NewsTopicChipShimmerRow(
                    modifier = Modifier
                        .padding(horizontal = 22.dp)
                        .padding(top = 4.dp, bottom = 16.dp)
                )
            } else {
                NewsTopicChipRow(
                    chips = state.chips,
                    selectedChipIds = state.selectedChipIds,
                    onChipClicked = { onEvent(NewsEvent.ChipClicked(it)) },
                    modifier = Modifier
                        .padding(horizontal = 22.dp)
                        .padding(top = 4.dp, bottom = 16.dp),
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = false,
                    ) {
                        items(5) { 
                            NewsArticleShimmerCard()
                        }
                    }
                    state.errorMessageResId != null -> NewsOfflineState(
                        onRetry = { onEvent(NewsEvent.RetryClicked) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                    state.articles.isEmpty() -> Text(
                        text = stringResource(R.string.news_empty_state),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.TextSecondary,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 22.dp),
                    )
                    else -> LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items = state.articles, key = { it.url }) { article ->
                            NewsArticleCard(
                                article = article,
                                onClick = { onEvent(NewsEvent.ArticleClicked(article.url)) },
                                onShareClick = { onEvent(NewsEvent.ShareClicked(article.url, article.title)) },
                            )
                        }
                    }
                }
            }
        }
    }
}
