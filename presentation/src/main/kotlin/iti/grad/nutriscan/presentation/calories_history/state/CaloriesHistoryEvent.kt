package iti.grad.nutriscan.presentation.calories_history.state

import java.time.LocalDate

sealed interface CaloriesHistoryEvent {
    data object NavigateBack : CaloriesHistoryEvent
    /** Opens the Material3 date picker dialog. */
    data object CalendarClicked : CaloriesHistoryEvent
    /** User confirmed a date in the date picker. */
    data class DateSelected(val date: LocalDate) : CaloriesHistoryEvent
    /** User dismissed the date picker or wants to clear the filter and go back to paginated history. */
    data object ClearDateFilter : CaloriesHistoryEvent
    /** User dismissed the date picker dialog without selecting a date. */
    data object DismissDatePicker : CaloriesHistoryEvent
    /** Fired when the user scrolls near the bottom of the list to load the next page. */
    data object LoadMore : CaloriesHistoryEvent
    /** Fired when the user taps retry after a failed first-page load. */
    data object Retry : CaloriesHistoryEvent
}
