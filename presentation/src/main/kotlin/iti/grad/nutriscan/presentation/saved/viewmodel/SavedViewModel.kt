package iti.grad.nutriscan.presentation.saved.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.foodlog.usecase.AddFoodEntryUseCase
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
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val addFoodEntryUseCase: AddFoodEntryUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SavedState())
    val state: StateFlow<SavedState> = _state.asStateFlow()

    private val _effect = Channel<SavedEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadMockData()
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
                    addToFoodLog(product)
                }
            }
        }
    }

    private fun addToFoodLog(product: ProductUiModel) {
        viewModelScope.launch {
            val entry = FoodLogEntry(
                id = UUID.randomUUID().toString(),
                productId = product.id,
                name = product.productName,
                calories = product.calories.toIntOrNull() ?: 0,
                imageUrl = product.imageUrl,
                verdict = product.verdict,
                loggedDate = LocalDate.now(),
                addedAt = Instant.now(),
            )
            addFoodEntryUseCase(entry)
                .onSuccess { _effect.send(SavedEffect.ShowAddedToFoodLogSnackbar(product.productName)) }
                .onFailure { _effect.send(SavedEffect.ShowAddErrorSnackbar) }
        }
    }

    private fun emitEffect(effect: SavedEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    private fun loadMockData() {
        val mockProducts = listOf(
            ProductUiModel(
                id = "1",
                productName = "Almarai Milk Full Fat",
                imageUrl = "https://images.unsplash.com/photo-1550583724-b2692b85b150?auto=format&fit=crop&w=400&q=80",
                verdict = ProductVerdict.SAFE,
                calories = "150"
            ),
            ProductUiModel(
                id = "2",
                productName = "Peanut Butter",
                imageUrl = "https://images.unsplash.com/photo-1588195538326-c5b1e9f80a1b?auto=format&fit=crop&w=400&q=80",
                verdict = ProductVerdict.CAUTION,
                calories = "190"
            ),
            ProductUiModel(
                id = "3",
                productName = "Chocolate Bar",
                imageUrl = "https://images.unsplash.com/photo-1549007994-cb92caebd54b?auto=format&fit=crop&w=400&q=80",
                verdict = ProductVerdict.UNSAFE,
                calories = "220"
            ),
            ProductUiModel(
                id = "4",
                productName = "Oatmeal Cookies",
                imageUrl = "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?auto=format&fit=crop&w=400&q=80",
                verdict = ProductVerdict.SAFE,
                calories = "120"
            ),
            ProductUiModel(
                id = "5",
                productName = "Energy Drink",
                imageUrl = "https://images.unsplash.com/photo-1622543925917-763c34d1a86e?auto=format&fit=crop&w=400&q=80",
                verdict = ProductVerdict.UNSAFE,
                calories = "110"
            ),
            ProductUiModel(
                id = "6",
                productName = "Greek Yogurt",
                imageUrl = "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=400&q=80",
                verdict = ProductVerdict.SAFE,
                calories = "90"
            )
        ).toImmutableList()

        _state.update {
            it.copy(
                products = mockProducts,
                filteredProducts = mockProducts
            )
        }
    }
}
