package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.HealthQuote
import iti.grad.nutriscan.domain.notification.repository.IQuoteRepository
import javax.inject.Inject

class GetRandomQuoteUseCase @Inject constructor(
    private val quoteRepository: IQuoteRepository
) {
    operator fun invoke(): HealthQuote = quoteRepository.getRandomQuote()
}
