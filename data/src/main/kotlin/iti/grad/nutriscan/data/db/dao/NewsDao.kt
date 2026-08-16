package iti.grad.nutriscan.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import iti.grad.nutriscan.data.db.entity.NewsArticleEntity

@Dao
interface NewsDao {
    @Query("SELECT * FROM news_articles WHERE feedKey = :feedKey ORDER BY position")
    suspend fun getArticles(feedKey: String): List<NewsArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<NewsArticleEntity>)

    @Query("DELETE FROM news_articles WHERE feedKey = :feedKey")
    suspend fun clearFeed(feedKey: String)

    /** Replaces the whole [feedKey] cache atomically so a failed/partial refresh never leaves a
     * mix of old and new articles for the same feed. */
    @Transaction
    suspend fun replaceFeed(feedKey: String, articles: List<NewsArticleEntity>) {
        clearFeed(feedKey)
        insertArticles(articles)
    }
}
