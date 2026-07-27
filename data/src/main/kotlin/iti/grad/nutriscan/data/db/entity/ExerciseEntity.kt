package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import iti.grad.nutriscan.data.db.converter.JsonTypeConverters

@Entity(tableName = "exercises")
@TypeConverters(JsonTypeConverters::class)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val bodyPart: String,
    val equipment: String,
    val target: String,
    val secondaryMuscles: List<String>,
    val instructions: Map<String, String>,
    val instructionSteps: Map<String, List<String>>,
    val imageUrl: String?,
    val gifUrl: String?,
    val repKcal: Double?,
    val minKcal: Double?,
)
