package iti.grad.nutriscan.presentation.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import iti.grad.presentation.R

/**
 * Thin wrapper around [AppEmptyStateWidget] for the "404 / Not Found" state.
 * Callers only need to provide the retry lambda — all copy and illustrations are handled here.
 */
@Composable
fun NotFoundStateWidget(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppEmptyStateWidget(
        lightImageRes = R.drawable.not_found_light,
        darkImageRes = R.drawable.not_found_dark,
        title = stringResource(R.string.not_found_title),
        subtitle = stringResource(R.string.not_found_subtitle),
        buttonText = stringResource(R.string.try_again),
        onButtonClick = onRetry,
        modifier = modifier,
    )
}
