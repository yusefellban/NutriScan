package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Temporary DummyEntity to satisfy Room compilation requirements.
@Entity(tableName = "dummy_table")
data class DummyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dummyField: String
)
