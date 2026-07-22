package iti.grad.nutriscan.domain.news.usecase

import iti.grad.nutriscan.domain.news.model.NewsArticle
import iti.grad.nutriscan.domain.news.repository.INewsRepository
import javax.inject.Inject

class GetHealthHeadlinesUseCase @Inject constructor(
    private val newsRepository: INewsRepository,
) {
    suspend operator fun invoke(): Result<List<NewsArticle>> = newsRepository.getHealthHeadlines()
}
