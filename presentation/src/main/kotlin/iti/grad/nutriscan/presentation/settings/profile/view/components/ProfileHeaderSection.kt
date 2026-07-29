package iti.grad.nutriscan.presentation.settings.profile.view.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.components.AvatarCircle
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans
import iti.grad.presentation.R

/**
 * Teal header for the Profile screen: avatar (tap opens Edit Profile), name,
 * and day-streak badge, over the theme-appropriate decorative edge shape.
 */
@Composable
fun ProfileHeaderSection(
    userName: String,
    avatarUrl: String?,
    avatarUpdatedAt: String?,
    streakDays: Int,
    onEditProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clipToBounds()
            .background(AppTheme.colors.ProfileHeaderBackground)
    ) {
        Image(
            painter = painterResource(R.drawable.profile_edge),
            contentDescription = null,
            colorFilter = ColorFilter.tint(AppTheme.colors.ProfileHeaderEdge),
            modifier = Modifier.align(Alignment.TopEnd),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 56.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarCircle(
                avatarUrl = avatarUrl,
                avatarUpdatedAt = avatarUpdatedAt,
                size = 56.dp,
                onClick = onEditProfileClick,
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    style = AppTheme.typography.titleLarge.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = AppTheme.colors.OnPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AppTheme.colors.ProfileStreakBadgeBackground)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.user_profile_day_streak, streakDays),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.Teal400,
                    )
                }
            }
        }
    }
}
