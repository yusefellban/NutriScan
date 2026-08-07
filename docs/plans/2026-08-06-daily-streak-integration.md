# Daily Streak API Integration

Integrate the `POST /api/v1/users/me/daily-streak` endpoint to sync the user's daily streak with the backend.

## User Review Required

Please review the proposed changes and answer the open questions below regarding the API contract, as the endpoint payload/response is not explicitly defined in the local codebase yet.

## Open Questions

> [!IMPORTANT]
> **API Contract Confirmation**
> 1. Does `POST /api/v1/users/me/daily-streak` require a request body? Or does the backend automatically increment the streak based on the auth token?
> 2. What does the API return on success? Does it return a JSON object with `currentStreak` and `longestStreak`? If so, could you provide a sample response?

## Proposed Changes

### Remote API Layer
#### [MODIFY] `UserApiService.kt`
- Add `@POST("v1/users/me/daily-streak") suspend fun updateDailyStreak(): DailyStreakDto`

#### [NEW] `DailyStreakDto.kt`
- Create DTO in `data/remote/dto/` to parse the API response (e.g. `currentStreak`, `longestStreak`).

### Data Source
#### [MODIFY] `IUserRemoteDataSource.kt`
- Add `suspend fun updateDailyStreak(): Result<DailyStreakDto>`

#### [MODIFY] `UserRemoteDataSourceImpl.kt`
- Implement `updateDailyStreak()` using `safeApiCall` (if applicable) or standard Retrofit try/catch.

### Repository Layer
#### [MODIFY] `IStreakRepository.kt` & `StreakRepositoryImpl.kt`
- Rename `recomputeStreak()` to `syncDailyStreak()`.
- The implementation will simply call `userRemoteDataSource.updateDailyStreak()` and remove all the previous local date/tracking calculation logic.

#### [MODIFY] `FoodLogRepositoryImpl.kt` & `DailyTrackingRepositoryImpl.kt`
- Remove all the old references calling `streakRepository.recomputeStreak()`.

### Domain Layer
#### [NEW] `SyncDailyStreakUseCase.kt`
- Create a new UseCase that invokes `streakRepository.syncDailyStreak()`.

### Presentation Layer
#### [MODIFY] `HomeViewModel.kt`
- Inject `SyncDailyStreakUseCase`.
- Call this UseCase in the `init {}` block (or initial event) so the API is triggered immediately when the Home screen opens.

## Verification Plan

### Manual Verification
- Test triggering a streak update (e.g., logging a meal or water) and verify that the `POST` API call is successfully executed.
- Verify the streak UI updates correctly from the local database after the API syncs the new streak count.
