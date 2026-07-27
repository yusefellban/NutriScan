package iti.grad.nutriscan.presentation.scan_history.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.scan.model.ScanHistoryEntry
import iti.grad.nutriscan.domain.scan.usecase.GetRecentScansUseCase
import iti.grad.nutriscan.presentation.common.model.HistoryItemUiModel
import iti.grad.nutriscan.presentation.common.model.UiText
import iti.grad.nutriscan.presentation.common.model.VerdictType
import iti.grad.nutriscan.presentation.scan_history.state.HistoryFilter
import iti.grad.nutriscan.presentation.scan_history.state.ScanHistoryEffect
import iti.grad.nutriscan.presentation.scan_history.state.ScanHistoryEvent
import iti.grad.nutriscan.presentation.scan_history.state.ScanHistoryState
import iti.grad.presentation.R
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ScanHistoryViewModel @Inject constructor(
    private val getRecentScansUseCase: GetRecentScansUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ScanHistoryState())
    val state: StateFlow<ScanHistoryState> = _state.asStateFlow()

    private val _effect = Channel<ScanHistoryEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val pageSize = 8

    init {
        loadInitial()
    }

    fun onEvent(event: ScanHistoryEvent) {
        when (event) {
            is ScanHistoryEvent.LoadMore -> loadMore()
            is ScanHistoryEvent.FilterSelected -> applyFilter(event.filter)
            is ScanHistoryEvent.ItemClicked -> emitEffect(ScanHistoryEffect.NavigateToProductDetails(event.scanId))
            is ScanHistoryEvent.BackClicked -> emitEffect(ScanHistoryEffect.NavigateBack)
            is ScanHistoryEvent.RetryLoad -> loadInitial()
        }
    }

    private fun loadInitial() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null, page = 0, isLastPage = false, allHistoryItems = emptyList()) }
        
        viewModelScope.launch {
            getRecentScansUseCase(page = 0, size = pageSize)
                .onSuccess { scans ->
                    val uiItems = mapScansToUi(scans)
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            allHistoryItems = uiItems,
                            isLastPage = scans.size < pageSize,
                        )
                    }
                    updateDisplayedItems()
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            error = error.message ?: "Failed to load history"
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
            getRecentScansUseCase(page = nextPage, size = pageSize)
                .onSuccess { scans ->
                    val newUiItems = mapScansToUi(scans)
                    _state.update { state ->
                        state.copy(
                            isPaginationLoading = false,
                            allHistoryItems = state.allHistoryItems + newUiItems,
                            isLastPage = scans.size < pageSize
                        )
                    }
                    updateDisplayedItems()
                }
                .onFailure {
                    // Revert page increment on failure
                    _state.update { state -> 
                        state.copy(
                            isPaginationLoading = false,
                            page = state.page - 1
                        )
                    }
                }
        }
    }

    private fun applyFilter(filter: HistoryFilter) {
        _state.update { it.copy(selectedFilter = filter) }
        updateDisplayedItems()
    }

    private fun updateDisplayedItems() {
        val currentState = _state.value
        val filtered = when (currentState.selectedFilter) {
            HistoryFilter.ALL -> currentState.allHistoryItems
            HistoryFilter.SAFE -> currentState.allHistoryItems.filter { it.verdictType == VerdictType.GREEN || it.verdictType == VerdictType.CYAN }
            HistoryFilter.CAUTION -> currentState.allHistoryItems.filter { it.verdictType == VerdictType.YELLOW }
            HistoryFilter.UNSAFE -> currentState.allHistoryItems.filter { it.verdictType == VerdictType.RED }
        }
        _state.update { it.copy(displayedHistoryItems = filtered.toImmutableList()) }
    }

    private fun mapScansToUi(scans: List<ScanHistoryEntry>): List<HistoryItemUiModel> {
        return scans.map { entry ->
            val verdictType = when (entry.verdict) {
                ProductVerdict.SAFE -> VerdictType.GREEN
                ProductVerdict.CAUTION -> VerdictType.YELLOW
                ProductVerdict.UNSAFE -> VerdictType.RED
                null -> VerdictType.CYAN
            }
            val verdictLabelResId = when (entry.verdict) {
                ProductVerdict.SAFE -> R.string.verdict_safe
                ProductVerdict.CAUTION -> R.string.verdict_caution
                ProductVerdict.UNSAFE -> R.string.verdict_unsafe
                null -> R.string.verdict_safe
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
                imageUrl = entry.imageUrl
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
