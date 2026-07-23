package iti.grad.nutriscan.presentation.home.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/**
 * Top greeting header showing the user avatar, name, subtitle, and notification bell.
 */
@Composable
fun HomeGreetingHeader(
    userName: String,
    avatarUrl: String?,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.Primary),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(52.dp).clip(CircleShape),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_person_solid),
                    contentDescription = null,
                    tint = AppTheme.colors.OnPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Greeting text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_greeting, userName),
                style = AppTheme.typography.headlineMedium,
                color = AppTheme.colors.GreetingTitleColor,
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.HealthSubtitleColor,
            )
        }

        // Notification bell
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onNotificationClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_notification),
                contentDescription = stringResource(R.string.nav_profile),
                tint = AppTheme.colors.PrimaryVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
