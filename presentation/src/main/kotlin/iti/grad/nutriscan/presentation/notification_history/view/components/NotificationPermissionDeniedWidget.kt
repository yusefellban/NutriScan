package iti.grad.nutriscan.presentation.notification_history.view.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

@Composable
fun NotificationPermissionDeniedWidget(
    onGoToSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val imageRes = if (AppTheme.isDark) {
            R.drawable.turn_on_notification_dark
        } else {
            R.drawable.turn_on_notification_light
        }
        
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(id = R.string.notification_permission_title),
            style = AppTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.TextPrimary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(id = R.string.notification_permission_subtitle),
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.TextPrimary.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        AppButton(
            textResId = R.string.notification_permission_button,
            isLoading = false,
            onClick = onGoToSettingsClick
        )
    }
}
