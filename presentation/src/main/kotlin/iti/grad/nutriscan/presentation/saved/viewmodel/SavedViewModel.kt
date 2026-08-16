package iti.grad.nutriscan.presentation.saved.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.common.CairoDateProvider
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.foodlog.usecase.AddFoodEntryUseCase
import iti.grad.nutriscan.domain.scan.model.ScanStatus
import iti.grad.nutriscan.domain.scan.usecase.GetSavedScansUseCase
import iti.grad.nutriscan.domain.scan.usecase.RefreshSavedScansUseCase
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import iti.grad.nutriscan.presentation.saved.state.SavedEffect
import iti.grad.nutriscan.presentation.saved.state.SavedEvent
import iti.grad.nutriscan.presentation.saved.state.SavedState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val addFoodEntryUseCase: AddFoodEntryUseCase,
    private val getSavedScansUseCase: GetSavedScansUseCase,
    private val refreshSavedScansUseCase: RefreshSavedScansUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SavedState())
    val state: StateFlow<SavedState> = _state.asStateFlow()

    private val _effect = Channel<SavedEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadSavedScans()
    }

    fun onEvent(event: SavedEvent) {
        when (event) {
            is SavedEvent.SearchQueryChanged -> {
                _state.update { currentState ->
                    val filtered = if (event.query.isBlank()) {
                        currentState.products
                    } else {
                        currentState.products.filter {
                            it.productName.contains(event.query, ignoreCase = true)
                        }.toImmutableList()
                    }
                    currentState.copy(
                        searchQuery = event.query,
                        filteredProducts = filtered
                    )
                }
            }
            is SavedEvent.ProductClicked -> {
                viewModelScope.launch {
                    _effect.send(SavedEffect.NavigateToProductDetail(event.product))
                }
            }
            is SavedEvent.SwipeToAddTriggered -> {
                val product = _state.value.products.find { it.id == event.productId }
                if (product != null) {
                    addToFoodLog(product, event.onResult)
                }
            }
            is SavedEvent.RetryLoad -> {
                loadSavedScans()
            }
            is SavedEvent.Refreshed -> refresh()
        }
    }

    private fun addToFoodLog(product: ProductUiModel, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val entry = FoodLogEntry(
                id = UUID.randomUUID().toString(),
                productId = product.id,
                name = product.productName,
                calories = product.calories.toIntOrNull() ?: 0,
                imageUrl = product.imageUrl,
                verdict = product.verdict,
                loggedDate = CairoDateProvider.today(),
                addedAt = java.time.Instant.now(),
            )
            addFoodEntryUseCase(entry)
                .onSuccess {
                    _effect.send(SavedEffect.ShowAddedToFoodLogSnackbar(product.productName))
                    onResult(true)
                }
                .onFailure {
                    _effect.send(SavedEffect.ShowAddErrorSnackbar)
                    onResult(false)
                }
        }
    }

    /** Re-collecting the Room flow would leak a second collector, so refresh only re-pulls the
     * backend — Room's own flow pushes whatever changed straight into the list. */
    private fun refresh() {
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            refreshSavedScansUseCase()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun emitEffect(effect: SavedEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    private fun loadSavedScans() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getSavedScansUseCase().collect { scans ->
                val uiModels = scans.map { scan ->
                    ProductUiModel(
                        id = scan.scanId,
                        productName = scan.productName ?: "",
                        imageUrl = scan.imageUrl,
                        verdict = scan.foodSafetyResponse?.verdict ?: ProductVerdict.SAFE,
                        calories = scan.nutritionFacts?.calories?.toString() ?: "0",
                        isFailed = scan.status == ScanStatus.FAILED,
                    )
                }.toImmutableList()

                _state.update { currentState ->
                    val filtered = if (currentState.searchQuery.isBlank()) {
                        uiModels
                    } else {
                        uiModels.filter {
                            it.productName.contains(currentState.searchQuery, ignoreCase = true)
                        }.toImmutableList()
                    }
                    currentState.copy(
                        products = uiModels,
                        filteredProducts = filtered,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }
}
