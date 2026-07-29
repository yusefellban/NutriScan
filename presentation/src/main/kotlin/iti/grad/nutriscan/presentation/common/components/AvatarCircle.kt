package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

/**
 * Shared user-avatar circle: teal ring border, cached avatar image (or a
 * fallback person icon), used by Home, Profile header, and Edit Profile so
 * the avatar reads as the same element across the app.
 */
@Composable
fun AvatarCircle(
    avatarUrl: String?,
    avatarUpdatedAt: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 1.dp,
    iconSize: Dp = size / 3,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .border(width = ringWidth, color = AppTheme.colors.OnPrimary, shape = CircleShape)
            .padding(ringWidth + 2.dp)
            .clip(CircleShape)
            .background(AppTheme.colors.ProfileHeaderAccent)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = rememberAvatarImageRequest(avatarUrl, avatarUpdatedAt),
                contentDescription = stringResource(R.string.user_profile_avatar_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_person_solid),
                contentDescription = stringResource(R.string.user_profile_avatar_description),
                tint = AppTheme.colors.OnPrimary,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
