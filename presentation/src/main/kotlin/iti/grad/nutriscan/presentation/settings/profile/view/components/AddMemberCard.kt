package iti.grad.nutriscan.presentation.settings.profile.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

/**
 * Small square "add a member" card. Used as the sole (left-aligned) item in
 * the Family Members row when empty, and as the trailing item once
 * populated — same component either way.
 */
@Composable
fun AddMemberCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outerBorderRadius = 12.dp
    val innerBorderRadius = 7.dp

    Column(
        modifier = modifier
            .width(90.dp)
            .height(95.dp)
            // No border mentioned in the new specs, so removing it
            .clip(RoundedCornerShape(outerBorderRadius))
            .background(AppTheme.colors.ScreenSurfaceBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = null,
            tint = AppTheme.colors.Teal1000,
            modifier = Modifier

                .size(32.dp) // Reduced size
                .clip(RoundedCornerShape(innerBorderRadius))
                .background(AppTheme.colors.ProfileAddIconBackground)
                .padding(6.dp), // Increased internal padding slightly to make the plus itself smaller relative to its background
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.user_profile_add_member),
            style = AppTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.ProfileAddText,
            textAlign = TextAlign.Center,
        )
    }
}


