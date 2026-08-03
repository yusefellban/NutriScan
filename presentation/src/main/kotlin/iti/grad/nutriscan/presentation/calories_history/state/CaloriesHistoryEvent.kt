package iti.grad.nutriscan.presentation.calories_history.state

sealed interface CaloriesHistoryEvent {
    data object NavigateBack : CaloriesHistoryEvent
    /** Placeholder — will open a date-picker in a future phase. */
    data object CalendarClicked : CaloriesHistoryEvent
}
