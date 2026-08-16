package iti.grad.nutriscan.domain.scan.usecase

import iti.grad.nutriscan.domain.scan.repository.IScanRepository
import javax.inject.Inject

/**
 * Fetches autocomplete product name suggestions for the Scan History search bar.
 *
 * Intended to be called with a debounced query string (300 ms) from the ViewModel.
 * Returns an empty list on network failure — the UI should degrade gracefully (no dropdown).
 */
class GetScanSuggestionsUseCase @Inject constructor(
    private val scanRepository: IScanRepository
) {
    suspend operator fun invoke(query: String): Result<List<String>> {
        if (query.isBlank()) return Result.success(emptyList())
        return scanRepository.getScanSuggestions(query)
    }
}
