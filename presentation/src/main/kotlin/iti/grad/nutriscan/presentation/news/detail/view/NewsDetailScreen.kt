package iti.grad.nutriscan.presentation.news.detail.view

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun NewsDetailScreen(
    article: NewsUiArticle,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.Background)
    ) {
        // Main Scrollable Content Container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Big Header/Hero Image (top aspect ratio box - increased height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            ) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = article.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.ic_image_placeholder),
                    placeholder = painterResource(id = R.drawable.ic_image_placeholder),
                )

                // Gradient overlay at the bottom of the image for white text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                                startY = 160f
                            )
                        )
                )

                // Category tag, Author, Bold Title, and Source Details inside the gradient overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
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

                        if (!article.author.isNullOrBlank()) {
                            Text(
                                text = article.author,
                                style = AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = article.title,
                        style = AppTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Location/Source + Time Metadata row matching the mockup at the bottom of the image overlay
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = article.sourceName,
                            style = AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_verified),
                            contentDescription = null,
                            tint = AppTheme.colors.NewsCategoryLabel,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "•",
                            style = AppTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Text(
                            text = formatPublishedAt(article.publishedAtLabel),
                            style = AppTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Floating Sheet Card body content directly following the image
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(AppTheme.colors.NewsCardBg)
                    .padding(horizontal = 22.dp, vertical = 24.dp)
            ) {
                // Header (Source Avatar + Name + Time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(AppTheme.colors.NewsSourceAvatarBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = article.sourceName.firstOrNull()?.uppercase().orEmpty(),
                            style = AppTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.NewsCategoryLabel
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = article.sourceName,
                                style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = AppTheme.colors.NewsCardTitle
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_verified),
                                contentDescription = null,
                                tint = AppTheme.colors.NewsCategoryLabel,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (!article.author.isNullOrBlank()) {
                            Text(
                                text = stringResource(R.string.news_card_byline, article.author),
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colors.NewsSourceText
                            )
                        }
                    }

                    Text(
                        text = formatPublishedAt(article.publishedAtLabel),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.NewsSourceText
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = AppTheme.colors.NewsDivider, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Bold lead paragraph sub-headline
                Text(
                    text = article.title,
                    style = AppTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.NewsCardTitle
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Detailed body paragraph
                article.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Text(
                        text = desc,
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.NewsSourceText
                    )
                }

                // CTA Read Full Button placed directly under text content in the scroll flow
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(article.url))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.NewsSourceAvatarBg,
                        contentColor = AppTheme.colors.NewsCategoryLabel
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.news_read_full_article),
                            style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Close and Share Buttons Overlay (layered on top-left/top-right of image, stays fixed on screen)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close "X" Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .clickable(onClick = onNavigateBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = stringResource(id = R.string.onboarding_back),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Share Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .clickable {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, article.title)
                            putExtra(Intent.EXTRA_TEXT, article.url)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(id = R.string.news_card_menu_content_description),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
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
