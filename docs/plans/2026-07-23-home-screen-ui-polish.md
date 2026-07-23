## 1. Goal Description

The Home screen (`presentation/.../home/`) has three independent problems to fix, all
scoped to Home-screen-adjacent files only:

1. **Typography mismatch** — every text element on Home screen currently uses the wrong
   `AppTheme.typography.*` slot (or the right slot with a redundant/wrong override) relative
   to the Figma spec. Section 4 below gives the exact current-vs-required mapping for all 14
   text elements and the exact fix for each.
2. **Missing "Explore" section** — Home screen needs a new "Explore" section (title +
   two rows: "Health News", "Chat with AI") inserted between the "Ready to scan?" card and
   the "Recent History" section. Tapping "Health News" must navigate to the existing
   `NewsScreen` (currently reachable only via a stray `FloatingActionButton` on the
   `Scaffold`, which must be removed). Tapping "Chat with AI" must navigate to a new
   placeholder screen (no real AI chat feature exists yet — just stub it like the project's
   other `PlaceholderScreen`-based routes).
3. **Bottom nav bar rendering bugs**:
   - Bar height needs to increase.
   - The bar's rounded top corners / center notch / sliding tab notch currently reveal a
     flat, mismatched `Background`-colored patch instead of true content underneath, because
     the screen's `LazyColumn` is fully clipped above the bar (via `Scaffold`'s `innerPadding`)
     and can never actually extend into that region.
   - The bar's bottom edge does not reach the true bottom of the screen — there's a visible
     gap/"white divider" below it — because the bar has no handling for the system
     navigation-bar inset.

All fixes must respect the project's existing conventions: centralized `AppTheme.colors` /
`AppTheme.typography` / `AppTheme.shapes` (no hardcoded hex colors, no hardcoded `.sp`/`.dp`
text styling inline), all user-facing strings from `strings.xml` + `values-ar/strings.xml`
(no hardcoded literals in Composables), and MVI conventions already used elsewhere in the
codebase (Event → ViewModel → Effect → NavController, ViewModel never touches
NavController).

## 2. User Review Required

- The two new icons (`news.svg`, `ai.svg`, supplied by the user) must be converted to
  Android vector drawable XML **pixel-for-pixel identical** to the source SVGs (same path
  data, same viewport) and placed under `presentation/src/main/res/drawable/`. Exact XML
  given in §4.4 — do not regenerate/simplify the path data.
- No Figma-exact pixel value was given for "increase the bottom nav bar height" — §4.3
  picks `80.dp` (up from the current `70.dp`) as a reasonable, moderate increase. If a
  precise Figma value becomes available later, swap it in; otherwise this value is fine to
  ship.
- The "Chat with AI" destination is explicitly a placeholder per the user's request (no
  domain/data/AI integration work is in scope here).

## 3. Scope / Non-goals

**In scope:** files under `presentation/.../home/`, `presentation/.../common/theme/`,
`presentation/.../common/components/AppBottomNavBar.kt` +
`BottomNavCurveShape.kt` (read-only reference, not modified), `app/.../navigation/`,
`presentation/src/main/res/values*/strings.xml`, `presentation/src/main/res/drawable/`
(2 new icon files only), and `presentation/src/test/.../home/HomeViewModelTest.kt`.

**Out of scope / do not touch:** any other screen's files, `AppColors.kt`'s existing
color values for other screens (only *reuse* existing tokens — do not redefine them),
`BottomNavCurveShape.kt` geometry (unaffected by the height fix — see §4.3 rationale),
build files, DI modules, any real AI-chat backend work.

---

## 4. Proposed Changes By Module

### 4.1 Typography — full mapping table

`AppTheme.typography` (Material3 `Typography`, defined in
`presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/theme/AppTypography.kt`)
already defines these relevant slots — **do not change their values**, only pick the right
slot at each call site:

| Slot | Family | Weight | Size/Line height |
|---|---|---|---|
| `headlineLarge` | PlusJakartaSans | Bold | 28sp/36sp |
| `headlineMedium` | PlusJakartaSans | SemiBold | 24sp/28sp |
| `titleSmall` | LexendDeca | Medium | 16sp/20sp |
| `titleMedium` | LexendDeca | Medium | 20sp/24sp |
| `bodyLarge` | LexendDeca | Regular | 16sp/24sp |
| `bodyMedium` | LexendDeca | Regular | 14sp/20sp |

None of the existing slots cover: SemiBold-14, Medium-18, Light-12, or Light-10 — so a new
`HomeTypography` object (mirroring the existing `CaloriesTypography` object right below it in
the same file) must be added for those four.

**Full text-element mapping (Figma spec → fix):**

| # | Text | Figma spec | Correct style | Current (wrong) style | File |
|---|---|---|---|---|---|
| 1 | "Hello, Noureldeen" | SemiBold 24 | `headlineMedium` | ✅ already correct | `HomeGreetingHeader.kt` |
| 2 | "Your Health Comes First" | Regular 14 | `bodyMedium` | ❌ `titleSmall` | `HomeGreetingHeader.kt` |
| 3 | "Daily Health Tip" | SemiBold 14 | `HomeTypography.dailyTipTitle` (new) | ❌ `titleSmall` | `DailyHealthTipCard.kt` |
| 4 | "Stay hydrated!..." | Regular 16 | `bodyLarge` | ❌ `titleSmall` | `DailyHealthTipCard.kt` |
| 5 | "Ready to scan?" | Bold 28 | `headlineLarge` | ✅ correct, but has redundant `fontWeight = FontWeight.Bold` override — remove it | `ScanReadyCard.kt` |
| 6 | "Check nutritional facts instantly" | Regular 14 | `bodyMedium` | ❌ `titleSmall` | `ScanReadyCard.kt` |
| 7 | "Explore" | SemiBold 24 | `headlineMedium` | (new text) | `HomeScreen.kt` |
| 8 | "Health News" | Medium 16 | `titleSmall` | (new text) | `ExploreItemRow.kt` (new) |
| 9 | "Chat with AI" | Medium 16 | `titleSmall` | (new text) | `ExploreItemRow.kt` (new) |
| 10 | "Recent History" | SemiBold 24 | `headlineMedium` | ❌ `headlineLarge` + explicit `fontWeight = FontWeight.Bold` override | `HomeScreen.kt` |
| 11 | "View All" | Regular 16 | `bodyLarge` | ❌ `titleMedium` | `HomeScreen.kt` |
| 12 | "Orange Juice" (history item title) | Medium 18 | `HomeTypography.historyItemTitle` (new) | ❌ `titleMedium` | `HistoryItemCard.kt` |
| 13 | "Today, 9:24 AM" (history item date) | Light 12 | `HomeTypography.historyItemDate` (new) | ❌ `bodyMedium` | `HistoryItemCard.kt` |
| 14 | "HEALTHY" (verdict badge) | Light 10 | `HomeTypography.verdictBadge` (new) | ❌ `labelSmall` + `fontWeight=Bold` + `fontSize=11.sp` overrides | `HistoryItemCard.kt` |

#### 4.1.1 File: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/theme/AppTypography.kt`

Insert a new `HomeTypography` object immediately **before** the existing `CaloriesTypography`
object (i.e., right after the closing `)` of the `AppTypography` `Typography(...)` value, and
before the KDoc comment that currently precedes `CaloriesTypography`). Exact insertion:

```kotlin
/**
 * One-off text styles for Home screen composables that don't fit Material3's fixed
 * 15-slot [Typography] scale (which can't gain new named roles) but are reused across
 * 2+ Home screen composables, so they live here rather than being duplicated inline
 * per call site.
 */
object HomeTypography {
    /** "Daily Health Tip" card title — Plus Jakarta Sans SemiBold 14sp/20sp. */
    val dailyTipTitle = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    /** Recent History item product name — Lexend Deca Medium 18sp/22sp. */
    val historyItemTitle = TextStyle(
        fontFamily = LexendDeca,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 22.sp
    )

    /** Recent History item scan date/time — Lexend Deca Light 12sp/15sp. */
    val historyItemDate = TextStyle(
        fontFamily = LexendDeca,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp,
        lineHeight = 15.sp
    )

    /** Verdict badge pill text ("HEALTHY", "PROBIOTIC"...) — Lexend Deca Light 10sp/13sp. */
    val verdictBadge = TextStyle(
        fontFamily = LexendDeca,
        fontWeight = FontWeight.Light,
        fontSize = 10.sp,
        lineHeight = 13.sp
    )
}
```

(No new imports needed — `TextStyle`, `FontWeight`, `.sp`, `PlusJakartaSans`, `LexendDeca`
are all already imported/defined in this file.)

Do not touch the existing `AppTypography` value or `CaloriesTypography` object.

#### 4.1.2 File: `.../home/view/components/HomeGreetingHeader.kt`

```
old_str:
            Text(
                text = stringResource(R.string.home_subtitle),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.HealthSubtitleColor,
            )

new_str:
            Text(
                text = stringResource(R.string.home_subtitle),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.HealthSubtitleColor,
            )
```

(Leave the "Hello, %1$s!" `Text` — using `headlineMedium` — unchanged; it's already correct.)

#### 4.1.3 File: `.../home/view/components/DailyHealthTipCard.kt`

Add one import: `import iti.grad.nutriscan.presentation.common.theme.HomeTypography`

```
old_str:
        Column {
            Text(
                text = stringResource(R.string.home_daily_tip_title),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.Teal800,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_daily_tip_body),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.PrimaryVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

new_str:
        Column {
            Text(
                text = stringResource(R.string.home_daily_tip_title),
                style = HomeTypography.dailyTipTitle,
                color = AppTheme.colors.Teal800,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_daily_tip_body),
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colors.PrimaryVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
```

#### 4.1.4 File: `.../home/view/components/ScanReadyCard.kt`

```
old_str:
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.home_ready_to_scan),
            style = AppTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.HealthSubtitleColor,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_scan_subtitle),
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.Teal800,
        )

new_str:
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.home_ready_to_scan),
            style = AppTheme.typography.headlineLarge,
            color = AppTheme.colors.HealthSubtitleColor,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_scan_subtitle),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.Teal800,
        )
```

Since the redundant `fontWeight = FontWeight.Bold` line is removed and `FontWeight` may
become an unused import — check the rest of the file: `FontWeight` is NOT used anywhere else
in `ScanReadyCard.kt`, so also remove the import line
`import androidx.compose.ui.text.font.FontWeight`.

#### 4.1.5 File: `.../home/view/components/HistoryItemCard.kt`

Add one import: `import iti.grad.nutriscan.presentation.common.theme.HomeTypography`

```
old_str:
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.productName,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.HistoryItemTitleColor,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.scanDate,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.HistoryItemDateColor,
            )
        }

new_str:
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.productName,
                style = HomeTypography.historyItemTitle,
                color = AppTheme.colors.HistoryItemTitleColor,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.scanDate,
                style = HomeTypography.historyItemDate,
                color = AppTheme.colors.HistoryItemDateColor,
            )
        }
```

```
old_str:
            Text(
                text = stringResource(item.verdictLabelResId),
                style = AppTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (item.verdictType == VerdictType.RED) AppTheme.colors.VerdictRedText else badgeColor,
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

new_str:
            Text(
                text = stringResource(item.verdictLabelResId),
                style = HomeTypography.verdictBadge,
                color = if (item.verdictType == VerdictType.RED) AppTheme.colors.VerdictRedText else badgeColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
```

After this edit, check whether `FontWeight` and `sp` (androidx.compose.ui.unit.sp) imports
are still used elsewhere in this file — they are not (the only other usages were the ones
just removed), so also remove:
`import androidx.compose.ui.text.font.FontWeight`
`import androidx.compose.ui.unit.sp`

---

### 4.2 Explore Section + Navigation Rewiring

#### 4.2.1 New strings

**File: `presentation/src/main/res/values/strings.xml`**

Under the existing `<!-- Home Screen -->` block, insert three new strings right after
`home_scan_subtitle` and before `home_recent_history`:

```
old_str:
    <string name="home_scan_subtitle">Check nutritional facts instantly</string>
    <string name="home_recent_history">Recent History</string>

new_str:
    <string name="home_scan_subtitle">Check nutritional facts instantly</string>
    <string name="home_explore">Explore</string>
    <string name="home_health_news">Health News</string>
    <string name="home_chat_with_ai">Chat with AI</string>
    <string name="home_recent_history">Recent History</string>
```

Then remove the now-obsolete `news_fab_content_description` string (the FAB is being
deleted) from the `<!-- News -->` block:

```
old_str:
    <string name="news_empty_state">No articles found for the selected topics.</string>
    <string name="news_fab_content_description">Health News</string>
    <string name="news_card_byline">By %1$s</string>

new_str:
    <string name="news_empty_state">No articles found for the selected topics.</string>
    <string name="news_card_byline">By %1$s</string>
```

**File: `presentation/src/main/res/values-ar/strings.xml`** — mirror both edits exactly:

```
old_str:
    <string name="home_scan_subtitle">تحقق من الحقائق الغذائية فوراً</string>
    <string name="home_recent_history">السجل الأخير</string>

new_str:
    <string name="home_scan_subtitle">تحقق من الحقائق الغذائية فوراً</string>
    <string name="home_explore">استكشف</string>
    <string name="home_health_news">أخبار صحية</string>
    <string name="home_chat_with_ai">الدردشة مع الذكاء الاصطناعي</string>
    <string name="home_recent_history">السجل الأخير</string>
```

```
old_str:
    <string name="news_empty_state">لا توجد مقالات للمواضيع المحددة.</string>
    <string name="news_fab_content_description">أخبار صحية</string>
    <string name="news_card_byline">بقلم %1$s</string>

new_str:
    <string name="news_empty_state">لا توجد مقالات للمواضيع المحددة.</string>
    <string name="news_card_byline">بقلم %1$s</string>
```

> Note: `home_health_news`'s value doubles as both the row's visible label and its icon's
> `contentDescription` (see §4.2.4). `home_chat_with_ai`'s value doubles as both the row's
> label and the placeholder screen's title (see §4.2.6). This intentionally avoids
> duplicate string keys for identical text, per the project's "no hardcoded / centralize
> everything" convention.

#### 4.2.2 New icons (convert `news.svg` / `ai.svg` exactly)

**New file: `presentation/src/main/res/drawable/ic_health_news.xml`**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="18dp"
    android:height="18dp"
    android:viewportWidth="18"
    android:viewportHeight="18">
    <path
        android:pathData="M13,3H16C16.2652,3 16.5196,3.10536 16.7071,3.29289C16.8946,3.48043 17,3.73478 17,4V15C17,15.5304 16.7893,16.0391 16.4142,16.4142C16.0391,16.7893 15.5304,17 15,17M15,17C14.4696,17 13.9609,16.7893 13.5858,16.4142C13.2107,16.0391 13,15.5304 13,15V2C13,1.73478 12.8946,1.48043 12.7071,1.29289C12.5196,1.10536 12.2652,1 12,1H2C1.73478,1 1.48043,1.10536 1.29289,1.29289C1.10536,1.48043 1,1.73478 1,2V14C1,14.7956 1.31607,15.5587 1.87868,16.1213C2.44129,16.6839 3.20435,17 4,17H15ZM5,5H9M5,9H9M5,13H9"
        android:strokeColor="#13A4AB"
        android:strokeWidth="2"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:fillColor="#00000000" />
</vector>
```

**New file: `presentation/src/main/res/drawable/ic_chat_ai.xml`**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp"
    android:height="22dp"
    android:viewportWidth="20"
    android:viewportHeight="22">
    <path
        android:pathData="M7.01238,3.448C7.61038,1.698 10.0284,1.645 10.7374,3.289L10.7974,3.449L11.6044,5.809C11.7893,6.35023 12.0882,6.84551 12.4808,7.26142C12.8734,7.67734 13.3507,8.00421 13.8804,8.22L14.0974,8.301L16.4574,9.107C18.2074,9.705 18.2604,12.123 16.6174,12.832L16.4574,12.892L14.0974,13.699C13.556,13.8838 13.0605,14.1826 12.6444,14.5753C12.2283,14.9679 11.9013,15.4452 11.6854,15.975L11.6044,16.191L10.7984,18.552C10.2004,20.302 7.78238,20.355 7.07438,18.712L7.01238,18.552L6.20638,16.192C6.02156,15.6506 5.72275,15.1551 5.33012,14.739C4.93749,14.3229 4.46017,13.9959 3.93038,13.78L3.71438,13.699L1.35438,12.893C-0.396622,12.295 -0.449622,9.877 1.19438,9.169L1.35438,9.107L3.71438,8.301C4.25561,8.11606 4.75089,7.81719 5.1668,7.42457C5.58271,7.03195 5.90959,6.55469 6.12538,6.025L6.20638,5.809L7.01238,3.448ZM8.90538,4.094L8.09938,6.454C7.81777,7.2793 7.35965,8.0333 6.75691,8.6635C6.15418,9.29369 5.42132,9.78493 4.60938,10.103L4.35938,10.194L1.99938,11L4.35938,11.806C5.18468,12.0876 5.93868,12.5457 6.56887,13.1485C7.19907,13.7512 7.6903,14.4841 8.00838,15.296L8.09938,15.546L8.90538,17.906L9.71138,15.546C9.99299,14.7207 10.4511,13.9667 11.0538,13.3365C11.6566,12.7063 12.3894,12.2151 13.2014,11.897L13.4514,11.807L15.8114,11L13.4514,10.194C12.6261,9.91239 11.8721,9.45427 11.2419,8.85154C10.6117,8.2488 10.1205,7.51595 9.80238,6.704L9.71238,6.454L8.90538,4.094ZM16.9054,0C17.0925,0 17.2758,0.0524783 17.4345,0.151472C17.5933,0.250465 17.7211,0.392003 17.8034,0.56L17.8514,0.677L18.2014,1.703L19.2284,2.053C19.4159,2.1167 19.5802,2.23462 19.7006,2.39182C19.821,2.54902 19.892,2.73842 19.9047,2.93602C19.9173,3.13362 19.871,3.33053 19.7716,3.50179C19.6722,3.67304 19.5242,3.81094 19.3464,3.898L19.2284,3.946L18.2024,4.296L17.8524,5.323C17.7886,5.51043 17.6706,5.6747 17.5133,5.79499C17.356,5.91529 17.1666,5.98619 16.969,5.99872C16.7714,6.01125 16.5746,5.96484 16.4034,5.86538C16.2322,5.76591 16.0944,5.61787 16.0074,5.44L15.9594,5.323L15.6094,4.297L14.5824,3.947C14.3949,3.8833 14.2305,3.76538 14.1101,3.60819C13.9898,3.45099 13.9187,3.26158 13.9061,3.06398C13.8935,2.86638 13.9398,2.66947 14.0392,2.49821C14.1385,2.32696 14.2865,2.18906 14.4644,2.102L14.5824,2.054L15.6084,1.704L15.9584,0.677C16.0258,0.479426 16.1534,0.307909 16.3232,0.186499C16.493,0.065089 16.6966,-0.000125281 16.9054,0Z"
        android:fillColor="#13A4AB" />
</vector>
```

Both use the raw `#13A4AB` (= `AppTheme.colors.Teal1000` / `Primary`) fill/stroke color as
the SVG source specifies — this is fine because every call site below applies an explicit
`tint =` on the `Icon(painterResource(...))`, which overrides the drawable's own color, so
theme/dark-mode correctness is still centralized at the call site, matching how every other
icon in `drawable/` (e.g. `ic_notification.xml`) already works in this project.

#### 4.2.3 New component: `ExploreItemRow.kt`

**New file:
`presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/view/components/ExploreItemRow.kt`**

```kotlin
package iti.grad.nutriscan.presentation.home.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

/**
 * A single row in the Home screen's "Explore" section: a leading icon in a soft circle,
 * a label, and a trailing chevron. Reuses the same centralized row/icon/label/chevron
 * color tokens as [iti.grad.nutriscan.presentation.settings.profile.view.components.ProfileMenuRow]
 * (`ProfileMenuRowBackground` / `ProfileMenuIconBackground` / `ProfileMenuLabel` /
 * `ProfileMenuChevron`) rather than introducing new, duplicate theme colors for the same
 * light-gray-row-with-teal-icon-circle look.
 */
@Composable
fun ExploreItemRow(
    iconResId: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.Medium)
            .background(AppTheme.colors.ProfileMenuRowBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.ProfileMenuIconBackground.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                tint = AppTheme.colors.Teal1000,
                modifier = Modifier.size(18.dp),
            )
        }

        Text(
            text = label,
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.ProfileMenuLabel,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )

        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = AppTheme.colors.ProfileMenuChevron,
            modifier = Modifier.size(20.dp),
        )
    }
}
```

#### 4.2.4 `HomeEvent.kt` — rename + add event

**File:
`presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/state/HomeEvent.kt`**

```
old_str:
sealed interface HomeEvent {
    data object ScanCardClicked : HomeEvent
    data object ViewAllHistoryClicked : HomeEvent
    data object NotificationClicked : HomeEvent
    data class BottomNavTabClicked(val tab: BottomNavTab) : HomeEvent
    data class HistoryItemClicked(val itemId: String) : HomeEvent
    data object NewsFabClicked : HomeEvent
}

new_str:
sealed interface HomeEvent {
    data object ScanCardClicked : HomeEvent
    data object ViewAllHistoryClicked : HomeEvent
    data object NotificationClicked : HomeEvent
    data class BottomNavTabClicked(val tab: BottomNavTab) : HomeEvent
    data class HistoryItemClicked(val itemId: String) : HomeEvent
    data object HealthNewsClicked : HomeEvent
    data object ChatWithAiClicked : HomeEvent
}
```

#### 4.2.5 `HomeEffect.kt` — add effect

**File:
`presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/state/HomeEffect.kt`**

```
old_str:
    data class NavigateToScanResult(val scanId: String) : HomeEffect
    data object NavigateToNews : HomeEffect
}

new_str:
    data class NavigateToScanResult(val scanId: String) : HomeEffect
    data object NavigateToNews : HomeEffect
    data object NavigateToChatWithAi : HomeEffect
}
```

#### 4.2.6 `HomeViewModel.kt` — wire the renamed/new event

**File:
`presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/viewmodel/HomeViewModel.kt`**

```
old_str:
            is HomeEvent.NewsFabClicked -> emitEffect(HomeEffect.NavigateToNews)

new_str:
            is HomeEvent.HealthNewsClicked -> emitEffect(HomeEffect.NavigateToNews)
            is HomeEvent.ChatWithAiClicked -> emitEffect(HomeEffect.NavigateToChatWithAi)
```

#### 4.2.7 `HomeScreen.kt` — remove FAB, add Explore section, wire effect, insets fix

This file gets several coordinated changes. Apply them as separate edits in this order:

**(a) Remove now-unused imports** (the FAB is being deleted, so
`FloatingActionButton`/`Icon`/`Icons`/`Icons.AutoMirrored.Filled.Article` become unused —
but `Icon`/`stringResource` may still be needed elsewhere, verify before deleting each):

```
old_str:
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text

new_str:
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
```

Add one more import for the new component:
```
import iti.grad.nutriscan.presentation.home.view.components.ExploreItemRow
```
(insert alphabetically among the existing
`iti.grad.nutriscan.presentation.home.view.components.*` imports, i.e. right after the
`DailyHealthTipCard` import line and before `HistoryItemCard`).

**(b) Add the new nav callback param + wire the new effect:**

```
old_str:
    onNavigateToScanResult: (String) -> Unit = {},
    onNavigateToNews: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is HomeEffect.NavigateToScan -> onNavigateToScan()
                is HomeEffect.NavigateToHistory -> onNavigateToHistory()
                is HomeEffect.NavigateToCalories -> onNavigateToCalories()
                is HomeEffect.NavigateToSaved -> onNavigateToSaved()
                is HomeEffect.NavigateToProfile -> onNavigateToProfile()
                is HomeEffect.NavigateToNotifications -> onNavigateToNotifications()
                is HomeEffect.NavigateToScanResult -> onNavigateToScanResult(effect.scanId)
                is HomeEffect.NavigateToNews -> onNavigateToNews()
            }
        }
    }

new_str:
    onNavigateToScanResult: (String) -> Unit = {},
    onNavigateToNews: () -> Unit = {},
    onNavigateToChatWithAi: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is HomeEffect.NavigateToScan -> onNavigateToScan()
                is HomeEffect.NavigateToHistory -> onNavigateToHistory()
                is HomeEffect.NavigateToCalories -> onNavigateToCalories()
                is HomeEffect.NavigateToSaved -> onNavigateToSaved()
                is HomeEffect.NavigateToProfile -> onNavigateToProfile()
                is HomeEffect.NavigateToNotifications -> onNavigateToNotifications()
                is HomeEffect.NavigateToScanResult -> onNavigateToScanResult(effect.scanId)
                is HomeEffect.NavigateToNews -> onNavigateToNews()
                is HomeEffect.NavigateToChatWithAi -> onNavigateToChatWithAi()
            }
        }
    }
```

**(c) Remove the FAB and fix Scaffold insets:**

```
old_str:
    Scaffold(
        containerColor = AppTheme.colors.Background,
        bottomBar = {
            AppBottomNavBar(
                selectedTab = state.selectedTab,
                onTabClick = { tab -> onEvent(HomeEvent.BottomNavTabClicked(tab)) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(HomeEvent.NewsFabClicked) },
                containerColor = AppTheme.colors.ScanButtonBackground,
                contentColor = AppTheme.colors.ScanButtonIconTint,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = stringResource(R.string.news_fab_content_description),
                )
            }
        },
    ) { innerPadding ->

new_str:
    Scaffold(
        containerColor = AppTheme.colors.Background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AppBottomNavBar(
                selectedTab = state.selectedTab,
                onTabClick = { tab -> onEvent(HomeEvent.BottomNavTabClicked(tab)) },
            )
        },
    ) { innerPadding ->
```

> **Why `contentWindowInsets = WindowInsets(0)`:** this is the same pattern already used in
> `AppSettingsScreen.kt` in this codebase for screens that manage their own insets. Without
> it, `Scaffold` would add the system status/navigation-bar insets *on top of* the
> `bottomBar`'s own measured height when computing `innerPadding`, double-counting the
> navigation-bar inset that `AppBottomNavBar` now also adds itself (§4.3) and re-introducing
> the exact "content can't reach behind the bar" problem this fix is meant to solve. Setting
> it to zero here makes `innerPadding.calculateBottomPadding()` equal exactly the bar's own
> total rendered height (bar content + its own nav-bar inset padding), and the status bar
> inset is instead applied explicitly to the `LazyColumn` in step (e) below.

**(d) Insert the Explore section into `HomeFeedContent`, between the Scan card and the
Recent History header:**

```
old_str:
            // ── Scan Ready Card ──
            item {
                Spacer(modifier = Modifier.height(10.dp))
                ScanReadyCard(
                    onClick = { onEvent(HomeEvent.ScanCardClicked) },
                )
            }

            // ── Recent History Header ──

new_str:
            // ── Scan Ready Card ──
            item {
                Spacer(modifier = Modifier.height(10.dp))
                ScanReadyCard(
                    onClick = { onEvent(HomeEvent.ScanCardClicked) },
                )
            }

            // ── Explore Section ──
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.home_explore),
                    style = AppTheme.typography.headlineMedium,
                    color = AppTheme.colors.PrimaryVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ExploreItemRow(
                        iconResId = R.drawable.ic_health_news,
                        label = stringResource(R.string.home_health_news),
                        onClick = { onEvent(HomeEvent.HealthNewsClicked) },
                    )
                    ExploreItemRow(
                        iconResId = R.drawable.ic_chat_ai,
                        label = stringResource(R.string.home_chat_with_ai),
                        onClick = { onEvent(HomeEvent.ChatWithAiClicked) },
                    )
                }
            }

            // ── Recent History Header ──
```

This requires adding `import androidx.compose.foundation.layout.Column` to `HomeScreen.kt`
if not already present — check: the file currently imports `Row` and `Spacer` but not
`Column`; add it alongside them.

**(e) Fix `HomeFeedContent`'s typography (per §4.1) and make it fill full height with its
own `contentPadding` instead of an outer bottom `Modifier.padding`, so content can render
behind the bottom nav bar's transparent notch/corner cutouts:**

```
old_str:
@Composable
private fun HomeFeedContent(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    innerPadding: androidx.compose.foundation.layout.PaddingValues
) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.Background)
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {

new_str:
@Composable
private fun HomeFeedContent(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    innerPadding: androidx.compose.foundation.layout.PaddingValues
) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.Background)
                .statusBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = innerPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
```

> **Why this achieves the requested effect:** `LazyColumn` now fills the entire Scaffold
> body (no bottom `Modifier.padding` subtracting the bar's height from its layout bounds),
> so its own `AppTheme.colors.Background` paints all the way down behind
> `AppBottomNavBar`, including behind its rounded top corners and both notches — the exact
> same color as the Scaffold's own `containerColor`, so the previous mismatched "opaque
> patch" is gone and, since the two colors are identical, it reads as if you can see straight
> through. `contentPadding.bottom = innerPadding.calculateBottomPadding()` (the bar's real
> total height, from §4.2.7(c)) still reserves enough scroll space that the last "Recent
> History" item settles fully above the visible bar at rest — nothing is ever permanently
> hidden — while list items are genuinely present underneath the bar during scroll and can be
> seen through its cutouts, matching the requested behavior.

**(f) "Recent History" and "View All" typography fix (per §4.1 rows 10–11):**

```
old_str:
                    Text(
                        text = stringResource(R.string.home_recent_history),
                        style = AppTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.PrimaryVariant,
                    )
                    Text(
                        text = stringResource(R.string.home_view_all),
                        style = AppTheme.typography.titleMedium,
                        color = AppTheme.colors.Teal800,

new_str:
                    Text(
                        text = stringResource(R.string.home_recent_history),
                        style = AppTheme.typography.headlineMedium,
                        color = AppTheme.colors.PrimaryVariant,
                    )
                    Text(
                        text = stringResource(R.string.home_view_all),
                        style = AppTheme.typography.bodyLarge,
                        color = AppTheme.colors.Teal800,
```

After this edit, check whether `androidx.compose.ui.text.font.FontWeight` is still used
anywhere else in `HomeScreen.kt` — it is not (this was its only use), so remove that import
too.

---

### 4.3 Bottom Nav Bar — height + transparency/inset fix

**File:
`presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/AppBottomNavBar.kt`**

**(a) Add imports:**

```
old_str:
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

new_str:
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
```

**(b) Bump the bar height + button offset constants:**

```
old_str:
/** Height of the visible tab-bar region — matches the iOS `CustomAnimatedTabBar.barHeight`. */
private val BarHeight = 70.dp

/** How far the floating button's center sits above the bar's top edge. */
private val FabOffsetY = (-28).dp

new_str:
/** Height of the visible tab-bar region — matches the iOS `CustomAnimatedTabBar.barHeight`. */
private val BarHeight = 80.dp

/** How far the floating button's center sits above the bar's top edge. */
private val FabOffsetY = (-32).dp
```

**(c) Extend the bar's background/clip/shadow to cover the system navigation-bar inset, so
the colored bar bleeds all the way to the real screen edge with no gap beneath it:**

```
old_str:
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .onGloballyPositioned {
                    barWidthPx = it.size.width.toFloat()
                    rowLeftInRoot = it.positionInRoot().x
                }
                .customShadow(
                    shape = curveShape,
                    color = AppTheme.colors.Primary.copy(alpha = 0.5f),
                    blurRadius = 70f,
                    offsetY = -10f,
                )
                .clip(curveShape)
                .background(AppTheme.colors.BottomNavBarBackground)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {

new_str:
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    barWidthPx = it.size.width.toFloat()
                    rowLeftInRoot = it.positionInRoot().x
                }
                .customShadow(
                    shape = curveShape,
                    color = AppTheme.colors.Primary.copy(alpha = 0.5f),
                    blurRadius = 70f,
                    offsetY = -10f,
                )
                .clip(curveShape)
                .background(AppTheme.colors.BottomNavBarBackground)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(BarHeight)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
```

> **Why this order matters (do not reorder these modifiers):** `.background(...)` and
> `.clip(curveShape)` must sit *outside* (i.e., appear earlier in the chain than)
> `.windowInsetsPadding(...)`, and `.windowInsetsPadding(...)` must sit outside
> `.height(BarHeight)`. Compose modifiers wrap the ones that come after them, so with this
> ordering: the tab icons are measured/arranged inside a fixed `BarHeight`-tall box (so
> `verticalAlignment = Alignment.CenterVertically` still centers them correctly, unaffected
> by the inset); `windowInsetsPadding` then adds the system nav-bar inset as extra space
> *below* that box (increasing the total measured height); and `background`/`clip`/
> `customShadow`, being outside all of that, paint over the *enlarged* total area — so the
> bar's color now visually extends all the way down to the true bottom edge of the screen
> with no unfilled gap, while the tab icons themselves stay visually anchored near the top of
> the (now taller) bar, exactly where they were before. `BottomNavCurveShape`'s rounded-top
> geometry is unaffected — it only shapes the top edge/notches; the bottom edge is always a
> flat line down to the shape's full height, so the extra height from the inset just extends
> that flat line further down, with zero distortion to the corners/notches.

**(d) No changes needed to `FloatingScanButton` placement logic** — it stays
`Modifier.offset(y = FabOffsetY)` inside the same outer `Box(contentAlignment =
Alignment.TopCenter)`; since the enlarged bar height is added *below* the Row's original top
edge (per (c)), the floating button's position relative to the top of the bar is unchanged
other than the slightly larger `FabOffsetY` constant from (b).

**Do not modify `BottomNavCurveShape.kt`** — no change is needed there; its notch/corner
math is independent of the bar's total height (see rationale above).

---

### 4.4 New placeholder screen: "Chat with AI"

**File: `app/src/main/kotlin/iti/grad/nutriscan/navigation/Route.kt`**

Add a new route object at the end of the file:

```
old_str:
@Serializable
object NewsRoute

new_str:
@Serializable
object NewsRoute

@Serializable
object ChatWithAiRoute
```

**File: `app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt`**

**(a)** Wire the new nav callback on the existing `HomeScreen(...)` call:

```
old_str:
                onNavigateToNews = { navController.navigate(NewsRoute) },
            )
        }

        // 8. Camera Scan

new_str:
                onNavigateToNews = { navController.navigate(NewsRoute) },
                onNavigateToChatWithAi = { navController.navigate(ChatWithAiRoute) },
            )
        }

        // 8. Camera Scan
```

**(b)** Add the new placeholder route, right after the existing `NewsRoute` composable
block (item 30 in the file's numbering):

```
old_str:
        // 30. News
        composable<NewsRoute> {
            NewsScreen(
                onNavigateBack = { navController.navigateUp() },
            )
        }

new_str:
        // 30. News
        composable<NewsRoute> {
            NewsScreen(
                onNavigateBack = { navController.navigateUp() },
            )
        }

        // 31. Chat with AI (Placeholder)
        composable<ChatWithAiRoute> {
            PlaceholderScreen(
                title = stringResource(R.string.home_chat_with_ai),
                buttonText = stringResource(R.string.action_go_back),
            ) {
                navController.navigateUp()
            }
        }
```

This reuses the file's existing private `PlaceholderScreen` composable (already defined at
the bottom of `NavGraph.kt`) and the existing `R.string.action_go_back` string (already used
by several other placeholder routes in this same file) — no new strings needed beyond
`home_chat_with_ai`, already added in §4.2.1.

---

## 5. Full File Change List (summary)

**Modified:**
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/theme/AppTypography.kt`
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/view/components/HomeGreetingHeader.kt`
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/view/components/DailyHealthTipCard.kt`
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/view/components/ScanReadyCard.kt`
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/view/components/HistoryItemCard.kt`
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/view/HomeScreen.kt`
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/state/HomeEvent.kt`
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/state/HomeEffect.kt`
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/viewmodel/HomeViewModel.kt`
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/AppBottomNavBar.kt`
- `presentation/src/main/res/values/strings.xml`
- `presentation/src/main/res/values-ar/strings.xml`
- `app/src/main/kotlin/iti/grad/nutriscan/navigation/Route.kt`
- `app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt`
- `presentation/src/test/java/iti/grad/nutriscan/presentation/home/HomeViewModelTest.kt` (§6)

**New:**
- `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/view/components/ExploreItemRow.kt`
- `presentation/src/main/res/drawable/ic_health_news.xml`
- `presentation/src/main/res/drawable/ic_chat_ai.xml`
- `docs/plans/2026-07-23-home-screen-ui-polish.md` (this doc)

**Explicitly NOT modified** (verified as correct/out-of-scope, do not touch):
- `presentation/.../common/components/BottomNavCurveShape.kt`
- `presentation/.../common/components/FloatingScanButton.kt`
- `presentation/.../common/theme/AppColors.kt` (only *reusing* existing tokens, no new/edited values)
- `presentation/.../common/theme/AppShapes.kt`
- `presentation/.../settings/profile/view/components/ProfileMenuRow.kt` (referenced/reused by
  color token only, not imported directly)
- `presentation/.../news/**` (NewsScreen is reused as-is via its existing route)

---

## 6. Test Update

**File:
`presentation/src/test/java/iti/grad/nutriscan/presentation/home/HomeViewModelTest.kt`**

Rename the existing test for the renamed event, and add a new test for the new event,
following the file's existing JUnit5 + Turbine pattern exactly:

```
old_str:
    @Test
    fun `when NewsFabClicked, effect is NavigateToNews`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.NewsFabClicked)
            assertEquals(HomeEffect.NavigateToNews, awaitItem())
        }
    }
}

new_str:
    @Test
    fun `when HealthNewsClicked, effect is NavigateToNews`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.HealthNewsClicked)
            assertEquals(HomeEffect.NavigateToNews, awaitItem())
        }
    }

    @Test
    fun `when ChatWithAiClicked, effect is NavigateToChatWithAi`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.ChatWithAiClicked)
            assertEquals(HomeEffect.NavigateToChatWithAi, awaitItem())
        }
    }
}
```

No other test file references `NewsFabClicked` or the old typography — confirmed by
project-wide search prior to writing this doc.

---

## 7. Verification Plan

### Automated
- Run `./gradlew :presentation:testDebugUnitTest --tests "*HomeViewModelTest*"` — all
  existing tests plus the two updated/added ones (§6) must pass.
- Run a full `./gradlew build` (or at minimum `:app:assembleDebug` +
  `:presentation:compileDebugKotlin`) to confirm no compile errors from the import
  cleanups (`FontWeight`, `sp`, `Icons`, `FloatingActionButton` removals in §4.1/§4.2) — a
  leftover unused-but-still-referenced import is the most likely mechanical mistake here, so
  double check each file where an import was removed still compiles.
- Confirm `R.drawable.ic_health_news`, `R.drawable.ic_chat_ai`,
  `R.string.home_explore`, `R.string.home_health_news`, `R.string.home_chat_with_ai` all
  resolve (i.e., resource IDs generate correctly) and that
  `R.string.news_fab_content_description` has zero remaining references anywhere in the
  codebase after its removal.
- Lint check that `values/strings.xml` and `values-ar/strings.xml` stay in sync (same set of
  `name=` keys in both files) — this project has no automated string-parity check, so verify
  manually by diffing the two files' `<string name="...">` keys.

### Manual (in Android Studio preview / emulator)
- **Typography:** open `HomeScreen` preview (light and dark theme) and visually confirm each
  of the 14 text elements in §4.1's table now renders at the correct family/weight/size —
  particularly that "Recent History" is no longer the same large 28sp weight as "Ready to
  scan?", and that "Orange Juice" / "Today, 9:24 AM" / "HEALTHY" in the history cards are
  visibly smaller/lighter than before.
- **Explore section:** confirm it renders between the "Ready to scan?" card and "Recent
  History", with the exact two rows/icons/labels from the Figma screenshot; tap "Health
  News" → lands on the real `NewsScreen`; tap "Chat with AI" → lands on the new placeholder
  screen with a working back button.
- **FAB removed:** confirm no floating action button remains anywhere on Home screen (the
  only floating button left should be the center Scan button that's part of
  `AppBottomNavBar` itself).
- **Bottom nav bar:** scroll the Recent History list to the very bottom and confirm the last
  item ("Granola Bar") is fully visible above the bar, not clipped; visually confirm the
  bar's rounded top corners and both notches (small sliding one + big fixed Scan one) no
  longer show a flat mismatched-color patch; confirm the bar's colored background now reaches
  the true bottom edge of the screen with no visible gap/line beneath it, on both a
  gesture-nav device/emulator and a 3-button-nav device/emulator (these have different
  navigation-bar inset sizes, and the fix should hold on both — a 3-button-nav emulator is
  the more informative check since its inset is taller and any residual gap would be obvious).
- **RTL/Arabic:** switch the app language to Arabic and confirm the two new Explore rows
  and their strings render correctly mirrored, with correct Arabic text.
- **Dark mode:** confirm the Explore rows' icon/label/chevron colors still look correct in
  dark mode (they use the existing `ProfileMenuRowBackground` / `ProfileMenuIconBackground` /
  `ProfileMenuLabel` / `ProfileMenuChevron` tokens, which already have proper dark-mode
  values defined in `AppColors.kt`, so this should need no extra work — just visual
  confirmation).

---

## 8. Delivery

After all edits pass the automated checks in §7:
1. Save this document (everything from "## 1. Goal Description" onward, i.e. excluding the
   preamble note box at the very top) as
   `docs/plans/2026-07-23-home-screen-ui-polish.md`, per `SKILL.md` §1.
2. Re-zip the full modified project root as `NutriScan.zip`.
3. Re-zip the full modified `docs/` folder as `docs.zip`.
4. Deliver both zip files to the user.
