package iti.grad.nutriscan.presentation.home.view.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.components.AvatarCircle
import iti.grad.nutriscan.presentation.common.components.HeroHeaderSubtitle
import iti.grad.nutriscan.presentation.common.components.HeroHeaderTitle
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

/**
 * Teal greeting header for Home — mirrors [iti.grad.nutriscan.presentation.settings.profile.view.components.ProfileHeaderSection]'s
 * shape, edge decoration, and avatar so both top-level screens read as one visual family.
 */
@Composable
fun HomeGreetingHeader(
    firstName: String,
    avatarUrl: String?,
    avatarUpdatedAt: String?,
    onNotificationClick: () -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clipToBounds()
            .background(AppTheme.colors.ProfileHeaderBackground),
    ) {
        // Purely decorative curve — pin to the physical top-right corner regardless of
        // locale, same as SectionHeroHeader.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Image(
                painter = painterResource(R.drawable.profile_edge),
                contentDescription = null,
                colorFilter = ColorFilter.tint(AppTheme.colors.ProfileHeaderEdge),
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }

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
                onClick = onAvatarClick,
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                HeroHeaderTitle(text = stringResource(R.string.home_greeting, firstName))
                Spacer(modifier = Modifier.height(4.dp))
                HeroHeaderSubtitle(text = stringResource(R.string.home_subtitle))
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
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
                    tint = AppTheme.colors.Teal300,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
