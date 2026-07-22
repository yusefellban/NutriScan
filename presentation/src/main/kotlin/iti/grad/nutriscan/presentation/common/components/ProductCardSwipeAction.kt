package iti.grad.nutriscan.presentation.common.components

import androidx.annotation.StringRes

/** What swiping a [ProductCard] does — Add (Saved catalog) or Remove (food log), each with its own icon/color. */
sealed interface ProductCardSwipeAction {
    @get:StringRes val hintResId: Int
    val onTriggered: () -> Unit

    data class Add(
        @StringRes override val hintResId: Int,
        override val onTriggered: () -> Unit,
    ) : ProductCardSwipeAction

    data class Remove(
        @StringRes override val hintResId: Int,
        override val onTriggered: () -> Unit,
    ) : ProductCardSwipeAction
}
