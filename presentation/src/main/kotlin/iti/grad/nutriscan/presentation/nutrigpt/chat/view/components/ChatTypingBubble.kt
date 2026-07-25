package iti.grad.nutriscan.presentation.nutrigpt.chat.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.getValue

@Composable
fun ChatTypingBubble(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")
    
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.3f at 0 with LinearOutSlowInEasing
                1f at 200 with LinearOutSlowInEasing
                0.3f at 400
                0.3f at 1000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.3f at 0
                0.3f at 200 with LinearOutSlowInEasing
                1f at 400 with LinearOutSlowInEasing
                0.3f at 600
                0.3f at 1000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.3f at 0
                0.3f at 400 with LinearOutSlowInEasing
                1f at 600 with LinearOutSlowInEasing
                0.3f at 800
                0.3f at 1000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    color = AppTheme.colors.ChatBotBubble,
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(AppTheme.colors.ChatDisclaimerIcon.copy(alpha = dot1Alpha), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(AppTheme.colors.ChatDisclaimerIcon.copy(alpha = dot2Alpha), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(AppTheme.colors.ChatDisclaimerIcon.copy(alpha = dot3Alpha), CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = AppTheme.colors.ChatDisclaimerIcon,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
