package iti.grad.nutriscan.presentation.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import iti.grad.nutriscan.presentation.common.model.AppErrorType

/**
 * Smart error widget — automatically renders [ServerProblemWidget] for 5xx server errors
 * and [OfflineStateWidget] for all network / unknown failures.
 *
 * Usage:
 * ```kotlin
 * AppErrorWidget(
 *     errorType = state.errorType,
 *     onRetry   = { onEvent(MyEvent.Retry) },
 * )
 * ```
 */
@Composable
fun AppErrorWidget(
    errorType: AppErrorType,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (errorType) {
        AppErrorType.SERVER -> ServerProblemWidget(onRetry = onRetry, modifier = modifier)
        else               -> OfflineStateWidget(onRetry = onRetry, modifier = modifier)
    }
}
