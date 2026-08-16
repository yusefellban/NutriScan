# News Feed — Align Chip Selection & Feed Logic with `NEWS_FEATURE_DESIGN_AND_LOGIC.md`

**Date:** 2026-08-16
**Feature area:** `News` (presentation, domain, data)
**Author:** AI agent (per user request, following `SKILL.md` §1 Feature Planning Standard)

---

## 1. Goal Description

The Android News screen currently has the right *shape* (header, search bar, chips,
article cards) but the underlying **chip selection and feed-fetching logic does not
match the iOS reference implementation** described in
`NEWS_FEATURE_DESIGN_AND_LOGIC.md`. The iOS developer confirmed the contract:

> Chips must be built from the current user's saved allergies and diseases. The
> first chip is always `All`, which returns the union of news for every one of the
> user's interests. Every other chip returns news for exactly that one allergy or
> disease. Follow the doc.

Concretely, three behaviors are wrong or missing today:

1. **"All" does not fan out per interest.** `NewsViewModel.fetchArticles()` currently
   sends `All` straight to `GetHealthHeadlinesUseCase()` (NewsAPI
   `/v2/top-headlines?category=health`) — a single generic request, not "the union of
   independent requests, one per profile interest" that the spec requires.
2. **Chip selection is multi-select today; the spec is single-select.** The doc's
   "Specific interest" section describes selecting **one** allergy or disease at a
   time; there's no mention of combining two specific chips with `OR`. The current
   `onChipClicked` allows selecting several non-`All` chips simultaneously and ORs
   their keywords into one query.
3. **Per-interest query terms don't match the documented format.** The doc's
   examples are `Peanuts allergy` and `Diabetes disease` (i.e. `"<name> allergy"` /
   `"<name> disease"`), but `BuildNewsTopicChipsUseCase` currently sets
   `searchKeyword = it.name` with no suffix.

This plan also captures the remaining, larger gaps against the doc (search
debounce/submit semantics, pagination, in-memory `FeedSnapshot` caching, and the
full screen-state matrix) so they can be scheduled as explicit follow-up phases
rather than silently deferred.

**Non-goals for Phase 1:** pagination, in-memory feed caching by `(chip, search)`,
and the full offline/server-error state matrix are described here for completeness
but implemented in later phases (see §5) to keep each PR reviewable.

---

## 2. User Review Required

Before implementation starts, please confirm:

1. **Query term suffixes** — Is `"<name> allergy"` / `"<name> disease"` (exact
   strings from the doc's examples) the final NewsAPI query term, or does the iOS
   app apply any additional normalization (e.g. lowercasing, stripping punctuation
   from condition names like "Type 2 Diabetes")? I will mirror iOS exactly if a
   sample of iOS's `NewsProfileAdapter` / query-builder output is available.
2. **Single-select confirmation** — Please confirm chips are strictly single-select
   (tapping a specific chip deselects any other specific chip and `All`; tapping the
   already-selected chip falls back to `All`). This is a **behavior change** users
   will notice, so calling it out explicitly.
3. **Partial-failure UX for "All"** — The doc says "if at least one interest request
   succeeds, its articles are displayed; an error is returned only when every
   interest request fails." Should a *partial* failure (e.g. 2 of 3 interests failed)
   surface any inline warning, or should it silently show only the successful
   interests' articles (my default assumption, matching the doc's wording)?
4. **Rollout scope** — Should Phase 1 (below) ship alone, or do you want pagination
   and the in-memory cache bundled into the same PR? Phase 1 alone is independently
   testable and already fixes the reported "All ≠ union" and "no chips" complaints;
   I'd recommend shipping it first.

---

## 3. Current State (for reference)

```
NewsViewModel.fetchArticles()
 ├─ selected == {All} or empty  → GetHealthHeadlinesUseCase()              // WRONG: generic health headlines
 └─ selected == {chip1, chip2…} → SearchNewsArticlesUseCase(keywords)      // WRONG: multi-select OR'd into one query
                                    └─ INewsRepository.searchArticles(keywords: List<String>)
                                         └─ NewsApiService.searchArticles(q = "(kw1 OR kw2)")

BuildNewsTopicChipsUseCase
 └─ NewsTopicChip(searchKeyword = it.name)   // WRONG: missing " allergy"/" disease" suffix

NewsViewModel.onChipClicked()
 └─ toggles chipId in/out of a Set, allowing 2+ specific chips at once     // WRONG: spec is single-select
```

Everything upstream of these three points (profile → `GetUserProfileUseCase`,
catalogs → `GetDiseasesUseCase`/`GetAllergiesUseCase`, chip filtering in
`BuildNewsTopicChipsUseCase`) was already audited and confirmed correct/backend-wired
in prior work on this feature — no changes needed there beyond the keyword suffix.

---

## 4. Proposed Changes (by module)

### 4.1 Domain layer

**`domain/.../news/repository/INewsRepository.kt`**
- Change `searchArticles(keywords: List<String>)` → `searchArticles(query: String)`.
  The repository should execute exactly the query string it's given, not build an
  `OR` clause itself — query composition (the `AND`/`OR` grouping rules) belongs in
  the domain use case per the doc's Clean Architecture boundary
  ("Keep filtering, merging, partial-failure, and Discover rules inside domain use
  cases").

**`data/.../repository/NewsRepositoryImpl.kt`**
- Update `searchArticles` to pass `query` straight through to
  `newsApiService.searchArticles(query = query)` (drop the `joinToString(" OR ")`
  wrapping — that logic moves to the new use case below).

**`domain/.../news/usecase/BuildNewsTopicChipsUseCase.kt`**
- Disease chip: `searchKeyword = "${it.name} disease"`.
- Allergy chip: `searchKeyword = "${it.name} allergy"`.
- No other change — the profile/catalog matching logic (`it.id in user.diseaseIds`,
  etc.) is already correct.

**New: `domain/.../news/usecase/FetchNewsFeedUseCase.kt`**
- Single entry point the ViewModel calls for every fetch (initial load, chip switch,
  search, retry). Signature:
  ```kotlin
  suspend operator fun invoke(
      selectedChipId: String,       // NewsTopicChip.ALL_CHIP_ID or a specific chip id
      chips: List<NewsTopicChip>,
      searchText: String,
  ): Result<List<NewsArticle>>
  ```
- Logic (mirrors doc's "Feed and Filter Logic" 1:1):
  1. **Specific chip selected** → one `searchArticles(query)` call where
     `query = "($searchText) AND ($keyword)"` if `searchText` is non-blank, else
     `"($keyword)"`.
  2. **`All` selected, profile has interests** → `async { }` one
     `searchArticles(query)` call per profile-interest chip (same query-building
     rule as above, per-interest), await all, then:
     - Merge all **successful** results only.
     - Dedupe by `url`.
     - Dedupe by normalized title (`trim().lowercase()`).
     - Sort by `publishedAt` descending.
     - Return `Result.failure` only if **every** interest request failed.
  3. **`All` selected, no profile interests (Discover fallback)**:
     - `searchText` blank → `getHealthHeadlines()`.
     - `searchText` non-blank → `searchArticles("($searchText)")`.
- Unit tests co-located per module convention (`domain/src/test/...`), covering all
  five branches above plus the "2 of 3 interests fail" partial-success case.

**Remove: `domain/.../news/usecase/SearchNewsArticlesUseCase.kt`**
- Superseded by `FetchNewsFeedUseCase`. Delete after confirming no other call sites
  (currently only `NewsViewModel` and its test).

### 4.2 Presentation layer

**`presentation/.../news/state/NewsState.kt`**
- Change `selectedChipIds: ImmutableSet<String>` → `selectedChipId: String` (default
  `NewsTopicChip.ALL_CHIP_ID`), reflecting single-select. `NewsTopicChipRow` already
  renders selection from a single active id comparison, so this is a compatible
  narrowing, not a UI rewrite.

**`presentation/.../news/viewmodel/NewsViewModel.kt`**
- `onChipClicked(chipId)`:
  ```kotlin
  private fun onChipClicked(chipId: String) {
      val newSelection = when {
          chipId == NewsTopicChip.ALL_CHIP_ID -> NewsTopicChip.ALL_CHIP_ID
          chipId == _state.value.selectedChipId -> NewsTopicChip.ALL_CHIP_ID // re-tap → back to All
          else -> chipId
      }
      if (newSelection == _state.value.selectedChipId) return
      _state.update { it.copy(selectedChipId = newSelection) }
      fetchArticles()
  }
  ```
- `fetchArticles()` now delegates entirely to `FetchNewsFeedUseCase(selectedChipId,
  chips, searchQuery)`, replacing the inline `GetHealthHeadlinesUseCase` /
  `SearchNewsArticlesUseCase` branching.
- Feed-label logic (`For You` / `Discover` / chip name) is unchanged — it already
  reads from `chips` + `selectedChipId`.
- **Search debounce (doc requirement, currently missing):** text changes go through
  a `MutableSharedFlow<String>` piped through `.debounce(350).distinctUntilChanged()`
  before triggering `fetchArticles()`; the search button (`FilterClicked`) bypasses
  the debounce and fetches immediately, matching "Submitting searches immediately."
- Constructor changes from `(BuildNewsTopicChipsUseCase, GetHealthHeadlinesUseCase,
  SearchNewsArticlesUseCase)` to `(BuildNewsTopicChipsUseCase, FetchNewsFeedUseCase)`.

**`presentation/.../news/view/components/NewsTopicChipRow.kt`**
- Update the "is this chip selected" check from `chipId in selectedChipIds` to
  `chipId == selectedChipId`. (Component already takes a selection predicate, so
  this is a call-site change, not a component rewrite.)

### 4.3 Tests (per `SKILL.md` §4 ViewModel Unit Testing Pattern)

**`presentation/src/test/.../news/NewsViewModelTest.kt`** — rewritten against the new
constructor and single-select behavior:
- `selecting a specific chip clears All and fetches only that interest`
- `re-tapping the selected chip falls back to All`
- `selecting a different specific chip switches directly (no multi-select)`
- `All with profile interests fetches the union via FetchNewsFeedUseCase`
- `search text change is debounced 350ms before fetching`
- `search submit (FilterClicked) fetches immediately, bypassing debounce`
- Existing error/retry/effect tests carried over with the new mocked use case.

**New: `domain/src/test/.../news/usecase/FetchNewsFeedUseCaseTest.kt`**
- `specific chip builds "(keyword)" query when search is blank`
- `specific chip builds "(search) AND (keyword)" when search is present`
- `All with interests issues one request per interest and merges results`
- `All merge dedupes by url`
- `All merge dedupes by normalized (trimmed, lowercased) title`
- `All merge sorts by publishedAt descending`
- `All returns failure only when every interest request fails`
- `All with zero profile interests + blank search → getHealthHeadlines (Discover)`
- `All with zero profile interests + search text → plain "(search)" query (Discover)`

### 4.4 No changes needed

- `GetUserProfileUseCase`, `GetDiseasesUseCase`, `GetAllergiesUseCase`, and their
  repository implementations — already backend-wired and self-syncing, confirmed in
  a prior audit of this feature.
- `NewsArticleCard`, `NewsSearchBar`, header/back-button styling — unrelated to this
  logic change.
- DI: both use cases use `@Inject constructor`, so no explicit Hilt module binding
  changes are required beyond the constructor signature Hilt already resolves.

---

## 5. Follow-Up Phases (explicitly deferred, not silently dropped)

These are real gaps against the doc but are large enough to warrant their own plan
document once Phase 1 lands and is verified on-device:

- **Phase 2 — Pagination:** page size 20, `hasMore = page * pageSize < totalResults`,
  next-page trigger at "last 3 visible items," single in-flight page request,
  merge+dedupe+sort on each page, inline retry footer on pagination failure only
  (doesn't replace the whole screen). For `All`, every profile interest is paged in
  lockstep at the same page/pageSize.
- **Phase 3 — In-memory `FeedSnapshot` cache:** keyed by
  `(selectedChipId, normalizedSearchText)`, storing articles/state/page/hasMore/error;
  restored instantly on chip-switch-back without refetching; cleared entirely (plus a
  fresh profile-interest refresh) on pull-to-refresh; memory-only, scoped to
  `NewsViewModel`'s lifetime.
- **Phase 4 — Full screen-state matrix:** dedicated `noSearchResults` (with Clear
  Search action) vs. empty-feed ("No news yet" + Refresh) vs. no-connection vs.
  server-problem states, with failure classification via `NetworkMonitor` first,
  then known offline exception types, falling back to "server problem."

---

## 6. Verification Plan

### Automated
- `./gradlew :domain:testDebugUnitTest --tests "*FetchNewsFeedUseCaseTest*"`
- `./gradlew :presentation:testDebugUnitTest --tests "*NewsViewModelTest*"`
- Full module test suites (`:domain:test`, `:presentation:test`) to catch any
  incidental breakage from the `INewsRepository` signature change.
- `./gradlew :app:assembleDebug` to confirm the module graph still compiles end to
  end (repository signature change ripples through DI).

### Manual (on-device / emulator)
1. **Chips load from backend:** open News with a test account that has ≥2 diseases
   and ≥1 allergy saved → confirm chip row shows `All, <disease1>, <disease2>,
   <allergy1>` (not just `All`). Cross-check against the `Timber` log line
   `"News chips loaded: N total -> ..."` already added in a prior change.
2. **All = union, not generic headlines:** with the same account, tap `All` and
   confirm the articles returned are relevant to the saved conditions/allergies
   (spot-check titles), not a generic health-category feed.
3. **Specific chip:** tap a disease chip → confirm only that chip is visually
   selected (single-select, `All` deselects) and the returned articles are on-topic
   for that condition.
4. **Re-tap deselect:** tap the already-selected chip again → confirm it falls back
   to `All` and refetches the union.
5. **Search + chip combination:** with a specific chip selected, type a search term,
   wait >350ms → confirm exactly one network call fires (check Logcat/network
   inspector) with the combined `(search) AND (keyword)` query, not one call per
   keystroke.
6. **Search submit button:** type quickly and tap the circular search button before
   the debounce window elapses → confirm the fetch fires immediately.
7. **Discover fallback:** with a test account that has *no* saved diseases/allergies,
   confirm the chip row shows only `All`, the card label reads `Discover` (not
   `For You`), and searching runs a plain text query.
8. **Partial failure resilience (requires a way to simulate one interest's request
   failing, e.g. airplane-mode toggle mid-flight or a debug network-fault flag):
   confirm articles from the succeeding interests still render, and the screen only
   shows the load-error state if *all* interest requests fail.
9. Regression pass on the rest of the News screen (image size, search bar shape,
   header alignment, three-dot menu) to confirm this change didn't touch that layout
   code, per §4.4.
