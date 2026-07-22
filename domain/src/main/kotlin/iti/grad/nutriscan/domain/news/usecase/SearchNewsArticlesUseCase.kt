package iti.grad.nutriscan.domain.news.usecase

import iti.grad.nutriscan.domain.news.model.NewsArticle
import iti.grad.nutriscan.domain.news.repository.INewsRepository
import javax.inject.Inject

class SearchNewsArticlesUseCase @Inject constructor(
    private val newsRepository: INewsRepository,
) {
    suspend operator fun invoke(keywords: List<String>): Result<List<NewsArticle>> =
        newsRepository.searchArticles(keywords)
}
