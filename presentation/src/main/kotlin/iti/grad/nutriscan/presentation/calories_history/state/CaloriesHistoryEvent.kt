package iti.grad.nutriscan.presentation.calories_history.state

sealed interface CaloriesHistoryEvent {
    data object NavigateBack : CaloriesHistoryEvent
    /** Placeholder — will open a date-picker in a future phase. */
    data object CalendarClicked : CaloriesHistoryEvent
    /** Fired when the user scrolls near the bottom of the list to load the next page. */
    data object LoadMore : CaloriesHistoryEvent
    /** Fired when the user taps retry after a failed first-page load. */
    data object Retry : CaloriesHistoryEvent
}
