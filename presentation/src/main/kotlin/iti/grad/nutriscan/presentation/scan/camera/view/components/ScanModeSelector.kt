package iti.grad.nutriscan.presentation.scan.camera.view.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.scan.camera.state.ScanInputMode
import iti.grad.presentation.R

@Composable
fun ScanModeSelector(
    selectedMode: ScanInputMode,
    hasSelectedGalleryImage: Boolean,
    onModeSelected: (ScanInputMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectorContentDescription = stringResource(R.string.scan_mode_selector_content_desc)

    // Instagram-style: just a horizontal row of icon circles, no container background
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .semantics { contentDescription = selectorContentDescription }
            .padding(horizontal = 8.dp),
    ) {
        ScanInputMode.entries.forEach { mode ->
            ModeIconButton(
                mode = mode,
                isSelected = mode == selectedMode,
                onClick = { onModeSelected(mode) },
            )
        }
    }
}

@Composable
private fun ModeIconButton(
    mode: ScanInputMode,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    // Selected = white filled circle (like Instagram active mode)
    // Unselected = semi-transparent dark circle
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium,
        ),
        label = "iconScale",
    )
    val alpha by animateFloatAsState(
        targetValue  = if (isSelected) 1f else 0.55f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label        = "iconAlpha",
    )

    val bgColor   = if (isSelected) Color.White                      else Color.Black.copy(alpha = 0.35f)
    val iconTint  = if (isSelected) Color.Black                      else Color.White

    Icon(
        imageVector       = mode.icon(),
        contentDescription = null,
        tint              = iconTint,
        modifier          = Modifier
            .scale(scale)
            .alpha(alpha)
            .size(46.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            )
            .padding(11.dp),
    )
}

private fun ScanInputMode.icon(): ImageVector = when (this) {
    ScanInputMode.PHOTO   -> Icons.Default.PhotoCamera
    ScanInputMode.GALLERY -> Icons.Default.Image
}

