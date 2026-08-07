# Onboarding Screen — Splash Implementation Plan

**Status:** ✅ Approved — Executing  
**Feature:** Splash Screen (Onboarding Entry Point)  
**Module:** `presentation` (splash feature) · `app` (theme, navigation, wiring)

---

## Goal

Implement a production-ready, fully animated Splash Screen for NutriScan AI that:
- Exactly replicates the iOS visual experience using Jetpack Compose-native APIs
- Shows NO white flash, NO black flash, NO default Android splash icon
- Feels like a single continuous experience from app cold-launch to Home
- Is fully theme-aware (Light / Dark)
- Follows Clean Architecture + MVI strictly

---

## Confirmed Decisions

| Topic | Decision |
|---|---|
| SplashScreen API | `installSplashScreen()` + `setKeepOnScreenCondition` — no fixed delays |
| Starting Window hold | ViewModel `isSplashReady: StateFlow<Boolean>`; UI holds draw until `true` |
| Logo | Single `app_name.xml` drawable; animated via Compose `ColorFilter.tint()` |
| Logo animation | White tint → `AppColors.Primary` tint; cross-fade + subtle scale via `graphicsLayer` |
| Dark theme | Full dark palette added to `AppColors`; `AppTheme` switches between light/dark |
| Status bar | `enableEdgeToEdge()` in `MainActivity`; deprecated `window.statusBarColor` removed from `AppTheme` |
| Navigation | Type-safe `SplashRoute` → `HomeRoute`; `popUpTo(SplashRoute) { inclusive = true }` |
| Home destination | `HomeScreen` placeholder wrapping existing `ThemeShowcase` |
| Blob blur | `Canvas` + `BlurMaskFilter(NORMAL)` — works on minSdk 30; no `Modifier.blur()` (API 31+) |

---

## Architecture Diagram

```
MainActivity
  └── installSplashScreen()
  │     └── setKeepOnScreenCondition { !splashVm.isSplashReady }
  └── NutriScanNavGraph (NavHost)
        ├── SplashRoute → SplashScreen
        │     ├── SplashViewModel (MVI)
        │     │     ├── SplashState
        │     │     ├── SplashEvent
        │     │     └── SplashEffect → NavigateToHome
        │     └── SplashAnimation (Canvas blobs)
        └── HomeRoute → HomeScreen (ThemeShowcase wrapper)
```

---

## Changes

### 1. Gradle — Version Catalog & Build Files

**libs.versions.toml** — Added:
- `navigationCompose = "2.8.9"`
- `splashscreen = "1.0.1"`
- Library entries for `androidx-navigation-compose` and `androidx-core-splashscreen`

**presentation/build.gradle.kts** — Added `navigation-compose`  
**app/build.gradle.kts** — Added `core-splashscreen` + `navigation-compose`

---

### 2. Resources

**themes.xml** — Added `Theme.NutriScan.Splash` extending `Theme.SplashScreen`:
- `windowSplashScreenBackground` = `@color/splash_background` (Teal1000)
- `windowSplashScreenAnimatedIcon` = transparent drawable (hides default Android icon)
- `postSplashScreenTheme` = `@style/Theme.NutriScan`

**values/colors.xml** — `splash_background = #FF13A4AB`  
**values-night/colors.xml** — `splash_background = #FF108188` (Teal1300)  
**values-night/themes.xml** — Dark variant of splash theme  
**drawable/ic_splash_transparent.xml** — 1×1 transparent vector

---

### 3. AndroidManifest.xml

`MainActivity` theme changed from `Theme.NutriScan` → `Theme.NutriScan.Splash`

---

### 4. Dark Theme Palette

**AppColors.kt** — Added `darkColors()` companion/function returning a dark-appropriate palette  
**AppTheme.kt** — Now selects light vs dark `AppColors` based on `isSystemInDarkTheme()`; deprecated `SideEffect` block removed

---

### 5. App Module — Navigation & MainActivity

**Route.kt** — `@Serializable object SplashRoute` + `@Serializable object HomeRoute`  
**NavGraph.kt** — Single `NavHost(startDestination = SplashRoute)` with both destinations  
**MainActivity.kt** — `enableEdgeToEdge()` → `installSplashScreen()` → `setKeepOnScreenCondition` → `AppTheme { NutriScanNavGraph() }`

---

### 6. Presentation Module — Splash MVI

Files under `presentation/…/onboarding/splash/`:

| File | Purpose |
|---|---|
| `SplashState.kt` | `data class SplashState(val isSplashReady: Boolean = false)` |
| `SplashEvent.kt` | `sealed interface SplashEvent { object AnimationCompleted }` |
| `SplashEffect.kt` | `sealed interface SplashEffect { object NavigateToHome }` |
| `SplashViewModel.kt` | MVI ViewModel; owns `isSplashReady StateFlow`; drives all timing |
| `SplashAnimation.kt` | Canvas blob layer — two blurred radial circles, position driven by animated `Float` |
| `SplashScreen.kt` | Full screen composable; wires VM + Animation + logo cross-fade |

#### Animation Sequence

| Time | Event |
|---|---|
| t=0 | Starting Window (teal) visible; `setKeepOnScreenCondition` holds |
| t=0 | Compose first frame ready → `isSplashReady=true` → Starting Window exits |
| t=500ms | `LaunchedEffect` fires `animPhase 0→1`; blobs + color + logo start animating |
| t=1700ms | Animation complete → `AnimationCompleted` event sent |
| t=2000ms | `NavigateToHome` effect emitted; nav pops Splash inclusive |

---

### 7. Home Placeholder

**HomeScreen.kt** under `presentation/…/home/` — wraps `ThemeShowcase`

---

## Verification

- [x] Build: `./gradlew :app:assembleDebug` clean
- [x] No white/black flash on cold launch
- [x] Starting Window teal matches Compose Splash teal — seamless
- [x] No default Android splash icon visible
- [x] Blobs animate diagonally top-right → bottom-left after 500ms
- [x] Background fades Teal → White (light) / Teal → DarkSurface (dark)
- [x] Logo cross-fades White → Primary teal
- [x] Navigates to Home; back does NOT return to Splash
- [x] Dark mode adapts automatically via theme colors
