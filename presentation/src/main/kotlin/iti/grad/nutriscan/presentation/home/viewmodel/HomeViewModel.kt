package iti.grad.nutriscan.presentation.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.scan.model.ScanHistoryEntry
import iti.grad.nutriscan.domain.dailytracking.usecase.ReconcileTodayUseCase
import iti.grad.nutriscan.domain.scan.usecase.GetRecentScansUseCase
import iti.grad.nutriscan.domain.streak.usecase.SyncDailyStreakUseCase
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import iti.grad.nutriscan.presentation.common.model.UiText
import iti.grad.nutriscan.presentation.home.state.HomeEffect
import iti.grad.nutriscan.presentation.home.state.HomeEvent
import iti.grad.nutriscan.presentation.common.model.HistoryItemUiModel
import iti.grad.nutriscan.presentation.common.model.VerdictType
import iti.grad.nutriscan.presentation.home.state.HomeState
import iti.grad.presentation.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    private val getRecentScansUseCase: GetRecentScansUseCase,
    private val reconcileTodayUseCase: ReconcileTodayUseCase,
    private val syncDailyStreakUseCase: SyncDailyStreakUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadRecentScans()

        viewModelScope.launch {
            reconcileTodayUseCase()
        }

        viewModelScope.launch {
            syncDailyStreakUseCase()
        }

        viewModelScope.launch {
            userRepository.getUserData().collectLatest { user ->
                if (user != null) {
                    _state.update {
                        it.copy(
                            firstName = user.firstName,
                            userName = "${user.firstName} ${user.lastName ?: ""}".trim(),
                            avatarUrl = user.avatarUrl,
                            avatarUpdatedAt = user.updatedAt
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.ScanCardClicked -> emitEffect(HomeEffect.NavigateToScan)
            is HomeEvent.ViewAllHistoryClicked -> emitEffect(HomeEffect.NavigateToHistory)
            is HomeEvent.NotificationClicked -> emitEffect(HomeEffect.NavigateToNotifications)
            is HomeEvent.AvatarClicked -> emitEffect(HomeEffect.NavigateToEditProfile)
            is HomeEvent.HistoryItemClicked -> emitEffect(
                HomeEffect.NavigateToScanResult(event.itemId)
            )
            is HomeEvent.HealthNewsClicked -> emitEffect(HomeEffect.NavigateToNews)
            is HomeEvent.ChatWithAiClicked -> emitEffect(HomeEffect.NavigateToChatWithAi)
            is HomeEvent.RetryLoadHistory -> loadRecentScans()
            is HomeEvent.RefreshHistorySilently -> refreshHistorySilently()
            is HomeEvent.Refreshed -> refresh()
        }
    }

    private fun loadRecentScans() {
        viewModelScope.launch {
            _state.update { it.copy(isHistoryLoading = true, historyError = null) }
            
            getRecentScansUseCase(page = 0, size = 3)
                .onSuccess { scans ->
                    _state.update { state ->
                        state.copy(
                            isHistoryLoading = false,
                            recentHistory = mapScansToUi(scans)
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(
                            isHistoryLoading = false, 
                            historyError = error.message ?: "Failed to load recent scans"
                        ) 
                    }
                }
        }
    }

    /** Pull-to-refresh: re-pulls both halves of the feed — the scan list and today's tracking —
     * since the profile header and greeting come from whatever the reconcile writes back. */
    private fun refresh() {
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            reconcileTodayUseCase()
            getRecentScansUseCase(page = 0, size = 3)
                .onSuccess { scans ->
                    _state.update { it.copy(recentHistory = mapScansToUi(scans), historyError = null) }
                }
                .onFailure { error ->
                    _state.update { it.copy(historyError = error.message ?: "Failed to load recent scans") }
                }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun refreshHistorySilently() {
        viewModelScope.launch {
            getRecentScansUseCase(page = 0, size = 3)
                .onSuccess { scans ->
                    _state.update { state ->
                        state.copy(
                            recentHistory = mapScansToUi(scans)
                        )
                    }
                }
        }
    }

    private fun mapScansToUi(scans: List<ScanHistoryEntry>): ImmutableList<HistoryItemUiModel> {
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
        }.toImmutableList()
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

    private fun emitEffect(effect: HomeEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun createInitialState(): HomeState = HomeState()
}
