package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.ExercisesDao
import iti.grad.nutriscan.data.remote.api.ExercisesApiService
import iti.grad.nutriscan.data.remote.dto.CategoriesResponseDto
import iti.grad.nutriscan.data.remote.dto.ExerciseDto
import iti.grad.nutriscan.data.remote.dto.ExercisesMetaDto
import iti.grad.nutriscan.data.remote.dto.ExercisesResponseDto
import iti.grad.nutriscan.data.remote.dto.PaginationDto
import iti.grad.nutriscan.data.remote.dto.SingleExerciseResponseDto
import iti.grad.nutriscan.domain.exercises.model.ExerciseQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExercisesRepositoryImplTest {

    private val api = mockk<ExercisesApiService>()
    private val dao = mockk<ExercisesDao>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ExercisesRepositoryImpl

    @BeforeEach
    fun setup() {
        repository = ExercisesRepositoryImpl(api, dao, testDispatcher)
    }

    @Test
    fun `getExercises parses responses successfully with fallback locale and default calories`() = runTest(testDispatcher) {
        val mockDto = ExerciseDto(
            id = "1",
            name = "Push Up",
            category = "strength",
            bodyPart = "chest",
            equipment = "body only",
            instructions = mapOf("en" to "Instructions in English", "tr" to "Instructions in Turkish"),
            instructionSteps = mapOf("en" to listOf("Step 1", "Step 2")),
            secondaryMuscles = listOf("triceps"),
            target = "pectorals",
            repKcal = null, // Trigger default fallback
            minKcal = null, // Trigger default fallback
            image = "image_url",
            gifUrl = "gif_url"
        )
        
        coEvery { api.getExercises(any(), any(), any(), any()) } returns ExercisesResponseDto(
            success = true,
            meta = ExercisesMetaDto(
                pagination = PaginationDto(
                    currentPage = 1,
                    totalPages = 5,
                    hasNext = true
                )
            ),
            data = listOf(mockDto)
        )

        val result = repository.getExercises(ExerciseQuery())
        Assertions.assertTrue(result.isSuccess)
        
        val page = result.getOrThrow()
        Assertions.assertEquals(1, page.currentPage)
        Assertions.assertEquals(5, page.totalPages)
        Assertions.assertTrue(page.hasNext)
        
        val exercise = page.exercises.first()
        Assertions.assertEquals("1", exercise.id)
        Assertions.assertEquals("Instructions in English", exercise.instructions)
        Assertions.assertEquals("Step 1", exercise.instructionSteps.first())
        Assertions.assertEquals(0.20, exercise.repKcal) // DEFAULT_REP_KCAL fallback
        Assertions.assertEquals(0.15, exercise.minKcal) // DEFAULT_MIN_KCAL fallback
        Assertions.assertEquals("https://exercises-dataset-mu.vercel.app/image_url", exercise.imageUrl)
        Assertions.assertEquals("https://exercises-dataset-mu.vercel.app/gif_url", exercise.gifUrl)
    }

    @Test
    fun `getExerciseById parses response successfully`() = runTest(testDispatcher) {
        val mockDto = ExerciseDto(
            id = "1",
            name = "Push Up",
            category = "strength",
            bodyPart = "chest",
            equipment = "body only",
            instructions = mapOf("en" to "Instructions in English"),
            instructionSteps = mapOf("en" to listOf("Step 1")),
            secondaryMuscles = listOf("triceps"),
            target = "pectorals",
            repKcal = 0.5,
            minKcal = 0.4,
            image = "image_url",
            gifUrl = "gif_url"
        )
        
        coEvery { api.getExerciseById("1") } returns SingleExerciseResponseDto(
            success = true,
            data = mockDto
        )

        val result = repository.getExerciseById("1")
        Assertions.assertTrue(result.isSuccess)
        
        val exercise = result.getOrThrow()
        Assertions.assertEquals("1", exercise.id)
        Assertions.assertEquals("Instructions in English", exercise.instructions)
        Assertions.assertEquals(0.5, exercise.repKcal)
        Assertions.assertEquals(0.4, exercise.minKcal)
    }

    @Test
    fun `getCategories returns list of categories`() = runTest(testDispatcher) {
        coEvery { api.getCategories() } returns CategoriesResponseDto(
            success = true,
            data = listOf("cardio", "strength")
        )

        val result = repository.getCategories()
        Assertions.assertTrue(result.isSuccess)
        Assertions.assertEquals(2, result.getOrThrow().size)
        Assertions.assertEquals("cardio", result.getOrThrow().first())
    }

    @Test
    fun `getExercises handles api exception and returns failure`() = runTest(testDispatcher) {
        coEvery { api.getExercises(any(), any(), any(), any()) } throws Exception("Api error")

        val result = repository.getExercises(ExerciseQuery())
        Assertions.assertTrue(result.isFailure)
        Assertions.assertEquals("Api error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getExercises falls back to local database when network fails`() = runTest(testDispatcher) {
        coEvery { api.getExercises(any(), any(), any(), any()) } throws java.io.IOException("Network error")
        
        val mockEntity = iti.grad.nutriscan.data.db.entity.ExerciseEntity(
            id = "1",
            name = "Push Up",
            category = "strength",
            bodyPart = "chest",
            equipment = "body only",
            instructions = "Instructions in English",
            instructionSteps = listOf("Step 1"),
            secondaryMuscles = listOf("triceps"),
            target = "pectorals",
            repKcal = 0.20,
            minKcal = 0.15,
            imageUrl = "image_url",
            gifUrl = "gif_url"
        )
        
        coEvery { dao.getExercisesPaginated(any(), any(), any(), any()) } returns listOf(mockEntity)

        val result = repository.getExercises(ExerciseQuery())

        Assertions.assertTrue(result.isSuccess)
        val page = result.getOrThrow()
        Assertions.assertEquals(1, page.exercises.size)
        val exercise = page.exercises.first()
        Assertions.assertEquals("1", exercise.id)
        Assertions.assertEquals("Push Up", exercise.name)
        Assertions.assertEquals("Instructions in English", exercise.instructions)
    }
}
