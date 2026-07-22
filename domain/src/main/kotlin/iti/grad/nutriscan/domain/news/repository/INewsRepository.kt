package iti.grad.nutriscan.domain.news.repository

import iti.grad.nutriscan.domain.news.model.NewsArticle

interface INewsRepository {
    suspend fun getHealthHeadlines(): Result<List<NewsArticle>>
    suspend fun searchArticles(keywords: List<String>): Result<List<NewsArticle>>
}
