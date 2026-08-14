package iti.grad.nutriscan.presentation.common.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.CaloriesTypography
import iti.grad.presentation.R
import kotlinx.coroutines.delay

/**
 * "Water"/count header above a shadowed card of glass icons. The trailing "+"
 * adds a new empty cup; tapping a cup fills/unfills it in order — only the
 * next empty cup or the last filled cup responds, enforced by the ViewModel.
 * Long-pressing a cup deletes it (only the last cup responds, same ordering rule).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WaterTrackerCard(
    waterConsumed: Int,
    waterGoal: Int,
    onAddWater: () -> Unit,
    onCupClicked: (Int) -> Unit,
    onCupLongPressed: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filledGlassTint = AppTheme.colors.Teal700
    val emptyGlassTint = AppTheme.colors.WaterEmptyGlassTint
    val addButtonBackground = AppTheme.colors.WaterAddButtonBackground
    val addButtonIconTint = AppTheme.colors.WaterAddButtonIconTint

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.water),
                style = CaloriesTypography.sectionTitle,
                color = AppTheme.colors.SectionSubtitle,
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = waterConsumed.toString(),
                        style = AppTheme.typography.bodyLarge,
                        color = AppTheme.colors.Teal1000,
                    )
                    Text(
                        text = " / ",
                        style = AppTheme.typography.bodyLarge,
                        color = AppTheme.colors.Teal1000,
                    )
                    Text(
                        text = waterGoal.toString(),
                        style = AppTheme.typography.bodyLarge,
                        color = AppTheme.colors.Teal1000,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.calorieCardSurface(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .horizontalScroll(rememberScrollState())
                    .padding(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(waterGoal) { index ->
                    WaterCup(
                        index = index,
                        isFilled = index < waterConsumed,
                        filledTint = filledGlassTint,
                        emptyTint = emptyGlassTint,
                        onClick = { onCupClicked(index) },
                        onLongClick = { onCupLongPressed(index) },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(addButtonBackground)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAddWater,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_plus),
                    contentDescription = stringResource(R.string.water),
                    tint = addButtonIconTint,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

/** A single cup icon, animating its fill level from the bottom up (or draining top-down on
 * unfill). Cups newly filled by a "jump to cup N" tap stagger their start by [index] so the
 * whole run fills cup-by-cup instead of all at once — already-filled cups don't replay this
 * since [LaunchedEffect] only re-fires when [isFilled] itself flips. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WaterCup(
    index: Int,
    isFilled: Boolean,
    filledTint: Color,
    emptyTint: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val fillFraction = remember { Animatable(if (isFilled) 1f else 0f) }
    LaunchedEffect(isFilled) {
        if (isFilled) delay(index * 60L)
        fillFraction.animateTo(if (isFilled) 1f else 0f, animationSpec = tween(300))
    }

    Box(
        modifier = Modifier
            .width(20.dp)
            .height(39.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Icon(
            painter = painterResource(R.drawable.cup_empty),
            contentDescription = null,
            tint = emptyTint,
            modifier = Modifier.fillMaxWidth(),
        )
        Icon(
            painter = painterResource(R.drawable.cup_filled),
            contentDescription = null,
            tint = filledTint,
            modifier = Modifier
                .fillMaxWidth()
                .drawWithContent {
                    val visibleHeight = size.height * fillFraction.value
                    clipRect(
                        left = 0f,
                        top = size.height - visibleHeight,
                        right = size.width,
                        bottom = size.height,
                    ) {
                        this@drawWithContent.drawContent()
                    }
                },
        )
    }
}
