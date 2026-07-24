package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_log")
data class WorkoutLogEntity(
    @PrimaryKey val date: String,
    val done: Boolean,
)
