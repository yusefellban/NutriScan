package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached family member row, scoped to [ownerUserId] so a device switching
 * accounts (or a destructive migration) doesn't leak members across users.
 * Reuses the existing [iti.grad.nutriscan.data.db.converter.IntListConverter]
 * already registered on [iti.grad.nutriscan.data.db.NutriScanDatabase] — no
 * new TypeConverter needed.
 */
@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey
    val id: String,
    val ownerUserId: String,
    val name: String,
    val relation: String = "",
    val allergyIds: List<Int> = emptyList(),
    val diseaseIds: List<Int> = emptyList(),
)
