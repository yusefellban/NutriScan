package iti.grad.nutriscan.presentation.product_details.view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.components.AppBackButton
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

@Composable
fun ProductDetailsTopBar(
    isBookmarked: Boolean,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 71.dp, bottom = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppBackButton(
            onClick = onBackClick,
            iconTint = Color.White,
            borderColor = Color.White
        )

        Text(
            text = stringResource(R.string.product_details_title),
            style = AppTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            ),
            color = Color.White,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AppTheme.colors.Accent) // A lighter teal for contrast
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBookmarkClick,
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    id = if (isBookmarked) R.drawable.ic_bookmark_solid
                    else R.drawable.ic_bookmark_outline
                ),
                contentDescription = stringResource(
                    if (isBookmarked) R.string.product_details_bookmark_remove
                    else R.string.product_details_bookmark_add
                ),
                tint = AppTheme.colors.PrimaryVariant, // Darker teal tint for icon
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
