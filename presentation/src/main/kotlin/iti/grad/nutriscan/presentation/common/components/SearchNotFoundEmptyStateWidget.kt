package iti.grad.nutriscan.presentation.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import iti.grad.presentation.R

/**
 * Thin wrapper around [AppEmptyStateWidget] for the "Search / No Results Found" state.
 *
 * @param showButton If true, renders a "Go to Scan" action button below the subtitle.
 * @param onScanNowClick Click handler for the action button (only invoked when [showButton] is true).
 */
@Composable
fun SearchNotFoundEmptyStateWidget(
    showButton: Boolean = false,
    onScanNowClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AppEmptyStateWidget(
        lightImageRes = R.drawable.search_reasult_not_found_light,
        darkImageRes = R.drawable.search_reasult_not_found_dark,
        title = stringResource(R.string.search_not_found_title),
        subtitle = stringResource(R.string.search_not_found_subtitle),
        buttonText = if (showButton) stringResource(R.string.search_not_found_button) else null,
        onButtonClick = if (showButton) onScanNowClick else null,
        modifier = modifier,
    )
}
