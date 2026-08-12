package iti.grad.nutriscan.presentation.news.home.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.components.shimmerEffect
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.news.state.NewsUiArticle
import kotlinx.collections.immutable.ImmutableList

/**
 * Horizontal pager showing breaking news cards with a peek of the next card
 * and a row of dot page indicators below.
 */
@Composable
fun BreakingNewsPager(
    articles: ImmutableList<NewsUiArticle>,
    onArticleClicked: (NewsUiArticle) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (articles.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { articles.size })

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(end = 40.dp),
            pageSpacing = 12.dp,
        ) { page ->
            BreakingNewsCard(
                article = articles[page],
                modifier = Modifier.clickable { onArticleClicked(articles[page]) },
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dot page indicators
        NewsPageIndicator(
            pageCount = articles.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/**
 * Shimmer loading placeholder for the breaking news pager.
 */
@Composable
fun BreakingNewsPagerShimmer(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .shimmerEffect(),
            )
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .shimmerEffect(),
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .shimmerEffect(),
                )
                if (it < 4) Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

@Composable
fun NewsPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            val shape = if (isActive) RoundedCornerShape(3.dp) else CircleShape
            val width = if (isActive) 18.dp else 6.dp
            val height = 6.dp
            Box(
                modifier = Modifier
                    .size(width = width, height = height)
                    .clip(shape)
                    .background(
                        if (isActive) AppTheme.colors.NewsCategoryLabel
                        else AppTheme.colors.NewsSearchBarBorder,
                    ),
            )
        }
    }
}

