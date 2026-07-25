package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors the Exercises API response exactly (see
 * `Exercises_API__Production__postman_collection.json`). `instructions` /
 * `instructionSteps` are locale-keyed maps because the API returns
 * en/it/tr/es/ru/zh/hi/pl/ko/fr — no `ar` — the `en` value is picked out at the
 * repository mapping boundary (`ExercisesRepositoryImpl.toDomain`).
 */
@Serializable
data class ExerciseDto(
    val id: String,
    val name: String,
    val category: String,
    @SerialName("body_part") val bodyPart: String,
    val equipment: String,
    val instructions: Map<String, String>? = null,
    @SerialName("instruction_steps") val instructionSteps: Map<String, List<String>>? = null,
    @SerialName("secondary_muscles") val secondaryMuscles: List<String>? = null,
    val target: String? = null,
    @SerialName("rep_kcal") val repKcal: Double? = null,
    @SerialName("min_kcal") val minKcal: Double? = null,
    val image: String? = null,
    @SerialName("gif_url") val gifUrl: String? = null,
)

@Serializable
data class ExercisesResponseDto(
    val success: Boolean,
    val meta: ExercisesMetaDto? = null,
    val data: List<ExerciseDto> = emptyList(),
)

@Serializable
data class ExercisesMetaDto(val pagination: PaginationDto)

@Serializable
data class PaginationDto(
    @SerialName("current_page") val currentPage: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("has_next") val hasNext: Boolean,
)

@Serializable
data class SingleExerciseResponseDto(val success: Boolean, val data: ExerciseDto)

@Serializable
data class CategoriesResponseDto(val success: Boolean, val data: List<String> = emptyList())
