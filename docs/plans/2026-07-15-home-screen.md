# Home Screen Implementation Plan

## Goal Description
Implement the "Home Screen" for NutriScan AI. The screen serves as the user landing hub, displaying a welcoming header with notification actions, a daily health tip, a scan CTA, a list of recent scan history items, and a bottom navigation bar. 

The implementation strictly adheres to Clean Architecture, MVI, and the rules defined in `AGENTS.md`.

---

## Technical Constraints & Safety Rules
- **Localization**: Absolutely no hardcoded string literals in composables. All texts must reside in `strings.xml` and `values-ar/strings.xml` to support English and Arabic seamlessly.
- **Theming**: Absolutely no hardcoded colors (e.g. `Color(0xFF...)`). Every visual element must reference the custom `AppTheme.colors` to ensure proper light and dark theme switching without runtime errors.
- **MVI Contract & Immutability**: All collections in UI states must use `kotlinx.collections.immutable` (e.g. `ImmutableList`) to ensure Jetpack Compose stability and prevent unnecessary recompositions.

---

## Proposed Changes

### Resources
#### [NEW] Vector Drawables
- `ic_home.xml` (filled home icon)
- `ic_history.xml` (history/clock icon)
- `ic_bookmark.xml` (bookmark outline icon)
- `ic_person.xml` (person outline icon)
- `ic_person_solid.xml` (person filled icon)
- `ic_scanner.xml` (center floating scanner icon)
- `ic_notification.xml` (notification bell icon)
- `ic_water_dot.xml` (water drop icon)
- `ic_verified.xml` (checkmark badge icon)
- `ic_qr_scan.xml` (barcode/QR scan card icon)

#### [MODIFY] strings.xml (English & Arabic)
- Add localized resources for greeting header, daily tip, scan card, history title, and bottom navigation labels.

### Presentation
#### [NEW] `presentation/.../home/state/VerdictType.kt`
- Enum representing scan result severity: `GREEN`, `YELLOW`, `RED`.

#### [NEW] `presentation/.../home/state/BottomNavTab.kt`
- Enum representing the 5 navigation destinations: `HOME`, `HISTORY`, `SCAN`, `SHOPPING`, `PROFILE`.

#### [NEW] `presentation/.../home/state/HomeHistoryItem.kt`
- Model representing single history list item.

#### [NEW] `presentation/.../home/state/HomeState.kt`, `HomeEvent.kt`, `HomeEffect.kt`
- MVI contract classes with `ImmutableList` list type.

#### [NEW] `presentation/.../home/viewmodel/HomeViewModel.kt`
- Hilt-enabled ViewModel that initializes with mock data matching the designs, handles user interactions via events, and emits side effects for navigation.

#### [NEW] Components
- `HomeGreetingHeader.kt` (Welcome row + notifications action)
- `DailyHealthTipCard.kt` (Water reminder tip)
- `ScanReadyCard.kt` (Barcode scan placeholder)
- `HistoryItemCard.kt` (Scan card with color-coded verdict badge)
- `HomeBottomNavBar.kt` (Custom bottom bar with floating center scan FAB)

#### [NEW] `presentation/.../home/view/HomeScreen.kt`
- Stateful MVI composable that observes state and handles side effects, delegating layout structure to stateless content composable.

### Navigation
#### [MODIFY] `app/.../navigation/NavGraph.kt`
- Wire the new `HomeScreen` into the flat NavHost. Connect navigation callbacks to route actions.

---

## Verification Plan

### Automated Tests
#### [NEW] `presentation/src/test/java/iti/grad/nutriscan/presentation/home/HomeViewModelTest.kt`
- Verify initial mock values and proper selected tab states.
- Verify side effect events are correctly emitted for `ScanCardClicked`, `ViewAllHistoryClicked`, `NotificationClicked`, and all `BottomNavTabClicked` options.

### Manual Verification
- Deploy to emulator/device.
- Toggle between light/dark modes and verify colors adjust automatically.
- Switch app locale to Arabic and verify text translates and flows correctly.
