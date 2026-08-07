# Plan: Foundational Network Layer Setup

## 1. Feature Summary
Initializing the base network layer (Retrofit, OkHttp, Interceptors) using `kotlinx.serialization` and `Hilt`. This follows the technical stack and architecture defined in `AGENTS.md`.

## 2. Files to Create
- `data/src/main/kotlin/iti/grad/nutriscan/data/remote/interceptor/NutriScanHttpException.kt`: Custom network exceptions.
- `data/src/main/kotlin/iti/grad/nutriscan/data/remote/interceptor/ErrorInterceptor.kt`: HTTP error code mapping to exceptions.
- `data/src/main/kotlin/iti/grad/nutriscan/data/remote/interceptor/AuthInterceptor.kt`: Authorization and content-type headers.
- `app/src/main/kotlin/iti/grad/nutriscan/di/NetworkModule.kt`: Hilt DI for Retrofit and OkHttp.

## 3. Files to Modify
- None.

## 4. Layer Breakdown
### Domain
- No changes in this phase. (Interceptors and DTOs belong to the Data layer).

### Data
- **Exceptions**: `NutriScanHttpException.kt`
- **Interceptors**: `ErrorInterceptor.kt`, `AuthInterceptor.kt`

### Presentation
- No UI changes.

## 5. Navigation Changes
- None.

## 6. Strings — MANDATORY (Zero Hardcoded Text)
- None required for this technical setup.

## 7. Testing Plan
- Build verification: `gradlew :app:assembleDebug` to ensure correct Hilt dependency wiring and Retrofit setup.

## 8. Edge Cases
- Handling 422 (OCR Low Confidence) specifically as requested by domain rules.
- Handling 5xx server errors via generic `ServerException`.

## 9. Definition of Done
- [ ] All 4 network files created.
- [ ] Project builds without errors.
- [ ] No hardcoded strings in code.
