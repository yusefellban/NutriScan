package iti.grad.nutriscan.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import iti.grad.nutriscan.domain.notification.model.HealthQuote
import iti.grad.nutriscan.domain.notification.repository.IQuoteRepository
import iti.grad.presentation.R
import javax.inject.Inject

class QuoteRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : IQuoteRepository {

    override fun getRandomQuote(): HealthQuote {
        val quotes = context.resources.getStringArray(R.array.health_quotes)
        return HealthQuote(text = quotes.random())
    }
}
