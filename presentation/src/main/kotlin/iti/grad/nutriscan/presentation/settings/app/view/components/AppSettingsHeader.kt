package iti.grad.nutriscan.presentation.settings.app.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.components.AppBackButton
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans

@Composable
fun AppSettingsHeader(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = AppTheme.colors.Teal1000,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            )
            .statusBarsPadding()
            .height(242.dp)
            .padding(start = 24.dp, end = 24.dp, top = 24.dp),
    ) {
        AppBackButton(
            onClick = onBackClick,
            iconTint = AppTheme.colors.Teal300,
            borderColor = AppTheme.colors.Teal300,
            modifier = Modifier.align(Alignment.TopStart),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    lineHeight = 35.sp,
                ),
                color = AppTheme.colors.Teal300,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppTheme.typography.titleSmall,
                    color = AppTheme.colors.Gray100,
                )
            }
        }
    }
}
