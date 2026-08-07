package iti.grad.nutriscan.domain.notification.repository

import iti.grad.nutriscan.domain.notification.model.HealthQuote

interface IQuoteRepository {
    fun getRandomQuote(): HealthQuote
}
