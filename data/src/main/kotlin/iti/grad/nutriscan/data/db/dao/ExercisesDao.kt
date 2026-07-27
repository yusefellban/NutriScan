package iti.grad.nutriscan.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import iti.grad.nutriscan.data.db.entity.ExerciseCategoryEntity
import iti.grad.nutriscan.data.db.entity.ExerciseEntity

@Dao
interface ExercisesDao {
    @Query("""
        SELECT * FROM exercises 
        WHERE (:bodyPart IS NULL OR bodyPart = :bodyPart) 
          AND (:searchQuery IS NULL OR name LIKE '%' || :searchQuery || '%') 
        ORDER BY name 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getExercisesPaginated(
        bodyPart: String?,
        searchQuery: String?,
        limit: Int,
        offset: Int
    ): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: String): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Query("DELETE FROM exercises")
    suspend fun clearExercises()

    @Query("SELECT name FROM exercise_categories ORDER BY name")
    suspend fun getCategories(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<ExerciseCategoryEntity>)

    @Query("DELETE FROM exercise_categories")
    suspend fun clearCategories()
}
