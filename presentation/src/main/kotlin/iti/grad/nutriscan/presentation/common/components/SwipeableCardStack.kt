package iti.grad.nutriscan.presentation.common.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.sign

private const val COMMIT_THRESHOLD_FRACTION = 0.25f

/** px/s — a quick flick commits the swipe even if the drag distance itself didn't cross
 * [COMMIT_THRESHOLD_FRACTION], matching how a native swipe gesture actually feels. */
private const val FLING_VELOCITY_THRESHOLD = 1000f

/** How far the back card's right edge pokes out from behind the front card at rest — the
 * "there's another card here" affordance the user asked for. Shrinks to 0 as the front card
 * is dragged away and the back card grows into its place. */
private val PEEK_OFFSET = 16.dp

/** Caps how far the front card lifts vertically during a full-width drag so a large swipe
 * never visually creeps into the list items above/below it in the Calories screen. */
private val MAX_LIFT = 16.dp

/**
 * Horizontally swipeable stack of cards, one visible at a time, with the next
 * card peeking (scaled down, offset) behind the current one — the front card
 * lifts and fades as it's dragged away while the back card grows into its
 * place, rather than a plain side-by-side pager scroll. Drag left or right
 * (or flick) past the commit threshold to advance; [currentPage] loops
 * through [pages].
 *
 * Live drag tracking is a plain synchronous [Float] state, not an
 * [Animatable] mutated from a freshly launched coroutine per pointer-move
 * event — the latter used to fire dozens of overlapping coroutines racing to
 * snap the same Animatable each frame, which is what caused the stutter.
 * The Animatable is only used for the single settle animation once the
 * gesture ends.
 */
@Composable
fun SwipeableCardStack(
    pages: List<@Composable () -> Unit>,
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var containerWidthPx by remember { mutableFloatStateOf(1f) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val settleOffset = remember { Animatable(0f) }
    var settling by remember { mutableStateOf(false) }
    val velocityTracker = remember { VelocityTracker() }
    val nextPage = (currentPage + 1) % pages.size

    val renderOffset = if (settling) settleOffset.value else dragOffset
    val progress = (renderOffset.absoluteValue / containerWidthPx).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { containerWidthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(currentPage) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        settling = false
                        velocityTracker.resetTracking()
                    },
                    onDragEnd = {
                        val flingVelocity = velocityTracker.calculateVelocity().x
                        val committed = dragOffset.absoluteValue > containerWidthPx * COMMIT_THRESHOLD_FRACTION ||
                            flingVelocity.absoluteValue > FLING_VELOCITY_THRESHOLD
                        val direction = when {
                            dragOffset != 0f -> sign(dragOffset)
                            flingVelocity != 0f -> sign(flingVelocity)
                            else -> 1f
                        }
                        val target = if (committed) containerWidthPx * direction else 0f
                        settling = true
                        scope.launch {
                            settleOffset.snapTo(dragOffset)
                            settleOffset.animateTo(
                                target,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                            if (committed) onPageChange(nextPage)
                            dragOffset = 0f
                            settleOffset.snapTo(0f)
                            settling = false
                        }
                    },
                    onDragCancel = {
                        settling = true
                        scope.launch {
                            settleOffset.snapTo(dragOffset)
                            settleOffset.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy))
                            dragOffset = 0f
                            settling = false
                        }
                    },
                ) { change, dragAmount ->
                    change.consume()
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    dragOffset = (dragOffset + dragAmount).coerceIn(-containerWidthPx, containerWidthPx)
                }
            },
    ) {
        // Back card — its right edge pokes out from behind the front card at rest (the scroll
        // affordance), grows to full size and slides in flush as the front card drags away.
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = 0.96f + 0.04f * progress
                scaleY = 0.96f + 0.04f * progress
                translationX = with(density) { (PEEK_OFFSET * (1f - progress)).toPx() }
                alpha = 0.85f + 0.15f * progress
            },
        ) {
            pages[nextPage]()
        }

        // Front card — follows the drag horizontally, lifts (capped) and fully fades as it's
        // swiped away.
        Box(
            modifier = Modifier.graphicsLayer {
                translationX = renderOffset
                translationY = -(renderOffset.absoluteValue / 6f).coerceAtMost(with(density) { MAX_LIFT.toPx() })
                alpha = 1f - progress
            },
        ) {
            pages[currentPage]()
        }
    }
}
