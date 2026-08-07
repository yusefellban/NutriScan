package iti.grad.nutriscan.presentation.home.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.components.customShadow
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.HomeTypography
import iti.grad.presentation.R

import androidx.compose.ui.res.stringArrayResource
import java.util.Calendar

/**
 * Daily Health Tip card displayed below the greeting header.
 */
@Composable
fun DailyHealthTipCard(
    modifier: Modifier = Modifier,
) {
    val dailyTips = stringArrayResource(id = R.array.daily_tips_array)
    val dayOfMonth = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }
    val tipIndex = (dayOfMonth - 1) % dailyTips.size
    val currentTip = dailyTips.getOrElse(tipIndex) { dailyTips.firstOrNull() ?: "" }

    val foregroundShadowColor = AppTheme.colors.Teal200.copy(alpha = 0.5f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .customShadow(
                shape = RoundedCornerShape(22.dp),
                color = AppTheme.colors.Teal200.copy(alpha = 0.2f),
                blurRadius = 0f,
                offsetY = 0f
            )
            .border(
                width = 1.dp,
                color = AppTheme.colors.Primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(22.dp)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(AppTheme.colors.SurfaceVariant)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            foregroundShadowColor,
                            Color.Transparent
                        ),
                        center = Offset(size.width, 0f),
                        radius = size.height * 1.5f
                    ),
                    center = Offset(size.width, 0f),
                    radius = size.height * 1.5f
                )
            }
            .padding(vertical = 20.dp, horizontal = 16.dp),
        // verticalAlignment = Alignment.CenterVertically,
    ) {
        // Water drop icon in circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFD4F1F2)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_water_dot),
                contentDescription = null,
                tint = AppTheme.colors.WaterDropIconTint,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = stringResource(R.string.home_daily_tip_title),
                style = HomeTypography.dailyTipTitle,
                color = AppTheme.colors.Teal800,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentTip,
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colors.PrimaryVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

