package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.SideEffect
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalView
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import iti.grad.nutriscan.presentation.common.theme.LexendDeca
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.foundation.Image
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text

@Composable
fun CustomAlertDialog(
    title: String,
    message: String,
    icon: Painter,
    iconBackgroundColor: Color = Color.Unspecified,
    iconContentColor: Color = Color.Unspecified,
    onDismiss: () -> Unit,
    buttons: @Composable RowScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.setDimAmount(0f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB30F474A))
                .clickable(
                    interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() },
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
                        interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Prevent clicks on the card from dismissing the dialog
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                // We wrap the Main Card and its Glow in a Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp) // Leave space for half the icon at the top
                ) {
                    // Glow effect behind the dialog
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .blur(radius = 48.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            .background(AppTheme.colors.Accent.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                    )

                    // Main Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(AppTheme.colors.Background)
                            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(12.dp)) // space for the icon overlap
                        
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
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            buttons()
                        }
                    }
                }
                
                // Overlapping Icon
                if (iconBackgroundColor != Color.Unspecified && iconContentColor != Color.Unspecified) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(iconBackgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = icon,
                            contentDescription = title,
                            colorFilter = ColorFilter.tint(iconContentColor),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Image(
                        painter = icon,
                        contentDescription = title,
                        modifier = Modifier.size(56.dp)
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

