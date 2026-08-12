package iti.grad.nutriscan.presentation.product_details.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.scan.model.FlaggedIngredient
import iti.grad.nutriscan.domain.scan.model.FoodSafetyResponse
import iti.grad.nutriscan.domain.scan.model.NutritionFacts
import iti.grad.nutriscan.domain.scan.model.ProductDetail
import iti.grad.nutriscan.domain.scan.model.ScanFlaggedIngredient
import iti.grad.nutriscan.domain.scan.model.ScanResult
import iti.grad.nutriscan.domain.scan.model.ScanStatus
import iti.grad.nutriscan.domain.scan.usecase.DeleteSavedScanUseCase
import iti.grad.nutriscan.domain.scan.usecase.GetSavedScanByIdUseCase
import iti.grad.nutriscan.domain.scan.usecase.SaveScanUseCase
import iti.grad.nutriscan.presentation.product_details.state.ProductDetailsEffect
import iti.grad.nutriscan.presentation.product_details.state.ProductDetailsEvent
import iti.grad.nutriscan.presentation.product_details.state.ProductDetailsState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ProductDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val saveScanUseCase: SaveScanUseCase,
    private val deleteSavedScanUseCase: DeleteSavedScanUseCase,
    private val getSavedScanByIdUseCase: GetSavedScanByIdUseCase,
    private val getScanResultUseCase: iti.grad.nutriscan.domain.scan.usecase.GetScanResultUseCase,
) : ViewModel() {

    private val scanId: String = checkNotNull(savedStateHandle["scanId"])

    private val _state = MutableStateFlow(ProductDetailsState())
    val state: StateFlow<ProductDetailsState> = _state.asStateFlow()

    private val _effect = Channel<ProductDetailsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadProductDetail()
    }

    fun onEvent(event: ProductDetailsEvent) {
        when (event) {
            is ProductDetailsEvent.BackClicked -> {
                viewModelScope.launch {
                    _effect.send(ProductDetailsEffect.NavigateBack)
                }
            }

            is ProductDetailsEvent.BookmarkToggled -> {
                val detail = _state.value.productDetail ?: return
                if (detail.isBookmarked) {
                    _state.update { it.copy(showDeleteDialog = true) }
                } else {
                    viewModelScope.launch {
                        val scanResult = mapToScanResult(detail)
                        saveScanUseCase(scanResult)
                        _state.update { currentState ->
                            currentState.copy(
                                productDetail = currentState.productDetail?.copy(isBookmarked = true)
                            )
                        }
                    }
                }
            }

            is ProductDetailsEvent.ConfirmDeleteBookmark -> {
                val detail = _state.value.productDetail ?: return
                viewModelScope.launch {
                    deleteSavedScanUseCase(detail.id)
                    _state.update { currentState ->
                        currentState.copy(
                            showDeleteDialog = false,
                            productDetail = currentState.productDetail?.copy(isBookmarked = false)
                        )
                    }
                }
            }

            is ProductDetailsEvent.DismissDeleteBookmark -> {
                _state.update { it.copy(showDeleteDialog = false) }
            }
        }
    }

    private fun loadProductDetail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val savedScan = getSavedScanByIdUseCase(scanId).getOrNull()
            
            if (savedScan != null) {
                val detail = mapToProductDetail(savedScan).copy(isBookmarked = true)
                _state.update { it.copy(isLoading = false, productDetail = detail) }
            } else {
                val apiResult = getScanResultUseCase(scanId).getOrNull()
                if (apiResult != null) {
                    val detail = mapToProductDetail(apiResult).copy(isBookmarked = false)
                    _state.update { it.copy(isLoading = false, productDetail = detail) }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Failed to load product details") }
                }
            }
        }
    }

    private fun mapToProductDetail(scanResult: ScanResult): ProductDetail {
        return ProductDetail(
            id = scanResult.scanId,
            productName = scanResult.productName ?: "Unknown Product",
            brand = "Unknown Brand",
            imageUrl = scanResult.imageUrl ?: "",
            status = scanResult.status,
            verdict = scanResult.foodSafetyResponse?.verdict ?: ProductVerdict.SAFE,
            scanDate = scanResult.scannedAt?.let { 
                try { LocalDate.parse(it.substringBefore("T")) } catch (e: Exception) { LocalDate.now() } 
            } ?: LocalDate.now(),
            safetyReasonText = scanResult.foodSafetyResponse?.summary,
            flaggedIngredients = scanResult.foodSafetyResponse?.flaggedIngredients?.map {
                FlaggedIngredient(
                    name = it.ingredient,
                    matchTag = it.type,
                    reason = it.reason
                )
            } ?: emptyList(),
            calories = scanResult.nutritionFacts?.calories?.toString() ?: "0",
            servingSize = "1",
            sugar = scanResult.nutritionFacts?.sugarG?.toString() ?: "0",
            fat = scanResult.nutritionFacts?.fatG?.toString() ?: "0",
            saturatedFat = "0",
            isBookmarked = true
        )
    }

    private fun mapToScanResult(detail: ProductDetail): ScanResult {
        return ScanResult(
            scanId = detail.id,
            status = detail.status,
            scannedAt = java.time.Instant.now().toString(),
            imageUrl = detail.imageUrl,
            productName = detail.productName,
            foodSafetyResponse = FoodSafetyResponse(
                verdict = detail.verdict,
                summary = detail.safetyReasonText,
                flaggedIngredients = detail.flaggedIngredients.map {
                    ScanFlaggedIngredient(
                        ingredient = it.name,
                        reason = it.reason,
                        type = it.matchTag,
                        name = listOf(it.name)
                    )
                }
            ),
            nutritionFacts = NutritionFacts(
                calories = detail.calories?.split(" ")?.firstOrNull()?.toLongOrNull() ?: 0L,
                sugarG = detail.sugar?.split(" ")?.firstOrNull()?.toFloatOrNull() ?: 0f,
                fatG = detail.fat?.split(" ")?.firstOrNull()?.toFloatOrNull() ?: 0f,
                proteinGrams = 0f, carbsGrams = 0f, fiberGrams = 0f, sodiumMg = 0f
            )
        )
    }
}
