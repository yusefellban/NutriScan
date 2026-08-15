# NutriScan UI/UX Overhaul — Login/Register header, Home tip card, Calories screen, Steps, Water, Saved swipe

## Context

Bundle of 8 UI/UX fixes gathered from the user in one request, split into 4 phases by risk/dependency:

- Auth screens show a blank white strip above the teal header (status-bar inset drawn with background color instead of teal).
- Home's daily health tip is hard-truncated (`maxLines = 2`) with no way to read the full tip.
- Calories screen: swipe-to-delete is a fiddly 44dp drag-pill instead of a normal button; no bulk/remove-all option; food cards float in a bare row instead of the bordered container shown in the target [Figma frame](https://www.figma.com/design/zs5EkmcWPyQJvwWY5awVQc/NutriScan?node-id=992-7155); a duplicate BMI/goals card needs removing.
- Steps only refresh when the Calories screen is composed (WorkManager sync is capped at a 15-min floor) — user wants true live updates even while backgrounded.
- Water tracker cup fill/unfill is an instant icon swap; jumping to cup N is currently a no-op (only ±1 boundary taps work).
- Saved screen's "swipe to add" requires dragging a tiny inner button, not the card itself.

Confirmed with user: BMI card to remove is `BmiGoalCard` inside `CalorieGoalsPager` (Calories tab — Home has no BMI card). Steps should use a real foreground Service, not just a tighter WorkManager interval. The "remove one-by-one" icon toggles a manage-mode that reveals per-card delete buttons (they're hidden by default).

Figma frame `992:7155` confirms the target look for Calories: a dashed-bordered rounded container wraps the whole product row, and each card has a thin red strip with a small trash glyph at the bottom instead of a swipe control.

## Phase 1 — Low-risk UI fixes (auth header, tip card, Saved swipe)

**1a. Auth screens' blank top strip**
- `presentation/common/components/AuthHeader.kt`: move the status-bar inset *inside* the teal panel. Add `Modifier.statusBarsPadding()` to the inner content (logo/title `Column`), not the outer `Box`, so the teal rounded panel's `background()` still starts at y=0 and paints behind the status bar; only the content shifts down.
- `presentation/auth/login/view/LoginScreen.kt` / `presentation/auth/register/view/RegisterScreen.kt`: stop consuming the Scaffold's top inset on the outer `Column` — change `.padding(paddingValues)` to only apply horizontal/bottom (`Modifier.padding(bottom = paddingValues.calculateBottomPadding())` + existing horizontal padding), so `AuthHeader` gets the full-bleed top.

**1b. Home daily tip card expand-on-click**
- `presentation/home/view/components/DailyHealthTipCard.kt`: add `var expanded by remember { mutableStateOf(false) }`, make the card `clickable { expanded = !expanded }`. Reuse the exact pattern from `presentation/settings/help/view/components/FaqAccordionItem.kt` (`AnimatedVisibility` with `fadeIn()+expandVertically()` / `fadeOut()+shrinkVertically()`, chevron `KeyboardArrowDown`/`KeyboardArrowUp` icon that rotates). Collapsed: `maxLines = 2, overflow = TextOverflow.Ellipsis`. Expanded: full text, no line cap.

**1c. ProductCard swipe mechanic → full-card drag**
- `presentation/common/components/ProductCard.kt`: rework `SwipeActionButton` so the drag target is the whole card surface (`Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }` on the outer `Surface`, with a reveal hint behind it), not the internal 44dp pill. Keep the same `Animatable` + `draggable` + spring-back-after-70% pattern, just widen the drag region to the full card width. This is a shared component change — it fixes Saved's "swipe right to add" immediately, and Calories still uses `ProductCardSwipeAction.Remove` until Phase 2 removes it, so both keep working through this phase.
- No call-site changes needed in `SavedProductGrid.kt` (still passes `ProductCardSwipeAction.Add`).

## Phase 2 — Calories screen (BMI removal, container wrap, button-based delete)

**2a. Remove BMI/goals card**
- `presentation/common/components/CalorieGoalsPager.kt`: remove the `BmiGoalCard` page/usage. Delete `BmiGoalCard.kt` if nothing else references it (grep first).

**2b. Wrap the food row in a bordered container (per Figma `992:7167`)**
- `presentation/main/calories/view/CaloriesScreen.kt` (~lines 184-223): wrap the existing horizontally-scrollable `Row` (leading `DashedActionCard` + `ProductCard`s) in a dashed-border rounded container (`RoundedCornerShape(24.dp)`, grey dashed stroke). Check `DashedActionCard.kt` first for an existing dash-border `Modifier`/util to reuse instead of hand-rolling a new `DashPathEffect` (ponytail rung 2 — reuse before inventing).

**2c. Replace swipe-to-remove with a delete button (Figma-style red strip)**
- `ProductCardSwipeAction.kt`: delete the `Remove` case (only `Add` remains — Saved is the only consumer of the swipe gesture now).
- `ProductCard.kt`: remove the `isRemove` branch/trash-icon path from `SwipeActionButton` (now Add-only). Add a new optional param `onDeleteClick: (() -> Unit)? = null` that renders a thin `AppTheme.colors.Error`-tinted bottom strip with a small `ic_trash` `IconButton`, matching Figma's `pajamas:remove` strip. Only rendered when non-null.
- `CaloriesScreen.kt`: drop `swipeAction = ProductCardSwipeAction.Remove(...)` from the `ProductCard` call; pass `onDeleteClick = { onEvent(CaloriesEvent.FoodItemDeleteClicked(food.logEntryId ?: food.id)) } }` instead, gated by manage-mode visibility (2d).
- `CaloriesEvent.kt`: rename `FoodItemSwipedToRemove` → `FoodItemDeleteClicked` (same payload). `CaloriesViewModel.kt`: same handler body, just renamed — `pendingRemoveFoodId` + `DeleteWarningAlert` wiring is unchanged (it's already the correct reusable pattern, just now triggered by a tap instead of a swipe).

**2d. Remove-all icon + manage-mode icon**
- `CaloriesState.kt`: add `manageModeEnabled: Boolean = false` and `showRemoveAllConfirmation: Boolean = false`.
- `CaloriesEvent.kt`: add `ManageModeToggled`, `RemoveAllFoodClicked`, `RemoveAllFoodConfirmed`, `RemoveAllFoodDismissed`.
- `CaloriesViewModel.kt`: `ManageModeToggled` flips `manageModeEnabled`. `RemoveAllFoodClicked` sets `showRemoveAllConfirmation = true`. `RemoveAllFoodConfirmed` loops `removeFoodEntry(...)` over `state.addedFoods` (no bulk domain use case exists — a loop is enough; add a real bulk use case only if this becomes a perf problem), then clears the flag. `RemoveAllFoodDismissed` clears it.
- `CaloriesScreen.kt`: two icon buttons near the "Daily Products" header (next to the Calorie badge, `992:7162` area) — bulk-trash icon → `RemoveAllFoodClicked`; manage-mode icon → `ManageModeToggled`. Per-card `onDeleteClick` (2c) is only passed when `state.manageModeEnabled` is true (hidden otherwise). Second `DeleteWarningAlert` instance gated by `state.showRemoveAllConfirmation` for the bulk confirm, mirroring the existing single-delete alert block.

## Phase 3 — Water tracker animation

- `presentation/common/components/WaterTrackerCard.kt`: replace the instant `cup_filled`/`cup_empty` icon swap with an animated bottom-to-top fill. Reuse the `animateFloatAsState` + `tween(400)` pattern already established in `StepsGaugeCard.kt` (closest existing analog) — animate a 0f→1f fraction per cup and drive a clipped fill (e.g. two stacked icons/`Canvas` with a vertical clip rect at `fraction * height`) so it visibly fills from the bottom up and drains top-down on unfill.
- `CaloriesViewModel.kt` `toggleWaterCup(index)`: extend beyond the current ±1-boundary-only logic to support "jump to cup N" — when `index >= state.waterConsumed`, step `waterConsumed` up to `index + 1` (via repeated `updateWaterCnt` calls or a direct set to `index + 1`); when below, no change (unfill stays boundary-only per existing decrement semantics, since removing water mid-stack isn't meaningful).
- UI cascade: stagger each cup's fill animation start by a small per-index delay (e.g. `LaunchedEffect(targetCount) { cups between old and new count animate with i * 60ms delay }`) so cups visibly fill one-by-one bottom-to-top when jumping to cup 8, not all at once.
- Update `CaloriesEvent.kt`'s doc comment on the water-cup-click event to reflect the new cascading semantics (currently documents the old boundary-only behavior).

## Phase 4 — Steps: real-time background tracking via foreground Service

- New `app/steps/StepsForegroundService.kt`: `@AndroidEntryPoint` `Service` (`START_STICKY`), starts `StepsRepositoryImpl`'s sensor observation and keeps it registered for the service's lifetime (not tied to Activity/Compose lifecycle), posts a persistent low-importance notification ("Tracking steps"). Reuses the existing `ObserveTodayStepsUseCase`/`StepsRepositoryImpl` — no new sensor code, just a longer-lived host than the Calories screen's `LaunchedEffect`.
- `AndroidManifest.xml`: add `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_HEALTH` (API 34+) permissions and a `<service android:foregroundServiceType="health">` entry.
- Start the service once `ACTIVITY_RECOGNITION` permission is granted (same trigger point as today's `CaloriesEvent.StepsCardClicked` permission check) and stop it on logout. Keep `StepsScheduler`/`StepsSyncWorker` (`app/steps/`) as-is as a restart safety net (WorkManager can re-launch the service if the OS kills it) — not redundant, no removal needed.
- `data/repository/StepsRepositoryImpl.kt`: since this file is being touched, bring it under the documented `@IoDispatcher`/`runCatchingCancellable` convention (`data/AGENTS.md` §7-8) instead of its ad-hoc `CoroutineScope(SupervisorJob() + Dispatchers.Default)` and bare `launch` calls around `preferences.saveBaseline`/`saveDailySteps`.

## Verification

- Every touched `*ViewModel.kt` (`CaloriesViewModel`) needs its `*ViewModelTest.kt` updated: new events (`FoodItemDeleteClicked`, `ManageModeToggled`, `RemoveAllFoodClicked/Confirmed/Dismissed`), the extended `toggleWaterCup` cascade logic, and error paths.
- Zero new hardcoded strings — any new UI text (manage-mode hint, remove-all confirm copy) goes in `strings.xml` (English + Arabic) first.
- Build + run: `./gradlew assembleDebug`, install and manually check: auth screens (no white strip, both light/dark), Home tip card expand/collapse, Calories (bordered food container, manage-mode toggle reveals delete buttons, remove-all confirms via `DeleteWarningAlert`, BMI card gone), Water (fill animation single-tap and jump-to-N cascade), Saved (full-card swipe-to-add), Steps (background the app, confirm count updates without reopening — check the persistent notification appears).
- `./gradlew test` for unit tests; `./gradlew lint` for the new Service/manifest entries and icon assets.
