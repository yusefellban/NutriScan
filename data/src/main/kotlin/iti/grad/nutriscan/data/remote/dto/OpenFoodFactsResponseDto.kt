package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenFoodFactsResponseDto(
    @SerialName("code") val code: String? = null,
    @SerialName("product") val product: OpenFoodFactsProductDto? = null,
    @SerialName("status") val status: Int? = null
)

@Serializable
data class OpenFoodFactsProductDto(
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_name_fr") val productNameFr: String? = null,
    @SerialName("image_front_small_url") val imageFrontSmallUrl: String? = null,
    @SerialName("brands") val brands: String? = null,
    @SerialName("ecoscore_grade") val ecoscoreGrade: String? = null,
    @SerialName("nutrition_grades_tags") val nutritionGradesTags: List<String>? = null
)
