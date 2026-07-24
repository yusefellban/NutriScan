package iti.grad.nutriscan.presentation.nutrigpt.chat.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.components.AppBackButton
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.nutrigpt.chat.state.ChatLanguage

@Composable
fun ChatTopBar(
    currentLanguage: ChatLanguage,
    onNavigateBack: () -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppBackButton(
            onClick = onNavigateBack,
            iconTint = AppTheme.colors.Primary,
            borderColor = AppTheme.colors.Primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = stringResource(id = R.string.nutrigpt_title),
            style = AppTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.PrimaryVariant
            ),
            modifier = Modifier.weight(1f)
        )
        
        // Language Toggle
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(AppTheme.colors.ChatBotBubble)
                .clickable { onToggleLanguage() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (currentLanguage == ChatLanguage.EN) {
                    stringResource(id = R.string.nutrigpt_lang_en)
                } else {
                    stringResource(id = R.string.nutrigpt_lang_ar)
                },
                style = AppTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.Primary
                )
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Sound/Action Icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.Primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = stringResource(id = R.string.nutrigpt_sparkle_content_description),
                tint = AppTheme.colors.OnPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
