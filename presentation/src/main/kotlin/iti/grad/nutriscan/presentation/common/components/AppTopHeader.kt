package iti.grad.nutriscan.presentation.common.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme

@Composable
fun AppTopHeader(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes actionIconResId: Int? = null,
    actionIconContentDescription: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 71.dp, bottom = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppBackButton(
            onClick = onBackClick,
            iconTint = Color.White,
            borderColor = Color.White
        )

        Text(
            text = title,
            style = AppTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            ),
            color = Color.White,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        
        if (actionIconResId != null && onActionClick != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppTheme.colors.Teal700)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onActionClick,
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = actionIconResId),
                    contentDescription = actionIconContentDescription,
                    tint = AppTheme.colors.Teal1600,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            // Placeholder to keep text centered when there is no action button
            Box(modifier = Modifier.size(48.dp))
        }
    }
}
