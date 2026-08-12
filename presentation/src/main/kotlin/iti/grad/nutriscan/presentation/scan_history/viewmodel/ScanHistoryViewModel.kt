package iti.grad.nutriscan.presentation.scan_history.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.scan.model.ScanHistoryEntry
import iti.grad.nutriscan.domain.scan.model.ScanStatus
import iti.grad.nutriscan.domain.scan.usecase.GetRecentScansUseCase
import iti.grad.nutriscan.domain.scan.usecase.GetScanSuggestionsUseCase
import iti.grad.nutriscan.presentation.common.model.HistoryItemUiModel
import iti.grad.nutriscan.presentation.common.model.UiText
import iti.grad.nutriscan.presentation.common.model.VerdictType
import iti.grad.nutriscan.presentation.scan_history.state.HistoryFilter
import iti.grad.nutriscan.presentation.scan_history.state.ScanHistoryEffect
import iti.grad.nutriscan.presentation.scan_history.state.ScanHistoryEvent
import iti.grad.nutriscan.presentation.scan_history.state.ScanHistoryState
import iti.grad.presentation.R
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class ScanHistoryViewModel @Inject constructor(
    private val getRecentScansUseCase: GetRecentScansUseCase,
    private val getScanSuggestionsUseCase: GetScanSuggestionsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanHistoryState())
    val state: StateFlow<ScanHistoryState> = _state.asStateFlow()

    private val _effect = Channel<ScanHistoryEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val pageSize = 8

    /** Raw query string emitted on every keystroke — debounced before hitting the API. */
    private val _searchQueryFlow = MutableStateFlow("")

    init {
        loadInitial()
        observeSearchQueryForSuggestions()
    }

    fun onEvent(event: ScanHistoryEvent) {
        when (event) {
            is ScanHistoryEvent.LoadMore -> loadMore()
            is ScanHistoryEvent.FilterSelected -> applyFilter(event.filter)
            is ScanHistoryEvent.ItemClicked -> emitEffect(ScanHistoryEffect.NavigateToProductDetails(event.scanId))
            is ScanHistoryEvent.BackClicked -> emitEffect(ScanHistoryEffect.NavigateBack)
            is ScanHistoryEvent.RetryLoad -> loadInitial()
            is ScanHistoryEvent.DateSelected -> handleDateSelected(event.dateMillis)
            is ScanHistoryEvent.ShowDatePicker -> _state.update { it.copy(showDatePicker = event.show) }
            is ScanHistoryEvent.ResetFilters -> handleResetFilters()
            // ── Search ──
            is ScanHistoryEvent.SearchQueryChanged -> handleSearchQueryChanged(event.query)
            is ScanHistoryEvent.SuggestionSelected -> handleSuggestionSelected(event.suggestion)
            is ScanHistoryEvent.SearchSubmitted -> handleSearchSubmitted()
            is ScanHistoryEvent.SearchCleared -> handleSearchCleared()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Search handlers
    // ──────────────────────────────────────────────────────────────

    private fun handleSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query, isSearchActive = query.isNotBlank()) }
        _searchQueryFlow.value = query
    }

    private fun handleSuggestionSelected(suggestion: String) {
        _state.update {
            it.copy(
                searchQuery = suggestion,
                suggestions = kotlinx.collections.immutable.persistentListOf(),
                isSearchActive = false,
            )
        }
        loadInitial()
    }

    private fun handleSearchSubmitted() {
        _state.update {
            it.copy(
                suggestions = kotlinx.collections.immutable.persistentListOf(),
                isSearchActive = false,
            )
        }
        loadInitial()
    }

    private fun handleSearchCleared() {
        _state.update {
            it.copy(
                searchQuery = "",
                suggestions = kotlinx.collections.immutable.persistentListOf(),
                isSearchActive = false,
                isSuggestionsLoading = false,
            )
        }
        _searchQueryFlow.value = ""
        loadInitial()
    }

    /** Watches the raw query flow, debounces 300 ms, then fetches suggestions. */
    private fun observeSearchQueryForSuggestions() {
        viewModelScope.launch {
            _searchQueryFlow
                .debounce(300L)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collect { query ->
                    _state.update { it.copy(isSuggestionsLoading = true) }
                    getScanSuggestionsUseCase(query)
                        .onSuccess { suggestions ->
                            _state.update {
                                it.copy(
                                    suggestions = suggestions.toImmutableList(),
                                    isSuggestionsLoading = false,
                                )
                            }
                        }
                        .onFailure {
                            // Degrade gracefully — no dropdown on network error
                            _state.update {
                                it.copy(
                                    suggestions = kotlinx.collections.immutable.persistentListOf(),
                                    isSuggestionsLoading = false,
                                )
                            }
                        }
                }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Existing handlers
    // ──────────────────────────────────────────────────────────────

    private fun handleDateSelected(dateMillis: Long?) {
        val newDate = if (dateMillis != null) {
            Instant.ofEpochMilli(dateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } else {
            null
        }
        
        if (_state.value.selectedDate != newDate) {
            _state.update { it.copy(showDatePicker = false, selectedDate = newDate) }
            loadInitial()
        } else {
            _state.update { it.copy(showDatePicker = false) }
        }
    }

    private fun handleResetFilters() {
        _state.update {
            it.copy(
                selectedFilter = HistoryFilter.ALL,
                selectedDate = null,
                searchQuery = "",
                suggestions = kotlinx.collections.immutable.persistentListOf(),
                isSearchActive = false,
            )
        }
        _searchQueryFlow.value = ""
        loadInitial()
    }

    private fun getVerdictParam(filter: HistoryFilter): String? = when (filter) {
        HistoryFilter.ALL -> null
        HistoryFilter.SAFE -> "SAFE"
        HistoryFilter.CAUTION -> "CAUTION"
        HistoryFilter.UNSAFE -> "UNSAFE"
    }

    private fun loadInitial() {
        if (_state.value.isLoading) return
        _state.update {
            it.copy(
                isLoading = true,
                error = null,
                page = 0,
                isLastPage = false,
                allHistoryItems = emptyList(),
                displayedHistoryItems = emptyList<HistoryItemUiModel>().toImmutableList(),
            )
        }

        viewModelScope.launch {
            val currentState = _state.value
            val verdictParam = getVerdictParam(currentState.selectedFilter)
            val queryParam = currentState.searchQuery.takeIf { it.isNotBlank() }

            getRecentScansUseCase(
                page = 0,
                size = pageSize,
                date = currentState.selectedDate,
                verdict = verdictParam,
                query = queryParam,
            )
                .onSuccess { scans ->
                    val uiItems = mapScansToUi(scans)
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            allHistoryItems = uiItems,
                            displayedHistoryItems = uiItems.toImmutableList(),
                            isLastPage = scans.size < pageSize,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load history",
                        )
                    }
                }
        }
    }

    private fun loadMore() {
        val currentState = _state.value
        if (currentState.isLoading || currentState.isPaginationLoading || currentState.isLastPage || currentState.error != null) {
            return
        }

        val nextPage = currentState.page + 1
        _state.update { it.copy(isPaginationLoading = true, page = nextPage) }

        viewModelScope.launch {
            val verdictParam = getVerdictParam(currentState.selectedFilter)
            val queryParam = currentState.searchQuery.takeIf { it.isNotBlank() }

            getRecentScansUseCase(
                page = nextPage,
                size = pageSize,
                date = currentState.selectedDate,
                verdict = verdictParam,
                query = queryParam,
            )
                .onSuccess { scans ->
                    val newUiItems = mapScansToUi(scans)
                    _state.update { state ->
                        val combined = state.allHistoryItems + newUiItems
                        state.copy(
                            isPaginationLoading = false,
                            allHistoryItems = combined,
                            displayedHistoryItems = combined.toImmutableList(),
                            isLastPage = scans.size < pageSize,
                        )
                    }
                }
                .onFailure {
                    _state.update { state ->
                        state.copy(
                            isPaginationLoading = false,
                            page = state.page - 1,
                        )
                    }
                }
        }
    }

    private fun applyFilter(filter: HistoryFilter) {
        _state.update { it.copy(selectedFilter = filter) }
        loadInitial()
    }

    private fun mapScansToUi(scans: List<ScanHistoryEntry>): List<HistoryItemUiModel> {
        return scans.map { entry ->
            val verdictType = if (entry.status == ScanStatus.FAILED) {
                VerdictType.FAILED
            } else {
                when (entry.verdict) {
                    ProductVerdict.SAFE -> VerdictType.GREEN
                    ProductVerdict.CAUTION -> VerdictType.YELLOW
                    ProductVerdict.UNSAFE -> VerdictType.RED
                    null -> VerdictType.CYAN
                }
            }
            val verdictLabelResId = if (entry.status == ScanStatus.FAILED) {
                R.string.scan_status_failed
            } else {
                when (entry.verdict) {
                    ProductVerdict.SAFE -> R.string.verdict_safe
                    ProductVerdict.CAUTION -> R.string.verdict_caution
                    ProductVerdict.UNSAFE -> R.string.verdict_unsafe
                    null -> R.string.verdict_unknown
                }
            }

            val scanDate = entry.scannedAt?.let { formatRelativeDate(it) } ?: UiText.DynamicString("Unknown Date")
            val productName = entry.productName?.takeIf { it.isNotBlank() && it.lowercase() != "unknown" }
                ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                ?: "Unknown Product"

            HistoryItemUiModel(
                id = entry.scanId,
                productName = productName,
                scanDate = scanDate,
                verdictLabelResId = verdictLabelResId,
                verdictType = verdictType,
                imageUrl = entry.imageUrl,
            )
        }
    }

    private fun formatRelativeDate(isoString: String): UiText {
        return try {
            val zonedDateTime = ZonedDateTime.parse(isoString)
            val localDateTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
            val today = LocalDate.now()
            val datePart = localDateTime.toLocalDate()
            val timeString = localDateTime.format(DateTimeFormatter.ofPattern("h:mm a"))

            when (datePart) {
                today -> UiText.StringResource(R.string.date_today, timeString)
                today.minusDays(1) -> UiText.StringResource(R.string.date_yesterday, timeString)
                else -> UiText.DynamicString(localDateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a")))
            }
        } catch (e: Exception) {
            UiText.DynamicString(isoString)
        }
    }

    private fun emitEffect(effect: ScanHistoryEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
