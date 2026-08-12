package iti.grad.nutriscan.presentation.common.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter

/** How long the shimmer placeholder stays up after a pull-to-refresh. */
private const val SHIMMER_HOLD_MS = 2_000L

/**
 * Pull-to-refresh that swaps [content] for a [shimmer] placeholder while the refresh runs.
 *
 * The placeholder is held for a fixed [SHIMMER_HOLD_MS] rather than until the data lands. That is
 * deliberate and product-driven: a cached refresh finishes in well under 2s, so the screen would
 * otherwise flash. It also means a refresh never *feels* faster than 2s even when it is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshShimmerBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    shimmer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var showShimmer by remember { mutableStateOf(false) }

    // snapshotFlow can only observe snapshot State. isRefreshing is a plain Boolean parameter, so
    // reading it directly inside the effect below captures whatever it was on first composition and
    // never sees another value — the shimmer would simply never appear. rememberUpdatedState gives
    // the flow real State to track.
    val refreshing by rememberUpdatedState(isRefreshing)

    // Keyed on Unit, not isRefreshing: a ViewModel that clears isRefreshing inside the hold window
    // would re-key the effect, cancelling the delay before it could set showShimmer back to false
    // and leaving the placeholder up forever. snapshotFlow only ever restarts the body on a *new*
    // refresh, so the false edge can no longer interrupt a hold in progress.
    LaunchedEffect(Unit) {
        snapshotFlow { refreshing }
            .filter { it }
            .collectLatest {
                showShimmer = true
                delay(SHIMMER_HOLD_MS)
                showShimmer = false
            }
    }

    PullToRefreshBox(
        // Keeps the spinner alive for the whole hold, so it never disappears above a still-
        // shimmering list when the ViewModel clears isRefreshing early.
        isRefreshing = isRefreshing || showShimmer,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        if (showShimmer) shimmer() else content()
    }
}
