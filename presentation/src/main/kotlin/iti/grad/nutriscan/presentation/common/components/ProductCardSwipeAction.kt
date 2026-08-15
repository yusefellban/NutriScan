package iti.grad.nutriscan.presentation.common.components

import androidx.annotation.StringRes

/** What swiping a [ProductCard] does — currently just Add (Saved catalog). Food log deletion
 * uses [ProductCard]'s `onDeleteClick` button instead of a swipe gesture. */
sealed interface ProductCardSwipeAction {
    @get:StringRes val hintResId: Int
    val onTriggered: () -> Unit

    data class Add(
        @StringRes override val hintResId: Int,
        override val onTriggered: () -> Unit,
    ) : ProductCardSwipeAction
}
