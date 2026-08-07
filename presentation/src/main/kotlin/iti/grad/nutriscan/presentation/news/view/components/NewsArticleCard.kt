package iti.grad.nutriscan.presentation.news.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import iti.grad.nutriscan.presentation.common.components.shimmerEffect
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.news.state.NewsUiArticle
import iti.grad.presentation.R
import java.time.Duration
import java.time.Instant

private val cardShape = RoundedCornerShape(16.dp)

@Composable
fun NewsArticleCard(
    article: NewsUiArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(width = 1.dp, color = AppTheme.colors.NewsCardBorder, shape = cardShape)
            .background(AppTheme.colors.NewsCardBg)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Thumbnail image — 96dp square, rounded
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppTheme.colors.NewsSourceAvatarBg),
            contentAlignment = Alignment.Center
        ) {
            if (!article.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = article.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_image_placeholder),
                    contentDescription = null,
                    tint = AppTheme.colors.NewsCategoryLabel,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Right side content
        Column(
            modifier = Modifier
                .weight(1f)
                .height(96.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Category + Title
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = article.category,
                    style = AppTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.NewsCategoryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = article.title,
                    style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.NewsCardTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Source row: person avatar circle + source name + dot + time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Teal circle with person icon
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(AppTheme.colors.NewsSourceAvatarBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_person_solid),
                        contentDescription = stringResource(id = R.string.news_source_avatar_content_description),
                        tint = AppTheme.colors.NewsCategoryLabel,
                        modifier = Modifier.size(10.dp),
                    )
                }

                Text(
                    text = article.sourceName,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.NewsSourceText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                Text(
                    text = stringResource(id = R.string.news_source_time_separator),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.NewsSourceText,
                )

                Text(
                    text = formatPublishedAt(article.publishedAtLabel),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.NewsSourceText,
                    maxLines = 1,
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

@Composable
fun NewsArticleShimmerCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(width = 1.dp, color = AppTheme.colors.NewsCardBorder, shape = cardShape)
            .background(AppTheme.colors.NewsCardBg)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .shimmerEffect()
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .height(96.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}
