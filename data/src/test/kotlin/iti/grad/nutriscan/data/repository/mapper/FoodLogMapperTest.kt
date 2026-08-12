package iti.grad.nutriscan.data.repository.mapper

import iti.grad.nutriscan.data.db.entity.FoodLogEntity
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class FoodLogMapperTest {

    @Test
    fun `toDomain carries mealCnt through`() {
        val entity = FoodLogEntity(
            id = "entry-1",
            userId = "user-1",
            productId = "product-1",
            name = "Apple",
            calories = 95,
            imageUrl = null,
            verdict = "SAFE",
            loggedDate = "2026-07-30",
            addedAtEpochMillis = 0L,
            mealCnt = 3,
        )

        assertEquals(3, entity.toDomain().mealCnt)
    }

    @Test
    fun `toEntity defaults backendCreated to false regardless of mealCnt`() {
        val entry = FoodLogEntry(
            id = "entry-1",
            productId = "product-1",
            name = "Apple",
            calories = 95,
            imageUrl = null,
            verdict = ProductVerdict.SAFE,
            loggedDate = LocalDate.parse("2026-07-30"),
            addedAt = Instant.EPOCH,
            mealCnt = 2,
        )

        val entity = entry.toEntity("user-1")

        assertEquals(2, entity.mealCnt)
        assertEquals(false, entity.backendCreated)
    }
}
