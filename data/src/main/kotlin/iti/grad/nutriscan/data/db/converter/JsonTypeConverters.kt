package iti.grad.nutriscan.data.db.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class JsonTypeConverters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return Json.encodeToString(value ?: emptyList())
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return value?.let {
            try { Json.decodeFromString<List<String>>(it) } catch (_: Exception) { emptyList() }
        } ?: emptyList()
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String {
        return Json.encodeToString(value ?: emptyMap())
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> {
        return value?.let {
            try { Json.decodeFromString<Map<String, String>>(it) } catch (_: Exception) { emptyMap() }
        } ?: emptyMap()
    }

    @TypeConverter
    fun fromStringListMap(value: Map<String, List<String>>?): String {
        return Json.encodeToString(value ?: emptyMap())
    }

    @TypeConverter
    fun toStringListMap(value: String?): Map<String, List<String>> {
        return value?.let {
            try { Json.decodeFromString<Map<String, List<String>>>(it) } catch (_: Exception) { emptyMap() }
        } ?: emptyMap()
    }
}
