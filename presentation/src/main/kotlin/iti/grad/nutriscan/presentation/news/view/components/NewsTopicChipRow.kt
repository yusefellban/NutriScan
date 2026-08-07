package iti.grad.nutriscan.presentation.news.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.domain.news.model.NewsTopicChip
import iti.grad.nutriscan.presentation.common.components.shimmerEffect
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

@Composable
fun NewsTopicChipRow(
    chips: ImmutableList<NewsTopicChip>,
    selectedChipIds: ImmutableSet<String>,
    onChipClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (chip in chips) {
            val isSelected = chip.id in selectedChipIds
            val label = if (chip.id == NewsTopicChip.ALL_CHIP_ID) {
                stringResource(R.string.news_chip_all)
            } else {
                chip.label
            }
            Text(
                text = label,
                style = AppTheme.typography.bodyMedium,
                color = if (isSelected) AppTheme.colors.NewsChipSelectedText else AppTheme.colors.NewsChipUnselectedText,
                modifier = Modifier
                    .clip(chipShape)
                    .background(if (isSelected) AppTheme.colors.NewsChipSelectedBg else AppTheme.colors.NewsChipUnselectedBg)
                    .then(
                        if (!isSelected) {
                            Modifier.border(
                                width = 1.dp,
                                color = AppTheme.colors.NewsChipUnselectedBorder,
                                shape = chipShape,
                            )
                        } else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onChipClicked(chip.id) },
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

private val chipShape = RoundedCornerShape(32.dp)

@Composable
fun NewsTopicChipShimmerRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val widths = listOf(60.dp, 80.dp, 100.dp, 70.dp, 90.dp)
        for (width in widths) {
            Box(
                modifier = Modifier
                    .width(width)
                    .height(36.dp)
                    .clip(chipShape)
                    .shimmerEffect()
            )
        }
    }
}
