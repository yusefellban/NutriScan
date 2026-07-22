# Implement Health Profile Setup Screen (Figma Parity & Restructuring)

This plan outlines the full set of steps taken to move the health profile setup feature into the `auth` directory structure and update the UI styling to align 1:1 with Figma designs (both Light and Dark themes).

## What Was Executed

### 1. Packaging & Imports Reorganization

- **Package Move**: Moved `profile_setup` from `iti.grad.nutriscan.presentation.onboarding.profile_setup` to `iti.grad.nutriscan.presentation.auth.profile_setup`.
- **Files Migrated**:
  - `HealthProfileSetupState.kt`
  - `HealthProfileSetupEvent.kt`
  - `HealthProfileSetupEffect.kt`
  - `HealthProfileSetupViewModel.kt`
  - `HealthProfileSetupScreen.kt`
  - `SelectableChip.kt`
  - `OtherInputChip.kt`
- **Cleanup**: Deleted original files under the old `onboarding` directory.

### 2. Navigation & Routing Integration

- **Modified `NavGraph.kt`**:
  - Updated the import path for `HealthProfileSetupScreen` to correctly refer to the new package under the `auth` directory structure.

### 3. Detailed UI Redesign & Figma Parity

- **Background & Structure**:
  - Scaffold container color mapped strictly to `#FFFFFF` for Light theme and `#0B0C0E` for Dark theme.
  - Background Heart SVG positioned explicitly at the top-left (`left: 22dp`, `top: 21dp`, size `150dp x 228dp`).
  - Adjusted the top-level layout spacing (Spacer height set to `185.dp`) to push the "Setup Your Health Profile" header down, aligning it precisely beneath the background heart graphic per the Figma layout.

- **Typography**:
  - Implemented exact font weights, sizes, and line-heights matching the design specifications for the Title, Subtitle, and Section Titles.

- **Component Customization (`SelectableChip` & `OtherInputChip`)**:
  - Redesigned chip shapes to use a fully rounded `Capsule` style.
  - Implemented theme-aware color mappings for selected and unselected states.
  - Added a custom path-effect dashed border styling to `OtherInputChip`.

- **Action Button Integration**:
  - Replaced the local, custom-built Save button with the shared `AppButton` component from `iti.grad.nutriscan.presentation.common.components.AppButton`.
  - Leveraged `AppButton`'s built-in 3D puffed shape, loading indicators, and neon-like drop shadow / glow ellipse.

## Verification

- **Automated Verification**: Build & compile project using `.\gradlew.bat :presentation:compileDebugKotlin` and `.\gradlew.bat :app:compileDebugKotlin` (Successfully completed).
- **Manual Verification**: Visual inspection of Light and Dark theme `@Preview` functions to ensure 1:1 layout parity with Figma designs.
