package iti.grad.nutriscan.data.repository.mapper

import iti.grad.nutriscan.data.db.entity.FoodLogEntity
import iti.grad.nutriscan.domain.common.CairoDateProvider
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import java.time.Instant
import java.time.LocalDate

fun FoodLogEntity.toDomain(): FoodLogEntry = FoodLogEntry(
    id = id,
    productId = productId,
    name = name,
    calories = calories,
    imageUrl = imageUrl,
    verdict = runCatching { ProductVerdict.valueOf(verdict) }.getOrDefault(ProductVerdict.SAFE),
    loggedDate = LocalDate.parse(loggedDate),
    addedAt = Instant.ofEpochMilli(addedAtEpochMillis),
    mealCnt = mealCnt,
)

fun FoodLogEntry.toEntity(userId: String): FoodLogEntity = FoodLogEntity(
    id = id,
    userId = userId,
    productId = productId,
    name = name,
    calories = calories,
    imageUrl = imageUrl,
    verdict = verdict.name,
    loggedDate = loggedDate.toString(),
    addedAtEpochMillis = addedAt.toEpochMilli(),
    mealCnt = mealCnt,
)

fun today(): LocalDate = CairoDateProvider.today()
