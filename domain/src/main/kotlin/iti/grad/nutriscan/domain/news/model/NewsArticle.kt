package iti.grad.nutriscan.domain.news.model

data class NewsArticle(
    val title: String,
    val description: String?,
    val url: String,
    val imageUrl: String?,
    val sourceName: String,
    val publishedAt: String?,
    val author: String?,
)
