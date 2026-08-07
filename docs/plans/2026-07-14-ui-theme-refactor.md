# UI/UX Theme Refactor
## Proposed Changes
### Directory Cleanup
* Move all contents of `app/src/main/java/` to `app/src/main/kotlin/`.
* Delete the `app/src/main/java/` directory.
### Fonts Setup
* Copy `Plus Jakarta Sans` and `Lexend Deca` `.ttf` files into `app/src/main/res/font/` formatted as `font_name_weight.ttf`.
### Theme Setup (`presentation` module)
* Delete `app/src/main/kotlin/iti/grad/nutriscan/ui/theme/`.
* [NEW] `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/theme/AppColors.kt`: Implement exact hex codes and semantic mappings.
* [NEW] `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/theme/AppTypography.kt`: Map Figma sizes to Compose TextStyles.
* [NEW] `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/theme/AppShapes.kt`: Map 12.dp, 14.dp, 24.dp, 32.dp.
* [NEW] `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/theme/AppTheme.kt`: Create CompositionLocalProviders, apply MaterialTheme fallback, and handle System UI insets.
### Main Application
* [MODIFY] `app/src/main/kotlin/iti/grad/nutriscan/MainActivity.kt`: Replace `NutriScanTheme` with `AppTheme`.
