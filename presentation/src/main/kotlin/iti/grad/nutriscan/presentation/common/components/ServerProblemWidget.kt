package iti.grad.nutriscan.presentation.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import iti.grad.presentation.R

/**
 * Thin wrapper around [AppEmptyStateWidget] for the "Server Problem / 5xx" state.
 * Callers only need to provide the retry lambda — all copy and illustrations are handled here.
 */
@Composable
fun ServerProblemWidget(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppEmptyStateWidget(
        lightImageRes = R.drawable.server_problem_ic,
        darkImageRes = R.drawable.server_problem_ic,
        title = stringResource(R.string.server_problem_title),
        subtitle = stringResource(R.string.server_problem_subtitle),
        buttonText = stringResource(R.string.server_problem_retry),
        onButtonClick = onRetry,
        modifier = modifier,
    )
}
