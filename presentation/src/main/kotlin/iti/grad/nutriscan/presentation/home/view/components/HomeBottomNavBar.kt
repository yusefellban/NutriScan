package iti.grad.nutriscan.presentation.home.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.home.state.BottomNavTab
import iti.grad.presentation.R

/**
 * Custom bottom navigation bar with a floating center Scan button.
 *
 * All icons come from drawable resources. All colors from [AppTheme.colors].
 */
@Composable
fun HomeBottomNavBar(
    selectedTab: BottomNavTab,
    onTabClick: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val curveShape = BottomNavCurveShape(cornerRadius = 60f, cutoutRadius = 140f)
    val fabShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Background bar with custom top shadow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .customShadow(
                    shape = curveShape,
                    color = AppTheme.colors.Primary.copy(alpha = 0.5f),
                    blurRadius = 70f,
                    offsetY = -10f
                )
                .clip(curveShape)
                .background(AppTheme.colors.BottomNavBarBackground)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavBarItem(
                    iconResId = R.drawable.ic_home,
                    contentDescription = stringResource(R.string.nav_home),
                    isSelected = selectedTab == BottomNavTab.HOME,
                    onClick = { onTabClick(BottomNavTab.HOME) },
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavBarItem(
                    iconResId = R.drawable.ic_history,
                    contentDescription = stringResource(R.string.nav_history),
                    isSelected = selectedTab == BottomNavTab.HISTORY,
                    onClick = { onTabClick(BottomNavTab.HISTORY) },
                )
            }

            // Spacer for center FAB
            Box(modifier = Modifier.weight(1.5f))

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavBarItem(
                    iconResId = R.drawable.ic_bookmark,
                    contentDescription = stringResource(R.string.nav_shopping),
                    isSelected = selectedTab == BottomNavTab.SHOPPING,
                    onClick = { onTabClick(BottomNavTab.SHOPPING) },
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavBarItem(
                    iconResId = R.drawable.ic_person,
                    contentDescription = stringResource(R.string.nav_profile),
                    isSelected = selectedTab == BottomNavTab.PROFILE,
                    onClick = { onTabClick(BottomNavTab.PROFILE) },
                )
            }
        }

        // Floating center Scan FAB with custom top shadow
        Box(
            modifier = Modifier
                .offset(y = (-44).dp)
                .size(64.dp)
                .clip(fabShape)
                .background(AppTheme.colors.PrimaryVariant)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onTabClick(BottomNavTab.SCAN) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_scanner),
                contentDescription = stringResource(R.string.nav_scan),
                tint = AppTheme.colors.OnPrimary,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun NavBarItem(
    iconResId: Int,
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
            painter = painterResource(iconResId),
            contentDescription = contentDescription,
            tint = if (isSelected) {
                AppTheme.colors.OnPrimary
            } else {
                AppTheme.colors.OnPrimary.copy(alpha = 0.5f)
            },
            modifier = Modifier.size(30.dp),
        )
    }
}

