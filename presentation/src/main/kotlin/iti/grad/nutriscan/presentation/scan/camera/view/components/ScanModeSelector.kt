package iti.grad.nutriscan.presentation.scan.camera.view.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.scan.camera.state.ScanInputMode
import iti.grad.presentation.R

@Composable
fun ScanModeSelector(
    selectedMode: ScanInputMode,
    hasSelectedGalleryImage: Boolean,
    onModeSelected: (ScanInputMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerShape = RoundedCornerShape(20.dp)
    val selectorContentDescription = stringResource(R.string.scan_mode_selector_content_desc)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(
                color = AppTheme.colors.BottomNavBarBackground.copy(alpha = 0.88f),
                shape = containerShape,
            )
            .padding(6.dp)
            .semantics {
                contentDescription = selectorContentDescription
            },
    ) {
        ScanInputMode.entries.forEach { mode ->
            ModeItem(
                mode = mode,
                labelResId = if (mode == ScanInputMode.GALLERY && hasSelectedGalleryImage) {
                    R.string.scan_mode_gallery_change
                } else {
                    mode.labelResId
                },
                isSelected = mode == selectedMode,
                onClick = { onModeSelected(mode) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ModeItem(
    mode: ScanInputMode,
    labelResId: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconScale by animateDpAsState(
        targetValue = if (isSelected) 20.dp else 18.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f),
        label = "modeIconScale",
    )
    val alpha = if (isSelected) 1f else 0.72f

    Box(
        modifier = modifier
            .height(56.dp)
            .background(
                color = if (isSelected) AppTheme.colors.PrimaryVariant else AppTheme.colors.PrimaryVariant.copy(alpha = 0f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.alpha(alpha),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = mode.icon(),
                contentDescription = null,
                tint = AppTheme.colors.OnPrimary,
                modifier = Modifier
                    .width(iconScale)
                    .height(iconScale),
            )
            Text(
                text = stringResource(labelResId),
                color = AppTheme.colors.OnPrimary,
                style = AppTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

private fun ScanInputMode.icon(): ImageVector = when (this) {
    ScanInputMode.QR -> Icons.Default.QrCode2
    ScanInputMode.PHOTO -> Icons.Default.PhotoCamera
    ScanInputMode.GALLERY -> Icons.Default.Image
}
