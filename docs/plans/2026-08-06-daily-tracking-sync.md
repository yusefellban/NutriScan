# Sync Daily Tracking Additional Fields

## Overview
This plan outlines the steps to include `stepsKcal`, `exerciseKcal`, and `exerciseMin` in the bidirectional synchronization logic with the backend. Currently, these fields are tracked locally but are neither sent to the remote API nor received and updated from it.

## Changes Made

### `data` module

#### `DailyTrackingDto.kt`
- Add `stepsKcal`, `exerciseKcal`, and `exerciseMin` (as `Double?`) to `DailyTrackingRequestDto`.

#### `DailyTrackingMapper.kt`
- Update `toRemoteSnapshot()` to map `stepsKcal`, `exerciseKcal`, and `exerciseMin` from the response DTO to the domain model snapshot.

#### `DailyTrackingRepositoryImpl.kt`
- In `syncPendingDay()`: Map local `caloriesBurnedSteps`, `exerciseKcal`, and `exerciseMinutes` to the `DailyTrackingRequestDto` when sending the `PATCH` request.
- In `fetchAndSeedToday()`: During the local db update, use `maxOf` for `exerciseKcal` and `exerciseMinutes` against the remote snapshot (applying the same monotonic rule used for `stepsCnt` to prevent walking backwards if local is ahead of remote).

### `domain` module

#### `DailyTrackingRemoteSnapshot.kt`
- Add `stepsKcal`, `exerciseKcal`, and `exerciseMinutes` (as `Int`) to the `DailyTrackingRemoteSnapshot` domain model.
