package iti.grad.nutriscan.presentation.settings.profile.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans
import iti.grad.nutriscan.presentation.settings.profile.state.FamilyMemberUiModel
import iti.grad.presentation.R
import kotlinx.collections.immutable.ImmutableList

/**
 * "Family Members" section: title, then a single dashed box holding a
 * horizontally-scrollable, left-aligned row of [FamilyMemberCard]s plus a
 * trailing [AddMemberCard] — when empty, the add card is simply the only
 * (left-aligned, not centered) item.
 */
@Composable
fun FamilyMembersSection(
    familyMembers: ImmutableList<FamilyMemberUiModel>,
    onAddMemberClick: () -> Unit,
    onMemberDetailClick: (String) -> Unit,
    onMemberLongPress: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.user_profile_family_members),
            style = AppTheme.typography.titleLarge.copy(fontFamily = PlusJakartaSans),
            color = AppTheme.colors.SectionSubtitle,
            modifier = Modifier.padding(end = 20.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))

        val dashedBorderColor = AppTheme.colors.Teal500
        // The dashed box itself overruns the right edge (clipped by
        // the sheet) so its dashes read as continuing off-screen there.
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .requiredWidth((LocalConfiguration.current.screenWidthDp).dp)
                .height(128.dp)
                .background(AppTheme.colors.ProfileFamilyBoxBackground, RoundedCornerShape(22.dp))
                .drawBehind {
                    drawRoundRect(
                        color = dashedBorderColor,
                        cornerRadius = CornerRadius(22.dp.toPx()),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                intervals = floatArrayOf(7.5f.dp.toPx(), 5.dp.toPx()),
                                phase = 0f,
                            ),
                        ),
                    )
                }
                .clip(RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(start = 12.dp, end = 4.dp),
            ) {
                items(items = familyMembers, key = { it.id }) { member ->
                    FamilyMemberCard(
                        member = member,
                        onShowDetailsClick = { onMemberDetailClick(member.id) },
                        onLongPress = { onMemberLongPress(member.id) },
                    )
                }
                item(key = "add_member_card") {
                    AddMemberCard(onClick = onAddMemberClick)
                }
            }
        }
    }
}
