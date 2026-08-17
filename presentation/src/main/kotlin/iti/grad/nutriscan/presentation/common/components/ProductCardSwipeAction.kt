package iti.grad.nutriscan.presentation.common.components

import androidx.annotation.StringRes

/** What swiping a [ProductCard] does — currently just Add (Saved catalog). Food log deletion
 * uses [ProductCard]'s `onDeleteClick` button instead of a swipe gesture. */
sealed interface ProductCardSwipeAction {
    @get:StringRes val hintResId: Int

    /** Invoked once the drag crosses the trigger threshold. Must call [onResult] with whether
     * the action actually succeeded so the button can show its submitting/success feedback. */
    val onTriggered: (onResult: (Boolean) -> Unit) -> Unit

    data class Add(
        @StringRes override val hintResId: Int,
        override val onTriggered: (onResult: (Boolean) -> Unit) -> Unit,
    ) : ProductCardSwipeAction
}
