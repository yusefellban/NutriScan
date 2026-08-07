package iti.grad.nutriscan.domain.dailytracking.model

data class DailyTrackingHistoryPage(
    val entries: List<DailyTrackingSummary>,
    val isLastPage: Boolean,
    val currentPage: Int,
)
