package iti.grad.nutriscan.presentation.common.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback

/** Same "tick" as [hapticTapFeedback] but for a specific action (nav tab, cup tap, card
 * click, swipe-delete...) instead of relying on the global pass-through detector — call this
 * directly where the tap is meaningful enough to want a deliberate, dependable buzz. */
fun HapticFeedback.tick() = performHapticFeedback(HapticFeedbackType.LongPress)

/**
 * App-wide tap haptic: a light "tick" on every press, anywhere in the subtree this is
 * attached to. Reads pointer events on the Initial pass without consuming them, so it never
 * blocks the click/scroll/drag handling underneath — just piggybacks a vibration on top.
 * Attach once at the navigation root instead of touching every clickable individually.
 *
 * Uses Compose's [LocalHapticFeedback] (which passes FLAG_IGNORE_VIEW_SETTING under the hood)
 * rather than a raw `HapticFeedbackConstants` value — constants like CONTEXT_CLICK are muted
 * to the point of being imperceptible on several OEM skins, while LongPress is the one Compose
 * itself uses everywhere (drag-to-reorder, etc.) and is reliably felt.
 */
fun Modifier.hapticTapFeedback(): Modifier = composed {
    val haptics = LocalHapticFeedback.current
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Initial)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}
