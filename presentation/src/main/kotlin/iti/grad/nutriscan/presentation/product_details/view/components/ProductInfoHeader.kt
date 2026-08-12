package iti.grad.nutriscan.presentation.product_details.view.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.scan.model.ScanStatus
import iti.grad.nutriscan.presentation.common.components.VerdictBadge
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.CaloriesTypography
import iti.grad.nutriscan.presentation.common.theme.ProductDetailsTypography
import iti.grad.presentation.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ProductInfoHeader(
    productName: String,
    status: ScanStatus,
    verdict: ProductVerdict,
    scanDate: LocalDate?,
    safetyReasonText: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        // ── Name + Scan Date ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = productName.ifBlank { stringResource(R.string.scan_product_unknown) },
                style = ProductDetailsTypography.productTitle,
                color = AppTheme.colors.ProductDetailTitleText,
                modifier = Modifier.weight(1f),
            )
            if (scanDate != null) {
                Column(
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = stringResource(R.string.product_details_scanned_at),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.ProductDetailScanDate,
                    )
                    Text(
                        text = scanDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.ProductDetailScanDateBadgeText,
                        modifier = Modifier
                            .background(
                                color = AppTheme.colors.ProductDetailScanDateBadgeBg,
                                shape = RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Verdict Badge + "For you" ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (status == ScanStatus.FAILED) {
                FailedStatusBadge()
            } else {
                VerdictBadge(verdict = verdict)
                Text(
                    text = stringResource(R.string.product_details_for_you),
                    style = AppTheme.typography.labelLarge,
                    color = AppTheme.colors.Teal1000,
                )
            }
        }

        // ── Safety Reason ──
        val reasonText = if (!safetyReasonText.isNullOrBlank()) {
            safetyReasonText
        } else {
            stringResource(R.string.no_specific_match_found)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row {
            Box(
                modifier = Modifier
                    .padding(top = 7.dp)
                    .size(5.dp)
                    .background(AppTheme.colors.ProductDetailSafetyReasonText, CircleShape),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = reasonText,
                style = CaloriesTypography.badgeText,
                color = AppTheme.colors.ProductDetailSafetyReasonText,
            )
        }
    }
}

@Composable
private fun FailedStatusBadge(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AppTheme.colors.VerdictRedBackground)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = stringResource(R.string.scan_status_failed),
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.VerdictRedText,
        )
    }
}
