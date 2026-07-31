package iti.grad.nutriscan.presentation.news.home.view.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.news.state.NewsUiArticle
import iti.grad.presentation.R
import java.time.Duration
import java.time.Instant

private val cardShape = RoundedCornerShape(16.dp)

/**
 * Large breaking news card with full-bleed image, gradient overlay at the bottom,
 * "News" category badge, source name + verified + time, and bold headline text.
 */
@Composable
fun BreakingNewsCard(
    article: NewsUiArticle,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(cardShape),
    ) {
        // Full-bleed background image
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Gradient overlay (bottom half → transparent to dark)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                        startY = 100f,
                    ),
                ),
        )

        // Category badge (top-left)
        Box(
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
                .background(
                    color = AppTheme.colors.NewsChipSelectedBg,
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = article.category,
                style = AppTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = AppTheme.colors.NewsChipSelectedText,
            )
        }

        // Bottom content: Source row + headline
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            // Source row: name + verified badge + time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Source avatar circle
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(AppTheme.colors.NewsSourceAvatarBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_person_solid),
                        contentDescription = null,
                        tint = AppTheme.colors.NewsCategoryLabel,
                        modifier = Modifier.size(10.dp),
                    )
                }

                Text(
                    text = article.sourceName,
                    style = AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Icon(
                    painter = painterResource(id = R.drawable.ic_verified),
                    contentDescription = stringResource(id = R.string.news_source_avatar_content_description),
                    tint = AppTheme.colors.NewsCategoryLabel,
                    modifier = Modifier.size(12.dp),
                )

                Text(
                    text = stringResource(id = R.string.news_source_time_separator),
                    style = AppTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )

                Text(
                    text = formatBreakingPublishedAt(article.publishedAtLabel),
                    style = AppTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Headline text
            Text(
                text = article.title,
                style = AppTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun formatBreakingPublishedAt(iso: String): String {
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
