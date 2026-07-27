package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_log")
data class WaterLogEntity(
    @PrimaryKey val date: String,
    val glassCount: Int,
    val goalGlasses: Int,
)
