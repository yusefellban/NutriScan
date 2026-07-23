# Plan: Health News Screen

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Checkboxes track progress.

## 1. Feature Summary

A new full-screen "Health News" view, reached via a FAB on `HomeScreen`. By default it
fetches NewsAPI's `/v2/top-headlines?category=health` (matching the Postman collection's
"Headlines By category" request). Chips above the article list let the user filter by their
own medical conditions/allergies (pulled from their health profile) instead: selecting any
non-"All" chip(s) switches to `/v2/everything?q=(term1 OR term2 ...)` (matching the
collection's "Headlines by query" request), OR-joining every selected chip's keyword.
"All" is exclusive with every other chip. Tapping an article opens it in Chrome Custom Tabs.
No local caching — network-only, since articles are not health-safety data (§13 Domain
Notice doesn't apply here) and go stale fast.

Not mapped to an existing NutriScan_AI_Screens_Documentation.md section — this is a new
screen added on top of the documented Home flow, per direct product request.

## 2. Files to Create

**Domain (`domain/news/`)**
- `model/NewsArticle.kt` — display model for one article
- `model/NewsTopicChip.kt` — one filter chip (All, or a disease/allergy)
- `repository/INewsRepository.kt` — repository contract
- `usecase/GetHealthHeadlinesUseCase.kt` — default "All" fetch
- `usecase/SearchNewsArticlesUseCase.kt` — keyword OR-search fetch
- `usecase/BuildNewsTopicChipsUseCase.kt` — combines user profile + disease/allergy catalogs into the chip list

**Data (`data/`)**
- `remote/dto/ArticleSourceDto.kt`
- `remote/dto/ArticleDto.kt`
- `remote/dto/NewsResponseDto.kt`
- `remote/api/NewsApiService.kt`
- `repository/NewsRepositoryImpl.kt`

**App (`app/`)**
- (no new files — `NetworkModule.kt` and `RepositoryModule.kt` are extended, see §3)

**Presentation (`presentation/news/`)**
- `state/NewsState.kt`
- `state/NewsEvent.kt`
- `state/NewsEffect.kt`
- `viewmodel/NewsViewModel.kt`
- `view/NewsScreen.kt`
- `view/components/NewsTopicChipRow.kt`
- `view/components/NewsArticleCard.kt`

**Tests**
- `presentation/src/test/kotlin/iti/grad/nutriscan/presentation/news/NewsViewModelTest.kt`

## 3. Files to Modify

- `local.properties` — add `NEWS_API_BASE_URL` and `NEWS_API_KEY` (gitignored, never committed)
- `app/build.gradle.kts` — load `local.properties` into `buildConfigField`s for the two keys above
- `gradle/libs.versions.toml` — add `androidxBrowser` version + `androidx-browser` library entry
- `presentation/build.gradle.kts` — add `implementation(libs.androidx.browser)`
- `app/src/main/kotlin/iti/grad/nutriscan/di/NetworkModule.kt` — add a News-only `OkHttpClient`/`Retrofit`/`NewsApiService` provider trio
- `app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt` — bind `INewsRepository`
- `app/src/main/kotlin/iti/grad/nutriscan/navigation/Route.kt` — add `NewsRoute`
- `app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt` — add `composable<NewsRoute>`
- `presentation/.../home/state/HomeEvent.kt` — add `NewsFabClicked`
- `presentation/.../home/state/HomeEffect.kt` — add `NavigateToNews`
- `presentation/.../home/viewmodel/HomeViewModel.kt` — handle the new event/effect
- `presentation/.../home/view/HomeScreen.kt` — add the FAB, thread `onNavigateToNews`
- `presentation/src/main/res/values/strings.xml` and `values-ar/strings.xml` — new strings (§6)

## 4. Layer Breakdown

### Domain

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/news/model/NewsArticle.kt
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
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/news/model/NewsTopicChip.kt
package iti.grad.nutriscan.domain.news.model

data class NewsTopicChip(
    val id: String,
    val label: String,
    /** Null for the "All" chip — it has no search keyword, it switches to top-headlines. */
    val searchKeyword: String?,
) {
    companion object {
        const val ALL_CHIP_ID = "all"
    }
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/news/repository/INewsRepository.kt
package iti.grad.nutriscan.domain.news.repository

import iti.grad.nutriscan.domain.news.model.NewsArticle

interface INewsRepository {
    suspend fun getHealthHeadlines(): Result<List<NewsArticle>>
    suspend fun searchArticles(keywords: List<String>): Result<List<NewsArticle>>
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/news/usecase/GetHealthHeadlinesUseCase.kt
package iti.grad.nutriscan.domain.news.usecase

import iti.grad.nutriscan.domain.news.model.NewsArticle
import iti.grad.nutriscan.domain.news.repository.INewsRepository
import javax.inject.Inject

class GetHealthHeadlinesUseCase @Inject constructor(
    private val newsRepository: INewsRepository,
) {
    suspend operator fun invoke(): Result<List<NewsArticle>> = newsRepository.getHealthHeadlines()
}
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/news/usecase/SearchNewsArticlesUseCase.kt
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
```

```kotlin
// domain/src/main/kotlin/iti/grad/nutriscan/domain/news/usecase/BuildNewsTopicChipsUseCase.kt
package iti.grad.nutriscan.domain.news.usecase

import iti.grad.nutriscan.domain.allergy.usecase.GetAllergiesUseCase
import iti.grad.nutriscan.domain.disease.usecase.GetDiseasesUseCase
import iti.grad.nutriscan.domain.news.model.NewsTopicChip
import iti.grad.nutriscan.domain.user.usecase.GetUserProfileUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Builds the News screen's chip list: a fixed "All" chip, followed by one chip per
 * disease/allergy the current user actually has on file (matched against the full
 * catalog to resolve id -> display name). Zero conditions/allergies on file -> only
 * "All" is shown.
 */
class BuildNewsTopicChipsUseCase @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getDiseasesUseCase: GetDiseasesUseCase,
    private val getAllergiesUseCase: GetAllergiesUseCase,
) {
    operator fun invoke(): Flow<List<NewsTopicChip>> = combine(
        getUserProfileUseCase(),
        getDiseasesUseCase(),
        getAllergiesUseCase(),
    ) { user, diseases, allergies ->
        val allChip = NewsTopicChip(id = NewsTopicChip.ALL_CHIP_ID, label = "All", searchKeyword = null)
        if (user == null) return@combine listOf(allChip)

        val diseaseChips = diseases
            .filter { it.id in user.diseaseIds }
            .map { NewsTopicChip(id = "disease:${it.id}", label = it.name, searchKeyword = it.name) }
        val allergyChips = allergies
            .filter { it.id in user.allergyIds }
            .map { NewsTopicChip(id = "allergy:${it.id}", label = it.name, searchKeyword = it.name) }

        listOf(allChip) + diseaseChips + allergyChips
    }
}
```

> Note: the "All" chip's `label` field is set to the literal `"All"` here only as a
> non-empty placeholder value — it is never rendered. `NewsTopicChipRow` (§4 Presentation)
> special-cases `id == ALL_CHIP_ID` and renders `stringResource(R.string.news_chip_all)`
> instead, since domain models never hold Android string resources.

### Data

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/ArticleSourceDto.kt
package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArticleSourceDto(
    val id: String? = null,
    val name: String? = null,
)
```

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/ArticleDto.kt
package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArticleDto(
    val source: ArticleSourceDto? = null,
    val author: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val urlToImage: String? = null,
    val publishedAt: String? = null,
    val content: String? = null,
)
```

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/NewsResponseDto.kt
package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NewsResponseDto(
    val status: String,
    val totalResults: Int = 0,
    val articles: List<ArticleDto> = emptyList(),
)
```

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/remote/api/NewsApiService.kt
package iti.grad.nutriscan.data.remote.api

import iti.grad.nutriscan.data.remote.dto.NewsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * NewsAPI.org — matches the "NutriScan News" Postman collection exactly:
 * "Headlines By category" (category=health) and "Headlines by query" (q=... OR ...).
 * The `apiKey` query param is injected by the News-only OkHttpClient, not here
 * (see NetworkModule.kt) — keeps the secret out of every call site.
 */
interface NewsApiService {
    @GET("v2/top-headlines")
    suspend fun getTopHeadlines(@Query("category") category: String): NewsResponseDto

    @GET("v2/everything")
    suspend fun searchArticles(@Query("q") query: String): NewsResponseDto
}
```

```kotlin
// data/src/main/kotlin/iti/grad/nutriscan/data/repository/NewsRepositoryImpl.kt
package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.data.remote.api.NewsApiService
import iti.grad.nutriscan.data.remote.dto.ArticleDto
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.news.model.NewsArticle
import iti.grad.nutriscan.domain.news.repository.INewsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val HEALTH_CATEGORY = "health"

class NewsRepositoryImpl @Inject constructor(
    private val newsApiService: NewsApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : INewsRepository {

    override suspend fun getHealthHeadlines(): Result<List<NewsArticle>> = withContext(ioDispatcher) {
        runCatchingCancellable {
            newsApiService.getTopHeadlines(category = HEALTH_CATEGORY).articles.map { it.toDomain() }
        }
    }

    override suspend fun searchArticles(keywords: List<String>): Result<List<NewsArticle>> =
        withContext(ioDispatcher) {
            runCatchingCancellable {
                val query = "(" + keywords.joinToString(" OR ") + ")"
                newsApiService.searchArticles(query = query).articles.map { it.toDomain() }
            }
        }

    private fun ArticleDto.toDomain(): NewsArticle = NewsArticle(
        title = title.orEmpty(),
        description = description,
        url = url.orEmpty(),
        imageUrl = urlToImage,
        sourceName = source?.name ?: "",
        publishedAt = publishedAt,
        author = author,
    )
}
```

**`local.properties`** — add these two lines (file is already gitignored; never commit it):

```properties
NEWS_API_BASE_URL=https://newsapi.org/
NEWS_API_KEY=a06999badc0d4b2ebbe795b1a4891167
```

**`app/build.gradle.kts`** — add a `Properties` loader above `android {}` and two `buildConfigField`s inside `defaultConfig {}`:

```kotlin
import java.util.Properties
import java.io.FileInputStream

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(FileInputStream(file))
}
```

```kotlin
        buildConfigField("String", "NUTRISCAN_BASE_URL", "\"https://nutriscan.dev/api/\"")
        buildConfigField("String", "KEYCLOAK_BASE_URL", "\"https://auth.nutriscan.dev/\"")
        buildConfigField(
            "String", "NEWS_API_BASE_URL",
            "\"${localProperties.getProperty("NEWS_API_BASE_URL", "https://newsapi.org/")}\""
        )
        buildConfigField(
            "String", "NEWS_API_KEY",
            "\"${localProperties.getProperty("NEWS_API_KEY", "")}\""
        )
```

**`app/src/main/kotlin/iti/grad/nutriscan/di/NetworkModule.kt`** — add (imports:
`okhttp3.Interceptor`, `javax.inject.Named`, `iti.grad.nutriscan.data.remote.api.NewsApiService`):

```kotlin
    @Provides
    @Singleton
    @Named("NewsOkHttpClient")
    fun provideNewsOkHttpClient(): OkHttpClient {
        // Deliberately its own client: the shared AuthInterceptor would attach our
        // backend's Bearer token to a third-party public API, and ErrorInterceptor's
        // 422 -> OcrLowConfidenceException mapping doesn't apply to newsapi.org.
        val apiKeyInterceptor = Interceptor { chain ->
            val original = chain.request()
            val urlWithKey = original.url.newBuilder()
                .addQueryParameter("apiKey", BuildConfig.NEWS_API_KEY)
                .build()
            chain.proceed(original.newBuilder().url(urlWithKey).build())
        }
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("NewsRetrofit")
    fun provideNewsRetrofit(
        @Named("NewsOkHttpClient") client: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.NEWS_API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideNewsApiService(@Named("NewsRetrofit") retrofit: Retrofit): NewsApiService =
        retrofit.create(NewsApiService::class.java)
```

**`app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt`** — add import
`iti.grad.nutriscan.domain.news.repository.INewsRepository` /
`iti.grad.nutriscan.data.repository.NewsRepositoryImpl`, and inside the class:

```kotlin
    @Binds
    @Singleton
    abstract fun bindNewsRepository(
        impl: NewsRepositoryImpl
    ): INewsRepository
```

### Presentation

```kotlin
// presentation/src/main/kotlin/iti/grad/nutriscan/presentation/news/state/NewsState.kt
package iti.grad.nutriscan.presentation.news.state

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import iti.grad.nutriscan.domain.news.model.NewsTopicChip

data class NewsUiArticle(
    val title: String,
    val description: String?,
    val url: String,
    val imageUrl: String?,
    val sourceName: String,
    val publishedAtLabel: String,
)

data class NewsState(
    val chips: ImmutableList<NewsTopicChip> = persistentListOf(),
    val selectedChipIds: ImmutableSet<String> = persistentSetOf(NewsTopicChip.ALL_CHIP_ID),
    val articles: ImmutableList<NewsUiArticle> = persistentListOf(),
    val isLoading: Boolean = true,
    val errorMessageResId: Int? = null,
)
```

```kotlin
// presentation/src/main/kotlin/iti/grad/nutriscan/presentation/news/state/NewsEvent.kt
package iti.grad.nutriscan.presentation.news.state

sealed interface NewsEvent {
    data class ChipClicked(val chipId: String) : NewsEvent
    data class ArticleClicked(val url: String) : NewsEvent
    data object BackClicked : NewsEvent
    data object RetryClicked : NewsEvent
}
```

```kotlin
// presentation/src/main/kotlin/iti/grad/nutriscan/presentation/news/state/NewsEffect.kt
package iti.grad.nutriscan.presentation.news.state

sealed interface NewsEffect {
    data object NavigateBack : NewsEffect
    data class OpenArticle(val url: String) : NewsEffect
}
```

```kotlin
// presentation/src/main/kotlin/iti/grad/nutriscan/presentation/news/viewmodel/NewsViewModel.kt
package iti.grad.nutriscan.presentation.news.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.news.model.NewsArticle
import iti.grad.nutriscan.domain.news.model.NewsTopicChip
import iti.grad.nutriscan.domain.news.usecase.BuildNewsTopicChipsUseCase
import iti.grad.nutriscan.domain.news.usecase.GetHealthHeadlinesUseCase
import iti.grad.nutriscan.domain.news.usecase.SearchNewsArticlesUseCase
import iti.grad.nutriscan.presentation.news.state.NewsEffect
import iti.grad.nutriscan.presentation.news.state.NewsEvent
import iti.grad.nutriscan.presentation.news.state.NewsState
import iti.grad.nutriscan.presentation.news.state.NewsUiArticle
import iti.grad.presentation.R
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val buildNewsTopicChipsUseCase: BuildNewsTopicChipsUseCase,
    private val getHealthHeadlinesUseCase: GetHealthHeadlinesUseCase,
    private val searchNewsArticlesUseCase: SearchNewsArticlesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NewsState())
    val state: StateFlow<NewsState> = _state.asStateFlow()

    private val _effect = Channel<NewsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            buildNewsTopicChipsUseCase().collectLatest { chips ->
                _state.update { it.copy(chips = chips.toPersistentList()) }
            }
        }
        fetchArticles()
    }

    fun onEvent(event: NewsEvent) {
        when (event) {
            is NewsEvent.ChipClicked -> onChipClicked(event.chipId)
            is NewsEvent.ArticleClicked -> emitEffect(NewsEffect.OpenArticle(event.url))
            is NewsEvent.BackClicked -> emitEffect(NewsEffect.NavigateBack)
            is NewsEvent.RetryClicked -> fetchArticles()
        }
    }

    private fun onChipClicked(chipId: String) {
        val current = _state.value.selectedChipIds
        val newSelection = when {
            chipId == NewsTopicChip.ALL_CHIP_ID -> persistentSetOf(NewsTopicChip.ALL_CHIP_ID)
            chipId in current -> (current - chipId).ifEmpty { persistentSetOf(NewsTopicChip.ALL_CHIP_ID) }
            else -> (current - NewsTopicChip.ALL_CHIP_ID) + chipId
        }
        _state.update { it.copy(selectedChipIds = newSelection.toPersistentSet()) }
        fetchArticles()
    }

    private fun fetchArticles() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessageResId = null) }
            val selected = _state.value.selectedChipIds
            val result = if (selected.contains(NewsTopicChip.ALL_CHIP_ID) || selected.isEmpty()) {
                getHealthHeadlinesUseCase()
            } else {
                val keywords = _state.value.chips
                    .filter { it.id in selected }
                    .mapNotNull { it.searchKeyword }
                searchNewsArticlesUseCase(keywords)
            }
            result
                .onSuccess { articles ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            articles = articles.map { article -> article.toUiModel() }.toPersistentList(),
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(isLoading = false, errorMessageResId = R.string.news_load_error)
                    }
                }
        }
    }

    private fun emitEffect(effect: NewsEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun NewsArticle.toUiModel(): NewsUiArticle = NewsUiArticle(
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        sourceName = sourceName,
        publishedAtLabel = publishedAt.orEmpty(),
    )
}
```

```kotlin
// presentation/src/main/kotlin/iti/grad/nutriscan/presentation/news/view/components/NewsTopicChipRow.kt
package iti.grad.nutriscan.presentation.news.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.domain.news.model.NewsTopicChip
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

@Composable
fun NewsTopicChipRow(
    chips: ImmutableList<NewsTopicChip>,
    selectedChipIds: ImmutableSet<String>,
    onChipClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (chip in chips) {
            val isSelected = chip.id in selectedChipIds
            val label = if (chip.id == NewsTopicChip.ALL_CHIP_ID) {
                stringResource(R.string.news_chip_all)
            } else {
                chip.label
            }
            Text(
                text = label,
                style = AppTheme.typography.bodyMedium,
                color = if (isSelected) AppTheme.colors.Teal1000 else AppTheme.colors.Gray700,
                modifier = Modifier
                    .clip(chipShape)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) AppTheme.colors.Teal1000 else AppTheme.colors.Gray400,
                        shape = chipShape,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onChipClicked(chip.id) },
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

private val chipShape = RoundedCornerShape(32.dp)
```

```kotlin
// presentation/src/main/kotlin/iti/grad/nutriscan/presentation/news/view/components/NewsArticleCard.kt
package iti.grad.nutriscan.presentation.news.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import iti.grad.nutriscan.presentation.common.components.customShadow
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.news.state.NewsUiArticle
import iti.grad.presentation.R

@Composable
fun NewsArticleCard(
    article: NewsUiArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .customShadow(shape = cardShape, color = AppTheme.colors.Teal1000.copy(alpha = 0.2f), blurRadius = 30f, offsetY = 15f)
            .clip(cardShape)
            .background(AppTheme.colors.Surface)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        AsyncImage(
            model = article.imageUrl,
            contentDescription = article.title,
            modifier = Modifier
                .size(width = 137.dp, height = 140.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
            error = painterResource(id = R.drawable.ic_scanner),
            placeholder = painterResource(id = R.drawable.ic_scanner),
        )
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .height(140.dp),
        ) {
            Text(
                text = article.title,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.Gray1600,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = article.sourceName,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.Gray600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
```

```kotlin
// presentation/src/main/kotlin/iti/grad/nutriscan/presentation/news/view/NewsScreen.kt
package iti.grad.nutriscan.presentation.news.view

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.common.components.AppBackButton
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.news.state.NewsEffect
import iti.grad.nutriscan.presentation.news.state.NewsEvent
import iti.grad.nutriscan.presentation.news.state.NewsState
import iti.grad.nutriscan.presentation.news.view.components.NewsArticleCard
import iti.grad.nutriscan.presentation.news.view.components.NewsTopicChipRow
import iti.grad.nutriscan.presentation.news.viewmodel.NewsViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun NewsScreen(
    viewModel: NewsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NewsEffect.NavigateBack -> onNavigateBack()
                is NewsEffect.OpenArticle -> {
                    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(effect.url))
                }
            }
        }
    }

    NewsContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
private fun NewsContent(
    state: NewsState,
    onEvent: (NewsEvent) -> Unit,
) {
    Scaffold(containerColor = AppTheme.colors.Background) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Start,
            ) {
                AppBackButton(onClick = { onEvent(NewsEvent.BackClicked) })
                Text(
                    text = stringResource(R.string.news_screen_title),
                    style = AppTheme.typography.titleLarge,
                    color = AppTheme.colors.TextPrimary,
                    modifier = Modifier.padding(start = 12.dp).align(Alignment.CenterVertically),
                )
            }

            NewsTopicChipRow(
                chips = state.chips,
                selectedChipIds = state.selectedChipIds,
                onChipClicked = { onEvent(NewsEvent.ChipClicked(it)) },
                modifier = Modifier.padding(horizontal = 22.dp),
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AppTheme.colors.Teal1000,
                    )
                    state.errorMessageResId != null -> Text(
                        text = stringResource(state.errorMessageResId),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.TextSecondary,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 22.dp),
                    )
                    state.articles.isEmpty() -> Text(
                        text = stringResource(R.string.news_empty_state),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.TextSecondary,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 22.dp),
                    )
                    else -> LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items = state.articles, key = { it.url }) { article ->
                            NewsArticleCard(
                                article = article,
                                onClick = { onEvent(NewsEvent.ArticleClicked(article.url)) },
                            )
                        }
                    }
                }
            }
        }
    }
}
```

**Home FAB wiring** — `HomeEvent.kt`: add `data object NewsFabClicked : HomeEvent`.
`HomeEffect.kt`: add `data object NavigateToNews : HomeEffect`. `HomeViewModel.onEvent`: add
`is HomeEvent.NewsFabClicked -> emitEffect(HomeEffect.NavigateToNews)`. `HomeScreen.kt`:
thread a new `onNavigateToNews: () -> Unit = {}` param, collect `HomeEffect.NavigateToNews ->
onNavigateToNews()` in the existing `LaunchedEffect`, and add to the `Scaffold` in
`HomeScreenContent`:

```kotlin
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(HomeEvent.NewsFabClicked) },
                containerColor = AppTheme.colors.ScanButtonBackground,
                contentColor = AppTheme.colors.ScanButtonIconTint,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = stringResource(R.string.news_fab_content_description),
                )
            }
        },
```

(imports to add in `HomeScreen.kt`: `androidx.compose.material3.FloatingActionButton`,
`androidx.compose.material3.Icon`, `androidx.compose.material.icons.Icons`,
`androidx.compose.material.icons.automirrored.filled.Article` — `material-icons-extended`
is already a `presentation` dependency, no new one needed.)

## 5. Navigation Changes

`Route.kt` — add:

```kotlin
@Serializable
object NewsRoute
```

`NavGraph.kt` — add `onNavigateToNews = { navController.navigate(NewsRoute) }` to the
existing `HomeScreen(...)` call, and a new composable block:

```kotlin
        // 30. News
        composable<NewsRoute> {
            NewsScreen(
                onNavigateBack = { navController.navigateUp() },
            )
        }
```

(plus `import iti.grad.nutriscan.presentation.news.view.NewsScreen`).

## 6. Strings — MANDATORY (Zero Hardcoded Text)

| Key (R.string.xxx)                     | English Value                                  | Arabic Value                                |
|------------------------------------------|-------------------------------------------------|------------------------------------------------|
| `news_screen_title`                     | "Health News"                                  | "أخبار صحية"                                  |
| `news_chip_all`                         | "All"                                          | "الكل"                                        |
| `news_load_error`                       | "Couldn't load news. Please try again."        | "تعذّر تحميل الأخبار. حاول مرة أخرى."         |
| `news_empty_state`                      | "No articles found for the selected topics."   | "لا توجد مقالات للمواضيع المحددة."            |
| `news_fab_content_description`          | "Health News"                                  | "أخبار صحية"                                  |

`action_retry` (already exists, line 99 of `strings.xml`) is reused for the retry button —
not a new string.

## 7. Testing Plan

`NewsViewModelTest.kt` (JUnit 5 + Turbine + MockK, per §11), covering:

1. **Initial state**: fake `BuildNewsTopicChipsUseCase` emits `[All, Diabetes, Allergies]`;
   fake `GetHealthHeadlinesUseCase` returns `Result.success(listOf(article1, article2))` —
   assert `state.value` has `chips.size == 3`, `selectedChipIds == {ALL_CHIP_ID}`,
   `articles.size == 2`, `isLoading == false`, `errorMessageResId == null`.
2. **ChipClicked(nonAllChip) while All selected**: assert `selectedChipIds == {chipId}`
   (All cleared) and `searchNewsArticlesUseCase` was invoked with that chip's keyword.
3. **ChipClicked(secondChip) while first non-All chip already selected**: assert
   `selectedChipIds` contains both (multi-select) and the search use case was invoked with
   both keywords OR-able (i.e. called with a 2-element list).
4. **ChipClicked(ALL_CHIP_ID) while other chips selected**: assert `selectedChipIds ==
   {ALL_CHIP_ID}` and `getHealthHeadlinesUseCase` was invoked (not search).
5. **ChipClicked(onlySelectedChip) deselecting it**: assert selection falls back to
   `{ALL_CHIP_ID}` (never an empty set) and headlines are refetched.
6. **RetryClicked after a failure**: fake headlines use case first returns
   `Result.failure(...)`, assert `state.value.errorMessageResId ==
   R.string.news_load_error`; then make it return success and dispatch `RetryClicked`,
   assert the error clears and `articles` populates.
7. **ArticleClicked emits effect**: dispatch `ArticleClicked("https://x")`, assert
   `effect` emits `NewsEffect.OpenArticle("https://x")` (via Turbine `test {}`).
8. **BackClicked emits effect**: assert `effect` emits `NewsEffect.NavigateBack`.

`HomeViewModel`'s existing test file gets one new case: dispatching
`HomeEvent.NewsFabClicked` emits `HomeEffect.NavigateToNews`.

## 8. Edge Cases

- User has zero diseases/allergies on file → chip list is `[All]` only, nothing else to
  toggle; screen still functions with the default headlines fetch.
- NewsAPI request fails (network error, non-2xx, rate limit) → `runCatchingCancellable`
  turns it into `Result.failure`, surfaced as the existing `news_load_error` string with a
  retry affordance — no crash, no silent empty list mistaken for "no articles".
- Zero articles returned for a legitimate query (e.g. an obscure allergy keyword with no
  matches) → distinct empty-state message (`news_empty_state`), not the error message.
- Deselecting the only selected non-"All" chip → selection must fall back to `{All}`,
  never leave `selectedChipIds` empty (which has no defined fetch behavior).
- Article missing `urlToImage`/`description`/`author` (all nullable in the Postman sample
  responses) → DTO fields are nullable with safe defaults; card shows title + source only.
- Rapid chip taps → each `ChipClicked` launches a new `fetchArticles()` coroutine inside
  `viewModelScope`; `_state.update` always applies against the latest `NewsState`, so the
  last fetch's result is authoritative (existing `MutableStateFlow.update` pattern — same
  as every other ViewModel in this codebase, no extra debouncing added since NewsAPI's
  response time in the sample data is well under typical double-tap intervals).

## 9. Definition of Done

- [ ] All files listed in §2/§3 created/modified
- [ ] `NewsViewModelTest.kt` cases 1–8 (§7) passing, plus the `HomeViewModel` addition
- [ ] All 5 strings in §6 present in both `values/strings.xml` and `values-ar/strings.xml`
- [ ] No hardcoded colors, strings, or dimensions in any new Composable
- [ ] `local.properties` has the two new keys; `NEWS_API_KEY` never appears in any
      committed file (verify with `git diff --cached` before committing)
- [ ] FAB on `HomeScreen` navigates to `NewsRoute`; back button returns to Home
- [ ] Tapping an article opens Chrome Custom Tabs with that article's `url`
- [ ] Manual check: default screen load shows health top-headlines; selecting one chip
      switches to keyword search; selecting a second chip ORs both keywords; selecting
      "All" again clears everything back to headlines
