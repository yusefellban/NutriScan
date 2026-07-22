package iti.grad.nutriscan.domain.news.usecase

import iti.grad.nutriscan.domain.allergy.usecase.GetAllergiesUseCase
import iti.grad.nutriscan.domain.disease.usecase.GetDiseasesUseCase
import iti.grad.nutriscan.domain.news.model.NewsTopicChip
import iti.grad.nutriscan.domain.user.usecase.GetUserProfileUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Builds the News screen's chip list: a fixed "All" chip, followed by one chip per
 * disease/allergy the current user actually has on file (matched against the full
 * catalog to resolve id -> display name). Zero conditions/allergies on file -> only
 * "All" is shown.
 */
class BuildNewsTopicChipsUseCase @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getDiseasesUseCase: GetDiseasesUseCase,
    private val getAllergiesUseCase: GetAllergiesUseCase,
) {
    operator fun invoke(): Flow<List<NewsTopicChip>> = combine(
        getUserProfileUseCase(),
        getDiseasesUseCase(),
        getAllergiesUseCase(),
    ) { user, diseases, allergies ->
        val allChip = NewsTopicChip(id = NewsTopicChip.ALL_CHIP_ID, label = "All", searchKeyword = null)
        if (user == null) return@combine listOf(allChip)

        val diseaseChips = diseases
            .filter { it.id in user.diseaseIds }
            .map { NewsTopicChip(id = "disease:${it.id}", label = it.name, searchKeyword = it.name) }
        val allergyChips = allergies
            .filter { it.id in user.allergyIds }
            .map { NewsTopicChip(id = "allergy:${it.id}", label = it.name, searchKeyword = it.name) }

        listOf(allChip) + diseaseChips + allergyChips
    }
}
