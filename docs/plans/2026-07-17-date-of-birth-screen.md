# Date of Birth Screen — Profile Setup Page 2

Replace the current `DateOfBirthPlaceholder` with a fully functional Date of Birth selection page matching the design screenshot provided.

## Design Analysis (from screenshot)

The screen has the following sections (top → bottom):
1. **Title**: "Your **Date of birth**" — same pattern as Gender page (prefix + highlighted keyword)
2. **Subtitle**: "Your age helps us provide more accurate nutrition and health guidance."
3. **Age Display Card**: A large teal rounded card showing "**23** Years" — the age number is inside a darker teal circle badge, with "Years" as large bold white text beside it.
4. **Date Input Field**: A rounded-corner outlined text field showing `23/11/2003` with a calendar icon on the right — clicking it opens a Material3 `DatePickerDialog`.

---

## Reusable Component Extraction

> [!IMPORTANT]
> The Title + Subtitle section is duplicated across **all 4 setup pages** (Gender, DOB, Height, Weight) with the same layout, fonts, and spacing. This should be extracted into a reusable component.

### [NEW] `ProfileSetupHeader.kt`

A shared composable that renders the Title (with highlighted keyword) + Subtitle block, used by all 4 pages:

```kotlin
@Composable
fun ProfileSetupHeader(
    prefixRes: Int,
    highlightRes: Int,
    suffixRes: Int? = null,       // optional (e.g. Gender has "?", DOB has none)
    subtitleRes: Int,
    modifier: Modifier = Modifier
)
```

- Title: `LexendDeca`, Bold, 26sp, `TextPrimary` color, highlighted word in `Teal1000`
- Subtitle: `LexendDeca`, Normal, 14sp, `ProfileSetupSubtitle` color
- `textAlign = TextAlign.Center`
- 8dp spacer between title and subtitle

After extraction, all 4 pages (Gender, DOB, Height, Weight) will call `ProfileSetupHeader(...)` instead of duplicating the Title/Subtitle code.

---

## Proposed Changes

### Reusable Components

#### [NEW] [ProfileSetupHeader.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/view/components/ProfileSetupHeader.kt)

Shared Title + Subtitle composable. Receives string resource IDs for prefix, highlight, optional suffix, and subtitle. Renders them with the established style (LexendDeca, Teal1000 highlight, etc.).

---

### State / Event / ViewModel

#### [MODIFY] [ProfileSetupPagerState.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/state/ProfileSetupPagerState.kt)

Add DOB fields:
```kotlin
// Page 2: Date of Birth
val selectedDateOfBirthMillis: Long? = null,   // epoch millis from DatePicker
```

#### [MODIFY] [ProfileSetupPagerEvent.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/state/ProfileSetupPagerEvent.kt)

Add DOB event:
```kotlin
// Page 2: Date of Birth
data class SelectDateOfBirth(val dateMillis: Long) : ProfileSetupPagerEvent
```

#### [MODIFY] [ProfileSetupPagerViewModel.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/viewmodel/ProfileSetupPagerViewModel.kt)

Handle the new event:
```kotlin
is ProfileSetupPagerEvent.SelectDateOfBirth -> _state.update {
    it.copy(selectedDateOfBirthMillis = event.dateMillis)
}
```

---

### DOB Screen

#### [MODIFY] [DateOfBirthPlaceholder.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/view/components/DateOfBirthPlaceholder.kt) → Rename to `DateOfBirthPage.kt`

Rewrite as a full screen with:

1. **`ProfileSetupHeader`** — reuses the new shared component
2. **Age Display Card** — large rounded teal card (`Teal500` background) showing:
   - A circular badge (`Teal1000` background, white text) with the calculated age number
   - Bold white "Years" text
3. **Date Input Field** — an outlined rounded field showing the formatted date (`dd/MM/yyyy`):
   - Clicking either the field or the calendar icon opens a Material3 `DatePickerDialog`
   - On date selection, fires `SelectDateOfBirth(millis)` event
   - Age is derived from `selectedDateOfBirthMillis` (calculated in the composable using `java.time.LocalDate`)

---

### Refactor Existing Pages to Use `ProfileSetupHeader`

#### [MODIFY] [GenderSelectionPage.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/view/components/GenderSelectionPage.kt)

Replace the inline Title + Subtitle block (lines 67–95) with `ProfileSetupHeader(...)`.

#### [MODIFY] [HeightSelectionPlaceholder.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/view/components/HeightSelectionPlaceholder.kt)

Replace the inline Title + Subtitle block with `ProfileSetupHeader(...)`.

#### [MODIFY] [WeightSelectionPlaceholder.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/view/components/WeightSelectionPlaceholder.kt)

Replace the inline Title + Subtitle block with `ProfileSetupHeader(...)`.

---

### Strings

#### [MODIFY] [strings.xml (EN)](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/res/values/strings.xml)

Add new strings:
```xml
<string name="profile_setup_dob_years">Years</string>
<string name="profile_setup_dob_date_hint">DD/MM/YYYY</string>
```

#### [MODIFY] [strings.xml (AR)](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/res/values-ar/strings.xml)

Add Arabic translations:
```xml
<string name="profile_setup_dob_years">سنة</string>
<string name="profile_setup_dob_date_hint">يوم/شهر/سنة</string>
```

---

### Theme Colors

#### [MODIFY] [AppColors.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/theme/AppColors.kt)

Add DOB-specific color tokens:
```kotlin
// --- Date of Birth ---
val DobCardBackground: Color,     // Light: Teal500, Dark: Teal1400
val DobAgeBadgeBackground: Color, // Light: Teal1000, Dark: Teal1000
val DobAgeBadgeText: Color,       // Light: White, Dark: White
val DobCardText: Color,           // Light: White, Dark: Teal100
val DobInputBorder: Color,        // Light: Gray400, Dark: Teal1300
val DobInputText: Color,          // Light: TextPrimary, Dark: Teal100
val DobCalendarIcon: Color,       // Light: Gray700, Dark: Teal500
```

---

### Pager Screen Update

#### [MODIFY] [ProfileSetupPagerScreen.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/view/components/../../../view/ProfileSetupPagerScreen.kt)

Update page 1 to pass state and onEvent:
```diff
- 1 -> DateOfBirthPlaceholder()
+ 1 -> DateOfBirthPage(
+     selectedDateMillis = state.selectedDateOfBirthMillis,
+     onEvent = viewModel::onEvent
+ )
```

---

## Open Questions

> [!IMPORTANT]
> **Default date value**: The screenshot shows `23/11/2003`. Should the date picker start empty (no default) or with a preset date? I'll implement it as **empty by default** — the age card will show "—" until a date is selected.

> [!IMPORTANT]
> **Age card visibility**: Should the age card be visible before a date is selected, or only appear after selection? I'll implement it to **always be visible** — showing "—" for age when no date is selected, then animating to the actual age once a date is picked.

---

## Verification Plan

### Build Check
- Run `./gradlew :presentation:compileDebugKotlin` to ensure no compilation errors.

### Manual Verification
- Verify that the DOB screen matches the provided screenshot.
- Verify that selecting a date calculates the correct age.
- Verify that the `ProfileSetupHeader` renders correctly on all 4 pages (Gender, DOB, Height, Weight) — no visual regressions.
- Verify light/dark theme correctness.
- Verify Arabic string rendering.
