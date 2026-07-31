package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Keyed by [userId], not a fixed row id. This was previously `@PrimaryKey val id: Int = 0`, so the
 * whole device shared a single streak row: every account that signed in read and overwrote the same
 * one, and switching accounts showed the previous user's streak.
 */
@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey val userId: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastActiveDate: String?,
)
