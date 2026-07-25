package iti.grad.nutriscan.data.db.converter

import androidx.room.TypeConverter
import iti.grad.nutriscan.data.db.entity.FamilyMemberEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FamilyMemberListConverter {
    @TypeConverter
    fun fromFamilyMemberList(value: List<FamilyMemberEntity>?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toFamilyMemberList(value: String?): List<FamilyMemberEntity>? {
        return value?.let { Json.decodeFromString(it) }
    }
}
