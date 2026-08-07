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
    /** True while a POST/PUT/DELETE meal sync to the backend is still owed. */
    val pendingSync: Boolean = false,
    /** Soft-delete tombstone: true means the user removed this locally but the
     * backend DELETE hasn't been confirmed yet — kept out of observeByUserAndDate
     * results, physically removed once the sync succeeds. */
    val deleted: Boolean = false,
    /** True server-side quantity for this product on this day — one row per (userId,
     * loggedDate, productId) now, incremented/decremented in place instead of one row per add.
     * Defaults to 1 so a fresh add starts life the same as before this column existed. */
    val mealCnt: Int = 1,
    /** True once a POST addMeal for this row has succeeded — tells the sync retry loop
     * (DailyTrackingSyncEngine) whether a pending push should replay as POST or PUT. */
    val backendCreated: Boolean = false,
)
