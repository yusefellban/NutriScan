package iti.grad.nutriscan.presentation.auth.forgot_password.view.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans
import iti.grad.nutriscan.presentation.common.util.directionalDrawable
import iti.grad.presentation.R

// ─────────────────────────────────────────────────────────────────────────────
// Selectable reset method card — matches Figma rounded-rect card with icon,
// title, subtitle, right chevron, and a teal border highlight when selected
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ResetMethodCard(
    @DrawableRes iconRes: Int,
    @StringRes titleResId: Int,
    @StringRes subtitleResId: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val cardBg = AppTheme.colors.Surface
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val iconTint = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        AppTheme.colors.MethodCardIconTintUnselected
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBg)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) borderColor else AppTheme.colors.Divider,
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Leading icon in tinted container ─────────────────────────────
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) AppTheme.colors.Teal400
                    else AppTheme.colors.MethodCardIconBgUnselected
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        // ── Title + Subtitle ─────────────────────────────────────────────
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(titleResId),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = AppTheme.colors.Teal1000
            )
            Text(
                text = stringResource(subtitleResId),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = AppTheme.colors.TextSecondary
            )
        }

        // ── Trailing chevron ─────────────────────────────────────────────
        Icon(
            painter = painterResource(directionalDrawable(R.drawable.ic_arrow_right, R.drawable.ic_arrow_left)),
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else AppTheme.colors.Gray600,
            modifier = Modifier.size(24.dp)
        )
    }
}

