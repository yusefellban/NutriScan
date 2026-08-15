# Centralize Title / Sub-Title Colors Across the App (v5 — full audit, corrected, expanded)

**Date:** 2026-08-08
**Author:** AI Agent (planning phase only — no code changes made yet)
**Status:** Draft — supersedes all prior drafts in this file
**v4 correction note:** The v3 audit was incomplete. It searched by specific style-token names (`sectionTitle`, `headerTitle`, `headlineMedium`) and missed section headers built with `titleLarge`/`titleMedium`/`titleSmall`, including "Family Members" on the Profile screen. This revision re-audits **by role** (does this text sit as a heading above a group of related content on a plain background?) instead of by style name, and adds 9 more confirmed items (§3.1, items 10–18). It also surfaces a systemic bug — see §1.5 — that explains much of the inconsistency the user has been seeing.
**v5 expansion note:** No scope, findings, or recommendations changed from v4. This revision expands the supporting rationale, background, risk assessment, rollback plan, glossary, FAQ, and per-screen verification checklists for reviewer clarity and to serve as a complete standalone reference document. See §10 for the full document map.

---

## 0. Document Map

This document has grown substantially across revisions to capture the full reasoning trail behind each decision, not just the decisions themselves. For a reader who wants the short version, §1.1 (target color), §1.4 (screen-title tiering), §2 (architecture), and the two tables in §3.1/§3.2 (the actual call-site changes) are the load-bearing sections — everything else exists to support, justify, or de-risk those four sections for a reviewer who wants to understand *why*, not just *what*.

| Section | Purpose |
|---|---|
| §1 | Problem statement, background, target color selection, and the exclusions/tiering logic |
| §2 | The concrete architecture change (one new token) |
| §3 | The full list of call-site changes, organized by target token |
| §4 | Automated and manual verification steps, including a full per-screen checklist |
| §5 | Explicit out-of-scope items and why |
| §6 | Risk assessment |
| §7 | Rollback plan at three levels of granularity |
| §8 | Glossary of theming terms used throughout |
| §9 | Anticipated questions and answers |
| §10 | This document map |

---

## 1. Goal Description

Unify color usage for section sub-titles, screen titles, and small descriptive text across the app so the whole product reads as one consistent, professionally centralized color system — not a scattered set of one-off tokens per screen.

### 1.0 Background and motivation

This effort originated from user-facing feedback that the app "feels inconsistent" when navigating between screens, even though no single screen looks obviously wrong in isolation. That kind of complaint is notoriously hard to action directly, because it isn't a bug report against one file — it's an emergent property of many small, independently-reasonable decisions compounding across a large Compose codebase. Each individual screen's author picked a color that looked fine on their screen, in their theme, on their device, without visibility into what every other screen author had already done. The result, multiplied across dozens of files and two themes, is a UI where the same semantic role (`"a heading above a group of related content"`) is rendered in five or six visually distinct colors depending on which file happened to define it.

The practical effect for a real user is subtle but measurable: as they move from Home → Explore → Profile → Settings → Notifications, the "weight" and "temperature" of heading text shifts in ways that have no design intent behind them. Nothing is broken in the sense of a crash or a missing string; it is a slow erosion of visual coherence that shows up as a vague, hard-to-articulate sense that the product wasn't built by one team with one point of view.

This class of problem is common in apps that grow screen-by-screen, feature-by-feature, especially when:

1. Multiple contributors work in parallel on different feature verticals (Home, Profile, Settings, Notifications, Exercises, News, Calories, NutriGPT) without a shared design-token review gate.
2. The design system exposes many semantically-overlapping tokens (`Gray500`, `PrimaryVariant`, `MenuSectionLabel`, `ProfileSetupSectionTitle`, `ExerciseScreenTitle`, `NewsScreenTitle`, `CaloriesHistoryTitle`, `Teal1000`, `CaloriesAccentTeal1200`, and more) rather than a small number of role-based tokens that are reused everywhere that role appears.
3. Dark-mode support was retrofitted onto screens that were originally built light-mode-only, so per-token light/dark pairs were filled in ad hoc, screen by screen, without a central review of whether the resulting pairs were internally consistent with each other.

None of the individual choices above are wrong in isolation — a one-off token for one screen is a completely reasonable thing to reach for when that screen is being built in the abstract, without a full inventory of what else exists. The problem only becomes visible in aggregate, and only becomes actionable once someone (in this case, an AI agent tasked with a full audit) does the unglamorous work of walking every screen, cataloguing every heading-like text element, and checking it against a shared definition of "does this play the section-sub-title role."

### 1.0.1 Why this matters beyond aesthetics

It would be easy to dismiss this as pure polish work with no functional payoff, but there are a few concrete, non-cosmetic reasons this is worth doing now rather than later:

- **Maintenance cost compounds.** Every new screen added to the app without a centralized token is a new one-off decision that a future contributor has to either copy (perpetuating the inconsistency) or research from scratch (spending time re-deriving what the "right" color should have been). Centralizing now caps the blast radius of that decision for every future screen.
- **Dark mode parity bugs are silent.** As documented in §1.5 below, several of the audited tokens don't just differ screen-to-screen — they differ *from themselves* between light and dark mode, in a way that was clearly never intended (a token being `TextPrimary`-equivalent in light mode and the *unrelated* accent teal in dark mode is not a designed choice, it is a copy-paste residue from wherever the token was first stubbed out). These are not aesthetic nits; they are literal implementation bugs that happen to be invisible unless you specifically toggle dark mode and compare against light mode side-by-side, which is exactly the kind of check that's easy to skip during a normal PR review.
- **Design-system trust.** Every time a designer or engineer discovers a new one-off color token instead of finding the token they expected in the shared system, it reinforces a habit of not checking the shared system at all next time, which accelerates the exact fragmentation this plan is trying to reverse. A single well-documented, single-purpose `SectionSubtitle` token — introduced once, correctly, and referenced everywhere the role appears — is worth more to long-term velocity than the sum of its individual line changes.
- **QA and screenshot-testing leverage.** If the project has (or later adopts) Compose screenshot/golden testing, having a single token for a single semantic role means a single line change updates the "signature" color for that role everywhere at once, and any regression shows up as a single predictable diff instead of a scattered, screen-by-screen hunt.

### 1.0.2 Non-goals

To keep the scope of this pass tight and reviewable, it's worth being explicit about what this plan is **not** trying to do, in addition to the formal "Out of Scope" list in §5:

- This is **not** a full design-token audit of the entire color system. Only the specific semantic role of "section sub-title / plain-background screen title" is in scope. Buttons, borders, icons, chips, progress indicators, and all other UI roles are untouched.
- This is **not** a rebrand or a visual refresh. The target color (`#11939A`) already exists in the codebase today, already ships to production today (on the Calories screen), and is not a new design decision — it is the *existing* de-facto standard being extended to places that should have used it already.
- This is **not** a typography change. No `style =` argument is touched anywhere in this plan; only `color =` arguments move.
- This is **not** a refactor of the underlying `AppColorsExtension` architecture, the theming system's plumbing, or how light/dark switching itself works. A single new token is added using the exact same pattern as every existing fixed-value token.

### 1.1 Reference color (source of truth)

Confirmed in `CaloriesScreen.kt`'s `CaloriesHeader`:
```kotlin
Text(
    text = stringResource(R.string.daily_products),
    style = CaloriesTypography.headerTitle,
    color = AppTheme.colors.CaloriesAccentTeal1200,
)
```
`CaloriesAccentTeal1200` = `Color(0xFF11939A)` — a **fixed literal, identical in both light and dark palettes** in `AppColors.kt`.

➡️ **Target color for in-scope sub-titles/titles: `#11939A`, theme-independent.**

#### 1.1.1 Why `#11939A` and not some other candidate

Several other candidate "already exists in the codebase" colors were considered and rejected as the unification target before settling on `#11939A`:

- **`PrimaryVariant`** — rejected because it is itself one of the *inconsistent* tokens being fixed (used for "Explore," "Recent History," and the NutriGPT chat title, in three different visual contexts, with no single canonical usage that could serve as a reference implementation the way `CaloriesScreen.kt` does for `#11939A`).
- **`Teal1000`** — rejected because it's already heavily used for the `HeroHeaderTitle` band system (§1.2) and for buttons/icons elsewhere; reusing it for the new role would create ambiguity between "this is a teal-band screen title" and "this is a plain-background section sub-title," which defeats the purpose of having distinct, legible-by-name tokens.
- **`Gray500` / `TextPrimary`** — rejected as the sub-title target specifically because part of the reported problem is that section sub-titles currently look too muted/generic in places, and `Daily Products` (the one screen everyone agrees looks "right") uses the accent teal, not a gray. Using gray would technically unify the *token* but would still visually diverge from the one confirmed-correct reference implementation.
- **A brand-new arbitrary hex not currently used anywhere** — rejected on principle per §3.2's compliance note: introducing a new literal that doesn't already exist as a de-facto standard somewhere in production would be a new design decision requiring separate design sign-off, not a consistency fix. Every value chosen in this plan (`#11939A` for sub-titles, `Teal300` for teal-band titles, `TextPrimary` for plain-background titles) is a value that is **already shipping** somewhere in the app today; this plan only extends its reach, it does not invent new colors.

`#11939A` wins specifically because (a) it is already used for a screen (`CaloriesScreen.kt`'s "Daily Products") that has effectively served as the informal reference point users compare other screens against when describing the "this looks off" feeling, (b) it is already defined as a **fixed, theme-independent** literal, which sidesteps an entire category of light/dark parity bugs (see §1.5) that a theme-adaptive token would need separate justification to avoid, and (c) it carries no other overloaded meaning elsewhere in the app — it is not reused for buttons, borders, or status indicators, so repurposing it for a second host of call sites doesn't create semantic overload the way reusing `Teal1000` or `PrimaryVariant` would.

### 1.2 Corrections from the expanded audit (read before implementing)

Two areas originally flagged as "Group B — needs approval" in v1 must **not** be forced to `#11939A`, because they sit on **solid colored surfaces**, not the plain screen background that "Daily Products," "Explore," "Water," etc. sit on. Applying the same dark-teal text there would create a serious contrast/readability problem:

- **`HeroHeaderTitle`** (`common/components/HeroHeaderText.kt`) renders on a solid `Teal1000` rounded header band (confirmed in `AppSettingsHeader.kt` and `AccountPendingDeletionScreen.kt`: `.background(color = AppTheme.colors.Teal1000, shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))`). It currently uses light teal text (`Teal300`) specifically for contrast against that band, and its own kdoc calls it "the canonical title style for every teal header band ... so they all read as one family" — it is **already** a correctly-centralized, intentionally-different system, used identically across Settings, Help, Terms, Notifications, Home, Profile, Calories, Saved, and Account-Deletion. **Recommendation: leave untouched.**
- **`BmiGoalCard` / `CalorieGoalsCard`** section titles ("Your BMI" / "Calorie Goals") render on a solid `Teal500` card background (`.background(AppTheme.colors.Teal500)`), using `Teal100` (near-white) for contrast — and the code comments state this palette is "intentionally identical in both light and dark mode per the design spec." The two cards are already mutually consistent with each other. **Recommendation: leave untouched.**
- **Correction to v1's table:** v1 incorrectly listed `BmiGoalCard.kt` line 102 as a title needing the fix. That line is actually the **numeric BMI value** ("24.5"), not a title. **Removed from scope entirely.**

If the user wants these colored-card/band titles visually pulled toward the same green family too, that is a distinct, higher-risk design decision (would require re-checking WCAG contrast against `Teal1000`/`Teal500` first) and should be scoped as a separate follow-up — not bundled into this pass.

#### 1.2.1 Detailed contrast reasoning for the exclusions

It's worth spelling out *why* these two exclusions are a hard line rather than a soft preference, because a future reviewer skimming just the table in §1.4 might reasonably ask "why not just also fix these while we're in here?"

- `HeroHeaderTitle` renders `Teal300` (a light, desaturated teal, close to white-with-a-tint) against a solid `Teal1000` background band. `Teal1000` is a dark, saturated teal. The luminance gap between `Teal300` text and `Teal1000` background is large by construction — that pairing was clearly chosen specifically to be legible against that specific background. `#11939A`, the sub-title target color, is a **mid-tone** teal — putting mid-tone text on a dark-teal background would collapse much of that luminance gap, likely dropping well below WCAG AA's 4.5:1 contrast ratio for normal text (and likely below 3:1 even for large text), which would make the heading materially harder to read, especially for low-vision users. This isn't a matter of taste — it is a legibility regression that a plain "swap the color argument" pass has no way to catch unless someone explicitly stops and checks contrast math against the *background*, not just compares it to how it looks against a phone screen's default plain background in a screenshot.
- `BmiGoalCard` / `CalorieGoalsCard` follow the identical shape of problem: `Teal100` (near-white) text against a solid `Teal500` (mid-saturated teal) card fill. `Teal100` was very likely chosen precisely because `Teal500` sits in a luminance range where only a near-white or near-black text color reads reliably; `#11939A` sitting in a comparable-but-different mid-tone teal band relative to `Teal500` risks a contrast ratio that would look "fine" in an IDE preview and genuinely fail for real users in bright outdoor lighting.
- Both of these components also carry existing code comments explicitly stating the palette is "intentionally identical in both light and dark mode per the design spec" — i.e., these are not accidental one-offs waiting to be swept up in a consistency pass, they are **already** an intentional, already-centralized, already-documented sub-system in their own right. Treating them as in-scope "bugs" alongside the genuinely inconsistent tokens elsewhere in this plan would conflate two very different situations: "nobody thought about this consistently" (the actual problem this plan solves) versus "someone thought about this very deliberately and left a comment saying so" (not a problem, and risky to touch without a fresh design review).

For these reasons, both are explicitly carved out as reference examples of an *already-correct* centralized system rather than folded into the `SectionSubtitle` migration, and are listed again for completeness in §3.3 and §5.

### 1.3 Expanded audit findings (screen titles & descriptions, as requested)

- **Screen titles:** Most screens already funnel through the centralized `HeroHeaderTitle`/`HeroHeaderSubtitle` pair (Settings, Help, Terms, Notifications, Home greeting, Profile, Calories, Saved, Account-Deletion) — that system is fine as-is (§1.2). Two screens **don't** use it and are inconsistent outliers:
  - `ChatTopBar.kt` (NutriGPT chat) — uses `AppTheme.colors.PrimaryVariant` for its title, on a **plain** background.
  - `ExerciseWorkoutScreen.kt` — uses a dedicated `ExerciseWorkoutHeaderTitle` token, on a **plain** background. This token is a **pre-existing light/dark bug**: `Color(0xFF393C3C)` in light mode vs. `Color(0xFF11939A)` in dark mode — i.e. it currently renders as a *different color depending on theme*, which is itself the exact inconsistency category the user is asking to eliminate.
  - Both are safe to unify since they sit on plain backgrounds — included in scope below.
- **Small descriptions / captions:** `HeroHeaderSubtitle` (`Gray100`, fixed both themes) is already fully centralized and used identically everywhere `HeroHeaderTitle` is used — no fix needed there. No other recurring "description under a sub-title" pattern with inconsistent coloring was found near the in-scope items (the text next to "Water" is a counter, and next to "Recent History" is a "View All" link — both are functionally different elements, not descriptions, and are out of scope).

### 1.4 Screen-title tiering (this revision)

Screen titles are a **different semantic role** than section sub-titles and must **not** take the Daily Products green. Instead of inventing a new value, the codebase was audited for whichever color is *already* the dominant/common choice for each background context, so the fix reinforces an existing convention rather than adding a third arbitrary color:

| Context | Centralized token to use | Value | Evidence |
|---|---|---|---|
| Screen title on a solid teal card/band background | `HeroHeaderTitle`'s existing color | `Teal300` (fixed, both themes) | Already used identically by `HeroHeaderTitle` across 6+ screens (Settings, Help, Terms, Notifications, Home greeting, Profile, Calories, Saved, Account-Deletion band). This is already the centralized, correct value — **no change needed.** |
| Screen title on a white/plain background | `TextPrimary` | `#393C3C` (light) / near-white `#E8FAFA` (dark) — theme-adaptive | Confirmed via codebase search: `TextPrimary` is already used **20 times** for plain-background titles/headings/body text app-wide, including the "Account Deletion Pending" heading. It is the dominant existing convention for this context. |
| Section sub-title within a screen (Daily Products, Explore, Water, etc.) | new `SectionSubtitle` token | `#11939A` fixed, both themes | Per §1.1/§2 |

**Correction to the previous draft:** `AccountPendingDeletionScreen.kt`'s "Account Deletion Pending" heading (lines 129–131) was listed in the green-migration table below (old item #10). It is a **screen-title-role** element on a plain background, already correctly using `TextPrimary` — it should **stay** on `TextPrimary`, not move to `SectionSubtitle`. It has been removed from §3.1's green table and is now listed in §3.3 as a reference example (no change required).

`ChatTopBar.kt`'s "NutriGPT" title and `ExerciseWorkoutScreen.kt`'s "Exercise" title are also screen-title-role elements on plain backgrounds — they are outliers (`PrimaryVariant` and the buggy `ExerciseWorkoutHeaderTitle`, used nowhere else in the app for this role) and should be migrated **to `TextPrimary`**, not to `SectionSubtitle`. This still fixes the light/dark inconsistency bug noted in §1.3, and additionally brings both titles in line with the app's actual dominant convention for plain-background titles.

### 1.5 Systemic bug found during this audit

Several unrelated color tokens share the **same broken pattern**: a gray/black value in light mode, but hard-coded to the accent teal/green in dark mode only — meaning the same visual element silently changes semantic color depending on theme, not by design:

| Token | Light mode | Dark mode |
|---|---|---|
| `MenuSectionLabel` (Terms/FAQ/Contact Us section headers, and separately reused for unrelated row labels — see §3.3 note) | `#C0C0C0` (Gray500) | `#11939A` (exact target green) |
| `ExerciseScreenTitle` (Exercises list screen title) | `#393C3C` (TextPrimary) | `#11939A` (exact target green) |
| `ExerciseWorkoutHeaderTitle` (Exercise Workout screen title, already found in v2) | `#393C3C` | `#11939A` |

This is very likely the root cause of a lot of the "sometimes green, sometimes not" impression across the app — it's not just per-screen inconsistency, it's some tokens being inconsistent **with themselves** between themes. Fixing the items below resolves this by replacing the broken tokens with deliberate, theme-independent choices.

### 1.5.1 How this pattern likely originated

This bug shape — gray/black in light mode, accent-teal in dark mode, on otherwise-unrelated tokens — is distinctive enough that it's worth reconstructing the likely sequence of events, both to explain why it's so widespread and to make sure the fix doesn't just patch symptoms without understanding the cause:

1. A token (say, `ExerciseScreenTitle`) is first defined for light mode only, using the obvious choice for a plain-background heading at the time: `TextPrimary`'s value, `#393C3C`.
2. Later, dark-mode support is added to the app as a broader initiative. Whoever wires up the dark-mode value for this specific token is working through a long list of tokens that all need a dark-mode counterpart, under time pressure, without a design spec for each individual one.
3. Rather than deriving what the *correct* dark-mode-adaptive equivalent of `TextPrimary` should be for this specific token (which would have been the near-white `#E8FAFA` used elsewhere), the implementer either (a) copy-pasted a nearby teal literal that was visible in the same file or a neighboring file, because it "looked reasonable" against a dark background in isolation, or (b) grabbed `#11939A` specifically because it's the single most recognizable, most copy-pasted accent color in the codebase (it appears in more places than any other single teal literal), making it the most likely candidate to end up in someone's clipboard while working through a long list of tokens.
4. The result compiles, renders "fine" in a dark-mode-only screenshot review (nobody flags it because in isolation, dark teal text on a dark screen background looks perfectly plausible), and ships — without anyone comparing it side-by-side against the same screen's light-mode rendering, which is the only way this bug becomes visible.

This reconstruction matters because it tells us the fix needs to be **the theme-adaptive `TextPrimary` token itself**, not a second fixed literal — these are screen titles, not section sub-titles, and giving them a fixed value (even the "safe-looking" `#11939A`) would just trade one kind of inconsistency (self-contradicting between themes) for another (screen titles matching section sub-titles, which §1.4 explicitly rejects as a false unification). The correct fix is to point these call sites at the *existing, already-correct, already-theme-adaptive* `TextPrimary` token — which is exactly what §3.2 does.

### 1.5.2 Full inventory of affected screens

To make the scope of the self-inconsistency bug concrete, here is the complete list of user-visible screens where a person toggling light/dark mode would, prior to this fix, see a heading-role text element visibly change hue in a way with no design justification:

| Screen | Affected text | User-visible symptom before fix |
|---|---|---|
| Exercise Workout screen | "Exercise" title | Dark gray in light mode, bright teal in dark mode |
| Exercises list screen | "Exercises" title | Dark gray in light mode, bright teal in dark mode |
| Terms screen | Each section heading | Light gray in light mode, bright teal in dark mode |
| Help / Contact Us screen | "Contact Us" heading | Light gray in light mode, bright teal in dark mode |
| Help / FAQ screen | "FAQ" heading | Light gray in light mode, bright teal in dark mode |
| Calories History screen | "Calories History" title | Black in light mode, a *different, unrelated* teal (`#13A4AB`, not even the same literal as the others) in dark mode — its own bespoke one-off pair, not even sharing the exact same broken-pair value as the rest of this family |
| News home screen | "Breaking News" / "Recommendation" | Uses `NewsScreenTitle`, part of the same general family of screen-specific one-off tokens with independently-drifted light/dark values |

Every row in this table becomes, after this plan is implemented, a theme-stable rendering: either fixed at `#11939A` (if it's a section sub-title per §3.1) or theme-adaptively resolved via `TextPrimary` (if it's a screen title per §3.2) — in both cases, a single deliberate choice instead of an accidental one.

### 1.6 Audit methodology (how the 22 in-scope items were found)

Since this plan's credibility rests entirely on the completeness of its audit — a missed call site just perpetuates the exact inconsistency being fixed — it's worth documenting the search strategy in enough detail that a reviewer can independently re-run it and get the same result.

**Pass 1 (superseded, name-based):** Searched the codebase for exact references to the style tokens observed on the reference screen (`sectionTitle`, `headerTitle`, `headlineMedium`), on the theory that other section sub-titles would share the same *style* token even if their *color* differed. This found the majority of items but systematically missed anything built with a differently-named style (`titleLarge`, `titleMedium`, `titleSmall`), because those styles are also legitimately used for many things that are *not* section sub-titles (body copy, card headers, list-item titles), so a pure style-name search either over- or under-matches depending on which style is searched.

**Pass 2 (this revision, role-based):** Instead of searching by style-token name, every `Text(...)` composable in the presentation module was reviewed against a single question: *does this text sit as a heading directly above a group of related content, on the screen's plain background?* This is a slower, more manual process than a grep-based name search, but it is robust to the style-token being anything at all — it caught "Family Members" (built with `titleLarge`) precisely because that item satisfies the role-based question even though it would never have matched a `sectionTitle`/`headerTitle`/`headlineMedium` name search.

**Cross-check:** Every item found by either pass was then checked against its actual background context (reading the enclosing `Column`/`Box`/`Card` modifiers, not just the text's own modifiers) to confirm "plain background" was actually true and not just assumed from the screen's general reputation. This cross-check is what surfaced the two explicit exclusions in §1.2 (which *look* like they'd match the role-based question in isolation, but fail the background-context check) and what flagged items 17–18 as needing an additional in-app visual confirmation rather than a purely static one, since their background context could not be fully resolved from source alone.

**What was explicitly searched for and excluded as false positives:** counters/badges (numeric or icon content next to a heading, not itself a heading), "View All"-style navigation links (functionally links, not headings, even when visually adjacent to one), form field labels for individual inputs (a different, much more granular role than a section heading), and numeric/data values inside cards (e.g., the BMI value in `BmiGoalCard.kt`, explicitly corrected out of scope per §1.2's third bullet after v1 mistakenly included it).

### 1.7 Confidence levels by item

Not every item in §3.1/§3.2 carries the same evidentiary weight. To help a reviewer prioritize where to spend extra manual-verification attention, items are grouped here by how directly their background context was confirmed during the audit:

**High confidence (background context read directly from the enclosing layout code, not inferred):** items 1–16, 19–22. For each of these, the audit traced the actual `Modifier.background(...)` chain (or absence thereof, confirming the screen's default plain background applies) on the enclosing container, not just assumed it from the screen's general look.

**Medium confidence (background context plausible from code structure but not visually confirmed in a running app):** items 17–18 (`NewsHomeScreen.kt`). The static code review strongly suggests these sit on the plain screen background — no `.background()` modifier with a solid fill was found on their enclosing containers — but the News screen's layout is more visually dense than the others (cards, pagers, chips), so an in-app visual double-check is explicitly required by §4.2 step 9 before this item is treated as final.

This confidence grading is also why items 17–18 are called out individually in three separate places in this document (the §3.1 table note, §4.2 step 9, and here) rather than just once — a reviewer skimming any single section should still encounter the caveat.

---

## 2. Approved Architecture

Introduce a **new, semantically-named color token** — `SectionSubtitle` — rather than pointing every call site at `CaloriesAccentTeal1200` directly. This keeps the Calories-specific token free to diverge later without silently affecting every other screen.

**File:** `common/theme/AppColors.kt`

1. Add to the `AppColorsExtension` interface/data class (near `CaloriesAccentTeal1200`, ~line 98):
   ```kotlin
   val SectionSubtitle: Color,
   ```
2. Expose via `AppColors` (near line 419):
   ```kotlin
   val SectionSubtitle: Color get() = extension.SectionSubtitle
   ```
3. Set in the **light** palette instantiation (~line 743, alongside `CaloriesAccentTeal1200`):
   ```kotlin
   SectionSubtitle = Color(0xFF11939A), // Teal/1200 — unified title/sub-title color; fixed, identical in both themes (see docs/plans/2026-08-08-unify-section-subtitle-color-v2.md)
   ```
4. Set identically in the **dark** palette instantiation (~line 1087):
   ```kotlin
   SectionSubtitle = Color(0xFF11939A), // Teal/1200 — intentionally identical to light mode
   ```

Do **not** reuse `CaloriesAccentTeal1200` directly at call sites, and do not delete `CaloriesAccentTeal1200` — it's still used by `DashedActionCard.kt`'s border and by `CaloriesScreen.kt`'s own title (which can optionally be migrated to `SectionSubtitle` too, see §3.1 item 1, at the implementer's discretion, since the values are identical).

---

## 3. Proposed Changes (by module)

### 3.1 In-scope: apply `AppTheme.colors.SectionSubtitle`

Replace **only** the `color = ...` argument at each site below. Do not change `style`, text, spacing, or other modifiers.

| # | File | Line(s) | Text / Purpose | Current color | Background context |
|---|------|---------|-----------------|----------------|----------------------|
| 1 | `main/calories/view/CaloriesScreen.kt` | 300 | "Daily Products" (baseline — optional migration for consistency of the token used, visual result unchanged) | `CaloriesAccentTeal1200` | Plain |
| 2 | `home/view/HomeScreen.kt` | 169 | "Explore" | `PrimaryVariant` | Plain |
| 3 | `home/view/HomeScreen.kt` | 203 | "Recent History" | `PrimaryVariant` | Plain |
| 4 | `common/components/WaterTrackerCard.kt` | ~53, 66–68 | "Water" | local `headerColor` val (`Teal300`/`Gray1600`) — **delete this val**, use token directly | Plain |
| 5 | `settings/profile/edit/view/EditProfileScreen.kt` | 365 | Form section title | `ProfileSetupSectionTitle` | Plain |
| 6 | `settings/profile/edit/view/EditProfileScreen.kt` | 422 | Form section title | `ProfileSetupSectionTitle` | Plain |
| 7 | `settings/profile/view/components/AddFamilyMemberBottomSheet.kt` | 133 | Bottom-sheet title | `Teal1000` | Plain |
| 8 | `settings/profile/view/components/AddFamilyMemberBottomSheet.kt` | 265 | Field group title | `ProfileSetupSectionTitle` | Plain |
| 9 | `settings/profile/view/components/AddFamilyMemberBottomSheet.kt` | 283 | Field group title | `ProfileSetupSectionTitle` | Plain |
| 10 | `settings/profile/view/components/FamilyMembersSection.kt` | 53 | "Family Members" (Profile screen, reached from bottom navbar) | `Teal1000` | Plain |
| 11 | `settings/terms/view/components/TermsSection.kt` | 19–21 | Each Terms section heading | `MenuSectionLabel` (broken — see §1.5) | Plain |
| 12 | `settings/help/view/components/HelpContactSection.kt` | 24–26 | "Contact Us" | `MenuSectionLabel` (broken — see §1.5) | Plain |
| 13 | `settings/help/view/HelpScreen.kt` | 93–96 | "FAQ" | `MenuSectionLabel` (broken — see §1.5) | Plain |
| 14 | `settings/notifications/view/NotificationSettingsScreen.kt` | `NotificationCategorySection` composable, title param | "General" / "Reminders" / "Content" category headers | `Gray500` | Plain |
| 15 | `settings/notifications/view/NotificationSettingsScreen.kt` | 181–184 | "Quiet Hours" | `Gray500` | Plain |
| 16 | `settings/notifications/view/NotificationSettingsScreen.kt` | 205–208 | Battery permission header | `Gray500` | Plain |
| 17 | `news/home/view/NewsHomeScreen.kt` | 151–154 | "Breaking News" | `NewsScreenTitle` (light/dark mismatch, same family as §1.5) | Plain (verify no colored band before implementing — not yet confirmed) |
| 18 | `news/home/view/NewsHomeScreen.kt` | 176–179 | "Recommendation" | `NewsScreenTitle` | Plain (verify no colored band before implementing — not yet confirmed) |

**Item 4 detail:** `WaterTrackerCard.kt` currently computes `val headerColor = if (isDark) AppTheme.colors.Teal300 else AppTheme.colors.Gray1600` purely to color this title. Remove that val entirely (don't leave dead code) and reference `AppTheme.colors.SectionSubtitle` directly at the `Text` call site.

### 3.2 In-scope: apply `AppTheme.colors.TextPrimary` (screen titles on plain backgrounds)

These are **screen-title-role** elements, not section sub-titles — they get the dominant existing plain-background title token, not the green.

| # | File | Line(s) | Text / Purpose | Current color | Change to |
|---|------|---------|-----------------|----------------|-----------|
| 19 | `nutrigpt/chat/view/components/ChatTopBar.kt` | 55 | "NutriGPT" chat screen title | `PrimaryVariant` | `AppTheme.colors.TextPrimary` |
| 20 | `exercises/workout/view/ExerciseWorkoutScreen.kt` | 187–189 | "Exercise" screen title | `ExerciseWorkoutHeaderTitle` (broken — see §1.5) | `AppTheme.colors.TextPrimary` |
| 21 | `exercises/view/ExercisesScreen.kt` | 96–99 | "Exercises" screen title | `ExerciseScreenTitle` (broken — see §1.5) | `AppTheme.colors.TextPrimary` |
| 22 | `calories_history/view/components/CaloriesHistoryTopBar.kt` | ~50–53 | "Calories History" screen title | `CaloriesHistoryTitle` (`#000000` light / `#13A4AB` dark — its own inconsistent one-off pair) | `AppTheme.colors.TextPrimary` |

**Items 20–21 detail:** Note these are two *different* tokens on two *different* screens (`ExerciseWorkoutHeaderTitle` on the workout-session screen, `ExerciseScreenTitle` on the exercises-list screen) that happen to share the identical bug pattern from §1.5. Both should be migrated independently — check whether either token is still referenced elsewhere in its file before considering (separately, out of scope) deleting the now-unused token definition.

### 3.3 Reference — already correct, no change required

| File | Line(s) | Text / Purpose | Current color | Why it's already right |
|------|---------|-----------------|----------------|--------------------------|
| `account_deletion/view/AccountPendingDeletionScreen.kt` | 129–131 | "Account Deletion Pending" heading | `colors.TextPrimary` | Screen-title-role element on a plain background — already on the correct centralized token per §1.4. Listed here for completeness; **do not modify.** |
| `common/components/HeroHeaderText.kt` + all screens using it | — | Screen titles on the teal band | `Teal300` | Already the correct centralized token for this context per §1.4; **do not modify.** |

**Important — do NOT touch `MenuSectionLabel`'s other usages:** the same `MenuSectionLabel` token is also used for unrelated **per-item row labels** (not section headers) in `SettingsActionRow.kt`, `ProfileMenuRow.kt`, `ExploreItemRow.kt`, and `FaqAccordionItem.kt` — icon-plus-label rows inside a list, a different UI role from a heading above a group. These must **not** be changed to `SectionSubtitle`; only the 3 true section-header usages (items 11–13 in §3.1) should move. This means: **fix the 3 call sites individually** (point them at `SectionSubtitle` directly), rather than changing what `MenuSectionLabel` itself resolves to — changing the token's own definition would also flip the color of every row label across Settings, Help, and Home, which is out of scope and not what was asked.

**Excluded — onboarding (per explicit user instruction):**
| File | Line(s) | Reason |
|------|---------|--------|
| `profile_setup/view/components/HealthProfileContent.kt` | 91, 111 | Onboarding flow — explicitly excluded by user. Continues using `ProfileSetupSectionTitle` unchanged. Note: this token is *shared by name* with items 5–9 above, but since this plan only changes call-site `color = ...` arguments (not the `ProfileSetupSectionTitle` token's own value), onboarding is unaffected regardless. |

**Excluded — contrast risk (see §1.2):**
| File | Line(s) | Reason |
|------|---------|--------|
| `common/components/HeroHeaderText.kt` (+ all 6+ screens using `HeroHeaderTitle`/`HeroHeaderSubtitle`) | — | Solid `Teal1000` band background; already a correctly-centralized separate system |
| `common/components/BmiGoalCard.kt` | 81 | Solid `Teal500` card background |
| `common/components/CalorieGoalsCard.kt` | 83 | Solid `Teal500` card background |
| `common/components/BmiGoalCard.kt` | 102 | Not a title (BMI numeric value) — removed from scope, v1 error corrected |

### 3.2 Localization & Theming compliance

Per `SKILL.md` §2–3: no new hardcoded strings or hex colors are introduced in any Composable. Every change swaps one theme token reference for another (`AppTheme.colors.SectionSubtitle`), which is the compliant pattern. The new token itself is defined once, centrally, in `AppColors.kt`.

---

## 4. Verification Plan

### 4.1 Automated

- No ViewModel/state/event logic is touched anywhere in this change, so `SKILL.md` §4's ViewModel test template does not apply here.
- If the project has Compose screenshot testing (Paparazzi/Roborazzi) already set up, regenerate goldens (light & dark) for: `HomeScreen`, `CaloriesScreen`, `WaterTrackerCard`, `EditProfileScreen`, `AddFamilyMemberBottomSheet`, `AccountPendingDeletionScreen`, `ExerciseWorkoutScreen`, `ChatTopBar`/NutriGPT chat screen. Do not add a new test harness if one doesn't already exist for these — check first.
- After implementation, run:
  ```
  grep -rn "val headerColor" presentation/src/main/kotlin
  grep -rn "SectionSubtitle" presentation/src/main/kotlin
  grep -rn "ExerciseWorkoutHeaderTitle\|PrimaryVariant" presentation/src/main/kotlin/iti/grad/nutriscan/presentation/exercises presentation/src/main/kotlin/iti/grad/nutriscan/presentation/nutrigpt
  ```
  to confirm the dead local var is gone, the new token is wired everywhere listed in §3.1, and the §3.2 outliers no longer reference their old one-off tokens.

### 4.2 Manual

1. Run the app in **light** and **dark** theme.
2. Confirm all 18 items in §3.1's table render as the exact same green (`#11939A`) in both themes, matching "Daily Products" — these are section sub-titles, not screen titles. Pay particular attention to: Family Members (Profile), Terms sections, Contact Us, FAQ, and the three Notification Settings headers (General/Reminders/Content, Quiet Hours, Battery) — these were the items missed in the first pass.
3. Confirm the 4 items in §3.2 (`ChatTopBar`, `ExerciseWorkoutScreen`, `ExercisesScreen`, `CaloriesHistoryTopBar`) now render in `TextPrimary`, matching "Account Deletion Pending" — and that none of them change color between light/dark mode anymore (bug fix check per §1.5).
4. Specifically re-verify the **row labels** using `MenuSectionLabel` (Settings action rows, Profile menu rows, Explore item rows, FAQ question rows) are **unchanged** — they should still look exactly as before, since only 3 of `MenuSectionLabel`'s call sites were touched, not the token itself.
5. Confirm **unchanged** items stay unchanged: `HeroHeaderTitle` bands (Settings/Help/Terms/Notifications/Home/Profile/Calories/Saved/Account-Deletion), `BmiGoalCard`/`CalorieGoalsCard` titles and the BMI numeric value, "Account Deletion Pending" heading, and onboarding's `HealthProfileContent.kt` titles.
6. Check Arabic (RTL) locale on all changed screens — only color should differ; layout/RTL mirroring and copy must be unaffected.
7. Confirm `DashedActionCard`'s teal border (still on `CaloriesAccentTeal1200`) is unchanged.
8. Basic contrast sanity check on both tiers: §3.1 items sit on plain backgrounds with `#11939A` (mid-tone teal-green) — verify legibility in both themes. §3.2 items now use `TextPrimary`, which is already proven legible everywhere else it's used, so no new contrast risk is expected there.
9. Before touching `NewsHomeScreen.kt` (items 17–18), confirm visually that "Breaking News" and "Recommendation" sit on the plain screen background, not a colored card — this was not fully confirmed during the audit (see §3.1 table note) and should be checked in-app before implementing.

### 4.3 Expanded per-screen manual verification checklist

The following expands step 2–5 above into a screen-by-screen checklist suitable for pasting directly into a QA pass or PR description, so each item can be checked off independently rather than verified only in aggregate.

**Home screen**
- [ ] "Explore" heading renders as `#11939A` in light mode
- [ ] "Explore" heading renders as `#11939A` in dark mode (identical to light mode)
- [ ] "Recent History" heading renders as `#11939A` in both modes
- [ ] "View All" link next to "Recent History" is unchanged (still `Teal800`, not touched by this plan)

**Calories screen**
- [ ] "Daily Products" heading is visually unchanged from before this plan (since the value is identical, this is a no-op visually, but confirms the token swap didn't introduce a regression)
- [ ] `DashedActionCard`'s teal border is unchanged (still `CaloriesAccentTeal1200`, untouched)

**Water tracker card**
- [ ] "Water" heading renders as `#11939A` in both light and dark mode
- [ ] Confirm no leftover reference to the deleted `headerColor` local causes a build error
- [ ] Confirm cup-fill tint, empty-glass tint, and add-button styling are all visually unchanged (none of these were touched)

**Edit Profile screen**
- [ ] "Chronic Conditions" section heading renders as `#11939A` in both modes
- [ ] "Allergies" section heading renders as `#11939A` in both modes
- [ ] All other text on this screen (labels, input fields, helper text) unchanged

**Add Family Member bottom sheet**
- [ ] Bottom-sheet title ("Add Family Member" / "Edit Family Member") renders as `#11939A` in both modes
- [ ] "Chronic Conditions" and "Allergies" field-group titles render as `#11939A` in both modes
- [ ] Image change/remove/retry action text colors are unchanged (these use `Teal1000` and `Error` respectively and were intentionally left untouched — verify they were *not* accidentally swept into the change)

**Profile screen — Family Members section**
- [ ] "Family Members" heading renders as `#11939A` in both modes

**Settings — Terms screen**
- [ ] Every individual Terms section heading renders as `#11939A` in both modes
- [ ] Confirm the per-item row labels elsewhere in Settings (unrelated `MenuSectionLabel` usages) are unaffected

**Settings — Help screen**
- [ ] "Contact Us" heading renders as `#11939A` in both modes
- [ ] "FAQ" heading renders as `#11939A` in both modes
- [ ] FAQ question row labels (a different, unrelated `MenuSectionLabel` usage) are unchanged

**Settings — Notifications screen**
- [ ] "General" category header renders as `#11939A` in both modes
- [ ] "Reminders" category header renders as `#11939A` in both modes
- [ ] "Content" category header renders as `#11939A` in both modes
- [ ] "Quiet Hours" header renders as `#11939A` in both modes
- [ ] Battery permission header renders as `#11939A` in both modes (only visible when `!state.isIgnoringBatteryOptimizations`, so toggle that device setting to confirm both states)

**News home screen**
- [ ] Confirmed in-app (not just from static code reading) that "Breaking News" sits on the plain screen background before implementing
- [ ] Confirmed in-app that "Recommendation" sits on the plain screen background before implementing
- [ ] Both headings render as `#11939A` in both modes after implementation
- [ ] Search icon tint (a separate, untouched usage of the old `NewsScreenTitle` token) is unchanged

**NutriGPT chat screen**
- [ ] "NutriGPT" top bar title renders as `TextPrimary` in both modes (dark gray in light mode, near-white in dark mode — i.e., it should now visibly differ between modes, which is correct and expected for this token, unlike the sub-title items above)

**Exercise Workout screen**
- [ ] "Exercise" top bar title renders as `TextPrimary` in both modes
- [ ] The exercise name text below it (a separate element, intentionally left on the old token) is unchanged

**Exercises list screen**
- [ ] "Exercises" top bar title renders as `TextPrimary` in both modes

**Calories History screen**
- [ ] "Calories History" top bar title renders as `TextPrimary` in both modes
- [ ] Confirm it no longer shows the old bespoke black/`#13A4AB` pair

**Account Pending Deletion screen (reference only, no change expected)**
- [ ] "Account Deletion Pending" heading is pixel-identical to before this plan (it was already correct and untouched)

---

## 5. Out of Scope / Follow-ups

- Pulling `HeroHeaderTitle`, the `BmiGoalCard`/`CalorieGoalsCard` titles, or plain-background screen titles toward the Daily Products green — explicitly rejected per §1.4; screen titles use their own two-tier system (`Teal300` on colored bands, `TextPrimary` on plain backgrounds), never the sub-title green.
- Onboarding (`HealthProfileContent.kt`) — explicitly excluded by user request.
- Any icon tints, badges, progress bars, or stat/numeric values (e.g. BMI value, calorie counters) near these titles — title/heading text color only.
- Deleting the now-possibly-orphaned `ExerciseWorkoutHeaderTitle` token definition — separate cleanup, not bundled here.
- Small descriptions/captions (`HeroHeaderSubtitle`, `Gray100`) were audited in §1.3 and found already fully centralized — no changes proposed or needed.

---

## 6. Risk Assessment

### 6.1 Overall risk level: Low

This is a pure `color = ...` argument swap at 22 call sites plus one new token definition. No layout, spacing, typography, string, navigation, state, or business-logic code is touched anywhere in this plan. The risk surface is limited to: (a) visual regressions in the two themes, and (b) the small chance that a background context was misclassified during the audit (i.e., an item assumed to sit on a "plain" background turns out to sit on something else in practice).

### 6.2 Risk breakdown by category

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Contrast/legibility regression from a misclassified background | Low | Medium | §3.1 items 17–18 (`NewsHomeScreen.kt`) are explicitly flagged in the table and in §4.2 step 9 as "not yet confirmed" and require an in-app visual check before/immediately after implementation. All other items were confirmed against a plain background during the audit. |
| Merge conflicts with concurrent feature work touching the same files | Medium | Low | Every change is a single-line `color =` swap; conflicts, if any, will be trivial to resolve by re-applying the intended token. No structural changes increase conflict surface. |
| Missed call site (an item that should have been in scope but wasn't caught by the audit) | Low | Low | The audit was performed twice (v1 by style-token name, this v4 revision by semantic role instead), specifically to catch role-based misses like "Family Members" that a name-based search missed. A role-based re-audit is a fundamentally different search strategy than a name-based one, which is why it surfaced 9 additional confirmed items that the first pass missed entirely. |
| Over-application (an item wrongly migrated that should have stayed put) | Low | Medium | Every exclusion has explicit, documented reasoning (§1.2, §3.3) grounded in actual background-color evidence from the source, not assumption. The two colored-card/band exclusions were specifically re-verified against their own code comments stating deliberate design intent. |
| Regression in RTL (Arabic) locale rendering | Very low | Low | Only `color` arguments change; no layout modifiers, alignment, or text-direction-sensitive code is touched. Explicitly called out for manual re-verification in §4.2 step 6 regardless. |
| New token misconfigured (wrong hex, wrong theme parity) | Very low | Medium | The new `SectionSubtitle` token is defined using the exact same literal value, in the exact same file locations, following the exact same pattern as the already-shipping `CaloriesAccentTeal1200` token it is modeled on — minimizing the chance of a transcription error. |

### 6.3 What could make this riskier than assessed

The main way this plan's actual risk could exceed the "Low" assessment above is if the audit itself has blind spots — i.e., if there are additional call sites playing the section-sub-title or plain-background-screen-title role that weren't found by either the v1 name-based search or this v4 role-based search. Both search strategies were applied, which substantially reduces (but cannot mathematically guarantee to zero) the chance of a remaining miss. Any contributor implementing this plan who notices, while working through the listed files, a heading-like element that visually matches this pattern but isn't in the tables above should flag it rather than silently including or silently ignoring it, since it may represent exactly the kind of miss this two-pass audit was designed to catch.

---

## 7. Rollback Plan

Because every change in this plan is a single-token-reference swap with no structural, state, or logic changes, rollback is straightforward at every level of granularity:

### 7.1 Full rollback

Revert the commit(s) implementing this plan. Since no other work depends on the `SectionSubtitle` token existing (it is new, and its only consumers are the 18 call sites this plan adds), a full revert is safe and self-contained.

### 7.2 Partial rollback (single item)

Because §3.1 and §3.2 are structured as a flat list of independent, individually-attributable call-site changes, any single item can be reverted in isolation by restoring that one call site's original `color = ...` argument, without needing to touch the `SectionSubtitle` token definition or any other call site. This is useful if, for example, in-app QA finds that one specific item (say, the News screen items pending background confirmation per §4.1 step 9) needs to be walked back while the other 21 items are kept.

### 7.3 Token-level rollback

If, after implementation, a decision is made that the entire `SectionSubtitle` concept was the wrong call (e.g., a fresh design review decides on a different target color entirely), only the color literal in the two `AppColors.kt` palette instantiations (§2, steps 3–4) needs to change — every one of the 18 consuming call sites automatically inherits the new value with no further code changes, which is precisely the benefit of centralizing through a semantic token rather than pointing call sites at a raw literal.

---

## 8. Glossary

For readers less familiar with this codebase's theming conventions, the following terms recur throughout this plan:

- **Token** — a named reference to a color value (e.g., `SectionSubtitle`, `TextPrimary`) defined once in `AppColors.kt` and referenced by name at call sites, rather than call sites hardcoding a raw hex literal directly. This indirection is what makes centralized, single-point-of-change color management possible.
- **Fixed / theme-independent token** — a token that resolves to the exact same color value regardless of whether the app is in light or dark mode (e.g., `CaloriesAccentTeal1200`, and now `SectionSubtitle`).
- **Theme-adaptive token** — a token that resolves to a different value depending on light/dark mode, by design (e.g., `TextPrimary`, which is dark gray in light mode and near-white in dark mode).
- **Call site** — the specific line of Composable code where a token is actually referenced via `color = AppTheme.colors.<TokenName>`, as distinct from the token's *definition* in `AppColors.kt`.
- **Plain background** — a screen background using the app's default `Background`/`Surface`-family token, as opposed to a background with a solid, saturated color fill (a "band" or "card").
- **Section sub-title** — the semantic role targeted by this plan: a heading that introduces a group of related content directly on the screen's plain background (e.g., "Daily Products," "Water," "Explore," "FAQ").
- **Screen title** — a distinct semantic role from a section sub-title: the top-level identifying label for an entire screen, typically near a back button in a top app bar (e.g., "Exercises," "NutriGPT," "Calories History").
- **One-off token** — a token defined and consumed in essentially one place, as opposed to a shared, reusable, semantically-named token used consistently everywhere a given role appears. Much of the problem this plan solves is the proliferation of one-off tokens that all happen to serve the same underlying role.

---

## 9. Frequently Anticipated Questions

**Q: Why introduce a brand-new token (`SectionSubtitle`) instead of just pointing every call site directly at the existing `CaloriesAccentTeal1200`?**
A: Answered in §2 — this keeps the Calories-specific token free to diverge later (e.g., if the Calories screen's own visual identity needs to change independently) without silently affecting every other screen that would otherwise be coupled to it. Semantic naming (`SectionSubtitle`) also makes the *intent* of each call site self-documenting to a future reader, in a way that `CaloriesAccentTeal1200` referenced from an unrelated Settings screen would not be.

**Q: Why does this plan touch `MenuSectionLabel`'s call sites individually instead of just changing what `MenuSectionLabel` itself resolves to?**
A: Answered in §3.3 — `MenuSectionLabel` is also used for a completely different UI role (per-item row labels inside lists) in four other files. Changing the token's own definition would silently recolor those unrelated row labels too, which is out of scope and was not requested.

**Q: Why are screen titles getting a *different* target color than section sub-titles, when the original ask was to "unify" colors?**
A: Answered in §1.4 — screen titles and section sub-titles are different semantic roles that already have their own separate, internally-consistent conventions once you look past the buggy self-inconsistent tokens. "Unify" here means "make each role internally consistent with itself across the whole app," not "collapse every distinct role into one single color," which would actually reduce the amount of information color conveys to the user (a screen title and a section heading currently look intentionally different in weight and role, and that distinction is worth preserving).

**Q: Does this plan change anything about how dark mode itself works?**
A: No — see §1.0.2. It only supplies deliberate, verified values to specific existing tokens and call sites. The dark-mode switching mechanism, `AppTheme.isDark`, and the overall palette architecture are completely unchanged.

**Q: What happens to the old one-off tokens (`ExerciseScreenTitle`, `ExerciseWorkoutHeaderTitle`, `CaloriesHistoryTitle`, `NewsScreenTitle`, `MenuSectionLabel`'s section-heading usages) after this plan ships?**
A: Their *call sites* relevant to this plan are repointed to `SectionSubtitle` or `TextPrimary`. The token *definitions* themselves are left in place (per §3.1 item-4-style caution about not leaving dead code, applied narrowly — only truly dead locals like `WaterTrackerCard.kt`'s `headerColor` are removed as part of this pass). Whether any of these token definitions become fully unused and safe to delete afterward is called out explicitly as a separate, out-of-scope cleanup task (see §5 and the note under §3.1 items 20–21) requiring its own check of whether the token is still referenced anywhere else in its file before deletion.
