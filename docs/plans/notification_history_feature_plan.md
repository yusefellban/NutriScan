# Notification History Feature — Implementation Plan

> **Status:** Awaiting approval  
> **Author:** AI Agent  
> **Date:** 2026-08-02  

---

## 1. Overview

Add a **Notification History** screen that locally persists every notification the app dispatches, displays them in a chronologically-ordered list (newest first), and supports single-item swipe-to-delete plus a "Clear All" action. The screen is reachable from the existing notification bell icon on the Home header.

### Entry Point Flow

```
Home Header (bell icon) → NotificationSettingsRoute (current)
```

**Proposed change:**

```
Home Header (bell icon) → NotificationHistoryRoute (NEW)
NotificationHistoryScreen top-bar → settings gear icon → NotificationSettingsRoute
```

The bell icon currently navigates to `NotificationSettingsRoute`. We will **redirect it** to the new `NotificationHistoryRoute` instead, and add a **settings gear icon** in the Notification History screen's top bar that navigates to the existing `NotificationSettingsRoute`. This keeps the notification settings accessible without adding a new entry point — users simply see their notification history first, with settings one tap away.

---

## 2. Database Migration Strategy

The project currently uses `fallbackToDestructiveMigration()` in `DatabaseModule`. The DB version will be bumped from **13 → 14**. Since destructive fallback is already in place, no explicit `Migration` object is needed — Room will wipe and recreate on upgrade. This is acceptable because:
- The notification history table is brand new (no pre-existing user data to preserve).
- All other tables already rely on the same destructive fallback strategy (see comment in [NutriScanDatabase.kt](file:///home/yousef/Desktop/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/db/NutriScanDatabase.kt#L47-L53)).

---

## 3. Notification Interception Strategy

Currently, each `*NotificationWorker` (e.g., `WaterNotificationWorker`, `StepsNotificationWorker`) independently builds and posts a notification via `NutriScanNotificationBuilder.build(...)` + `NotificationManagerCompat.notify(...)`. There are 9 workers + the `TestNotificationSenderImpl`.

**Approach:** Instead of modifying all 9+ workers individually, we will **add a single persistence helper** that wraps the notification-posting step. We'll create a `NotificationHistoryRecorder` interface in the domain layer and its implementation in the data layer, then inject it into `NutriScanNotificationBuilder` (converting it from `object` to a class) — or more practically, add a companion method / top-level extension that workers call. 

**Chosen approach (minimal diff):** Create a simple `NotificationHistoryRecorder` (injectable) in the `app` module that each worker calls **after** a notification is posted. This is a single-line addition per worker: `recorder.record(type, title, body)`. The `TestNotificationSenderImpl` will also call it.

---

## 4. Task Breakdown

### Task 1: Data Layer — Room Entity, DAO & Database Update

**Files to create:**
- `data/.../db/entity/NotificationHistoryEntity.kt` — Room entity for `notification_history` table
- `data/.../db/dao/NotificationHistoryDao.kt` — DAO with `insert`, `getAll`, `deleteById`, `deleteAll`, `getUnreadCount`

**Files to modify:**
- [NutriScanDatabase.kt](file:///home/yousef/Desktop/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/db/NutriScanDatabase.kt) — Add entity to `@Database`, bump version 13→14, add abstract DAO accessor
- [DatabaseModule.kt](file:///home/yousef/Desktop/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/di/DatabaseModule.kt) — Provide the new DAO via Hilt

**Entity schema:**

```kotlin
@Entity(tableName = "notification_history")
data class NotificationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val type: String,           // NotificationType.name
    val timestamp: Long,        // System.currentTimeMillis()
    val isRead: Boolean = false,
)
```

**DAO methods:**
- `fun getAll(): Flow<List<NotificationHistoryEntity>>` — ordered by timestamp DESC
- `suspend fun insert(entity: NotificationHistoryEntity)`
- `suspend fun deleteById(id: Long)`
- `suspend fun deleteAll()`
- `suspend fun markAsRead(id: Long)`
- `fun getUnreadCount(): Flow<Int>`

---

### Task 2: Domain Layer — Model, Repository Interface & Use Cases

**Files to create:**
- `domain/.../notification/model/NotificationHistoryItem.kt` — Domain model
- `domain/.../notification/repository/INotificationHistoryRepository.kt` — Repository interface
- `domain/.../notification/usecase/GetNotificationHistoryUseCase.kt`
- `domain/.../notification/usecase/SaveNotificationUseCase.kt`
- `domain/.../notification/usecase/DeleteNotificationUseCase.kt`
- `domain/.../notification/usecase/ClearNotificationHistoryUseCase.kt`
- `domain/.../notification/usecase/MarkNotificationReadUseCase.kt`

**Domain model:**

```kotlin
data class NotificationHistoryItem(
    val id: Long,
    val title: String,
    val body: String,
    val type: NotificationType,
    val timestamp: Long,
    val isRead: Boolean,
)
```

**Repository interface:**

```kotlin
interface INotificationHistoryRepository {
    fun getNotifications(): Flow<List<NotificationHistoryItem>>
    suspend fun saveNotification(item: NotificationHistoryItem)
    suspend fun deleteNotification(id: Long)
    suspend fun clearAllNotifications()
    suspend fun markAsRead(id: Long)
    fun getUnreadCount(): Flow<Int>
}
```

**Use cases** follow the existing pattern (single-responsibility, `@Inject constructor`, `operator fun invoke()`).

---

### Task 3: Data Layer — Repository Implementation & Notification Recording

**Files to create:**
- `data/.../repository/NotificationHistoryRepositoryImpl.kt` — Maps between entity ↔ domain model, delegates to DAO

**Files to modify:**
- [RepositoryModule.kt](file:///home/yousef/Desktop/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt) — Bind `INotificationHistoryRepository` → `NotificationHistoryRepositoryImpl`

**Notification recording (intercept):**
- Create `domain/.../notification/repository/INotificationHistoryRecorder.kt` — Single method interface: `suspend fun record(type: NotificationType, title: String, body: String)`
- Create `data/.../repository/NotificationHistoryRecorderImpl.kt` — Implementation that inserts into Room
- Bind in [RepositoryModule.kt](file:///home/yousef/Desktop/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/di/RepositoryModule.kt)
- Modify each notification worker (all 9 in `app/.../notification/worker/`) to inject `INotificationHistoryRecorder` and call `recorder.record(...)` after posting
- Modify [TestNotificationSenderImpl.kt](file:///home/yousef/Desktop/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/notification/TestNotificationSenderImpl.kt) similarly

---

### Task 4: Presentation Layer — MVI Contract, ViewModel & UI Screen

**Files to create (under `presentation/.../notification_history/`):**

```
presentation/src/main/kotlin/iti/grad/nutriscan/presentation/notification_history/
├── state/
│   ├── NotificationHistoryState.kt
│   ├── NotificationHistoryEvent.kt
│   └── NotificationHistoryEffect.kt
├── view/
│   ├── NotificationHistoryScreen.kt
│   └── components/
│       ├── NotificationHistoryItem.kt        (single item card)
│       └── NotificationHistoryEmptyState.kt  (empty state widget)
└── viewmodel/
    └── NotificationHistoryViewModel.kt
```

**MVI Contract:**

```kotlin
// State
@Immutable
data class NotificationHistoryState(
    val isLoading: Boolean = true,
    val notifications: ImmutableList<NotificationHistoryItemUi> = persistentListOf(),
    val isEmpty: Boolean = false,
)

// Presentation model
data class NotificationHistoryItemUi(
    val id: Long,
    val title: String,
    val body: String,
    val typeIcon: ImageVector,
    val relativeTime: UiText,
    val isRead: Boolean,
)

// Event
sealed interface NotificationHistoryEvent {
    data object LoadHistory : NotificationHistoryEvent
    data class DeleteNotification(val id: Long) : NotificationHistoryEvent
    data object ClearAll : NotificationHistoryEvent
    data class NotificationClicked(val id: Long) : NotificationHistoryEvent
    data object BackClicked : NotificationHistoryEvent
    data object NavigateToSettingsClicked : NotificationHistoryEvent
}

// Effect
sealed interface NotificationHistoryEffect {
    data object NavigateBack : NotificationHistoryEffect
    data object NavigateToSettings : NotificationHistoryEffect
    data class ShowUndoSnackbar(val messageResId: Int) : NotificationHistoryEffect
}
```

**ViewModel:** Standard MVI pattern matching [NotificationSettingsViewModel](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/notifications/viewmodel/NotificationSettingsViewModel.kt) — `MutableStateFlow`, `Channel<Effect>(BUFFERED)`, `onEvent()` dispatcher.

**UI Screen:**
- Uses `Scaffold` with `AppSettingsHeader` (title: "Notification History", with back button)
- Adds a settings gear `IconButton` in the header area (navigate to NotificationSettingsRoute)
- `LazyColumn` of notification items with swipe-to-dismiss (`SwipeToDismissBox`)
- "Clear All" as a `TextButton` / icon in the top bar
- Empty state widget following [ScanHistoryEmptyStateWidget](file:///home/yousef/Desktop/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan_history/view/components/ScanHistoryEmptyStateWidget.kt) pattern
- All colors from `AppTheme.colors`, typography from `AppTheme.typography`
- All strings from `stringResource(R.string.xxx)`
- Dark/Light theme support (conditional images/colors)
- RTL-aware layouts (`start`/`end` padding, not `left`/`right`)
- Snackbar with "Undo" for single-item deletion

---

### Task 5: Navigation Integration & Localization

**Files to modify:**
- [Route.kt](file:///home/yousef/Desktop/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/navigation/Route.kt) — Add `@Serializable object NotificationHistoryRoute`
- [NavGraph.kt](file:///home/yousef/Desktop/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt) — Add `composable<NotificationHistoryRoute>` destination, update `onNavigateToNotifications` to navigate to `NotificationHistoryRoute`
- [strings.xml (en)](file:///home/yousef/Desktop/NutriScan/presentation/src/main/res/values/strings.xml) — Add all notification history strings
- [strings.xml (ar)](file:///home/yousef/Desktop/NutriScan/presentation/src/main/res/values-ar/strings.xml) — Add Arabic translations

**String keys to add:**

| Key | EN | AR |
|---|---|---|
| `notification_history_title` | Notification History | سجل الإشعارات |
| `notification_history_empty_title` | No Notifications Yet | لا توجد إشعارات بعد |
| `notification_history_empty_subtitle` | Your notifications will appear here | ستظهر إشعاراتك هنا |
| `notification_history_clear_all` | Clear All | مسح الكل |
| `notification_history_deleted` | Notification deleted | تم حذف الإشعار |
| `notification_history_all_cleared` | All notifications cleared | تم مسح جميع الإشعارات |
| `notification_history_undo` | Undo | تراجع |
| `notification_history_settings` | Notification Settings | إعدادات الإشعارات |
| `notification_history_time_just_now` | Just now | الآن |
| `notification_history_time_minutes_ago` | %d min ago | منذ %d دقيقة |
| `notification_history_time_hours_ago` | %d hr ago | منذ %d ساعة |
| `notification_history_time_days_ago` | %d days ago | منذ %d أيام |
| `notification_history_confirm_clear_title` | Clear All Notifications? | مسح جميع الإشعارات؟ |
| `notification_history_confirm_clear_message` | This action cannot be undone. | لا يمكن التراجع عن هذا الإجراء. |
| `notification_history_confirm_clear_yes` | Clear | مسح |
| `notification_history_confirm_clear_no` | Cancel | إلغاء |

---

## 5. Dependency Graph

```
NotificationHistoryEntity (data/db/entity)
       ↓
NotificationHistoryDao (data/db/dao)
       ↓
NotificationHistoryRepositoryImpl (data/repository) ← maps Entity ↔ Domain model
       ↓ implements
INotificationHistoryRepository (domain/notification/repository)
       ↓ injected into
UseCases (domain/notification/usecase)
       ↓ injected into
NotificationHistoryViewModel (presentation/notification_history/viewmodel)
       ↓ drives
NotificationHistoryScreen (presentation/notification_history/view)
       ↓ registered in
NavGraph + Route (app/navigation)
```

---

## 6. Files Summary — All Changes

### New Files (18)

| Module | Path | Purpose |
|---|---|---|
| data | `db/entity/NotificationHistoryEntity.kt` | Room entity |
| data | `db/dao/NotificationHistoryDao.kt` | Room DAO |
| data | `repository/NotificationHistoryRepositoryImpl.kt` | Repo impl (entity ↔ domain) |
| data | `repository/NotificationHistoryRecorderImpl.kt` | Records notifications to Room |
| domain | `notification/model/NotificationHistoryItem.kt` | Domain model |
| domain | `notification/repository/INotificationHistoryRepository.kt` | Repo interface |
| domain | `notification/repository/INotificationHistoryRecorder.kt` | Recorder interface |
| domain | `notification/usecase/GetNotificationHistoryUseCase.kt` | Get all history |
| domain | `notification/usecase/SaveNotificationUseCase.kt` | Save a notification |
| domain | `notification/usecase/DeleteNotificationUseCase.kt` | Delete by ID |
| domain | `notification/usecase/ClearNotificationHistoryUseCase.kt` | Clear all |
| domain | `notification/usecase/MarkNotificationReadUseCase.kt` | Mark as read |
| presentation | `notification_history/state/NotificationHistoryState.kt` | MVI State |
| presentation | `notification_history/state/NotificationHistoryEvent.kt` | MVI Event |
| presentation | `notification_history/state/NotificationHistoryEffect.kt` | MVI Effect |
| presentation | `notification_history/view/NotificationHistoryScreen.kt` | Screen composable |
| presentation | `notification_history/view/components/NotificationHistoryItemCard.kt` | Item card |
| presentation | `notification_history/view/components/NotificationHistoryEmptyState.kt` | Empty state |
| presentation | `notification_history/viewmodel/NotificationHistoryViewModel.kt` | ViewModel |

### Modified Files (15)

| Module | File | Change |
|---|---|---|
| data | `NutriScanDatabase.kt` | Add entity + DAO accessor, bump version |
| app | `DatabaseModule.kt` | Provide DAO |
| app | `RepositoryModule.kt` | Bind repo + recorder |
| app | `Route.kt` | Add `NotificationHistoryRoute` |
| app | `NavGraph.kt` | Add composable destination, redirect bell icon |
| app | `worker/WaterNotificationWorker.kt` | Add recorder call |
| app | `worker/StepsNotificationWorker.kt` | Add recorder call |
| app | `worker/WorkoutNotificationWorker.kt` | Add recorder call |
| app | `worker/FoodNotificationWorker.kt` | Add recorder call |
| app | `worker/NewsNotificationWorker.kt` | Add recorder call |
| app | `worker/QuoteNotificationWorker.kt` | Add recorder call |
| app | `worker/ScanNotificationWorker.kt` | Add recorder call |
| app | `worker/StreakNotificationWorker.kt` | Add recorder call |
| app | `worker/BreakNotificationWorker.kt` | Add recorder call |
| app | `TestNotificationSenderImpl.kt` | Add recorder call |
| presentation | `res/values/strings.xml` | EN strings |
| presentation | `res/values-ar/strings.xml` | AR strings |

---

## 7. Verification Plan

1. **Build check** — `./gradlew assembleDebug` passes with zero errors after each task.
2. **UI verification** — After Task 4 + 5, navigate to Notification History from Home header bell icon. Verify:
   - Empty state renders correctly
   - Settings gear navigates to NotificationSettingsRoute
   - After triggering a test notification, the history item appears
   - Swipe-to-delete works with undo snackbar
   - Clear All shows confirmation dialog, then clears
   - Dark/Light mode renders correctly
   - Arabic RTL layout is mirrored correctly
3. **Worker verification** — Trigger a test notification. Verify the item appears in history.

---

## 8. Open Questions

None — all requirements are unambiguous based on the existing codebase patterns.
