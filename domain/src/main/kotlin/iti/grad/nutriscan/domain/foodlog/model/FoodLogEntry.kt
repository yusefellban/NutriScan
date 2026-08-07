package iti.grad.nutriscan.domain.foodlog.model

import iti.grad.nutriscan.domain.common.model.ProductVerdict
import java.time.Instant
import java.time.LocalDate

data class FoodLogEntry(
    val id: String,
    val productId: String?,
    val name: String,
    val calories: Int,
    val imageUrl: String?,
    val verdict: ProductVerdict,
    val loggedDate: LocalDate,
    val addedAt: Instant,
    val mealCnt: Int = 1,
)
