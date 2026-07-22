package iti.grad.nutriscan.presentation.product_details.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.scan.model.FlaggedIngredient
import iti.grad.nutriscan.domain.scan.model.ProductDetail
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
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class ProductDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val productJson: String = checkNotNull(savedStateHandle["product"])
    private val product: ProductUiModel = Json.decodeFromString(productJson)

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
                _state.update { currentState ->
                    val current = currentState.productDetail ?: return@update currentState
                    currentState.copy(
                        productDetail = current.copy(isBookmarked = !current.isBookmarked)
                    )
                }
            }
        }
    }

    /**
     * Mock data — will be replaced with a real use case once the backend
     * exposes full product details with ingredient analysis.
     */
    private fun loadProductDetail() {
        val mockProducts = buildMockCatalog()
        val detail = mockProducts[product.id] ?: buildFallbackDetail(product)

        _state.update {
            it.copy(
                isLoading = false,
                productDetail = detail,
            )
        }
    }

    private fun buildMockCatalog(): Map<String, ProductDetail> = mapOf(
        "1" to ProductDetail(
            id = "1",
            productName = "Almarai Milk Full Fat",
            brand = "Almarai",
            imageUrl = "https://images.unsplash.com/photo-1550583724-b2692b85b150?auto=format&fit=crop&w=800&q=80",
            verdict = ProductVerdict.UNSAFE,
            scanDate = LocalDate.of(2026, 7, 13),
            safetyReasonText = "Contains hazelnuts and milk, both of which match allergies on your profile.",
            flaggedIngredients = listOf(
                FlaggedIngredient(
                    name = "Hazelnuts",
                    matchTag = "Tree Nuts Allergy",
                    reason = "Matches allergy in your profile",
                ),
                FlaggedIngredient(
                    name = "Skimmed Milk Powder",
                    matchTag = "Lactose Intolerance",
                    reason = "Contains milk ingredient",
                ),
            ),
            calories = "80 kcal",
            servingSize = "15 g",
            sugar = "8.5 g",
            fat = "4.5 g",
            saturatedFat = "1.6 g",
            isBookmarked = true,
        ),
        "2" to ProductDetail(
            id = "2",
            productName = "Peanut Butter",
            brand = "Skippy",
            imageUrl = "https://images.unsplash.com/photo-1588195538326-c5b1e9f80a1b?auto=format&fit=crop&w=800&q=80",
            verdict = ProductVerdict.CAUTION,
            scanDate = LocalDate.of(2026, 7, 15),
            safetyReasonText = "Contains peanuts which may trigger allergic reaction. High calorie content.",
            flaggedIngredients = listOf(
                FlaggedIngredient(
                    name = "Peanuts",
                    matchTag = "Peanut Allergy",
                    reason = "Matches allergy in your profile",
                ),
            ),
            calories = "190 kcal",
            servingSize = "32 g",
            sugar = "3 g",
            fat = "16 g",
            saturatedFat = "3.3 g",
            isBookmarked = false,
        ),
        "3" to ProductDetail(
            id = "3",
            productName = "Chocolate Bar",
            brand = "Cadbury",
            imageUrl = "https://images.unsplash.com/photo-1549007994-cb92caebd54b?auto=format&fit=crop&w=800&q=80",
            verdict = ProductVerdict.UNSAFE,
            scanDate = LocalDate.of(2026, 7, 18),
            safetyReasonText = "Contains milk solids and soy lecithin, both of which match conditions on your profile.",
            flaggedIngredients = listOf(
                FlaggedIngredient(
                    name = "Milk Solids",
                    matchTag = "Lactose Intolerance",
                    reason = "Contains dairy ingredient",
                ),
                FlaggedIngredient(
                    name = "Soy Lecithin",
                    matchTag = "Soy Allergy",
                    reason = "Matches allergy in your profile",
                ),
            ),
            calories = "220 kcal",
            servingSize = "40 g",
            sugar = "22 g",
            fat = "13 g",
            saturatedFat = "7.5 g",
            isBookmarked = false,
        ),
        "4" to ProductDetail(
            id = "4",
            productName = "Oatmeal Cookies",
            brand = "Nature Valley",
            imageUrl = "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?auto=format&fit=crop&w=800&q=80",
            verdict = ProductVerdict.SAFE,
            scanDate = LocalDate.of(2026, 7, 20),
            safetyReasonText = null,
            flaggedIngredients = emptyList(),
            calories = "120 kcal",
            servingSize = "30 g",
            sugar = "6 g",
            fat = "5 g",
            saturatedFat = "0.8 g",
            isBookmarked = true,
        ),
        "5" to ProductDetail(
            id = "5",
            productName = "Energy Drink",
            brand = "Red Bull",
            imageUrl = "https://images.unsplash.com/photo-1622543925917-763c34d1a86e?auto=format&fit=crop&w=800&q=80",
            verdict = ProductVerdict.UNSAFE,
            scanDate = LocalDate.of(2026, 7, 21),
            safetyReasonText = "High sugar content — exceeds daily limit for Diabetes condition on your profile.",
            flaggedIngredients = listOf(
                FlaggedIngredient(
                    name = "Sugar (27g)",
                    matchTag = "Diabetes",
                    reason = "Exceeds safe sugar intake",
                ),
            ),
            calories = "110 kcal",
            servingSize = "250 ml",
            sugar = "27 g",
            fat = "0 g",
            saturatedFat = "0 g",
            isBookmarked = false,
        ),
        "6" to ProductDetail(
            id = "6",
            productName = "Greek Yogurt",
            brand = "Fage",
            imageUrl = "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=800&q=80",
            verdict = ProductVerdict.SAFE,
            scanDate = LocalDate.of(2026, 7, 22),
            safetyReasonText = null,
            flaggedIngredients = emptyList(),
            calories = "90 kcal",
            servingSize = "170 g",
            sugar = "4 g",
            fat = "0.7 g",
            saturatedFat = "0.3 g",
            isBookmarked = true,
        ),
    )
    private fun buildFallbackDetail(uiModel: iti.grad.nutriscan.presentation.common.model.ProductUiModel): ProductDetail = ProductDetail(
        id = uiModel.id,
        productName = uiModel.productName,
        brand = "Unknown Brand",
        imageUrl = uiModel.imageUrl,
        verdict = uiModel.verdict,
        scanDate = LocalDate.now(),
        safetyReasonText = "No specific match found.",
        flaggedIngredients = emptyList(),
        calories = uiModel.calories,
        servingSize = "1 serving",
        sugar = "0 g",
        fat = "0 g",
        saturatedFat = "0 g",
        isBookmarked = false,
    )
}
