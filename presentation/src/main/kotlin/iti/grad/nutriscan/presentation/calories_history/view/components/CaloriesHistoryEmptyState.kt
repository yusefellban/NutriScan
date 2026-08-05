package iti.grad.nutriscan.presentation.calories_history.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

import coil3.compose.AsyncImage

@Composable
fun CaloriesHistoryEmptyState(
    onAddMealsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val assetName = if (AppTheme.isDark) {
            "ic_no_calories_history_dark.svg"
        } else {
            "ic_no_calories_history_light.svg"
        }
        
        AsyncImage(
            model = "file:///android_asset/$assetName",
            contentDescription = null,
            modifier = Modifier.size(306.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.calories_history_empty_title),
            style = AppTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AppTheme.colors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.calories_history_empty_subtitle),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        AppButton(
            textResId = R.string.calories_history_empty_button,
            isLoading = false,
            onClick = onAddMealsClick,
        )
    }
}
