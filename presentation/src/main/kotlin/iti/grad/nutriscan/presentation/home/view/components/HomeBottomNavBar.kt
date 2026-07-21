package iti.grad.nutriscan.presentation.home.view.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.home.state.BottomNavTab
import iti.grad.presentation.R
import kotlin.math.abs

/** Height of the visible tab-bar region — matches the iOS `CustomAnimatedTabBar.barHeight`. */
private val BarHeight = 60.dp

/** How far the floating button's center sits above the bar's top edge. */
private val FabOffsetY = (-28).dp

/** Small notch is hidden once it slides within this distance of the fixed center notch. */
private val NotchOverlapThreshold = 50.dp

/** Shared spring for both the notch and the floating button, mirroring the iOS `tabSpring`. */
private val TabBarSpring = spring<Float>(dampingRatio = 0.65f, stiffness = 110f)

/**
 * Custom bottom navigation bar with a floating center Scan button.
 *
 * Mirrors the iOS `CustomAnimatedTabBar` 1:1:
 * - A fixed center notch always sits under the floating Scan button.
 * - A small notch slides beneath whichever tab is selected, animated with a shared spring.
 * - Both notches are drawn by [BottomNavCurveShape], animating its `curveX`/`notchX` params.
 *
 * All icons come from drawable resources. All colors from [AppTheme.colors].
 */
@Composable
fun HomeBottomNavBar(
    selectedTab: BottomNavTab,
    onTabClick: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    // Tracks each tab's horizontal center, in px, relative to the Row's own left edge —
    // the same coordinate space the bar's curve Shape is drawn in.
    var barWidthPx by remember { mutableFloatStateOf(0f) }
    var rowLeftInRoot by remember { mutableFloatStateOf(0f) }
    val tabPositions = remember { mutableStateMapOf<BottomNavTab, Float>() }

    val notchX = barWidthPx / 2f
    val animatedCurveX = remember { Animatable(0f) }

    LaunchedEffect(selectedTab, tabPositions[selectedTab], notchX) {
        val target = tabPositions[selectedTab] ?: notchX
        if (target != 0f) {
            if (animatedCurveX.value == 0f) {
                animatedCurveX.snapTo(target)
            } else {
                animatedCurveX.animateTo(target, animationSpec = TabBarSpring)
            }
        }
    }

    val overlapThresholdPx = with(density) { NotchOverlapThreshold.toPx() }
    val hideSmallNotch = selectedTab == BottomNavTab.SCAN ||
        abs(animatedCurveX.value - notchX) < overlapThresholdPx

    val curveShape = BottomNavCurveShape(
        curveX = animatedCurveX.value,
        notchX = notchX,
        hideSmallNotch = hideSmallNotch,
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .onGloballyPositioned {
                    barWidthPx = it.size.width.toFloat()
                    rowLeftInRoot = it.positionInRoot().x
                }
                .customShadow(
                    shape = curveShape,
                    color = AppTheme.colors.Primary.copy(alpha = 0.5f),
                    blurRadius = 70f,
                    offsetY = -10f,
                )
                .clip(curveShape)
                .background(AppTheme.colors.BottomNavBarBackground)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabSlot(
                modifier = Modifier.weight(1f),
                selectedIconResId = R.drawable.ic_home_solid,
                unselectedIconResId = R.drawable.ic_home_outline,
                contentDescription = stringResource(R.string.nav_home),
                isSelected = selectedTab == BottomNavTab.HOME,
                onClick = { onTabClick(BottomNavTab.HOME) },
                onPositioned = { center -> tabPositions[BottomNavTab.HOME] = center - rowLeftInRoot },
            )
            TabSlot(
                modifier = Modifier.weight(1f),
                selectedIconResId = R.drawable.ic_fire_solid,
                unselectedIconResId = R.drawable.ic_fire_outline,
                contentDescription = stringResource(R.string.nav_history),
                isSelected = selectedTab == BottomNavTab.HISTORY,
                onClick = { onTabClick(BottomNavTab.HISTORY) },
                onPositioned = { center -> tabPositions[BottomNavTab.HISTORY] = center - rowLeftInRoot },
            )

            // Reserved center slot for the Scan tab — its icon renders inside the floating
            // button instead, matching the invisible `TabIconButton` iOS keeps for `.scan`.
            Box(modifier = Modifier.weight(1f))

            TabSlot(
                modifier = Modifier.weight(1f),
                selectedIconResId = R.drawable.ic_bookmark_solid,
                unselectedIconResId = R.drawable.ic_bookmark_outline,
                contentDescription = stringResource(R.string.nav_shopping),
                isSelected = selectedTab == BottomNavTab.SHOPPING,
                onClick = { onTabClick(BottomNavTab.SHOPPING) },
                onPositioned = { center -> tabPositions[BottomNavTab.SHOPPING] = center - rowLeftInRoot },
            )
            TabSlot(
                modifier = Modifier.weight(1f),
                selectedIconResId = R.drawable.ic_profile_solid,
                unselectedIconResId = R.drawable.ic_profile_outline,
                contentDescription = stringResource(R.string.nav_profile),
                isSelected = selectedTab == BottomNavTab.PROFILE,
                onClick = { onTabClick(BottomNavTab.PROFILE) },
                onPositioned = { center -> tabPositions[BottomNavTab.PROFILE] = center - rowLeftInRoot },
            )
        }

        FloatingScanButton(
            isSelected = selectedTab == BottomNavTab.SCAN,
            onClick = { onTabClick(BottomNavTab.SCAN) },
            modifier = Modifier.offset(y = FabOffsetY),
        )
    }
}

@Composable
private fun TabSlot(
    modifier: Modifier,
    selectedIconResId: Int,
    unselectedIconResId: Int,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPositioned: (centerXInRoot: Float) -> Unit,
) {
    Box(
        modifier = modifier.onGloballyPositioned { coords ->
            onPositioned(coords.positionInRoot().x + coords.size.width / 2f)
        },
        contentAlignment = Alignment.Center,
    ) {
        NavBarItem(
            selectedIconResId = selectedIconResId,
            unselectedIconResId = unselectedIconResId,
            contentDescription = contentDescription,
            isSelected = isSelected,
            onClick = onClick,
        )
    }
}

@Composable
private fun NavBarItem(
    selectedIconResId: Int,
    unselectedIconResId: Int,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (isSelected) selectedIconResId else unselectedIconResId),
            contentDescription = contentDescription,
            tint = if (isSelected) {
                AppTheme.colors.OnPrimary
            } else {
                AppTheme.colors.OnPrimary.copy(alpha = 0.6f)
            },
            modifier = Modifier.size(30.dp),
        )
    }
}
