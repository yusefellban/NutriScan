package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

@Composable
fun SearchNotFoundEmptyStateWidget(
    showButton: Boolean = false,
    onScanNowClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val imageRes = if (AppTheme.isDark) {
            R.drawable.search_reasult_not_found_dark
        } else {
            R.drawable.search_reasult_not_found_light
        }
        
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(id = R.string.search_not_found_title),
            style = AppTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.TextPrimary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(id = R.string.search_not_found_subtitle),
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.TextPrimary.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
        
        if (showButton) {
            Spacer(modifier = Modifier.height(32.dp))
            
            AppButton(
                textResId = R.string.search_not_found_button,
                isLoading = false,
                onClick = onScanNowClick,
            )
        }
    }
}
