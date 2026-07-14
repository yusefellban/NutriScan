package iti.grad.nutriscan.presentation.auth.register.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans
import iti.grad.presentation.R

// ─────────────────────────────────────────────────────────────────────────────
// Header: full-width teal panel, bottom corners rounded 32dp
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RegisterHeader(isDark: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Glow ellipse behind the header (Figma: blur 15px teal ellipse)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(29.dp)
                .align(Alignment.BottomCenter)
                .blur(15.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(AppTheme.colors.Teal1000)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppTheme.colors.Teal1000,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(top = 84.dp, bottom = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // NutriScan logo — tinted white so paths show on teal bg
                Image(
                    painter = painterResource(R.drawable.nutriscan_light),
                    contentDescription = "NutriScan",
                    colorFilter = ColorFilter.tint(AppTheme.colors.OnPrimary),
                    modifier = Modifier
                        .width(183.dp)
                        .height(34.dp)
                )
                // "Sign Up For Free!" — Teal200 on teal bg
                Text(
                    text = stringResource(R.string.signup_title),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 35.sp,
                    color = AppTheme.colors.Teal200
                )
            }
        }
    }
}
