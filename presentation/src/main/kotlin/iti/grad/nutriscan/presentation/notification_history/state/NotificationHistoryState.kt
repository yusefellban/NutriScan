package iti.grad.nutriscan.presentation.notification_history.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import iti.grad.nutriscan.presentation.common.model.UiText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class NotificationHistoryState(
    val isLoading: Boolean = true,
    val notifications: ImmutableList<NotificationHistoryItemUi> = persistentListOf(),
    val isEmpty: Boolean = false,
)

data class NotificationHistoryItemUi(
    val id: Long,
    val title: String,
    val body: String,
    val type: iti.grad.nutriscan.domain.notification.model.NotificationType,
    val relativeTime: UiText,
    val isRead: Boolean,
)
