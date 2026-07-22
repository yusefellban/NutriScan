package iti.grad.nutriscan.data.db.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class IntListConverter {
    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        return value?.let { Json.decodeFromString(it) }
    }
}
