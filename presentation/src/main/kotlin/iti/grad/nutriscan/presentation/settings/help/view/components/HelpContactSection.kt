package iti.grad.nutriscan.presentation.settings.help.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.app.view.components.SettingsActionRow
import iti.grad.presentation.R

@Composable
fun HelpContactSection(
    onContactSupportClick: () -> Unit,
    onSendFeedbackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.help_contact_section_title),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.AppSettingsRowLabel,
        )
        SettingsActionRow(
            icon = rememberVectorPainter(Icons.Filled.Email),
            label = stringResource(R.string.help_contact_support),
            onClick = onContactSupportClick,
        )
        SettingsActionRow(
            icon = rememberVectorPainter(Icons.Filled.Feedback),
            label = stringResource(R.string.help_send_feedback),
            onClick = onSendFeedbackClick,
        )
    }
}
