package iti.grad.nutriscan.presentation.profile_setup.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
fun WeightSelectionPlaceholder(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(104.dp))

        // Title + Subtitle
        ProfileSetupHeader(
            prefixRes = R.string.profile_setup_weight_title_prefix,
            highlightRes = R.string.profile_setup_weight_title_highlight,
            subtitleRes = R.string.profile_setup_weight_subtitle,
            currentPage = currentPage,
            pageCount = pageCount
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Placeholder body — TODO: implement actual weight picker
        Text(
            text = stringResource(R.string.profile_setup_placeholder_coming_soon),
            fontFamily = LexendDeca,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = AppTheme.colors.TextSecondary
        )
    }
}
