package iti.grad.nutriscan.presentation.profile_setup.view.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca

/**
 * Shared header composable for all Profile Setup pager pages.
 * Renders a title with an optionally highlighted keyword and a subtitle below it.
 *
 * @param prefixRes     String resource for the text before the highlighted word.
 * @param highlightRes  String resource for the highlighted (teal) keyword.
 * @param subtitleRes   String resource for the subtitle paragraph.
 * @param suffixRes     Optional string resource for text after the highlighted word (e.g. "?").
 * @param modifier      Modifier for the outer layout.
 */
@Composable
fun ProfileSetupHeader(
    prefixRes: Int,
    highlightRes: Int,
    subtitleRes: Int,
    suffixRes: Int? = null,
    currentPage: Int? = null,
    pageCount: Int? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Optional Page Indicator
        if (currentPage != null && pageCount != null) {
            ProfileSetupPageIndicator(
                currentPage = currentPage,
                pageCount = pageCount
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Title with highlighted keyword
    Text(
        text = buildAnnotatedString {
            append(stringResource(prefixRes))
            withStyle(SpanStyle(color = AppTheme.colors.Teal1000, fontWeight = FontWeight.Bold)) {
                append(stringResource(highlightRes))
                if (suffixRes != null) {
                    append(stringResource(suffixRes))
                }
            }
        },
        fontFamily = LexendDeca,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        color = AppTheme.colors.TextPrimary,
        textAlign = TextAlign.Center,
        modifier = modifier
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Subtitle
    Text(
        text = stringResource(subtitleRes),
        fontFamily = LexendDeca,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = AppTheme.colors.ProfileSetupSubtitle,
        textAlign = TextAlign.Center
    )
}
}
