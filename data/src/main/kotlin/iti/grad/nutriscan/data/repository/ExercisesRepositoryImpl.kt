package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.data.db.dao.ExercisesDao
import iti.grad.nutriscan.data.db.entity.ExerciseEntity
import iti.grad.nutriscan.data.db.entity.ExerciseCategoryEntity
import iti.grad.nutriscan.data.remote.api.ExercisesApiService
import iti.grad.nutriscan.data.remote.dto.ExerciseDto
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.exercises.model.Exercise
import iti.grad.nutriscan.domain.exercises.model.ExercisePage
import iti.grad.nutriscan.domain.exercises.model.ExerciseQuery
import iti.grad.nutriscan.domain.exercises.repository.IExercisesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** The API has no `ar` translations — always request/display `en` content (plan §2.1). */
private const val FALLBACK_LOCALE = "en"

/**
 * Conservative flat kcal estimates used whenever the API's `rep_kcal` / `min_kcal` are
 * null (frequently true in the sample data) — same order of magnitude as the previous
 * mock values. See plan §2.3; a nutritionist/product-owner-sourced MET table would be a
 * good future follow-up.
 */
private const val DEFAULT_MIN_KCAL = 0.15
private const val DEFAULT_REP_KCAL = 0.20

class ExercisesRepositoryImpl @Inject constructor(
    private val api: ExercisesApiService,
    private val dao: ExercisesDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IExercisesRepository {

    override suspend fun getExercises(query: ExerciseQuery): Result<ExercisePage> =
        withContext(ioDispatcher) {
            runCatchingCancellable {
                val response = api.getExercises(
                    page = query.page,
                    limit = query.limit,
                    bodyPart = query.bodyPart,
                    q = query.query,
                )
                val domainExercises = response.data.map { it.toDomain() }
                dao.insertExercises(domainExercises.map { it.toEntity() })
                ExercisePage(
                    exercises = domainExercises,
                    currentPage = response.meta?.pagination?.currentPage ?: query.page,
                    totalPages = response.meta?.pagination?.totalPages ?: 1,
                    hasNext = response.meta?.pagination?.hasNext ?: false,
                )
            }.recoverCatching { throwable ->
                val offset = (query.page - 1) * query.limit
                val localEntities = dao.getExercisesPaginated(
                    bodyPart = if (query.bodyPart.isNullOrEmpty()) null else query.bodyPart,
                    searchQuery = if (query.query.isNullOrEmpty()) null else query.query,
                    limit = query.limit,
                    offset = offset
                )
                if (localEntities.isNotEmpty()) {
                    ExercisePage(
                        exercises = localEntities.map { it.toDomain() },
                        currentPage = query.page,
                        totalPages = if (localEntities.size < query.limit) query.page else query.page + 1,
                        hasNext = localEntities.size >= query.limit
                    )
                } else {
                    throw throwable
                }
            }
        }

    override suspend fun getExerciseById(id: String): Result<Exercise> =
        withContext(ioDispatcher) {
            runCatchingCancellable {
                val exercise = api.getExerciseById(id).data.toDomain()
                dao.insertExercises(listOf(exercise.toEntity()))
                exercise
            }.recoverCatching { throwable ->
                val cached = dao.getExerciseById(id)
                cached?.toDomain() ?: throw throwable
            }
        }

    override suspend fun getCategories(): Result<List<String>> =
        withContext(ioDispatcher) {
            runCatchingCancellable {
                val categories = api.getCategories().data
                dao.insertCategories(categories.map { ExerciseCategoryEntity(it) })
                categories
            }.recoverCatching { throwable ->
                val cached = dao.getCategories()
                if (cached.isNotEmpty()) cached else throw throwable
            }
        }

    private fun ExerciseDto.toDomain() = Exercise(
        id = id,
        name = name,
        category = category,
        bodyPart = bodyPart,
        equipment = equipment,
        target = target.orEmpty(),
        secondaryMuscles = secondaryMuscles.orEmpty(),
        instructions = instructions.orEmpty(),
        instructionSteps = instructionSteps.orEmpty(),
        imageUrl = image?.let { if (it.startsWith("http")) it else "https://exercises-dataset-mu.vercel.app/$it" },
        gifUrl = gifUrl?.let { if (it.startsWith("http")) it else "https://exercises-dataset-mu.vercel.app/$it" },
        repKcal = repKcal ?: DEFAULT_REP_KCAL,
        minKcal = minKcal ?: DEFAULT_MIN_KCAL,
    )

    private fun Exercise.toEntity() = ExerciseEntity(
        id = id,
        name = name,
        category = category,
        bodyPart = bodyPart,
        equipment = equipment,
        target = target,
        secondaryMuscles = secondaryMuscles,
        instructions = instructions,
        instructionSteps = instructionSteps,
        imageUrl = imageUrl,
        gifUrl = gifUrl,
        repKcal = repKcal,
        minKcal = minKcal
    )

    private fun ExerciseEntity.toDomain() = Exercise(
        id = id,
        name = name,
        category = category,
        bodyPart = bodyPart,
        equipment = equipment,
        target = target,
        secondaryMuscles = secondaryMuscles,
        instructions = instructions,
        instructionSteps = instructionSteps,
        imageUrl = imageUrl,
        gifUrl = gifUrl,
        repKcal = repKcal,
        minKcal = minKcal
    )
}
