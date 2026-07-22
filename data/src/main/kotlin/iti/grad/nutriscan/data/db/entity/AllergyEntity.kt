package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "allergies")
data class AllergyEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val description: String?
)
