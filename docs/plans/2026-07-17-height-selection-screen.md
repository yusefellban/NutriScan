# Height Selection Screen — Profile Setup Page 3

Implement the Height Selection page for the NutriScan profile setup flow, matching the design specifications and maintaining consistency with the established Gender and Date of Birth pages.

## Design Analysis

The screen contains (top → bottom):
1. **Title**: "How **tall** are you?" — using the shared `ProfileSetupHeader` component (with "tall" highlighted in `Teal1000`).
2. **Subtitle**: "We\'ll use your height to personalize your nutrition insights and calorie calculations."
3. **Selected Height Pointer**: A downward pointing triangle icon (`ic_selected_arrow`) centered above the selector.
4. **Horizontal Pager for Height Cards**:
   - A list of heights in centimeters (ranging from 100 cm to 250 cm).
   - Centered card is selected, showing a large size, rounded corner container in `HeightSelectedCardBackground` (Teal500), displaying the number (e.g. "183") in `HeightSelectedCardText` (dark teal) and the unit "cm" below it.
   - Neighboring cards are unselected, rendering smaller in size, in `HeightUnselectedCardBackground` with gray text.
   - Smooth animated scaling/alpha transitions as cards scroll.
5. **Horizontal Ruler Picker**:
   - Located below the height cards.
   - Ticks are drawn dynamically via `Canvas` representing each cm.
   - Major ticks (multiples of 5) are taller, with the height number drawn below them.
   - Minor ticks are shorter and subtle.
   - Synced in lockstep with the cards pager using a shared scroll state and custom gear-ratio drag input.
6. **Haptic Audio Feedback**:
   - A click sound (`click.mp3`) is played every time the selected height value changes (simulating a physical scroll wheel).

---

## Proposed Changes

### Resources

#### [NEW] [click.mp3](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/res/raw/click.mp3)
- Create `presentation/src/main/res/raw/` directory and move/copy the `click.mp3` file from the root to this location.

---

### Theme Colors

#### [MODIFY] [AppColors.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/theme/AppColors.kt)
Add color tokens for height selector and ruler:
```kotlin
    // --- Height Selection ---
    val HeightSelectedCardBackground: Color,
    val HeightSelectedCardText: Color,
    val HeightSelectedCardUnitText: Color,
    val HeightUnselectedCardBackground: Color,
    val HeightUnselectedCardText: Color,
    val HeightUnselectedCardUnitText: Color,
    val HeightRulerMajorTick: Color,
    val HeightRulerMinorTick: Color,
    val HeightRulerText: Color,
```

Configure Light Mode and Dark Mode palettes with these new tokens.

---

### State / Event / ViewModel

#### [MODIFY] [ProfileSetupPagerState.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/state/ProfileSetupPagerState.kt)
Add:
```kotlin
    // Page 3: Height
    val selectedHeightCm: Int = 170,
```

#### [MODIFY] [ProfileSetupPagerEvent.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/state/ProfileSetupPagerEvent.kt)
Add:
```kotlin
    // Page 3: Height
    data class SelectHeight(val heightCm: Int) : ProfileSetupPagerEvent
```

#### [MODIFY] [ProfileSetupPagerViewModel.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/viewmodel/ProfileSetupPagerViewModel.kt)
Handle:
```kotlin
            is ProfileSetupPagerEvent.SelectHeight -> _state.update {
                it.copy(selectedHeightCm = event.heightCm)
            }
```

---

### UI Implementation

#### [NEW] [HeightSelectionPage.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/view/components/HeightSelectionPage.kt)
Create the fully featured page component:
- Title and Subtitle rendering via `ProfileSetupHeader`.
- Center alignment triangle marker.
- `HorizontalPager` with range 100 to 250 cm.
- Micro-animations for card size, alpha, and elevation during scroll.
- Dynamically rendered `Canvas` ruler mapping pixel drag gestures directly to pager scroll.
- `MediaPlayer` integration to play `R.raw.click` as pages snap.

#### [MODIFY] [ProfileSetupPagerScreen.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/view/ProfileSetupPagerScreen.kt)
Wire page index 2 to `HeightSelectionPage`:
```kotlin
                    2 -> HeightSelectionPage(
                        selectedHeightCm = state.selectedHeightCm,
                        currentPage = state.currentPage,
                        pageCount = state.pageCount - 1,
                        onEvent = viewModel::onEvent
                    )
```

#### [DELETE] [HeightSelectionPlaceholder.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/profile_setup/view/components/HeightSelectionPlaceholder.kt)
- Remove placeholder file.

---

### Unit Tests

#### [MODIFY] [ProfileSetupPagerViewModelTest.kt](file:///d:/ITI/Projects/Graduation%20Project/NutriScan/presentation/src/test/java/iti/grad/nutriscan/presentation/profile_setup/ProfileSetupPagerViewModelTest.kt)
Add tests for height setup events:
- Default selected height is 170.
- `SelectHeight` updates the height in state.

---

## Verification Plan

### Automated Tests
- Run `./gradlew :presentation:test` to verify viewmodel test cases pass.
- Run `./gradlew :presentation:compileDebugKotlin` to verify compilation.

### Manual Verification
- Scroll height cards and ruler to verify lockstep synchronization.
- Check page indicator shows page 3 of 4.
- Confirm click audio plays when snapped to new values.
- Verify Light & Dark mode colors and Arabic localization.
