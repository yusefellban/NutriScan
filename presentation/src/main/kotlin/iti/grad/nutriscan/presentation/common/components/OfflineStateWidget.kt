package iti.grad.nutriscan.presentation.common.components

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

@Composable
fun OfflineStateWidget(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val imageRes = if (AppTheme.isDark) {
            R.drawable.no_network_connection_dark
        } else {
            R.drawable.no_network_connection_light
        }

        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            contentScale = ContentScale.Fit
        )
        
        Text(
            text = stringResource(id = R.string.offline_state_title),
            style = AppTheme.typography.titleLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = AppTheme.colors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = stringResource(id = R.string.offline_state_subtitle),
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.TextPrimary.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        AppButton(
            textResId = R.string.offline_state_retry,
            isLoading = false,
            onClick = onRetry
        )
    }
}
