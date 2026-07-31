# News UI Redesign — Match "Discover" Screenshot- Implmentation of NewsHome and NewsDescriptions Screen With Backend Integration

## 0. Purpose and Scope of This Document

This document is the complete, exhaustive engineering plan for redesigning the News feature's list screen (and, in Part 2, the News Details screen) to match the provided "Discover" screenshot pixel-for-pixel, while strictly following the app's existing architectural rules: no hardcoded strings, no hardcoded colors, no hardcoded fonts, clean architecture separation between data/domain/presentation layers, and a fully centralized theme system (`AppTheme.colors`, `AppTheme.typography`, `AppTheme.shapes`, `AppTheme.dimens`). Every section below expands on the "what" with a corresponding "why," a description of the current state, the target state, the intermediate steps required to get there, the risks involved, and the verification steps needed to confirm correctness. This plan intentionally over-explains each area so that any engineer — regardless of prior familiarity with the NutriScan codebase — can pick up any single section and execute it without needing additional context from a meeting or a Slack thread.

This is a living planning document. As implementation proceeds, checkboxes and status annotations should be added inline so that reviewers can see, at a glance, which parts of the plan are complete, in progress, or blocked. Where a step depends on a decision that hasn't been made yet (for example, exact hex values for a new gray tone), that dependency is called out explicitly rather than silently assumed.

---

## 1. Background and Motivation

### 1.1 Why We Are Doing This Redesign

The News feature was originally implemented as a fairly generic list screen: a centered app logo in the header, shadow-based Material cards, and a simple three-dot overflow menu on each card for sharing. While functional, this treatment does not match the visual language of the rest of the app, which has since moved toward a flatter, more editorial "Discover"-style aesthetic used elsewhere in the product (notably in the Recipes and Articles-adjacent surfaces). Design has now provided a reference screenshot showing exactly how the News list screen should look once brought in line with this newer aesthetic, and the goal of this plan is to translate that screenshot, element by element, into concrete code changes.

### 1.2 Design Philosophy Behind the New Screen

The new design favors:
- Flat surfaces over elevated/shadowed cards, using dividers instead of shadows to separate list items.
- A stronger editorial hierarchy: large title, subdued subtitle, and a clear "category" label per article that mimics a real news app.
- Filled, high-contrast selected states for filter chips instead of subtle outlined selection, making the current filter obvious even to visually-impaired or low-attention users.
- Reduced chrome per row (removing the three-dot share menu) in favor of a cleaner, denser list that puts more emphasis on the content itself and pushes secondary actions (like sharing) into the detail screen instead.
- A dedicated search affordance directly in the list screen, which did not exist before, reflecting user feedback that finding a specific topic in the News feed was previously only possible via the topic chips.

### 1.3 Constraints We Must Respect

Whatever we build must:
1. Never hardcode a string literal in a Composable — every user-visible piece of text must be a string resource, with matching Arabic translations, so that the app's existing localization pipeline continues to work without manual intervention.
2. Never hardcode a color value (no raw `Color(0xFF...)` calls inside screen or component code) — every color must be defined once in the centralized `AppColors` theme object and referenced via `AppTheme.colors.<TokenName>`, so dark mode and any future re-theming continue to "just work."
3. Never hardcode a font family, size, or weight inline — all text styling must go through `AppTheme.typography`.
4. Preserve the existing clean architecture boundaries: the presentation layer (Composables, ViewModel, MVI state/event/effect classes) must not reach into the data layer directly; all data access continues to flow through existing use cases / repositories.
5. Not regress existing functionality that is out of scope for this redesign (e.g., pull-to-refresh, pagination, and analytics events already wired into the News feature) unless explicitly called out as removed or changed.

### 1.4 Non-Goals

This plan explicitly does NOT cover:
- Backend or API changes. The shape of the News API response (article title, description, source, published date, url, image url) is assumed unchanged.
- Analytics platform changes, other than added event names required by new interactions (search, filter icon tap) if analytics for those already exists elsewhere in the app's conventions.
- Push notification behavior related to news.
- Any change to how articles are fetched, cached, or paginated at the repository/use-case level, except where the search functionality requires new client-side filtering logic in the ViewModel.

---

## 2. Screenshot Analysis (Expanded)

The table below is the original condensed summary. Each row is then expanded into its own subsection with much more detail on exact visual treatment, spacing, and interaction behavior, since "matching the screenshot exactly" requires more precision than a one-line table cell can convey.

| Element | Current | Target |
|---|---|---|
| **Header** | Back button + centered NutriScan logo | Back button + **"Discover"** large title + subtitle "News from all around the world" |
| **Search Bar** | None | Search bar with filter icon below header |
| **Chip Row** | Outlined border-only chips | Selected chip has **filled teal background + white text**, unselected has **gray outlined** |
| **Article Card** | Shadow card, teal title, source bold teal, time at bottom | Flat card with divider, **category label in teal** ("News"/"Health"), **bold dark title**, source row with avatar icon + dot separator + time |
| **Card Image** | 137×140, left side | **100×100 rounded** thumbnail, left side |
| **Card Actions** | Three-dot menu (share) | Removed — card is simpler |

### 2.1 Header — Detailed Breakdown

**Current behavior:** The header is a simple top app bar with a back/navigation icon on the leading edge and the NutriScan wordmark/logo centered horizontally. This treatment is shared across several unrelated screens in the app and was never specific to News.

**Target behavior:** The header becomes a two-line, left-aligned text block:
- Line 1: "Discover" rendered in a large, bold display-style typography token (something in the range of the app's `AppTheme.typography.HeadlineLarge` or equivalent — exact token to be confirmed against the existing typography scale so we don't introduce a new one-off size).
- Line 2: "News from all around the world" rendered in a smaller, muted subtitle style, using a secondary text color token (e.g., `AppTheme.colors.Gray700` or whatever the canonical "muted body text" token is named in this codebase).
- The back button remains present but shifts from being the dominant visual element (previously paired with a centered logo) to a lightweight, top-leading icon button that sits above or beside the title block, depending on final spacing decisions.

Design rationale: This mirrors a common "editorial home" pattern seen in many news-aggregator apps (large title + descriptive subtitle), replacing the more generic "logo in every screen" pattern, which becomes repetitive and doesn't help the user understand what screen they're on.

Open questions to resolve before implementation is "done" (not blocking, but should be tracked):
- Should the back button remain visible on this screen at all, given it's called "Discover" and might be a top-level destination reached from a bottom nav bar rather than a pushed screen? Assumption for this plan: yes, keep it, since the existing navigation graph pushes News as a detail-level route from Home, not as a tab destination. This assumption should be verified with design before final sign-off.
- Exact vertical spacing between "Discover" and the subtitle line — to be pulled from the screenshot's pixel measurements once available in a design tool (Figma) rather than eyeballed.

### 2.2 Search Bar — Detailed Breakdown

**Current behavior:** No search affordance exists anywhere in the News feature. The only way to narrow the list is via the topic chip row.

**Target behavior:** A new search bar component appears directly below the header block and directly above the chip row. Visually:
- Rounded "pill" shape (large corner radius, likely using a `Shapes.full` or `RoundedCornerShape(percent = 50)` token from the centralized shape system).
- Light gray fill background, distinguishable from the white/near-white screen background but not so dark that it competes visually with the header.
- A search (magnifying glass) icon anchored to the leading edge, inside the pill.
- Placeholder text "Search" in a muted gray tone, left-aligned after the icon.
- A filter icon anchored to the trailing edge of the pill, functioning as a secondary/tertiary action (tapping it triggers a `FilterClicked` event — see the MVI section below for what this should eventually do, even if the initial implementation is a no-op placeholder or opens a bottom sheet that is stubbed for now).

Interaction behavior:
- Typing in the search field updates `NewsState.searchQuery` via a `SearchQueryChanged` event.
- The ViewModel filters the already-fetched article list client-side (see section 5) rather than issuing a new network request per keystroke, at least for this iteration, to avoid introducing new debounce/network logic that isn't strictly required by the screenshot.
- Clearing the search text (either by deleting all characters or via a potential future "clear" affordance) resets the visible list back to the full, unfiltered set for the currently selected chip.

Accessibility notes:
- The search field must have a proper content description / label so screen readers announce it as a search field, not a generic text field.
- The filter icon must have a content description sourced from `news_search_filter_content_description` (see string resources section) rather than an inline literal.

### 2.3 Chip Row — Detailed Breakdown

**Current behavior:** All chips (selected or not) use the same outlined, border-only visual treatment, differentiated only subtly (likely via border color or weight) to indicate selection. This is visually weak and can be missed by users at a glance.

**Target behavior:**
- **Selected chip:** Solid filled background using the app's primary teal token (`AppTheme.colors.Teal1000`), with white text on top, and no visible border. This is a strong, unambiguous "this is currently active" signal.
- **Unselected chips:** White (or fully transparent, matching whatever the screen background is) fill, with a thin gray border (`Gray400` or similar token) and gray text (`Gray700` or similar token) rather than black, to visually recede behind the selected chip.
- Chip row remains horizontally scrollable (assumption — to be confirmed against current implementation, but almost certainly already the case since topic lists can exceed screen width).
- Padding and spacing between chips should be preserved as-is unless the screenshot clearly shows a change; this redesign is primarily about fill/border/text color rather than layout/spacing of the chip row itself.

Interaction behavior is unchanged: tapping a chip re-triggers article loading/filtering scoped to that topic, exactly as it does today. Only the visual treatment changes.

### 2.4 Article Card — Detailed Breakdown

This is the most visually significant change in the whole redesign, so it gets the most detailed breakdown.

**Current card layout (as currently implemented):**
- A `Card`-like container with Material elevation/shadow.
- An image on the left sized 137dp × 140dp.
- To the right: a teal-colored title, a bold teal source name, and a "time ago" label positioned at the very bottom of the card.
- A three-dot overflow icon somewhere on the card (top-right, presumably) that opens a menu with at least a "Share" action.

**Target card layout (from screenshot):**
- No shadow/elevation at all. The card is visually "flat" — it sits directly on the screen background.
- Separation between consecutive cards is achieved via a **thin horizontal divider line** at the bottom of each card (or equivalently, between each row in the list), not via card elevation or margin+shadow.
- Image shrinks from 137×140 down to a smaller, squarer **100dp × 100dp**, with **rounded corners** (something like 12dp corner radius, to be confirmed pixel-exactly from the screenshot) rather than being a plain rectangle. It remains left-aligned in the row.
- To the right of the image, from top to bottom:
  1. A **category label** in teal text (e.g., "News" or "Health" depending on the article's associated topic/chip) — this is new; the current card has no equivalent element.
  2. The **article title**, now in a bold, dark neutral color (NOT teal as before) — this is a significant color change from the current teal title treatment. Truncates at 2 lines with ellipsis.
  3. A **source row** consisting of: a small circular avatar icon (likely a placeholder circle, possibly showing the first letter of the source name, matching the pattern later described for the Details screen's avatar), the source name, a **dot separator** ("·"), and the "time ago" label — all on one line, replacing the previous layout where the time was pinned to the bottom in isolation from the source name.
- The **three-dot menu / share action is removed entirely** from the card. The list becomes simpler and denser as a result. Sharing, if needed, is expected to move to the (new) News Details screen instead (see Part 2).

Rationale for removing the shared action from the card: the screenshot shows no such affordance, and design intent appears to be "tap the card to read", with secondary actions like sharing living on the detail screen where there's more room and more context (author, full description, etc.) to justify an explicit share button.

### 2.5 Card Image — Detailed Breakdown

- Dimensions change from 137×140 (a slightly portrait-oriented rectangle) to a perfectly square 100×100.
- Corner radius: previously either square or very slightly rounded (to be confirmed from current code); target explicitly rounds all four corners with a radius large enough to be clearly visible as "rounded thumbnail" rather than "barely-rounded rectangle." A reasonable default assumption, pending exact screenshot measurement, is 12dp, consistent with rounded corners used elsewhere in the app (e.g., avatar containers, other thumbnail treatments).
- Placeholder / loading state: while the image loads (or if the image fails to load / is null), a placeholder should be shown. This should reuse whatever existing placeholder/fallback drawable or shimmer approach is already used elsewhere in the app for network images, rather than introducing a new one specific to News.
- Content scale: `ContentScale.Crop` is almost certainly correct here (as it likely already is) so that arbitrary source aspect ratios don't distort inside the new square frame.

### 2.6 Card Actions — Detailed Breakdown

- The three-dot / overflow menu Composable currently embedded in the card should be deleted, not merely hidden, since we are also removing the corresponding `ShareClicked` event and `ShareArticle` effect from the MVI contracts (see section 5). Leaving dead code around "just in case" runs contrary to the clean architecture principle of not carrying unused surface area.
- Any menu-related test coverage (unit tests on the ViewModel handling `ShareClicked`, UI tests tapping the three-dot menu) must be removed or updated in the same change, to avoid leaving broken/orphaned tests in the suite.

---

## 3. Proposed Changes (Expanded)

This section takes each of the original bullet-point proposed changes and expands them into full sub-plans, including exact rationale, edge cases, and sequencing considerations (i.e., what must be done before what).

### 3.1 String Resources

#### 3.1.1 [MODIFY] `strings.xml` (default/English)

The following string resources must be added or changed. Each is listed with its key, its value, and a note on where it's consumed.

- `news_screen_title` → changed from whatever the previous NutriScan-branded title was, to **"Discover"**. Consumed by the header title text in `NewsScreen.kt`.
- `news_screen_subtitle` → **"News from all around the world"**. New string; consumed by the header subtitle text in `NewsScreen.kt`.
- `news_search_placeholder` → **"Search"**. New string; consumed as the placeholder/hint text inside `NewsSearchBar.kt`.
- `news_search_filter_content_description` → **"Filter"**. New string; consumed as the `contentDescription` for the filter icon inside `NewsSearchBar.kt`, ensuring the icon is accessible to screen readers.
- `news_category_label` → **"News"** (used as a default/fallback category label when an article's real category/topic can't be determined). Consumed inside `NewsArticleCard.kt` and/or the ViewModel's mapping logic that produces `NewsUiArticle.category`.
- `news_source_time_separator` → **" · "** (a single dot surrounded by thin spaces, used between the source name and the "time ago" text in the source row). Consumed inside `NewsArticleCard.kt`'s source row Composable.
- `news_source_avatar_content_description` → **"Source avatar"**. New string; consumed as the `contentDescription` of the small circular avatar icon in the source row, so screen readers don't announce it as an unlabeled image.

Additional considerations:
- Every one of these keys must be verified against the existing `strings.xml` to ensure no name collision with an already-existing key used elsewhere in the app (a quick search/grep across the resource file before adding is a cheap and worthwhile sanity check).
- If `news_screen_title` was previously reused by other Composables (for example, in a shared top bar component used by multiple screens), changing its value to "Discover" could unintentionally affect those other screens. This must be checked; if the key is shared, we should instead introduce a **new** key (e.g., `news_discover_title`) rather than mutating a shared one, and only reference the new key from `NewsScreen.kt`.

#### 3.1.2 [MODIFY] `strings.xml` (Arabic / `values-ar`)

Every string added or changed above must have a matching, contextually accurate Arabic translation added to `values-ar/strings.xml`, so that RTL/Arabic users see a fully localized screen rather than falling back to the English default (which the Android resource system would otherwise silently do, masking the omission until a QA pass in Arabic locale catches it).

Translation guidance (not final approved copy, but a reasonable placeholder set for engineering purposes, to be reviewed by a native speaker/localization reviewer before shipping):
- `news_screen_title` → an Arabic equivalent of "Discover" (e.g., "اكتشف").
- `news_screen_subtitle` → an Arabic equivalent of "News from all around the world" (e.g., "أخبار من جميع أنحاء العالم").
- `news_search_placeholder` → Arabic equivalent of "Search" (e.g., "بحث").
- `news_search_filter_content_description` → Arabic equivalent of "Filter" (e.g., "تصفية").
- `news_category_label` → Arabic equivalent of "News" (e.g., "أخبار").
- `news_source_time_separator` → likely unchanged (a dot separator generally doesn't need translation, but it should still be defined as a string resource rather than hardcoded per the app's rules, and confirmed it renders correctly in RTL context, i.e., doesn't visually invert in a confusing way).
- `news_source_avatar_content_description` → Arabic equivalent of "Source avatar" (e.g., "صورة المصدر").

Important RTL consideration: the entire redesigned screen (header, search bar with leading/trailing icons, chip row scroll direction, and the article card's image-left/content-right layout) needs to be verified under RTL layout direction, since Compose's `LayoutDirection.Rtl` will naturally mirror `Row` ordering unless explicitly overridden. Specifically: does the article image move to the right-hand side in RTL, with content on the left? Does the search bar's search icon move to the trailing edge and filter icon to the leading edge? These are exactly the kinds of details that are easy to get "technically translated" but visually wrong, so this must be part of the manual verification pass (see section 6).

### 3.2 News State (MVI)

#### 3.2.1 [MODIFY] `NewsState.kt`

Two changes are required to the state class:

1. **Add `searchQuery: String = ""` to `NewsState`.**
   - This field holds the live text currently typed into the search bar.
   - Defaulting to an empty string ensures existing code paths that construct `NewsState()` without arguments (e.g., in tests, or as an initial ViewModel state) continue to compile without modification.
   - This field should be treated as the single source of truth for what's currently in the search box — the Composable should not maintain its own separate local `remember { mutableStateOf("") }` for the text field value, in order to keep the architecture unidirectional (state flows down from ViewModel, events flow up from UI) and avoid the classic bug where UI-local state and ViewModel state drift out of sync.

2. **Add `category: String` field to `NewsUiArticle`.**
   - This is a new field on the UI-facing article model (as opposed to the domain-level `Article` model, which should NOT need to change, keeping this a presentation-layer-only concern).
   - The value is derived at mapping time (see ViewModel changes below) from either the article's associated topic/chip, or falls back to the default `news_category_label` string resource value ("News") if no more specific category can be determined.
   - This field is purely for display purposes in `NewsArticleCard.kt`'s new category label text.

Additional considerations:
- Since `NewsUiArticle` is likely a `data class`, adding a new field is technically a breaking change for any code that constructs `NewsUiArticle` positionally without named arguments; a repo-wide search for `NewsUiArticle(` should be performed to catch and update all call sites, including any test fixtures/mocks that build sample `NewsUiArticle` instances for previews or unit tests.
- Consider giving `category` a sensible default value in the data class declaration itself (e.g., defaulting to an empty string, with the actual display fallback logic resolved at the Composable layer via the `news_category_label` string resource) versus resolving the fallback at mapping time in the ViewModel. Either approach is architecturally defensible; this plan recommends resolving it at mapping time in the ViewModel so that `NewsArticleCard.kt` doesn't need to reach for string resources tied to "business rules" (what counts as a fallback category) versus purely presentational strings.

#### 3.2.2 [MODIFY] `NewsEvent.kt`

Three changes:

1. **Add `SearchQueryChanged(query: String)` event** — fired every time the user types/deletes a character in the search bar. The ViewModel receiving this event updates `NewsState.searchQuery` and recomputes the filtered article list.
2. **Remove `ShareClicked` event** — since the three-dot share menu is being removed from the card entirely, this event no longer has any UI trigger and should be deleted rather than left dangling. Any ViewModel `when` branch handling it must also be removed (see ViewModel section).
3. **Add `FilterClicked` event** — fired when the user taps the filter icon inside the new search bar. For this iteration, the exact behavior this should trigger (e.g., opening a bottom sheet with advanced filter options, or simply being a stubbed/no-op placeholder for a future feature) is not fully specified by the screenshot alone, since a static image cannot show what happens after a tap. This plan recommends implementing it as a stub that, for now, does nothing more than perhaps log an analytics event or is entirely a no-op, with a `// TODO:` comment clearly marking it as intentionally unimplemented pending further product/design direction. This avoids scope creep into designing an entire filter bottom sheet UI that wasn't part of the original screenshot brief.

#### 3.2.3 [MODIFY] `NewsEffect.kt`

One change:

- **Remove `ShareArticle` effect** — this was presumably consumed by a `LaunchedEffect` in `NewsScreen.kt` that triggered an Android share `Intent` when the user tapped "Share" in the old three-dot menu. Since that entire interaction path is removed from the list screen, this effect becomes dead code and must be deleted. Any corresponding `LaunchedEffect` branch in `NewsScreen.kt` handling `NewsEffect.ShareArticle` must also be removed (see Main Screen section below).
- Note: sharing an article is not gone from the *app* entirely — it is expected to reappear in the new Details screen (Part 2 of this plan), which will have its own share button and, presumably, its own effect (or will handle the share `Intent` directly/locally) scoped to that screen's own MVI contracts, entirely separate from the list screen's now-simplified contracts.

### 3.3 ViewModel

#### 3.3.1 [MODIFY] `NewsViewModel.kt`

Several distinct pieces of logic need to change or be added:

1. **Handle `SearchQueryChanged` event → filter articles client-side by title/description.**
   - On receiving this event, update `NewsState.searchQuery` to the new value.
   - Recompute the visible/filtered article list by checking whether the article's title OR description contains the search query as a substring, case-insensitively. This should almost certainly be implemented using something like `article.title.contains(query, ignoreCase = true) || article.description?.contains(query, ignoreCase = true) == true`, guarding against a null description.
   - This filtering should be layered on top of (i.e., applied after) whatever topic/chip-based filtering is already happening, so that a user who has selected the "Health" chip and then types a search query sees articles that are both in the Health category AND match the search text — not one or the other.
   - Consider whether this filtering should happen against the full, already-fetched list held in memory, or whether it should trigger a fresh network fetch scoped by the search term. This plan recommends purely client-side/in-memory filtering for this iteration, since (a) the screenshot doesn't indicate any loading spinner behavior tied to typing, suggesting instant/local filtering is the intended UX, and (b) introducing debounced network search is a meaningfully larger scope increase that should be its own follow-up plan if truly desired.
   - Edge case: what happens if the user clears the chip-filtered list down to zero results after typing a search query that matches nothing? The existing "empty state" UI (assuming one already exists for the "no articles for this topic" case) should be reused here rather than building a second, search-specific empty state, unless the existing empty state's copy is specifically about topics/chips and would read oddly in a "no search results" context — in which case, a new, more generic or parameterized empty-state string may be warranted as a small follow-up.

2. **Remove `ShareClicked` handling.**
   - Delete the `when` branch (or equivalent handling logic) in the event-processing function that previously handled `NewsEvent.ShareClicked` and emitted `NewsEffect.ShareArticle`.
   - Delete any now-unused helper functions that only existed to support that flow (for example, a function that built a share `Intent` payload from an article, if such logic lived in the ViewModel rather than the Composable).

3. **Map chip label into `NewsUiArticle.category`.**
   - When mapping domain `Article` models into `NewsUiArticle` for display, populate the new `category` field using the currently selected topic/chip's label if the article is associated with a specific topic, or the `news_category_label` string resource default ("News") if not.
   - This mapping logic should live in whichever existing mapper function already converts domain articles into `NewsUiArticle` (rather than being scattered inline in multiple places), consistent with the existing clean architecture pattern of centralizing model-to-UI-model mapping.

Testing implications: existing ViewModel unit tests that assert on `ShareClicked` handling must be deleted or rewritten; new unit tests must be added covering: (a) that `SearchQueryChanged` correctly narrows the visible list, (b) that combining a selected chip with a search query correctly intersects both filters, (c) that clearing the search query restores the full chip-filtered list, and (d) that `category` is correctly populated on mapped `NewsUiArticle` instances, both when a topic is present and when it falls back to the default.

### 3.4 UI Components

#### 3.4.1 [MODIFY] `NewsTopicChipRow.kt`

Restyle chips to match screenshot, specifically:

- **Selected chip:**
  - Background: `AppTheme.colors.Teal1000` (filled, solid).
  - Text color: white (should reference whatever the canonical "on-teal" or "on-primary" text color token is in this theme system — likely something like `AppTheme.colors.White` or an explicit "OnPrimary" token if one exists; avoid introducing a raw `Color.White` literal if a themed equivalent already exists, for dark-mode consistency, though white-on-teal is likely correct in both light and dark mode since the teal itself presumably doesn't change enough between modes to need a different foreground).
  - No border.
- **Unselected chip:**
  - Background: white/transparent (matching screen background — use whatever surface/background token the screen itself uses, so the chip visually "disappears" into the background except for its border).
  - Border: thin, gray (`AppTheme.colors.Gray400` or equivalent token — exact token name to be confirmed against whatever gray scale already exists in `AppColors.kt`).
  - Text color: gray (`AppTheme.colors.Gray700` or equivalent — deliberately not pure black, to keep unselected chips visually receding relative to the bold selected one).

Implementation notes:
- This is likely a small, surgical change to whatever conditional styling logic already exists inside the chip Composable (e.g., a `if (isSelected) { ... } else { ... }` block choosing background/border/text colors) — the underlying chip shape, padding, and click handling almost certainly do not need to change, only the color tokens referenced.
- Confirm corner radius/shape of the chip is unchanged from the current implementation unless the screenshot clearly shows otherwise (the original proposed-changes summary did not call out a shape change, only a color/fill change).
- Preview functions (`@Preview` Composables) for this component should be updated/added to show both selected and unselected states side-by-side, so any future changes can be visually diffed quickly in Android Studio's preview pane without running the full app.

#### 3.4.2 [MODIFY] `NewsArticleCard.kt`

This is the single largest and most involved UI change in the entire plan. Breaking it into sub-steps:

1. **Layout:** Change from whatever shadow-`Card`-based container currently wraps the row, to a plain `Row` (or a `Surface` with zero elevation and no shadow, if a `Surface` is still desired for semantic/clickable-ripple purposes) containing the image on the left and a `Column` of content on the right, with a thin `Divider`-equivalent Composable placed at the very bottom of each card/row (either as part of the card itself, or as a separator between items in the parent `LazyColumn`, whichever is architecturally cleaner and consistent with how dividers are handled elsewhere in the app, if anywhere).

2. **Image:** Change size from 137×140 to a perfectly square 100×100, and apply rounded corners (approx. 12dp, pending exact confirmation) via `clip(RoundedCornerShape(12.dp))` or the equivalent themed shape token if one exists for "thumbnail" or "small rounded image" elsewhere in the codebase (reuse rather than reinvent if such a token exists).

3. **Category:** Add a new `Text` Composable above the title, styled in teal (`AppTheme.colors.Teal1000` or whatever the "category label" token ends up being named in `AppColors.kt`, per the new `NewsCategoryLabel` color token proposed in section 3.5), displaying `NewsUiArticle.category`.

4. **Title:** Change color from teal to a dark/bold neutral tone (`AppTheme.colors.Gray1600` or equivalent "near-black" token, and — per the dark mode note in the original color list — `Teal300` when in dark mode, presumably because a very dark near-black might have insufficient contrast or might look visually "off" against a dark background, whereas a lighter, muted teal reads better; this dark-mode-specific override is unusual enough that it should be flagged and double-checked visually once dark mode is implemented, since it's an atypical pattern compared to how most other text colors in this app probably just use a semantic "primary text" token that's simply light or dark depending on mode, rather than switching hues entirely). Font weight becomes bold; max lines remain 2 with ellipsis truncation, consistent with the current implementation's truncation behavior (only the color and weight change, not the truncation logic itself, unless testing reveals the new bold weight causes layout/line-height issues that didn't exist with the previous font weight).

5. **Source Row:** Replace the current "bold teal source name, plus a separately bottom-pinned time label" layout with a single horizontal `Row` containing, in order: a small circular avatar (a simple colored circle is sufficient for now — using `NewsSourceAvatarBg` teal fill per the new color token list — with no letter/initial inside it unless later decided to mirror the Details screen's letter-avatar pattern; the original card proposed-changes summary says "avatar icon" without specifying letter content, so a plain colored circle is the safe minimal interpretation unless the screenshot clearly shows a letter inside it, in which case the implementation should match the screenshot exactly), the source name (styled with the `NewsSourceText` gray token, no longer bold/teal as before), the dot separator string resource (`news_source_time_separator`), and the "time ago" text, all in the same visual weight/color so they read as a single cohesive metadata line rather than the source name visually competing with the title for attention (which was arguably a weakness of the old design where the source was bold and teal, nearly as visually loud as the title itself).

6. **No shadow:** explicitly set elevation to 0 (or remove the `Card`'s default elevation entirely if switching to a plain `Row`/`Surface`), and add a subtle bottom divider instead, using the new `NewsDivider` color token (`Gray300` or equivalent).

7. **Remove `onShareClick` parameter:** delete this parameter from the Composable's function signature entirely, along with the three-dot icon button and its associated dropdown/menu Composable that consumed it. Any call site constructing `NewsArticleCard(...)` with an `onShareClick = { ... }` argument must be updated to drop that argument (this will be a compile error until fixed, which is actually a helpful forcing function to ensure no call site is missed).

8. **Update shimmer card to match new layout:** the loading/skeleton placeholder version of this card (commonly implemented as a near-identical Composable with shimmering gray blocks instead of real content, shown while articles are being fetched) must be updated to mirror the new dimensions and layout exactly — specifically, the shimmer's image placeholder block must shrink from the old 137×140 to the new 100×100 rounded shape, and the shimmer's text placeholder blocks must be repositioned/resized to approximate the new category+title+source-row structure rather than the old title+source+time structure, so that the loading state doesn't visually "jump" or resize awkwardly once real content replaces it.

#### 3.4.3 [NEW] `NewsSearchBar.kt`

A brand new Composable file/component, since nothing like this currently exists in the News feature. Requirements:

- **Shape:** Rounded pill shape — a large corner radius, effectively fully rounded ends (e.g., `RoundedCornerShape(percent = 50)` or the equivalent themed "pill"/"full" shape token if the app's `AppTheme.shapes` already defines one, which should be preferred over a raw literal for consistency with the "no hardcoded values" rule, since shapes are conceptually similar to colors/fonts in that they should be centrally themed where a suitable token already exists).
- **Background:** Light gray fill (`NewsSearchBarBg`, a new `Gray100`-equivalent token per section 3.5).
- **Border:** A subtle gray border (`NewsSearchBarBorder`, `Gray400`-equivalent) — to be confirmed against the screenshot whether a border is actually visible or whether the fill color alone provides sufficient visual separation from the white screen background; if the screenshot shows no visible border, this token/usage should be dropped to avoid an unnecessary visual element not actually present in the design.
- **Leading icon:** A search/magnifying-glass icon, tinted with `NewsSearchIconTint` (`Gray700`-equivalent).
- **Trailing icon:** A filter icon, tinted with `NewsFilterIconTint` (`Gray700`-equivalent), wired to emit the `FilterClicked` event when tapped.
- **Placeholder text:** Sourced from `news_search_placeholder` ("Search"), styled with an appropriately muted text color and the app's standard body typography token.
- **Text field behavior:** The actual typed value should be a controlled input bound to `NewsState.searchQuery` (passed in as a parameter) with an `onValueChange` lambda that emits `NewsEvent.SearchQueryChanged(it)` up to the ViewModel, rather than maintaining independent local Compose state, per the unidirectional data flow principle described in section 3.2.1.
- **Colors/typography:** All colors from `AppTheme.colors`, typography from `AppTheme.typography` — no raw values anywhere in this new file, consistent with every other rule in this plan.
- **Accessibility:** The filter icon must carry the `news_search_filter_content_description` content description; the text field itself should have an appropriate semantic role so screen readers correctly announce it as a search input rather than a generic text field (Compose's `Modifier.semantics { }` or the platform-provided search-field semantics, if such a convenience already exists in the Compose version this app targets, should be used here).
- **Preview:** Add at least one `@Preview` Composable showing the search bar in both an empty state (showing placeholder text) and a populated state (showing a sample typed query), so its appearance can be sanity-checked in isolation without running the full News screen.

### 3.5 Main Screen

#### 3.5.1 [MODIFY] `NewsScreen.kt`

Restructure the screen's top-level layout to match the screenshot, specifically:

1. **Header:** Replace the old back-button-plus-centered-logo top bar with: a back button (retained, per the assumption discussed in section 2.1) positioned at the top-leading edge, followed immediately below (or beside, depending on final spacing decisions once pixel measurements are available) by the large "Discover" title text and, beneath that, the "News from all around the world" subtitle text — both sourced from the new `news_screen_title` and `news_screen_subtitle` string resources respectively.
2. **Search bar:** Insert the new `NewsSearchBar` Composable directly below the header block, passing `state.searchQuery` and wiring its callback to dispatch `NewsEvent.SearchQueryChanged` (and, separately, `NewsEvent.FilterClicked` for the filter icon tap) to the ViewModel via whatever the existing event-dispatch mechanism is (likely a lambda like `onEvent: (NewsEvent) -> Unit` passed down from the screen's composition root).
3. **Chip row:** No structural change to where the chip row sits in the overall vertical layout — it remains directly below the search bar, in the same relative position it previously occupied directly below the header (only its internal styling changes, as covered in section 3.4.1).
4. **Article list:** The `LazyColumn` (or equivalent) rendering the list of `NewsArticleCard` composables needs to switch from item-to-item vertical spacing (likely achieved today via `Arrangement.spacedBy(...)` combined with each card having its own shadow/elevation to visually separate itself from its neighbors) to relying on the new per-card bottom divider for visual separation instead, meaning the explicit spacing value between items may need to be reduced or removed entirely so the list reads as a continuous, divided list rather than a series of visually separated floating cards. This is a subtle but important layout change: simply adding dividers on top of the existing spacing would likely look wrong (too much whitespace between a card's own bottom divider and the next card), so the two changes (removing shadow, adding divider) must be considered together as a single cohesive layout adjustment rather than two independent tweaks.
5. **Remove share intent handling from `LaunchedEffect`:** the screen's top-level `LaunchedEffect` block that presumably observes `NewsEffect` values and, upon receiving `NewsEffect.ShareArticle`, launches an Android share `Intent`, must have that specific branch removed, consistent with the effect itself being deleted from `NewsEffect.kt` in section 3.2.3. If the `LaunchedEffect` block handles multiple effect types in a single `when` statement, only the `ShareArticle` branch is removed; other branches (e.g., navigation effects, error-toast effects) remain untouched.

Additional considerations for this file:
- Given the number of structural changes, this is a good candidate for splitting the screen's top-level Composable into smaller, named sub-Composables (e.g., a private `NewsHeader(...)` Composable, distinct from the screen-level `NewsScreen(...)` entry point) purely for readability and testability, even though the original proposed-changes summary doesn't explicitly call this out — this is a reasonable engineering judgment call to make during implementation, not a strict requirement of the screenshot itself.
- Existing UI/instrumentation tests (if any exist for this screen) that assert on the presence of the old NutriScan logo header, or that simulate tapping the three-dot share menu, must be updated or removed to reflect the new structure; new tests should be added asserting the "Discover" title and subtitle are displayed, the search bar is present and accepts input, and that typing into it correctly narrows the visible list of article cards (this last one may require either a fake/test ViewModel or a real one backed by an in-memory fake repository, depending on existing test infrastructure conventions in this codebase).

### 3.6 Theme Colors

#### 3.6.1 [MODIFY] `AppColors.kt`

Add News-specific color tokens to `AppColorsExtension3` (or whatever the correct existing extension object is — the original plan references `AppColorsExtension3` specifically, implying there are already at least two prior extension objects, `AppColorsExtension` and `AppColorsExtension2` presumably, following some existing convention in this codebase for grouping theme tokens; this convention should be followed exactly rather than introducing a fourth object with a different naming pattern, unless there's a clear reason — e.g., token count limits per object — that the existing pattern already accounts for).

Full list of new tokens, each with its light-mode intended value (as inferred from the plan) and any dark-mode consideration called out explicitly where the original plan mentions one:

- `NewsChipSelectedBg` — `Teal1000` (filled selected chip background).
- `NewsChipSelectedText` — `White` (text on top of the selected chip's teal fill).
- `NewsChipUnselectedBg` — Transparent/White (background of an unselected chip, effectively matching the screen background).
- `NewsChipUnselectedBorder` — `Gray400` (border color of an unselected chip).
- `NewsChipUnselectedText` — `Gray700` (text color of an unselected chip).
- `NewsCategoryLabel` — `Teal1000` (color of the small category label text above each article title).
- `NewsCardTitle` — `Gray1600` in light mode (dark, near-black neutral) / `Teal300` in dark mode (a lighter, muted teal, called out explicitly in the original plan as a dark-mode-specific override rather than a simple light/dark neutral swap — this asymmetry should be double-checked with design once dark mode is actually implemented and viewed, since it's an unusual pattern relative to typical "just invert lightness" dark-mode conventions).
- `NewsSourceText` — `Gray700` (color of the source name / time ago metadata row text).
- `NewsSourceAvatarBg` — `Teal1000` (fill color of the small circular avatar in the source row).
- `NewsSearchBarBg` — `Gray100` (background fill of the new search bar pill).
- `NewsSearchBarBorder` — `Gray400` (border color of the search bar, pending confirmation per section 3.4.3 as to whether a border is actually present in the screenshot).
- `NewsSearchIconTint` — `Gray700` (tint of the search/magnifying-glass icon).
- `NewsFilterIconTint` — `Gray700` (tint of the filter icon).
- `NewsDivider` — `Gray300` (color of the new thin horizontal divider line separating article cards).

Implementation considerations:
- Before adding any of these as brand-new tokens, cross-reference the existing color palette (`Teal1000`, `Gray100`, `Gray300`, `Gray400`, `Gray700`, `Gray1600`, `Teal300`, `White`, etc.) to confirm these base swatches already exist somewhere in the app's core color palette (as opposed to the News-specific semantic tokens being proposed here, which are meant to alias/reference those base swatches, not redefine new raw hex values). If any of the referenced base swatches (e.g., `Gray1600` or `Teal300`) do NOT already exist in the palette, that is a blocking dependency that must be resolved first — either by adding the missing base swatch to the core palette (with design sign-off on its exact hex value) or by re-mapping the semantic token to the nearest existing swatch as an interim measure, clearly flagged as such.
- These News-specific tokens are being defined as *semantic* aliases (e.g., `NewsCardTitle` aliasing to `Gray1600`) rather than having `NewsArticleCard.kt` reference `Gray1600` directly, which is the correct clean-architecture-for-theming approach: if design later decides article titles should be a slightly different shade than other "near-black" text elsewhere in the app, only the alias needs to change, not every call site.
- Dark mode: for every token above where a dark-mode-specific value isn't explicitly called out in the original plan (i.e., all of them except `NewsCardTitle`), the default assumption should be that the app's *existing* light/dark color-scheme-switching mechanism (however it works — likely a `LightColors`/`DarkColors` pair of objects providing different values for the same semantic token names) is followed, meaning each of these new tokens needs both a light-mode and dark-mode value defined, even though this plan (mirroring the original request) only explicitly discusses light-mode intended values for most of them. This is called out here explicitly so it isn't silently missed during implementation — a token defined only in the light scheme, with no dark-mode counterpart, is either a compile error (if the theming system requires exhaustive definitions) or a silent visual bug (if it falls back to some default), depending on how the theming system is structured, and either outcome should be treated as a defect to fix before considering this task complete.

---

## 4. Verification Plan (Expanded)

### 4.1 Manual Verification

- **Build the project** with `./gradlew assembleDebug` to verify compilation. This is the absolute minimum bar — a failing build blocks everything else. This should be run after each meaningfully-sized chunk of work (e.g., after the string resources and MVI contract changes, again after the ViewModel changes, again after each UI component change), not just once at the very end, so that compile errors are caught close to their source rather than accumulating into a single large, hard-to-debug failure at the end.
- **Visual inspection against the screenshot.** This should be done side-by-side, ideally with the running app and the reference screenshot open at the same time (or overlaid with reduced opacity, if the team has tooling for that kind of pixel-diffing), checking specifically: header title/subtitle font size and weight, spacing between header and search bar, search bar pill shape/fill/icon placement, chip selected/unselected colors, card image size/corner radius, category label color, title color/weight, source row icon/text/separator/time arrangement, and the presence/absence/thickness of the divider between cards.
- **Verify dark mode colors adapt correctly.** Toggle the device/emulator into dark mode and re-run the same visual inspection checklist above, paying particular attention to the `NewsCardTitle` token's explicit light/dark hue swap (`Gray1600` vs `Teal300`) called out in section 3.6.1, since this is the one token with genuinely different treatment (not just a lightness inversion) between modes and is therefore the most likely place for a dark-mode-specific visual bug to hide.
- **Verify Arabic/RTL layout works correctly.** Switch the device/app locale to Arabic and re-check: header text alignment (should likely become right-aligned rather than left-aligned), search bar icon positions (search icon should logically move to the layout's new "leading" edge, which is now the right side, with the filter icon moving to the new "trailing"/left edge — unless the design intentionally keeps icon positions fixed regardless of layout direction, which would be an explicit deviation from default RTL mirroring behavior and should be a conscious decision, not an accident), chip row scroll direction, and article card image position (does the image correctly move to the right side of the row in RTL, with text content on the left, mirroring the whole row rather than just flipping text alignment while leaving the image pinned to the same physical side?).

### 4.2 Additional Verification Steps Beyond the Original Plan

The original verification plan was quite minimal (three bullet points). Given the scope of this redesign, the following additional verification steps are strongly recommended:

- **Accessibility pass:** Use TalkBack (or the equivalent Android screen reader) to navigate through the redesigned screen and confirm: the header title/subtitle are announced sensibly, the search field is announced as a search input (not a generic text field), the filter icon announces "Filter" (from `news_search_filter_content_description`), each chip announces its label and selected/unselected state, and each article card's avatar announces "Source avatar" (from `news_source_avatar_content_description`) rather than being silently skipped or announced as an unlabeled image.
- **Empty state verification:** Confirm that typing a search query with no matching results shows a sensible empty state (per the discussion in section 3.3.1, item 1's edge case), rather than an awkward blank list or a crash.
- **Loading/shimmer state verification:** Confirm the updated shimmer card (section 3.4.2, item 8) visually matches the new card's dimensions closely enough that there's no jarring "pop"/resize when real content replaces the shimmer placeholders.
- **Regression check on removed functionality:** Since the three-dot share menu and its associated `ShareClicked`/`ShareArticle` MVI contracts are being fully removed from this screen, explicitly confirm there is no other, unrelated code path in the app that depended on those specific class/event names existing (for example, an analytics wrapper, a deep-link handler, or a different screen's test suite that imports and references `NewsEvent.ShareClicked` for some unrelated reason) — a project-wide search for the deleted symbol names before considering this task complete is a cheap, high-value safety check.
- **Performance sanity check:** Since search filtering is now happening client-side on every keystroke (section 3.3.1, item 1), confirm with a reasonably large fetched article list (e.g., several dozen to a hundred items, whatever a realistic "worst case" page size might be) that typing quickly into the search field doesn't introduce visible jank/lag in the list re-render. If it does, consider adding a lightweight debounce (e.g., 150–300ms) before re-filtering, even though this wasn't explicitly called for in the original plan — this is a reasonable, low-risk defensive addition given that unfiltered per-keystroke recomputation over a non-trivial list size is a known common source of jank in list-heavy screens.
- **Localization completeness check:** Run a lint/resource-check tool (if the project has one configured, such as Android Studio's built-in "Missing Translation" lint check) to confirm every new string resource added to the default `strings.xml` has a corresponding entry in `values-ar/strings.xml`, catching any translation that was accidentally missed during implementation.

---

## Part 2: News Details Screen Implementation Plan (Added July 31, 2026)

### 5. Goal (Expanded)

Implement a fully functional **News Details Screen** that displays detailed article contents when an article is clicked, replacing the current temporary behavior of opening the article directly in a Chrome Custom Tab. The new screen should match the provided Details screenshot exactly, and — consistent with every principle established in Part 1 of this plan — must fully support localization (including Arabic/RTL), light/dark color modes, and must reuse the app's centralized theme tokens rather than introducing any new hardcoded strings, colors, or fonts.

Why this change matters: today, tapping an article in the list immediately kicks the user out of the app's own visual context and into a browser-like Custom Tabs view, which is jarring and loses the app's branding/context entirely for what could otherwise be a much richer in-app reading experience (showing a nicely laid-out hero image, author byline, and lead paragraph before optionally deferring to the web only for the full article body). This also creates an opportunity to reintroduce the "share" functionality that was deliberately removed from the list screen's cards in Part 1 — moving it to a more contextually appropriate location, the details screen, where there's a clear, single, unambiguous "share this specific article" action rather than a per-row overflow menu cluttering a dense list.

### 6. Screenshot Analysis of Details UI (Expanded)

#### 6.1 Background & Header Image

- A full-bleed (edge-to-edge, no padding/margins) hero image occupies the top portion of the screen. "Full-bleed" here means it extends behind the status bar as well, i.e., the system status bar icons should likely be drawn in a light/white tint on top of the image (assuming the image is generally darker at the very top, or specifically because of the gradient overlay described next) rather than the app's default status bar icon tint, which may need to change depending on the current theme (light vs dark) versus what's needed specifically for legibility against a photo.
- Layered on top of this hero image, near the top edge: a close button (an "X" icon, not a back-arrow, signaling "dismiss this modal-like detail view" rather than "navigate back through a stack," though functionally it likely still just pops the back stack) positioned at the top-leading edge, and a share button positioned at the top-trailing edge, both rendered as icon buttons directly on top of the image (likely with a subtle scrim/circular semi-transparent background behind each icon to ensure legibility regardless of what's directly behind them in the photo).

#### 6.2 Metadata Overlay

- Overlaid at the bottom portion of the hero image, on top of a dark gradient scrim (a gradient that transitions from transparent at the top of the overlay region to a darker, semi-opaque black towards the very bottom, ensuring white text remains legible regardless of the underlying image's own colors/brightness at that specific spot) are, from top to bottom within that overlay region:
  1. A **category badge** (e.g., "News") — likely a small pill-shaped label, possibly reusing the same visual treatment/color token approach as the category label already introduced in Part 1's article card redesign (`NewsCategoryLabel`), though here it may be rendered as a filled badge rather than plain text, given it's sitting on top of a photo rather than a plain background, and plain teal text might not have sufficient contrast against an arbitrary photo the way it does against a flat white/gray card background.
  2. The **author's name**.
  3. The **title text** of the article, in large, bold, white text.
  4. The **source publisher name**.
  5. The **time ago** label.
- All of the above text elements sit on top of the image within this gradient region and must be white or near-white to remain legible against the dark gradient, regardless of light/dark app theme — this is one of the rare cases in this app where the text color is likely NOT theme-dependent (i.e., it should probably stay white in both light and dark app mode, since it's sitting on top of a photo with a fixed dark scrim, not on top of a themed surface color) and this distinction should be explicitly called out in the color token naming (e.g., a token like `NewsDetailOverlayText` that is deliberately the same white value in both the light and dark theme variants, rather than accidentally being wired up as a normal semantic "primary text" token that would incorrectly flip to a dark color in light mode and become invisible against the dark scrim).

#### 6.3 Floating Card Content

Below the hero image region sits a rounded-top "sheet" or "card" (white in light mode, using the app's standard surface color token in dark mode) that contains the bulk of the readable content:

1. **Publisher source row:** a circular avatar showing the first letter of the source name (e.g., a stylized "S" for "San Francisco Chronicle" — this is explicitly a *letter*-based avatar here, as opposed to the plain colored circle recommended as the minimal-viable interpretation for the list screen's card avatar in section 3.4.2; the details screen's letter-avatar pattern could optionally be back-ported to the list card avatar in a future iteration for consistency, but that is explicitly out of scope for Part 1 of this plan since the original list-card screenshot did not clearly show a letter inside the avatar), the publisher name, a verified badge (a small checkmark-in-a-circle icon or similar, presumably reused from wherever "verified" badges already exist elsewhere in the app, e.g., a verified-user or verified-source icon used in a different feature, rather than commissioning a brand new icon asset for this single use case if a suitable one already exists in the app's icon set), and the time-ago label. Beneath this row, on its own line, sits the author's byline, formatted as "By {Author Name}" (e.g., "By Aidin Vaziri") — the "By " prefix must be a string resource (something like `news_detail_byline_prefix`) with the author's name interpolated into it via a formatted string resource (`getString(R.string.news_detail_byline_prefix, authorName)` or the Compose equivalent using `stringResource(id, authorName)`), never string-concatenated inline with a hardcoded "By " literal.
2. **Horizontal divider line** — visually separating the publisher/byline block above from the article body content below, presumably reusing the same `NewsDivider` color token introduced in Part 1 (section 3.6.1) for consistency, since both are conceptually "a thin separator line within the News feature," rather than introducing a second, differently-named-but-identical divider token specific to the details screen.
3. **Large bold lead paragraph / article sub-headline** — a prominent, larger-than-body-text paragraph presumably corresponding to the article's `description` field from the domain model (a short summary/teaser, distinct from the full body).
4. **Scrollable body paragraph description** — the fuller article text content, presumably also part of the existing domain model (or possibly the same `description` field reused, if the News API doesn't actually provide a separate "full body" field distinct from the short description used elsewhere in the app — this is a data-availability question that must be resolved against the actual `Article` domain model's available fields before this section can be implemented faithfully; if no separate long-form body field exists in the API response, the "lead paragraph" and "scrollable body" may need to both derive from the same underlying `description` string, perhaps with the lead paragraph showing the first sentence/portion and the body showing the remainder, or simply duplicating/reusing the same text in both places as a pragmatic compromise, clearly flagged as such rather than silently done).
5. **A styled primary CTA button** at the bottom of the sheet, reading **"Read Full Article on Web"**, accompanied by a compass/explore icon, which triggers opening the original source URL via Chrome Custom Tabs — i.e., this is where the *previous* screen's entire behavior (immediately opening Custom Tabs on tap) gets relocated to, now as an explicit, secondary, opt-in action taken from within the richer in-app details view, rather than being the *only* option as it is today.

### 7. Navigation Layer (`app/`) — Expanded

#### 7.1 New Route Definition

Add a new serializable route data class to `Route.kt`:

```kotlin
@Serializable
data class NewsDetailRoute(
    val title: String,
    val description: String?,
    val url: String,
    val imageUrl: String?,
    val sourceName: String,
    val publishedAtLabel: String,
    val author: String?,
    val category: String
)
```

Detailed rationale for each field:
- `title: String` — non-nullable, since every article realistically must have a title to be displayed at all in the list screen in the first place (if an article had no title, it likely wouldn't have been shown as a tappable card to begin with).
- `description: String?` — nullable, since not every article/source guarantees a description field is populated; the details screen must handle a null description gracefully (see section 8 below on fallback/empty-state handling for this field).
- `url: String` — non-nullable, required for the "Read Full Article on Web" CTA and the share action to function at all; if this were ever null/blank, arguably the article shouldn't have been navigable to a details screen in the first place, so treating this as guaranteed non-null at the navigation layer is a reasonable simplification, though defensive handling (e.g., disabling the CTA button if the URL is blank) is still cheap insurance worth adding.
- `imageUrl: String?` — nullable, since some articles may not have an associated image; the hero image region must handle this gracefully with a fallback/placeholder treatment (see section 8).
- `sourceName: String` — non-nullable, used both in the metadata overlay and the publisher source row, as well as to derive the letter-avatar (first character of this string).
- `publishedAtLabel: String` — passed as an already-formatted display string (e.g., "3h ago") rather than a raw timestamp, consistent with how the list screen presumably already formats its own "time ago" labels, so that the same formatting logic/utility is reused rather than duplicated between the list and details screens, and so that the details screen doesn't need its own separate date-formatting dependency.
- `author: String?` — nullable, since not all articles have an identifiable author; the byline row must be conditionally hidden or replaced with a fallback treatment when this is null (see section 8).
- `category: String` — passed through from the list screen's already-resolved `NewsUiArticle.category` field (see Part 1, section 3.2.1), so the same category label ("News", "Health", etc.) shown on the card in the list is exactly what's shown in the details screen's overlay badge, ensuring visual/data consistency between the two screens rather than the details screen re-deriving or re-guessing the category independently.

#### 7.2 Registering the Route

Register `NewsDetailRoute` in `NavGraph.kt`, and replace the current direct `CustomTabsIntent` web launch (which today presumably fires immediately from within the list screen's card-tap handler) with a navigation call instead:

```kotlin
navController.navigate(
    NewsDetailRoute(
        title = article.title,
        description = article.description,
        url = article.url,
        imageUrl = article.imageUrl,
        sourceName = article.sourceName,
        publishedAtLabel = article.publishedAtLabel,
        author = article.author,
        category = article.category
    )
)
```

Considerations:
- This change means the card's tap handler in the list screen changes from directly firing a `CustomTabsIntent` (likely today living inline in `NewsScreen.kt` or dispatched via a `NewsEffect` that the screen observes) to instead firing a navigation event/effect that the navigation graph observer picks up and turns into an actual `navController.navigate(...)` call, consistent with however this app already structures navigation-triggering effects for its other screens (this plan assumes such a pattern already exists elsewhere in the app for at least one other screen's navigation, and that this new News-to-Details navigation should follow that same existing pattern rather than inventing a new one).
- If `article` here refers to the `NewsUiArticle` presentation model (as it should, given the ViewModel already computes `publishedAtLabel`-equivalent formatted strings and the `category` field per Part 1), then the mapping from `NewsUiArticle` fields to `NewsDetailRoute` fields should be a straightforward one-to-one field copy, assuming the field names/types line up closely enough (some renaming, e.g., `NewsUiArticle`'s existing "time ago" field name mapping onto `NewsDetailRoute.publishedAtLabel`, may be required and should be handled carefully to avoid a subtle mismatch bug where the wrong field gets passed to the wrong parameter due to a name/order confusion).

### 8. Screen & UI Implementation (`presentation/news/detail/`) — Expanded

#### 8.1 New File: `NewsDetailScreen.kt`

Create this new Composable file with the following clean separation, as specified in the original plan, now expanded with implementation guidance for each named section:

1. **Header section** (the hero image + overlaid close/share buttons + metadata overlay described in section 6.1/6.2):
   - Should be its own private, named Composable (e.g., `NewsDetailHeader(...)`) taking the image URL, category, author, title, source name, and time label as parameters, rather than being inlined directly into the top-level screen Composable, both for readability and to make it independently previewable.
   - Must handle a null `imageUrl` gracefully — e.g., falling back to a solid-color or branded placeholder background behind the gradient/metadata overlay, rather than crashing or showing a broken-image icon, reusing whatever existing image-loading library conventions (e.g., Coil) and placeholder/error drawable the rest of the app already uses for network images.
   - The close button dispatches a "navigate back" action (popping the back stack), consistent with its "X means dismiss" visual semantics discussed in section 6.1.
   - The share button triggers building and launching a standard Android `Intent.ACTION_SEND` (type `"text/plain"`) populated with the article's URL (and likely its title, for a more useful shared message, e.g., "{title} — {url}", though the exact share text format should ideally reuse a string resource template such as `news_detail_share_text_format` rather than hardcoding string concatenation inline, keeping with the no-hardcoded-strings rule established throughout this entire plan).
   - The category badge, author, title, source, and time text elements must all use the dedicated "always white regardless of theme" text color token discussed in section 6.2 (e.g., `NewsDetailOverlayText`), not a theme-flipping semantic token, since they sit on a fixed dark gradient regardless of app theme.

2. **Sheet container** (the rounded-top white/surface-colored card described in section 6.3):
   - Should likely use the standard app `Surface` composable with the theme's surface color token (so it's white in light mode and whatever the app's dark-mode surface tone is in dark mode) and a shape with only the top two corners rounded (e.g., `RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)`, exact radius to be confirmed against the screenshot).
   - Should visually overlap the bottom portion of the hero image slightly (a common "floating sheet over hero image" pattern), which likely requires either negative top padding/offset on the sheet, or structuring the whole screen as a `Box` with the hero image as the bottom-most layer and the sheet as a layer on top of it, offset downward to only cover the lower portion of the image — the exact layering approach should be decided based on whatever the app's existing conventions are for any similar "hero image + overlapping sheet" pattern elsewhere (if one exists already, it should be reused/mirrored here rather than reinvented).

3. **Scrollable body** (containing the publisher row, byline, divider, lead paragraph, and body description described in section 6.3, items 1–4):
   - Must be wrapped in a vertically scrollable container (e.g., `Modifier.verticalScroll(rememberScrollState())`, or, if the sheet needs to coexist with the CTA button pinned at the bottom of the screen rather than scrolling away with the rest of the content, a `Column` with `weight(1f)` for the scrollable region and the CTA button placed outside/below it as a fixed footer — this exact structural choice depends on whether the screenshot shows the "Read Full Article on Web" button as always-visible/pinned versus scrolling away with the rest of the content; the phrase "at the bottom" in the original plan's description suggests a pinned footer button is the more faithful interpretation, and this plan recommends implementing it that way unless the actual screenshot clearly shows otherwise).
   - The **publisher source row** renders the letter-avatar (first character of `sourceName`, uppercased, inside a colored circle — reusing the `NewsSourceAvatarBg` token from Part 1 for the circle's fill color, for visual consistency between the list and details screens), the publisher name, the verified badge icon (conditionally shown only if the app's data model actually carries a "verified" flag for the source — if no such flag currently exists anywhere in the domain model, this is a data-availability gap that must be flagged and resolved, e.g., by adding the flag to the domain model and API mapping layer, or by omitting the badge entirely for this iteration if reliable "verified" data simply isn't available from the News API being used, rather than showing a badge that's always present/never present regardless of actual verification status), and the time-ago label.
   - The **byline** ("By {Author}") is only rendered if `author` is non-null/non-blank; if null, the entire byline row should be omitted from the layout entirely (not rendered as an empty/blank line, which would leave an odd gap), per the nullable `author` field's handling requirement noted in section 7.1.
   - The **divider** reuses the `NewsDivider` token from Part 1, per the rationale in section 6.3, item 2.
   - The **lead paragraph** is styled distinctly (larger size and/or bold weight) from the **body description** beneath it, per section 6.3 items 3–4, with the important caveat flagged in that same section regarding whether these two visually distinct blocks actually correspond to two genuinely distinct data fields, or whether they're a presentational split of a single underlying `description` string — this must be resolved against the real API/domain model before implementation, and whichever interpretation is chosen should be clearly documented in a code comment at the point where this splitting/duplication happens, so a future engineer doesn't mistake it for a data availability bug.
   - If `description` is null (nullable per section 7.1), both the lead paragraph and body sections must gracefully collapse/hide rather than show blank space or a null-pointer crash — this should be one of the very first things covered by a unit or Compose UI test for this screen, given how easy this specific null case is to overlook.

4. **"Read Full Article on Web" button**:
   - A large, clearly primary-styled button (likely using the app's standard primary/filled button component/theme if one already exists elsewhere in the app, rather than a bespoke one-off button style for just this screen) reading `news_detail_read_full_article_button` (a new string resource to be added alongside all the others), with a compass/explore icon leading the text.
   - Tapping it launches the article's `url` via Chrome Custom Tabs — i.e., exactly the same `CustomTabsIntent` mechanism the *old* list-screen tap behavior used, just now triggered from this explicit, clearly-labeled button instead of implicitly from every card tap.
   - This button's tap should ideally also be guarded against a blank/empty `url` (see section 7.1's note on `url` being non-nullable-but-not-necessarily-guaranteed-non-blank) by disabling the button or hiding it if no valid URL is present, rather than attempting to launch Custom Tabs with an empty string and presumably crashing or silently failing.

#### 8.2 Additional Implementation Notes Not Explicitly Covered in the Original Plan

- **New string resources required for the Details screen** (to be added to both default and Arabic `strings.xml`, following the exact same process as section 3.1): `news_detail_byline_prefix` ("By %1$s"), `news_detail_read_full_article_button` ("Read Full Article on Web"), `news_detail_close_content_description` ("Close"), `news_detail_share_content_description` ("Share article"), `news_detail_share_text_format` (a template string used when constructing the share `Intent`'s text payload, e.g., "%1$s — %2$s" for title and URL), and `news_detail_source_avatar_content_description` (reusing or closely mirroring the pattern of `news_source_avatar_content_description` from Part 1, but scoped to the details screen if the two need to be independently maintained rather than shared).
- **New color tokens required**: `NewsDetailOverlayText` (fixed white, both light/dark theme, per section 6.2's rationale), `NewsDetailScrimStart` and `NewsDetailScrimEnd` (the two gradient stops used for the dark scrim behind the metadata overlay — these should be defined as theme tokens even though they're likely just varying alpha values of black, so they remain centrally adjustable rather than hardcoded inline as a raw `Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))` literal within the Composable itself), and `NewsDetailSheetBg` (aliasing to whatever the app's standard surface/background token already is, added here mainly for semantic clarity within this specific screen's code rather than necessarily being a genuinely new raw value).
- **ViewModel for the Details screen**: the original plan does not explicitly mention a `NewsDetailViewModel`, which raises the question of whether this screen needs a ViewModel/MVI contract at all, or whether it can be a purely "dumb"/stateless Composable driven entirely by the navigation route's already-resolved arguments (title, description, url, etc., all passed in directly from `NewsDetailRoute`), with only local, ephemeral UI state (e.g., whether the CTA button was just tapped, for a brief loading/disabled state while Custom Tabs launches) managed via simple `remember { mutableStateOf(...) }` calls rather than a full MVI stack. Given that this screen doesn't appear to need to fetch any additional data beyond what's already available from the list screen's already-fetched article (no additional network call is described anywhere in the original plan), this plan recommends the simpler "stateless Composable driven by route arguments" approach, avoiding the overhead of standing up a full ViewModel/State/Event/Effect trio for a screen that doesn't have any asynchronous data-fetching or complex state transitions of its own — though this is an architecture judgment call that should be confirmed against this codebase's existing conventions (if every other screen, no matter how simple, has its own ViewModel per an established team convention, that convention should probably be followed here too for consistency, even if not strictly necessary from a pure functionality standpoint).
- **Testing considerations for the Details screen**: unit/Compose UI tests should cover, at minimum: correct rendering of all fields when fully populated; correct graceful omission of the byline when `author` is null; correct graceful handling of a null `description` (collapsing the lead paragraph/body sections without crashing); correct fallback treatment when `imageUrl` is null; correct behavior of the close button (triggering back navigation); correct behavior of the share button (launching a share `Intent` with the expected text, formatted via `news_detail_share_text_format`); and correct behavior of the "Read Full Article on Web" button (launching Custom Tabs with the expected `url`, and being disabled/hidden if the URL is blank).

---

## 9. Overall Sequencing Recommendation

Given the number of interdependent pieces across both Part 1 and Part 2 of this plan, the following implementation order is recommended to minimize the amount of time the codebase spends in a broken/non-compiling intermediate state:

1. Add all new/changed string resources (default + Arabic) for Part 1 first, since nothing else depends on anything except strings existing.
2. Add all new color tokens to `AppColors.kt` for Part 1, confirming base swatch dependencies as discussed in section 3.6.1.
3. Update the MVI contracts (`NewsState.kt`, `NewsEvent.kt`, `NewsEffect.kt`) for Part 1, accepting that this will temporarily break compilation of the ViewModel and Composables that reference the old `ShareClicked`/`ShareArticle` symbols, to be fixed in the very next step.
4. Update `NewsViewModel.kt` to match the new contracts, restoring compilation.
5. Update `NewsTopicChipRow.kt`, `NewsArticleCard.kt` (including its shimmer variant), and create the new `NewsSearchBar.kt`.
6. Update `NewsScreen.kt` to wire everything together, restoring full compilation and a working (if not yet pixel-perfect) screen.
7. Perform the full manual + accessibility + dark mode + RTL verification pass described in section 4 for Part 1 before moving on to Part 2, to avoid compounding unverified changes across both parts simultaneously.
8. Once Part 1 is verified, begin Part 2: add its new string resources and color tokens first (mirroring steps 1–2 above).
9. Add the `NewsDetailRoute` to `Route.kt` and register it in `NavGraph.kt`, updating the list screen's card-tap handling to navigate instead of directly launching Custom Tabs.
10. Build out `NewsDetailScreen.kt` and its sub-Composables (header, sheet, scrollable body, CTA button).
11. Perform a second full verification pass (manual, accessibility, dark mode, RTL, plus the Details-screen-specific null-field edge cases called out in section 8.2) before considering the entire two-part plan complete.

This sequencing ensures that at almost every intermediate commit/checkpoint, the app remains in a buildable and (mostly) functional state, which is generally good engineering practice for a change of this size and reduces the risk of a large, hard-to-bisect regression if something goes wrong partway through.

---

## 10. Summary of All Net-New Files

For quick reference, the following files are entirely new as a result of this plan (as opposed to modifications of existing files):

1. `NewsSearchBar.kt` (Part 1, section 3.4.3)
2. `NewsDetailScreen.kt` (Part 2, section 8.1)

All other files referenced throughout this plan are modifications to existing files already present in the codebase.

---

## 11. Summary of All Deletions

For quick reference, the following symbols/code paths are being deleted (not merely deprecated or hidden) as part of this plan:

1. `NewsEvent.ShareClicked` (section 3.2.2)
2. `NewsEffect.ShareArticle` (section 3.2.3)
3. The three-dot overflow menu Composable and its `onShareClick` parameter inside `NewsArticleCard.kt` (section 3.4.2, item 7)
4. Any `LaunchedEffect` branch in `NewsScreen.kt` handling `NewsEffect.ShareArticle` (section 3.5.1, item 5)
5. The direct `CustomTabsIntent` launch previously triggered immediately on card tap in the list screen, now replaced by navigation to `NewsDetailRoute` (section 7.2)

Each of these deletions should be accompanied by removal of any now-orphaned tests, as noted throughout the relevant sections above.

---

*End of plan.*

---

## 12. Risk Register

This section catalogs risks associated with this redesign, their likelihood, potential impact, and mitigation strategy.

### 12.1 Risk: Shared String Key Collision
**Description:** As noted in section 3.1.1, `news_screen_title` may currently be shared with other screens via a common top bar component. If so, changing its value to "Discover" could silently break unrelated screens.
**Likelihood:** Medium.
**Impact:** High if unnoticed — an unrelated screen could suddenly display "Discover" instead of its intended branding.
**Mitigation:** Grep the entire resource directory for `news_screen_title` usage before changing its value; if used elsewhere, introduce a new, News-scoped key instead.

### 12.2 Risk: Missing Base Color Swatches
**Description:** Section 3.6.1 flags that tokens like `Gray1600` and `Teal300` may not yet exist in the core palette.
**Likelihood:** Medium.
**Impact:** Medium — blocks compilation until resolved, caught at build time rather than shipping silently wrong colors.
**Mitigation:** Audit the palette early before any UI component code is written against these tokens.

### 12.3 Risk: RTL Layout Regressions
**Description:** Icon positions, image position within the card row, and header text alignment could be subtly wrong even after correct text localization.
**Likelihood:** Medium-High.
**Impact:** Medium — affects Arabic-locale users, an intentionally supported segment given the existing `values-ar` resources.
**Mitigation:** Explicit RTL verification pass per section 4.1.

### 12.4 Risk: Client-Side Search Performance
**Description:** Per-keystroke, undebounced client-side filtering over a large article list could cause jank.
**Likelihood:** Low-Medium.
**Impact:** Low-Medium.
**Mitigation:** Performance sanity check per section 4.2; add debounce as a defensive follow-up if needed.

### 12.5 Risk: Data Model Gaps for the Details Screen
**Description:** Open questions on whether the domain model has a "verified source" flag and a distinct "full body" field separate from the short `description`.
**Likelihood:** High.
**Impact:** Medium.
**Mitigation:** Resolve explicitly against the real domain model before implementing the body-rendering logic; document whichever pragmatic interpretation is chosen.

### 12.6 Risk: Orphaned Tests After Deletions
**Description:** Deleting `ShareClicked`, `ShareArticle`, the three-dot menu, and the direct Custom Tabs launch could leave stale test references.
**Likelihood:** Medium.
**Impact:** Low-Medium — typically a compile error, caught in CI.
**Mitigation:** Project-wide symbol search before considering any deletion "done."

### 12.7 Risk: Divider-Instead-Of-Shadow Layout Looking Wrong
**Description:** Layering a divider on top of unchanged item spacing could look wrong (excess whitespace) instead of matching the clean flat-list look in the screenshot.
**Likelihood:** Medium.
**Impact:** Low — purely visual, but undermines the goal of matching the screenshot exactly.
**Mitigation:** Treat shadow removal, divider addition, and spacing adjustment as one cohesive change; verify against the screenshot specifically for inter-card spacing.

---

## 13. Glossary

- **MVI (Model-View-Intent):** The unidirectional pattern used throughout this app's presentation layer — `State` (source of truth for rendering), `Event` (user intents flowing up), `Effect` (one-shot side effects flowing down, e.g. navigation, not re-triggered on recomposition).
- **Clean Architecture:** The data/domain/presentation layering used throughout the app, with dependencies pointing inward only.
- **Semantic color token:** A named color reference (e.g. `NewsCardTitle`) aliasing a raw palette swatch (e.g. `Gray1600`), so the same name can resolve differently per theme.
- **Shimmer:** The animated gray-block loading placeholder shown while real data is fetched.
- **Scrim:** A semi-transparent overlay (often a gradient) placed over an image to keep overlaid text/icons legible.
- **Custom Tabs:** Android's Chrome Custom Tabs API for opening a URL in a browser-like view that retains some host-app branding.
- **Content description:** The accessibility property that lets screen readers announce non-text elements meaningfully.

---

## 14. Frequently Anticipated Questions

**Q: Why remove share from the list card instead of restyling the three-dot menu?**
A: The screenshot shows no such affordance on the card; design favors a denser list with secondary actions deferred to the details screen, which reintroduces sharing with more room to do it justice.

**Q: Should chip shape or spacing change?**
A: Not unless the screenshot clearly shows it — scope here is fill/border/text color only.

**Q: Why filter search client-side instead of hitting the network per keystroke?**
A: No loading-spinner behavior is implied by the screenshot; instant local filtering matches the likely intended UX and avoids meaningfully expanding scope.

**Q: Does the domain `Article` model need new fields?**
A: Not for Part 1 — `category` lives only on the presentation-layer model. Part 2 raises open questions (12.5) about a verified flag and a distinct body field that must be resolved against the real API.

**Q: Is a new ViewModel required for the Details screen?**
A: Not necessarily — it can be a stateless Composable driven by route arguments, unless team convention dictates otherwise.

**Q: Does the back button on "Discover" still make sense?**
A: Assumed yes per section 2.1, based on News being pushed as a detail route rather than a nav-bar tab — flagged as an assumption to confirm with design.

**Q: What happens with a null `imageUrl`?**
A: Falls back to the app's existing placeholder/error-image treatment in both the list card and the details hero.

---

## 15. Per-File Change Checklist

### 15.1 Part 1 Files
- [ ] `strings.xml` (default) — add/modify 7 string keys
- [ ] `strings.xml` (values-ar) — matching Arabic translations
- [ ] `NewsState.kt` — add `searchQuery`, add `category` to `NewsUiArticle`
- [ ] `NewsEvent.kt` — add `SearchQueryChanged`/`FilterClicked`, remove `ShareClicked`
- [ ] `NewsEffect.kt` — remove `ShareArticle`
- [ ] `NewsViewModel.kt` — search filtering, remove share handling, category mapping
- [ ] `NewsTopicChipRow.kt` — restyle selected/unselected colors
- [ ] `NewsArticleCard.kt` — full redesign + shimmer update
- [ ] `NewsSearchBar.kt` (NEW)
- [ ] `NewsScreen.kt` — restructure header/search/list, remove share effect handling
- [ ] `AppColors.kt` — add 14 new tokens

### 15.2 Part 2 Files
- [ ] `strings.xml` (default + values-ar) — Details-screen strings
- [ ] `AppColors.kt` — Details-screen tokens
- [ ] `Route.kt` — add `NewsDetailRoute`
- [ ] `NavGraph.kt` — register route, replace direct Custom Tabs launch
- [ ] `NewsDetailScreen.kt` (NEW)

### 15.3 Test Files
- [ ] ViewModel tests — remove share coverage, add search/category coverage
- [ ] Chip row preview/tests
- [ ] Article card tests — new layout, remove share-menu tests
- [ ] Search bar preview tests
- [ ] Screen-level UI tests — header, search-narrows-list, remove menu-tap test
- [ ] Details screen tests — null author/description/imageUrl, close/share/CTA behavior

---

## 16. Testing Matrix

| Behavior | Unit Test | Compose UI Test | Manual/Accessibility |
|---|---|---|---|
| Search narrows list | Yes | Yes | Yes |
| Search + chip filter combine | Yes | Optional | Yes |
| Clear search restores list | Yes | Optional | Yes |
| Empty search-results state | Optional | Yes | Yes |
| Category mapped onto article | Yes | No | No |
| Share symbols fully removed | No | No | Yes (symbol search) |
| Chip selected/unselected colors | No | Yes | Yes |
| Article card new layout | No | Yes | Yes |
| Shimmer matches new layout | No | Yes | Yes |
| Search bar renders/accepts input | No | Yes | Yes |
| Filter icon dispatches event | Yes | Yes | Yes |
| Header title/subtitle render | No | Yes | Yes |
| Dark mode colors | No | No | Yes |
| RTL mirroring | No | No | Yes |
| Details: full field rendering | No | Yes | Yes |
| Details: null author omits byline | No | Yes | No |
| Details: null description collapses body | No | Yes | No |
| Details: null imageUrl fallback | No | Yes | Yes |
| Details: close navigates back | No | Yes | Yes |
| Details: share Intent text correct | Yes | Yes | Yes |
| Details: CTA launches Custom Tabs | No | Yes | Yes |
| Details: CTA disabled on blank URL | Yes | Yes | No |

---

## 17. Definition of Done

This plan is complete when:

1. Every checklist item in section 15 is checked off.
2. Every row in the testing matrix has actual coverage/verification performed.
3. `./gradlew assembleDebug` (and relevant test tasks) pass cleanly with no new warnings from these changes.
4. Manual visual comparison against both screenshots has been performed in light/dark mode and LTR/RTL, with no outstanding discrepancies beyond consciously deferred ones.
5. Every open question flagged in this plan is either resolved or explicitly deferred with a tracked follow-up.
6. All new strings exist in both default and Arabic resources, verified via lint.
7. All deleted symbols are confirmed, via project-wide search, to have no remaining references anywhere, including tests.
8. Code review is complete and approved, with particular attention to the no-hardcoded-strings/colors/fonts rules.

---

*End of plan.*

---

## 18. Dependency Graph Between Work Items

Understanding which pieces of work block which other pieces of work is essential for parallelizing this effort across more than one engineer, if the team chooses to split it up rather than have a single engineer work through the sequencing in section 9 serially.

- String resources (3.1) block nothing else from starting in parallel, but every Composable change should reference the final key names, so it's best if string resources land first or are at least agreed upon in a shared branch/PR before other work begins referencing them.
- Color tokens (3.6.1) similarly block nothing else from starting in parallel in terms of raw compilation, but again, agreeing on final token names early prevents rework later when other files reference them.
- MVI contract changes (3.2.1–3.2.3) block the ViewModel changes (3.3.1), since the ViewModel's event-handling `when` block must compile against whatever the final shape of `NewsEvent`/`NewsEffect`/`NewsState` is.
- ViewModel changes (3.3.1) block the screen-level wiring (3.5.1), since the screen needs to dispatch the new events and observe the new state shape.
- The chip row (3.4.1), article card (3.4.2), and search bar (3.4.3) component changes can all be worked on in parallel by different engineers, since they are independent Composables with no direct dependency on one another, only a shared dependency on the string/color resources already discussed above.
- The screen-level file (3.5.1) depends on all three of the above components being in at least a compilable state, since it composes all of them together.
- Part 2's navigation layer changes (7.1–7.2) depend on Part 1 being functionally complete enough that `NewsUiArticle.category` exists and is populated, since `NewsDetailRoute` takes a `category` field sourced from it.
- Part 2's `NewsDetailScreen.kt` (8.1) depends on the navigation layer changes being in place, since it needs the route's arguments to exist before it can consume them.
- Given this graph, a reasonable two-engineer parallelization would have one engineer own strings/colors/MVI-contracts/ViewModel (the "foundation" layer) while a second engineer stubs out UI components against placeholder/mock state shapes, converging once both are ready for the final screen-level wiring step.

---

## 19. Dark Mode Token Value Reference (To Be Finalized With Design)

The table below lists every new color token introduced by this plan, its light-mode intended source swatch, and whether a dark-mode-specific value is already known from the original request or still needs sign-off. This is meant as a single at-a-glance reference to be filled in and confirmed rather than duplicated from the narrative sections above.

| Token | Light Mode Source | Dark Mode Source | Confirmed? |
|---|---|---|---|
| `NewsChipSelectedBg` | Teal1000 | Likely same Teal1000 | Needs confirmation |
| `NewsChipSelectedText` | White | Likely same White | Needs confirmation |
| `NewsChipUnselectedBg` | Transparent/White | Likely dark surface equivalent | Needs confirmation |
| `NewsChipUnselectedBorder` | Gray400 | Likely a darker-mode-appropriate gray | Needs confirmation |
| `NewsChipUnselectedText` | Gray700 | Likely a lighter-mode-appropriate gray | Needs confirmation |
| `NewsCategoryLabel` | Teal1000 | Likely same or lighter teal | Needs confirmation |
| `NewsCardTitle` | Gray1600 | Teal300 | Confirmed (explicit in original request) |
| `NewsSourceText` | Gray700 | Likely a lighter-mode-appropriate gray | Needs confirmation |
| `NewsSourceAvatarBg` | Teal1000 | Likely same Teal1000 | Needs confirmation |
| `NewsSearchBarBg` | Gray100 | Likely a dark-surface-appropriate gray | Needs confirmation |
| `NewsSearchBarBorder` | Gray400 | Likely a darker-mode-appropriate gray | Needs confirmation |
| `NewsSearchIconTint` | Gray700 | Likely a lighter-mode-appropriate gray | Needs confirmation |
| `NewsFilterIconTint` | Gray700 | Likely a lighter-mode-appropriate gray | Needs confirmation |
| `NewsDivider` | Gray300 | Likely a darker-mode-appropriate gray | Needs confirmation |
| `NewsDetailOverlayText` | White (fixed) | White (fixed, same as light) | Confirmed (deliberately theme-independent, per section 6.2) |
| `NewsDetailScrimStart` | Transparent | Likely same | Needs confirmation |
| `NewsDetailScrimEnd` | Black at ~70% alpha | Likely same or slightly adjusted | Needs confirmation |
| `NewsDetailSheetBg` | Surface/White | App's standard dark surface token | Needs confirmation |

Rows marked "Needs confirmation" should not block implementation from starting — a reasonable placeholder (matching light mode, or the nearest existing dark-mode-equivalent swatch already used elsewhere for similar semantic purposes) can be used initially, with a follow-up design review specifically covering dark mode before this work is considered fully done, per item 4 of the Definition of Done in section 17.

---

## 20. Detailed RTL Consideration Notes

Because RTL correctness is flagged as a Medium-High risk in section 12.3, this section provides an additional, more granular checklist specifically for the RTL verification pass referenced in section 4.1, so that whoever performs that pass has a concrete list to work through rather than a vague "check RTL" instruction.

1. Header: confirm "Discover" and its subtitle are right-aligned (or whatever the app's existing RTL text-alignment convention is for similar large-title headers elsewhere), and that the back button, if retained, sits on the correct (now right-hand) leading edge.
2. Search bar: confirm the search icon moves to the new leading (right) edge and the filter icon moves to the new trailing (left) edge, mirroring the whole row rather than leaving icons pinned to their LTR physical positions unless a conscious, documented decision was made to keep icon positions fixed regardless of layout direction.
3. Chip row: confirm the horizontal scroll direction and the starting/first-visible chip make sense in RTL (typically, the first chip in the list should appear at the right-hand edge, with scrolling proceeding leftward).
4. Article card: confirm the image moves to the right-hand side of the row with content (category, title, source row) on the left, mirroring the entire row layout — this is the single most likely place for a "text is translated but layout still looks LTR" bug to hide, since it's easy to accidentally pin an `Image` to a fixed `Alignment.Start` rather than using a layout-direction-aware modifier.
5. Source row within the card: confirm the avatar-then-name-then-separator-then-time ordering visually mirrors correctly (i.e., in RTL, is it read right-to-left as avatar-name-separator-time, or does mirroring accidentally reverse only some of the sub-elements while leaving others in their LTR order — a subtle bug that can occur if some elements are wrapped in an explicitly-LTR-locked sub-`Row` for some unrelated technical reason, like rendering a fixed-direction icon).
6. Details screen hero overlay: confirm category badge, author, title, source, and time text block right-align correctly, and that the close button (top-leading, now right) and share button (top-trailing, now left) swap sides correctly.
7. Details screen publisher row and byline: confirm the letter-avatar, publisher name, verified badge, and time mirror correctly, and that the "By {Author}" formatted string reads correctly with the interpolated name in an RTL sentence structure (Arabic sentence structure and name placement within a formatted string can behave differently than a simple LTR concatenation, which is exactly why section 8.2 calls for using a proper formatted string resource rather than manual concatenation).
8. Details screen CTA button: confirm the compass/explore icon and the button label text correctly swap relative order (icon should move to the new trailing edge if it was originally leading, or vice versa, consistent with whatever the app's existing convention is for icon-plus-label buttons elsewhere).

---

## 21. Rollout and Follow-Up Considerations

While not explicitly requested in the original plan, the following rollout considerations are worth tracking as this work moves toward release, given the number of interconnected pieces:

- **Feature flagging:** consider whether this redesign should ship behind a remote feature flag, allowing a staged rollout and a quick kill-switch if an unforeseen issue (particularly around the RTL or dark-mode risks flagged above) surfaces in production before it's caught in QA. This is a judgment call dependent on this app's existing release conventions and risk tolerance, not a hard requirement of this plan.
- **Analytics continuity:** confirm that any existing analytics events tied to the News screen (screen-view events, chip-tap events, article-tap events) continue to fire correctly after the restructuring, and consider whether new analytics events are warranted for the new search and filter-icon interactions, even though this plan does not mandate any specific new analytics events given the original request didn't call for them.
- **Follow-up for the `FilterClicked` stub:** section 3.2.2 recommends implementing this as an intentionally unimplemented stub for this iteration; a follow-up ticket should be filed to track the eventual real behavior once product/design defines what tapping the filter icon should actually do, so this doesn't quietly remain a dead button indefinitely.
- **Follow-up for letter-avatar consistency:** section 6.3 notes that the list screen's card avatar (Part 1) is implemented as a plain colored circle for this iteration, while the details screen's avatar (Part 2) uses a letter-based treatment; a follow-up ticket should be filed to consider back-porting the letter-avatar treatment to the list card for visual consistency between the two screens, once/if design confirms that's the intended long-term direction.
- **Follow-up for verified-badge data availability:** if section 12.5's investigation reveals the domain model has no reliable "verified source" flag, a follow-up ticket should be filed either to add that data (if the underlying News API actually supports it and it's simply not yet mapped through the app's data layer) or to formally drop the verified badge from the design with design's sign-off, rather than leaving this plan's Details-screen implementation in a permanently "partially matches the screenshot" state.

---

*End of plan.*

---

## 22. Appendix A: Illustrative Pseudocode Sketches

The following pseudocode sketches are illustrative only — they are meant to communicate structure and intent, not to be copy-pasted verbatim, since exact syntax will depend on final naming decisions, existing helper functions, and Compose/Kotlin version specifics already established elsewhere in this codebase.

### 22.1 `NewsState.kt` Sketch

```kotlin
data class NewsState(
    val articles: List<NewsUiArticle> = emptyList(),
    val filteredArticles: List<NewsUiArticle> = emptyList(),
    val selectedTopic: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class NewsUiArticle(
    val id: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val sourceName: String,
    val publishedAtLabel: String,
    val url: String,
    val author: String?,
    val category: String
)
```

### 22.2 `NewsEvent.kt` Sketch

```kotlin
sealed interface NewsEvent {
    data class TopicSelected(val topic: String) : NewsEvent
    data class SearchQueryChanged(val query: String) : NewsEvent
    data object FilterClicked : NewsEvent
    data class ArticleClicked(val article: NewsUiArticle) : NewsEvent
    data object Refreshed : NewsEvent
}
```

### 22.3 `NewsEffect.kt` Sketch

```kotlin
sealed interface NewsEffect {
    data class NavigateToDetail(val route: NewsDetailRoute) : NewsEffect
    data class ShowError(val message: String) : NewsEffect
}
```

### 22.4 `NewsViewModel.kt` Search-Filtering Sketch

```kotlin
private fun applyFilters(state: NewsState): NewsState {
    val query = state.searchQuery.trim()
    val byTopic = if (state.selectedTopic != null) {
        state.articles.filter { it.category == state.selectedTopic }
    } else {
        state.articles
    }
    val byQuery = if (query.isNotEmpty()) {
        byTopic.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.description?.contains(query, ignoreCase = true) == true
        }
    } else {
        byTopic
    }
    return state.copy(filteredArticles = byQuery)
}

fun onEvent(event: NewsEvent) {
    when (event) {
        is NewsEvent.SearchQueryChanged -> {
            _state.update { applyFilters(it.copy(searchQuery = event.query)) }
        }
        is NewsEvent.TopicSelected -> {
            _state.update { applyFilters(it.copy(selectedTopic = event.topic)) }
        }
        NewsEvent.FilterClicked -> {
            // TODO: intentionally unimplemented stub, see section 3.2.2 / 21
        }
        is NewsEvent.ArticleClicked -> {
            viewModelScope.launch {
                _effect.emit(
                    NewsEffect.NavigateToDetail(
                        NewsDetailRoute(
                            title = event.article.title,
                            description = event.article.description,
                            url = event.article.url,
                            imageUrl = event.article.imageUrl,
                            sourceName = event.article.sourceName,
                            publishedAtLabel = event.article.publishedAtLabel,
                            author = event.article.author,
                            category = event.article.category
                        )
                    )
                )
            }
        }
        NewsEvent.Refreshed -> { /* existing refresh logic unchanged */ }
    }
}
```

### 22.5 `NewsArticleCard.kt` Structural Sketch

```kotlin
@Composable
fun NewsArticleCard(
    article: NewsUiArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(vertical = AppTheme.dimens.spacingMedium),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.spacingMedium)
        ) {
            AsyncImage(
                model = article.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.category,
                    style = AppTheme.typography.LabelSmall,
                    color = AppTheme.colors.NewsCategoryLabel
                )
                Text(
                    text = article.title,
                    style = AppTheme.typography.TitleMedium,
                    color = AppTheme.colors.NewsCardTitle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(AppTheme.colors.NewsSourceAvatarBg)
                            .semantics {
                                contentDescription = sourceAvatarContentDescription
                            }
                    )
                    Text(
                        text = article.sourceName,
                        style = AppTheme.typography.BodySmall,
                        color = AppTheme.colors.NewsSourceText
                    )
                    Text(
                        text = stringResource(R.string.news_source_time_separator),
                        color = AppTheme.colors.NewsSourceText
                    )
                    Text(
                        text = article.publishedAtLabel,
                        style = AppTheme.typography.BodySmall,
                        color = AppTheme.colors.NewsSourceText
                    )
                }
            }
        }
        HorizontalDivider(color = AppTheme.colors.NewsDivider, thickness = 1.dp)
    }
}
```

### 22.6 `NewsSearchBar.kt` Structural Sketch

```kotlin
@Composable
fun NewsSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onFilterClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(AppTheme.colors.NewsSearchBarBg)
            .padding(horizontal = AppTheme.dimens.spacingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = AppTheme.colors.NewsSearchIconTint
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = AppTheme.dimens.spacingSmall),
            textStyle = AppTheme.typography.BodyMedium.copy(color = AppTheme.colors.NewsSourceText),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.news_search_placeholder),
                        style = AppTheme.typography.BodyMedium,
                        color = AppTheme.colors.NewsSearchIconTint
                    )
                }
                innerTextField()
            }
        )
        IconButton(onClick = onFilterClicked) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = stringResource(R.string.news_search_filter_content_description),
                tint = AppTheme.colors.NewsFilterIconTint
            )
        }
    }
}
```

### 22.7 `Route.kt` Sketch

```kotlin
@Serializable
data class NewsDetailRoute(
    val title: String,
    val description: String?,
    val url: String,
    val imageUrl: String?,
    val sourceName: String,
    val publishedAtLabel: String,
    val author: String?,
    val category: String
)
```

### 22.8 `NewsDetailScreen.kt` Structural Sketch

```kotlin
@Composable
fun NewsDetailScreen(
    route: NewsDetailRoute,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(modifier = modifier.fillMaxSize()) {
        NewsDetailHeader(
            imageUrl = route.imageUrl,
            category = route.category,
            author = route.author,
            title = route.title,
            sourceName = route.sourceName,
            publishedAtLabel = route.publishedAtLabel,
            onCloseClicked = onCloseClicked,
            onShareClicked = {
                val shareText = context.getString(
                    R.string.news_detail_share_text_format,
                    route.title,
                    route.url
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(intent, null))
            }
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(AppTheme.colors.NewsDetailSheetBg)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(AppTheme.dimens.spacingMedium)
            ) {
                NewsDetailPublisherRow(
                    sourceName = route.sourceName,
                    publishedAtLabel = route.publishedAtLabel
                )
                if (!route.author.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.news_detail_byline_prefix, route.author),
                        style = AppTheme.typography.BodySmall,
                        color = AppTheme.colors.NewsSourceText
                    )
                }
                HorizontalDivider(color = AppTheme.colors.NewsDivider)
                if (!route.description.isNullOrBlank()) {
                    Text(
                        text = route.description,
                        style = AppTheme.typography.TitleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = route.description,
                        style = AppTheme.typography.BodyMedium
                    )
                }
            }
            Button(
                onClick = {
                    if (route.url.isNotBlank()) {
                        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(route.url))
                    }
                },
                enabled = route.url.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimens.spacingMedium)
            ) {
                Icon(imageVector = Icons.Default.Explore, contentDescription = null)
                Spacer(modifier = Modifier.width(AppTheme.dimens.spacingSmall))
                Text(text = stringResource(R.string.news_detail_read_full_article_button))
            }
        }
    }
}
```

Note on the sketch above: as flagged in section 6.3/12.5, the lead paragraph and scrollable body both currently draw from `route.description` as a pragmatic placeholder pending confirmation of whether a genuinely separate "full body" field exists in the underlying data model; this duplication is intentional and documented here, not an oversight, and should be revisited once that data-availability question is resolved.

---

## 23. Appendix B: Suggested String Resource File Fragments

For convenience, the following fragments approximate what the final additions to `strings.xml` might look like, gathering every new key introduced across both parts of this plan into one place.

```xml
<!-- News list screen -->
<string name="news_screen_title">Discover</string>
<string name="news_screen_subtitle">News from all around the world</string>
<string name="news_search_placeholder">Search</string>
<string name="news_search_filter_content_description">Filter</string>
<string name="news_category_label">News</string>
<string name="news_source_time_separator"> · </string>
<string name="news_source_avatar_content_description">Source avatar</string>

<!-- News details screen -->
<string name="news_detail_byline_prefix">By %1$s</string>
<string name="news_detail_read_full_article_button">Read Full Article on Web</string>
<string name="news_detail_close_content_description">Close</string>
<string name="news_detail_share_content_description">Share article</string>
<string name="news_detail_share_text_format">%1$s — %2$s</string>
<string name="news_detail_source_avatar_content_description">Source avatar</string>
```

Each of these must have a corresponding entry added to `values-ar/strings.xml`, translated appropriately and reviewed by a native speaker before this plan can be considered fully done, per section 17, item 6.

---

*End of plan.*