package iti.grad.nutriscan.presentation.settings.profile.view.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.profile.state.FamilyMemberUiModel
import iti.grad.presentation.R

/**
 * A single card in the horizontally-scrollable Family Members row: avatar
 * on the left with the name (wraps up to 2 lines) to its right, and a
 * full-width "Show Details" button below. Tap → show details, long-press →
 * request removal.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FamilyMemberCard(
    member: FamilyMemberUiModel,
    onShowDetailsClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(112.dp)
            .height(96.dp)
            .border(
                width = 1.5.dp,
                color = AppTheme.colors.ProfileMemberCardBorder,
                shape = RoundedCornerShape(16.dp),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.ProfileMemberCardBackground)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onShowDetailsClick,
                onLongClick = onLongPress,
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (member.avatarUrl != null) {
                AsyncImage(
                    model = member.avatarUrl,
                    contentDescription = stringResource(
                        R.string.user_profile_member_avatar_description,
                        member.name,
                    ),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(32.dp).clip(CircleShape),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_person_solid),
                    contentDescription = stringResource(
                        R.string.user_profile_member_avatar_description,
                        member.name,
                    ),
                    tint = AppTheme.colors.Teal1000,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.Teal1600)
                        .padding(6.dp),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = member.name,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.Teal1000,
                textAlign = TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AppTheme.colors.ProfileShowDetailsBackground)
                .padding(vertical = 6.dp),
        ) {
            Text(
                text = stringResource(R.string.user_profile_show_details),
                style = AppTheme.typography.labelSmall,
                color = AppTheme.colors.Teal100,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
