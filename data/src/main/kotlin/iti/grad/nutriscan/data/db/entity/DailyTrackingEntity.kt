package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity

@Entity(tableName = "daily_tracking", primaryKeys = ["userId", "date"])
data class DailyTrackingEntity(
    val userId: String,
    val date: String,
    val targetWaterCnt: Int,
    val waterCnt: Int,
    val stepsCnt: Int,
    val caloriesBurnedSteps: Int,
    val syncedToBackend: Boolean,
)
