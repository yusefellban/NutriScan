package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One shimmering placeholder block. Screen skeletons are built out of these so each one mirrors the
 * real layout it stands in for — a single generic list skeleton looked wrong everywhere except the
 * one screen it was shaped after.
 */
@Composable
fun ShimmerBlock(
    height: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .shimmerEffect(),
    )
}

/**
 * Scroll container shared by the screen skeletons. Stays scrollable so the pull-to-refresh gesture
 * still works while the placeholder is up.
 */
@Composable
private fun ShimmerScaffold(
    modifier: Modifier,
    contentPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        content()
    }
}

/** Daily tip, scan card, two explore rows, then the recent-history list. */
@Composable
fun HomeScreenShimmer(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 16.dp),
) {
    ShimmerScaffold(modifier, contentPadding) {
        ShimmerBlock(height = 104.dp, modifier = Modifier.fillMaxWidth())
        ShimmerBlock(height = 132.dp, modifier = Modifier.fillMaxWidth())
        ShimmerBlock(height = 20.dp, modifier = Modifier.width(120.dp), shape = RoundedCornerShape(6.dp))
        repeat(2) {
            ShimmerBlock(height = 56.dp, modifier = Modifier.fillMaxWidth())
        }
        ShimmerBlock(height = 20.dp, modifier = Modifier.width(150.dp), shape = RoundedCornerShape(6.dp))
        repeat(3) {
            ShimmerBlock(height = 84.dp, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Calorie total, the horizontal food-card strip, goals pager, steps/exercise pair, water card. */
@Composable
fun CaloriesScreenShimmer(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 20.dp),
) {
    ShimmerScaffold(modifier, contentPadding) {
        ShimmerBlock(height = 26.dp, modifier = Modifier.width(170.dp), shape = RoundedCornerShape(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                ShimmerBlock(height = 172.dp, modifier = Modifier.width(140.dp))
            }
        }
        ShimmerBlock(height = 180.dp, modifier = Modifier.fillMaxWidth())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShimmerBlock(height = 140.dp, modifier = Modifier.weight(1f))
            ShimmerBlock(height = 140.dp, modifier = Modifier.weight(1.2f))
        }
        ShimmerBlock(height = 150.dp, modifier = Modifier.fillMaxWidth())
    }
}

/** Family-member avatars, then the menu rows. */
@Composable
fun ProfileScreenShimmer(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 24.dp),
) {
    ShimmerScaffold(modifier, contentPadding) {
        ShimmerBlock(height = 20.dp, modifier = Modifier.width(140.dp), shape = RoundedCornerShape(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) {
                ShimmerBlock(height = 64.dp, modifier = Modifier.size(64.dp), shape = CircleShape)
            }
        }
        repeat(3) {
            ShimmerBlock(height = 60.dp, modifier = Modifier.fillMaxWidth())
        }
    }
}
