package iti.grad.nutriscan.presentation.profile_setup.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca
import iti.grad.presentation.R

@Composable
fun HeightSelectionPlaceholder(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        // Title with highlighted word
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.profile_setup_height_title_prefix))
                withStyle(SpanStyle(color = AppTheme.colors.Primary, fontWeight = FontWeight.Bold)) {
                    append(stringResource(R.string.profile_setup_height_title_highlight))
                }
                append(stringResource(R.string.profile_setup_height_title_suffix))
            },
            fontFamily = LexendDeca,
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            color = AppTheme.colors.TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = stringResource(R.string.profile_setup_height_subtitle),
            fontFamily = LexendDeca,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = AppTheme.colors.TextSecondary
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Placeholder body — TODO: implement actual height ruler picker
        Text(
            text = stringResource(R.string.profile_setup_placeholder_coming_soon),
            fontFamily = LexendDeca,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = AppTheme.colors.TextSecondary
        )
    }
}
