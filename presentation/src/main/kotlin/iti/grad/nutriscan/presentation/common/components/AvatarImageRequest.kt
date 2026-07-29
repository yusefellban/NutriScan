package iti.grad.nutriscan.presentation.common.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Centralized [ImageRequest] builder for the user's avatar.
 *
 * Every screen that renders `User.avatarUrl` (Edit Profile, the Profile header,
 * etc.) must build its Coil request through this helper instead of calling
 * `ImageRequest.Builder(...).data(avatarUrl)` directly. Centralizing it here
 * guarantees:
 *  - one crossfade behavior everywhere the avatar appears
 *  - one cache-busting strategy: the memory/disk cache key includes
 *    [avatarUpdatedAt] (the server's `updatedAt` timestamp), so a freshly
 *    re-uploaded photo is never served stale from cache even when the backend
 *    happens to reuse the same URL for the new image.
 *
 * Returns `null` when there is no avatar to show, so callers can simply do
 * `if (request != null) AsyncImage(...) else <placeholder>`.
 */
@Composable
fun rememberAvatarImageRequest(
    avatarUrl: String?,
    avatarUpdatedAt: String?
): ImageRequest? {
    val context = LocalContext.current
    if (avatarUrl == null) return null

    return remember(avatarUrl, avatarUpdatedAt) {
        val cacheKey = "avatar_${avatarUrl}_${avatarUpdatedAt.orEmpty()}"
        ImageRequest.Builder(context)
            .data(avatarUrl)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .crossfade(true)
            .build()
    }
}
