# Plan: News UI Redesign — "Discover" Screen + News Home Screen

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Checkboxes track progress.

## 1. Feature Summary

Redesign the existing News screen UI to match a new "Discover" design. The current card-heavy
shadow layout with NutriScan logo header is replaced by a cleaner flat design with:
- Large "Discover" title + subtitle header
- Search bar with filter icon
- Filled teal chips (selected) vs gray outlined (unselected)
- Flat article cards with category label, bold title, source avatar row, bottom divider
- Smaller 100×100dp thumbnails

No domain/data layer changes — purely a presentation-layer UI restyle.

## 2. Files to Modify

### Theme (`presentation/common/theme/`)
- [x] `AppColors.kt` — add 14 News-specific colors to `AppColorsExtension3` + forwarding properties

### String Resources (`presentation/src/main/res/`)
- [x] `values/strings.xml` — add new strings, update existing
- [x] `values-ar/strings.xml` — Arabic translations

### State (`presentation/news/state/`)
- [x] `NewsState.kt` — add `searchQuery`, `category` to `NewsUiArticle`
- [x] `NewsEvent.kt` — add `SearchQueryChanged`, `FilterClicked`; remove `ShareClicked`
- [x] `NewsEffect.kt` — remove `ShareArticle`

### ViewModel (`presentation/news/viewmodel/`)
- [x] `NewsViewModel.kt` — handle search, remove share, map category

### Components (`presentation/news/view/components/`)
- [x] `NewsTopicChipRow.kt` — restyle chips (filled selected, outlined unselected)
- [x] `NewsArticleCard.kt` — complete redesign to flat card with category/title/source row
- [x] `NewsSearchBar.kt` — **NEW** search bar component

### Screen (`presentation/news/view/`)
- [x] `NewsScreen.kt` — restructure layout (header, search bar, chips, flat list)

## 3. Design Tokens (AppColors)

| Token | Light | Dark | Usage |
|---|---|---|---|
| NewsChipSelectedBg | Teal1000 | Teal1000 | Filled chip background |
| NewsChipSelectedText | White | White | Chip text when selected |
| NewsChipUnselectedBg | White | Teal1600 | Unselected chip bg |
| NewsChipUnselectedBorder | Gray400 | Teal1400 | Unselected chip border |
| NewsChipUnselectedText | Gray700 | Teal400 | Unselected chip text |
| NewsCategoryLabel | Teal1000 | Teal400 | "News"/"Health" label |
| NewsCardTitle | Gray1600 | Teal300 | Article title text |
| NewsSourceText | Gray700 | Teal1200 | Source + time text |
| NewsSourceAvatarBg | Teal1000 | Teal1400 | Source avatar circle bg |
| NewsSearchBarBg | Gray100 | Teal1600 | Search bar background |
| NewsSearchBarBorder | Gray400 | Teal1400 | Search bar border |
| NewsSearchIconTint | Gray700 | Teal400 | Search/filter icon tint |
| NewsDivider | Gray300 | Teal1400 | Card bottom divider |
| NewsScreenTitle | Gray1600 | Teal300 | "Discover" title color |

# News Home Screen — Implementation Plan

## Goal
Create a new **NewsHomeScreen** that serves as the landing page when "Health News" is tapped from the Home screen, matching the provided screenshot exactly.

## Screenshot Analysis

| Section | Description |
|---------|-------------|
| **Header** | Back button (`<`) left + Search icon (`🔍`) right, same row |
| **Breaking News** | Large horizontal pager cards with full-bleed image, gradient overlay at bottom, "News" teal badge top-left, source name + verified + time, bold headline. Peek next card. Teal/gray dot indicators below. |
| **Recommendation** | Section title + vertical list of white bordered cards (reuses existing `NewsArticleCard`) |

## Proposed Changes

### Navigation Layer (`app/`)

#### [MODIFY] [Route.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/navigation/Route.kt)
- Add `@Serializable object NewsHomeRoute`

#### [MODIFY] [NavGraph.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt)
- Add `composable<NewsHomeRoute>` with navigation callbacks
- Change `onNavigateToNews` to navigate to `NewsHomeRoute` instead of `NewsRoute`

---

### String Resources (`presentation/src/main/res/`)

#### [MODIFY] [strings.xml](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/res/values/strings.xml)
#### [MODIFY] [strings.xml (AR)](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/res/values-ar/strings.xml)

New strings: `news_home_breaking_news`, `news_home_recommendation`, `news_home_search_content_description`

---

### MVI State (`presentation/news/home/state/`)

#### [NEW] `NewsHomeState.kt`
```kotlin
data class NewsHomeState(
    val breakingArticles: ImmutableList<NewsUiArticle>,
    val recommendationArticles: ImmutableList<NewsUiArticle>,
    val isLoading: Boolean,
    val errorMessageResId: Int?,
)
```

#### [NEW] `NewsHomeEvent.kt`
- BackClicked, SearchClicked, BreakingArticleClicked, RecommendationArticleClicked, RetryClicked

#### [NEW] `NewsHomeEffect.kt`
- NavigateBack, NavigateToDiscover, OpenArticle(url)

---

### ViewModel (`presentation/news/home/viewmodel/`)

#### [NEW] `NewsHomeViewModel.kt`
- Injects `GetHealthHeadlinesUseCase`
- Fetches headlines, splits first 5 → breaking, rest → recommendations
- Standard MVI pattern with StateFlow + Channel

---

### Components (`presentation/news/home/view/components/`)

#### [NEW] `BreakingNewsCard.kt`
- Full-bleed AsyncImage background, gradient overlay, "News" badge, source row, headline

#### [NEW] `BreakingNewsPager.kt`
- `HorizontalPager` + `NewsPageIndicator`, peek next card

#### [NEW] `NewsPageIndicator.kt`
- Row of dots: teal active, gray inactive

---

### Screen (`presentation/news/home/view/`)

#### [NEW] `NewsHomeScreen.kt`
- Scaffold → Column(header row, breaking section, recommendation section)
- Reuses `AppBackButton`, `NewsArticleCard`

---

## Data Layer

**No changes needed** — reuses existing `GetHealthHeadlinesUseCase` which calls `NewsApiService.getTopHeadlines(category=health)`.

## Verification Plan

### Automated Tests
```
./gradlew :presentation:compileDebugKotlin
```

### Manual Verification
- Navigation flow: Home → NewsHome → Discover (search icon)
- Breaking news pager swipes and dot indicators
- Recommendation cards match existing design
- Dark/Light mode toggle
- AR/EN language switch

