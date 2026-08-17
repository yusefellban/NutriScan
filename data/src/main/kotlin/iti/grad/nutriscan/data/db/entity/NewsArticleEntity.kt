package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached News article. [feedKey] separates the health-headlines feed from a search's result set
 * so a cached search doesn't leak into the headlines screen (and vice versa) — see
 * [iti.grad.nutriscan.data.repository.NewsRepositoryImpl].
 */
@Entity(tableName = "news_articles", primaryKeys = ["url", "feedKey"])
data class NewsArticleEntity(
    val url: String,
    val feedKey: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val sourceName: String,
    val publishedAt: String?,
    val author: String?,
    /** Preserves API ordering on cache read — Room has no notion of insertion order. */
    val position: Int,
)
