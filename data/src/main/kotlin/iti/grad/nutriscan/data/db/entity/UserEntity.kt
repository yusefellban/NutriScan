package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val firstName: String,
    val lastName: String? = null,
    val email: String,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val diseaseIds: List<Int> = emptyList(),
    val allergyIds: List<Int> = emptyList(),
    val avatarUrl: String? = null
)
