package iti.grad.nutriscan.presentation.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import iti.grad.presentation.R

/**
 * Thin wrapper around [AppEmptyStateWidget] for the "No Internet / Offline" state.
 * Callers only need to provide the retry lambda — all copy and illustrations are handled here.
 */
@Composable
fun OfflineStateWidget(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppEmptyStateWidget(
        lightImageRes = R.drawable.no_network_connection_light,
        darkImageRes = R.drawable.no_network_connection_dark,
        title = stringResource(R.string.offline_state_title),
        subtitle = stringResource(R.string.offline_state_subtitle),
        buttonText = stringResource(R.string.offline_state_retry),
        onButtonClick = onRetry,
        modifier = modifier,
    )
}
