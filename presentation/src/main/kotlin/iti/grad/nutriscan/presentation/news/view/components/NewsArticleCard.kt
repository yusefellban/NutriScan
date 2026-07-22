package iti.grad.nutriscan.presentation.news.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import iti.grad.nutriscan.presentation.common.components.customShadow
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.news.state.NewsUiArticle
import iti.grad.presentation.R
import java.time.Duration
import java.time.Instant

/**
 * Fixed-size image + flexible text column: the text side is never height-capped, so it
 * grows with the card instead of clipping when the user bumps up system font scale
 * (image stays pinned to its own fixed size at the top).
 */
@Composable
fun NewsArticleCard(
    article: NewsUiArticle,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(16.dp)
    val isDark = AppTheme.isDark
    val titleColor = if (isDark) AppTheme.colors.Teal300 else AppTheme.colors.Gray1600
    val bylineColor = if (isDark) AppTheme.colors.Teal1200 else AppTheme.colors.Gray600
    val metaColor = if (isDark) AppTheme.colors.Teal1200 else AppTheme.colors.Gray600
    val dotsColor = if (isDark) AppTheme.colors.Teal400 else AppTheme.colors.Gray1600

    Row(
        modifier = modifier
            .fillMaxWidth()
            .customShadow(shape = cardShape, color = AppTheme.colors.Teal1000.copy(alpha = 0.2f), blurRadius = 30f, offsetY = 15f)
            .clip(cardShape)
            .background(AppTheme.colors.Surface)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        AsyncImage(
            model = article.imageUrl,
            contentDescription = article.title,
            modifier = Modifier
                .size(width = 137.dp, height = 140.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
            error = painterResource(id = R.drawable.ic_scanner),
            placeholder = painterResource(id = R.drawable.ic_scanner),
        )
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .fillMaxWidth()
                .heightIn(min = 140.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = article.title,
                    style = AppTheme.typography.bodySmall,
                    color = titleColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = article.sourceName,
                    style = AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.Teal600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val author = article.author?.takeIf { it.isNotBlank() }
                if (author != null) {
                    Text(
                        text = stringResource(R.string.news_card_byline, author),
                        style = AppTheme.typography.bodySmall,
                        color = bylineColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatPublishedAt(article.publishedAtLabel),
                    style = AppTheme.typography.bodySmall,
                    color = metaColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.MoreHoriz,
                    contentDescription = stringResource(R.string.news_card_menu_content_description),
                    tint = dotsColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onShareClick,
                        )
                        .padding(4.dp),
                )
            }
        }
    }
}

@Composable
private fun formatPublishedAt(iso: String): String {
    if (iso.isBlank()) return ""
    val minutesAgo = runCatching {
        Duration.between(Instant.parse(iso), Instant.now()).toMinutes()
    }.getOrNull() ?: return ""

    return when {
        minutesAgo < 1 -> stringResource(R.string.news_time_just_now)
        minutesAgo < 60 -> stringResource(R.string.news_time_minutes_ago, minutesAgo.toInt())
        minutesAgo < 24 * 60 -> stringResource(R.string.news_time_hours_ago, (minutesAgo / 60).toInt())
        else -> stringResource(R.string.news_time_days_ago, (minutesAgo / (24 * 60)).toInt())
    }
}
