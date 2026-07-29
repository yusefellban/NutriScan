package iti.grad.nutriscan.presentation.home.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.util.directionalDrawable
import iti.grad.presentation.R

/**
 * A single row in the Home screen's "Explore" section: a leading icon in a soft circle,
 * a label, and a trailing chevron. Reuses the same centralized row/icon/label/chevron
 * color tokens as [iti.grad.nutriscan.presentation.settings.profile.view.components.ProfileMenuRow]
 * (`ProfileMenuRowBackground` / `ProfileMenuIconBackground` / `ProfileMenuLabel` /
 * `ProfileMenuChevron`) rather than introducing new, duplicate theme colors for the same
 * light-gray-row-with-teal-icon-circle look.
 */
@Composable
fun ExploreItemRow(
    iconResId: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.Medium)
            .background(AppTheme.colors.ProfileMenuRowBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.ProfileMenuIconBackground.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                tint = AppTheme.colors.Teal1000,
                modifier = Modifier.size(18.dp),
            )
        }

        Text(
            text = label,
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.ProfileMenuLabel,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )

        Icon(
            painter = painterResource(directionalDrawable(R.drawable.ic_arrow_right, R.drawable.ic_arrow_left)),
            contentDescription = null,
            tint = AppTheme.colors.ProfileMenuChevron,
            modifier = Modifier.size(20.dp),
        )
    }
}
