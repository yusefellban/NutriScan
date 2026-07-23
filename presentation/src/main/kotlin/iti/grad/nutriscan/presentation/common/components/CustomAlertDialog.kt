package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca

@Composable
fun CustomAlertDialog(
    title: String,
    message: String,
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconContentColor: Color,
    glowColor: Color,
    onDismiss: () -> Unit,
    buttons: @Composable RowScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        val view = androidx.compose.ui.platform.LocalView.current
        androidx.compose.runtime.SideEffect {
            val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            window?.setDimAmount(0f)
        }

        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Color(0x800F474A))
                .clickable(
                    interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .clickable(
                        interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Prevent clicks on the card from dismissing the dialog
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                // Glow effect behind the dialog
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp) // align with the main card
                        .height(200.dp) // approximate height to create a halo
                        .blur(radius = 48.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .background(glowColor.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                )

                // Main Card
                Column(
                    modifier = Modifier
                        .padding(top = 28.dp) // Leave space for half the icon
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(AppTheme.colors.AuthDialogBackground)
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp)) // space for the icon overlap
                    
                    Text(
                        text = title,
                        style = AppTheme.typography.titleLarge.copy(
                            fontFamily = LexendDeca,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = AppTheme.colors.ProfileSetupTitle,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = message,
                        style = AppTheme.typography.bodyMedium.copy(
                            fontFamily = LexendDeca
                        ),
                        color = AppTheme.colors.AuthDialogSubtitle,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        buttons()
                    }
                }
                
                // Overlapping Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(iconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconContentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.AlertButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppTheme.typography.labelLarge.copy(
                fontFamily = LexendDeca,
                fontWeight = FontWeight.Medium
            ),
            color = textColor
        )
    }
}
