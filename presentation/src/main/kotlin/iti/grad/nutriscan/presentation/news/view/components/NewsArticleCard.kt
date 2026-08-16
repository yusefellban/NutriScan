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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
private val cardHeight = 156.dp
private val cardContentInset = 8.dp
private val articleImageSize = cardHeight - (cardContentInset * 2) // 140.dp

@Composable
fun NewsArticleCard(
    article: NewsUiArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onShareClick: () -> Unit = {},
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(cardShape)
            .border(width = 1.dp, color = AppTheme.colors.NewsCardBorder, shape = cardShape)
            .background(AppTheme.colors.NewsCardBg)
            .clickable(onClick = onClick)
            .padding(cardContentInset),
        horizontalArrangement = Arrangement.spacedBy(cardContentInset),
    ) {
        // Thumbnail image — derived from card height so left/bottom gaps stay equal (140dp)
        Box(
            modifier = Modifier
                .size(articleImageSize)
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
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Right side content
        Column(
            modifier = Modifier
                .weight(1f)
                .height(articleImageSize),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Title + optional author
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = article.title,
                    style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.NewsCardTitle,
                    maxLines = if (article.author.isNullOrBlank()) 5 else 4,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!article.author.isNullOrBlank()) {
                    Text(
                        text = stringResource(id = R.string.news_author_prefix, article.author),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.NewsSourceText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Bottom row: feed label + dot + time, and a trailing three-dot menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(
                        text = article.category,
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.NewsCategoryLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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

                Spacer(modifier = Modifier.weight(1f))

                Box {
                    IconButton(
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = stringResource(id = R.string.news_article_menu_content_description),
                            tint = AppTheme.colors.NewsSourceText,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.news_menu_open_article)) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null)
                            },
                            onClick = {
                                isMenuExpanded = false
                                onClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.news_menu_share)) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                            },
                            onClick = {
                                isMenuExpanded = false
                                onShareClick()
                            },
                        )
                    }
                }
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
            .height(cardHeight)
            .clip(cardShape)
            .border(width = 1.dp, color = AppTheme.colors.NewsCardBorder, shape = cardShape)
            .background(AppTheme.colors.NewsCardBg)
            .padding(cardContentInset),
        horizontalArrangement = Arrangement.spacedBy(cardContentInset),
    ) {
        Box(
            modifier = Modifier
                .size(articleImageSize)
                .clip(RoundedCornerShape(12.dp))
                .shimmerEffect()
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .height(articleImageSize),
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
