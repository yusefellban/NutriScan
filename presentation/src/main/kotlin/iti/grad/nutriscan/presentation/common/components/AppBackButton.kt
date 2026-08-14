package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

/** Where an [AppBackButton] sits, so its color reads correctly in both themes. */
enum class BackButtonSurface {
    /** On a teal/cyan accent panel (e.g. App Settings header). */
    OnAccent,

    /** On a white/plain surface. */
    OnLight,
}

@Composable
fun AppBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    surface: BackButtonSurface = BackButtonSurface.OnLight,
) {
    // OnAccent's teal panel (#13A4AB / Teal1000) is a fixed brand color in both themes — the
    // icon must stay white regardless of the app's light/dark setting, or it blends into the
    // panel whenever isDark flips it to the same teal. OnLight's teal icon already contrasts
    // fine against both a white background and a dark (#0F474A) one.
    val color = when (surface) {
        BackButtonSurface.OnAccent -> Color.White
        BackButtonSurface.OnLight -> AppTheme.colors.Teal1000
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .border(width = 1.dp, color = color, shape = RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_back),
            contentDescription = stringResource(R.string.onboarding_back),
            tint = color,
            modifier = Modifier.size(48.dp)
        )
    }
}
