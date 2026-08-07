# CLAUDE.md

This repo's actual rules live in `AGENTS.md` (root) plus `data/AGENTS.md`,
`domain/AGENTS.md`, and `presentation/AGENTS.md` (module-scoped). Read the
relevant ones before touching code in that area — they are long and
detailed on purpose; this file is just the pointer and the TL;DR.

## Commands

Multi-module Gradle project: `:app`, `:domain`, `:data`, `:presentation`.

- Build debug APK: `./gradlew assembleDebug`
- Unit tests (all modules): `./gradlew test`
- Unit tests, single module: `./gradlew :presentation:test`
- Single test class: `./gradlew :presentation:test --tests "*.ScanResultViewModelTest"`
- Lint: `./gradlew lint`

## Non-negotiables (see root `AGENTS.md` for full detail)

- **Plan first.** Before writing/modifying code for any non-trivial feature,
  save an implementation plan to `docs/plans/YYYY-MM-DD-feature-name.md`
  (§12). Skipping this is a protocol violation, not a shortcut.
- **Every ViewModel needs a test file.** `*ViewModelTest.kt` covering initial
  state, every Event → State transition, every Event → Effect, and error
  paths (§11.2). A ViewModel change without a test update is incomplete.
- **Zero hardcoded user-facing strings.** Every string goes in
  `strings.xml` (English + Arabic) before it appears in Kotlin/Compose code
  (§14.4).
- **Strict layer boundaries**: `presentation → domain ← data`. Domain has
  zero Android/framework dependencies — pure Kotlin only. Never import a
  presentation type from domain or data.
- **Destructive actions require confirmation** (§13.9) — the real,
  established pattern in this codebase is a nullable/boolean field in
  `State` gating a `ConfirmationDialog` (see `AppSettingsScreen`,
  `UserProfileScreen`, and the Calories food-log swipe-to-remove flow), not
  a raw Material3 `AlertDialog`.
- **`AppTheme.colors` / `AppTheme.typography` / `AppTheme.shapes` only** —
  never a hardcoded `Color(0xFF...)`, raw `sp`/`dp` text style, or ad-hoc
  corner radius in a Composable.
- **No AI co-author trailer on commits** (§17.3) — commits made with AI
  agent assistance must not include a `Co-Authored-By:` line for the agent.

## Where to look for precedent

- **Offline-first Room repository**: `data/repository/FoodLogRepositoryImpl.kt`
  is the reference implementation of the `runCatchingCancellable` +
  `@IoDispatcher` pattern (root `AGENTS.md` §6.4/§9) — copy its shape for new
  repositories, not the older ones that predate it.
- **Reusable swipeable product card**: `presentation/common/components/ProductCard.kt`
  takes an optional `ProductCardSwipeAction` (`Add` or `Remove`) — extend
  that sealed interface for a new swipe mode rather than forking the
  component.
- **Plan file format**: any file under `docs/plans/` is a real example of
  the required structure.

If anything here conflicts with `AGENTS.md`, `AGENTS.md` wins — update this
file to match, don't silently deviate.
