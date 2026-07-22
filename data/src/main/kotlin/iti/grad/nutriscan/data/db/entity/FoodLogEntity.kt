package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_log")
data class FoodLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val productId: String?,
    val name: String,
    val calories: Int,
    val imageUrl: String?,
    val verdict: String,
    val loggedDate: String,
    val addedAtEpochMillis: Long,
)
