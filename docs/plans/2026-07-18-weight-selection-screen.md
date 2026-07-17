# Plan: Weight Selection Screen — Profile Setup Page 4

## 1. Feature Summary

Implement the Weight Selection page (pager index 3) for the NutriScan profile setup flow. The screen matches the Figma design: horizontal weight card pager (59 | **60** | 61), downward pointer arrow, and a curved arc dial synced with the pager. Follows the same MVI + component patterns as `HeightSelectionPage`.

## 2. Files to Create

| File | Purpose |
|------|---------|
| `presentation/.../WeightSelectionPage.kt` | Full weight picker page composable |
| `presentation/.../WeightCard.kt` | Scaled card for selected/unselected weight values |
| `presentation/.../WeightArcRuler.kt` | Canvas curved arc dial with drag sync |

## 3. Files to Modify

| File | Change |
|------|--------|
| `ProfileSetupPagerState.kt` | Add `selectedWeightKg: Int = 60` |
| `ProfileSetupPagerEvent.kt` | Add `SelectWeight(val weightKg: Int)` |
| `ProfileSetupPagerViewModel.kt` | Handle `SelectWeight` event |
| `ProfileSetupPagerScreen.kt` | Wire page 3 to `WeightSelectionPage` |
| `ProfileSetupPagerViewModelTest.kt` | Add weight selection tests |
| `strings.xml` / `strings-ar.xml` | No new strings needed (title/subtitle already exist) |

## 4. Files to Delete

| File | Reason |
|------|--------|
| `WeightSelectionPlaceholder.kt` | Replaced by `WeightSelectionPage` |

## 5. Layer Breakdown

### Domain
No changes — weight persistence deferred until `SaveProfile` is extended.

### Presentation
- **State:** `selectedWeightKg: Int = 60`
- **Event:** `SelectWeight(val weightKg: Int)`
- **ViewModel:** `_state.update { it.copy(selectedWeightKg = event.weightKg) }`
- **UI:** Horizontal pager (30–200 kg), `WeightCard`, `WeightArcRuler`, click sound on change

## 6. Navigation Changes

None — weight remains page 3 inside `ProfileSetupPagerRoute`.

## 7. Strings

No new strings — existing keys cover title and subtitle:

| Key | English | Arabic |
|-----|---------|--------|
| `profile_setup_weight_title_prefix` | "Your current " | "وزنك " |
| `profile_setup_weight_title_highlight` | "weight" | "الحالي" |
| `profile_setup_weight_subtitle` | "Your weight helps us estimate your daily calorie needs more accurately." | "وزنك يساعدنا في تقدير احتياجاتك اليومية من السعرات الحرارية بدقة أكبر." |

## 8. Testing Plan

- Initial state default `selectedWeightKg` is 60
- `SelectWeight(75)` updates state to 75
- `SelectWeight` replaces previous value

## 9. Edge Cases

- Weight clamped to 30–200 kg range
- Pager ↔ ruler bidirectional sync without fighting during drag
- First render skips click sound

## 10. Definition of Done

- [x] Plan saved
- [ ] WeightSelectionPage replaces placeholder
- [ ] State/Event/ViewModel wired
- [ ] ViewModel tests passing
- [ ] No hardcoded strings or colors
- [ ] Reuses existing Height color tokens for cards/ruler
