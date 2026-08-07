package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.model.SocialMediaProvider
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import androidx.compose.material3.MaterialTheme
import iti.grad.presentation.R

@Composable
fun SocialLoginRow(
    onSocialClick: (SocialMediaProvider) -> Unit
) {
    // Border color and tint from semantic AppColors
    val borderColor = AppTheme.colors.SocialButtonBorder
    val iconTint    = AppTheme.colors.SocialButtonIconTint

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SocialIconButton(
            iconRes = R.drawable.ic_facebook,
            contentDescription = "Facebook Login",
            borderColor = borderColor,
            iconTint = iconTint,
            onClick = { onSocialClick(SocialMediaProvider.FACEBOOK) }
        )
        SocialIconButton(
            iconRes = R.drawable.ic_google,
            contentDescription = "Google Login",
            borderColor = borderColor,
            iconTint = iconTint,
            onClick = { onSocialClick(SocialMediaProvider.GOOGLE) }
        )
        SocialIconButton(
            iconRes = R.drawable.ic_instagram,
            contentDescription = "Instagram Login",
            borderColor = borderColor,
            iconTint = iconTint,
            onClick = { onSocialClick(SocialMediaProvider.INSTAGRAM) }
        )
    }
}

@Composable
private fun SocialIconButton(
    iconRes: Int,
    contentDescription: String,
    borderColor: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}
