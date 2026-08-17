package iti.grad.nutriscan.presentation.product_details.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.domain.scan.model.FamilyAlert
import iti.grad.nutriscan.presentation.common.components.VerdictBadge
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

/**
 * Displays the per-family-member safety alerts section on the Product Details screen.
 *
 * ⚠️ Health-critical: Each [FamilyAlert] represents a family member who may be at risk
 * from this product. This section must only be hidden when the list is genuinely empty —
 * never suppress alerts for UX reasons.
 *
 * Call site must guard: `if (familyAlerts.isNotEmpty()) FamilyAlertsSection(...)`
 */
@Composable
fun FamilyAlertsSection(
    alerts: List<FamilyAlert>,
    modifier: Modifier = Modifier,
) {
    if (alerts.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.product_details_family_alerts_title),
            style = AppTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = AppTheme.colors.ProductDetailWhyNotSafeText,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        alerts.forEach { alert ->
            FamilyAlertCard(alert = alert)
        }
    }
}

@Composable
private fun FamilyAlertCard(
    alert: FamilyAlert,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = AppTheme.colors.ProductDetailIngredientCardBorder,
                shape = RoundedCornerShape(12.dp),
            )
            .background(
                color = AppTheme.colors.SurfaceVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (alert.targetImageUrl != null) {
            iti.grad.nutriscan.presentation.common.components.AvatarCircle(
                avatarUrl = alert.targetImageUrl,
                avatarUpdatedAt = null,
                size = 40.dp,
                ringWidth = 0.dp,
            )
        } else {
            // Avatar — first letter of the profile name
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = AppTheme.colors.Teal1000,
                        shape = CircleShape,
                    ),
            ) {
                Text(
                    text = alert.targetProfile.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = AppTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.Teal100,
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = alert.targetProfile,
                    style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppTheme.colors.TextPrimary,
                    modifier = Modifier.weight(1f, fill = false),
                )
                VerdictBadge(verdict = alert.severity)
            }

            if (alert.reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.reason,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.TextSecondary,
                )
            }
        }
    }
}
