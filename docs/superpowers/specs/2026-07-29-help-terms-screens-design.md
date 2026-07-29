# Help & Terms and Conditions Screens — Design

## Context

`HelpRoute` and `TermsAndConditionsRoute` currently render a generic `PlaceholderScreen`
(see `app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt` lines 332-350).
Both are reachable from `AppSettingsScreen`'s "Terms & Conditions" and "Help" rows.
This spec replaces both placeholders with real screens.

## Scope

1. Help screen: FAQ (expandable) + contact support (email) + feedback (mailto) + credits footer.
2. Terms & Conditions screen: static legal text, scrollable.
3. Enable `BuildConfig.VERSION_NAME` access in `:presentation` module for the credits footer.

Out of scope: remote/CMS-driven legal text, backend feedback storage, analytics on FAQ usage.

## Module structure

Follow existing `settings/app/{view,viewmodel,state}` convention:

```
presentation/settings/help/
  view/HelpScreen.kt
  view/components/FaqItem.kt
  view/components/HelpContactSection.kt
  viewmodel/HelpViewModel.kt
  state/HelpState.kt
  state/HelpEvent.kt
  state/HelpEffect.kt

presentation/settings/terms/
  view/TermsAndConditionsScreen.kt
```

Terms screen is static content only — no ViewModel, no state module (nothing to
mutate: no toggles, no async data, no navigation beyond back). It takes
`onNavigateBack: () -> Unit` directly, matching the pattern of other purely
navigational leaf screens in this codebase.

## Help screen

### State (`HelpState`)
```kotlin
data class HelpState(
    val faqItems: List<FaqItem> = FaqItem.default(), // static, id + expanded flag
    val expandedFaqId: Int? = null,       // only one open at a time (accordion)
    val showFeedbackDialog: Boolean = false,
    val feedbackText: String = "",
)
```

### Events
- `FaqItemClicked(id: Int)` → toggle `expandedFaqId`
- `ContactSupportClicked` → effect `OpenEmail(subject = help_contact_subject, body = "")`
- `SendFeedbackClicked` → `showFeedbackDialog = true`
- `FeedbackTextChanged(text: String)`
- `FeedbackSubmitClicked` → effect `OpenEmail(subject = help_feedback_subject, body = feedbackText)`, close dialog
- `FeedbackDismissed` → `showFeedbackDialog = false`
- `BackClicked` → effect `NavigateBack`

### Effects
- `NavigateBack`
- `OpenEmail(subject: String, body: String)` — screen builds an `ACTION_SENDTO`
  `mailto:` intent with `ahmedtayseer424@gmail.com` as recipient (displayed to the
  user only as "NutriScan Support"), subject and body pre-filled. No backend call —
  this is a device email-app handoff, matching the "Compose email intent" decision.

### FAQ content (10 items, `strings.xml`, EN + AR)

App usage:
1. How do I scan a food item? — Tap the scan icon on the Home screen, point your
   camera at the product's barcode or label, and NutriScan looks up its nutrition
   info automatically.
2. Can I scan a grocery receipt instead of one item at a time? — Yes. Use
   "Receipt Capture" to scan a whole receipt and log multiple items at once.
3. How does NutriGPT work? — NutriGPT is an in-app AI assistant you can chat with
   for nutrition questions and personalized suggestions based on your logged data.
4. How do I log calories and exercises? — Open the Calories or Exercises tab to
   record meals and workouts; your daily totals update automatically.
5. Can I track nutrition for my family? — Yes. Add family members under
   "Manage Family" and switch between profiles when logging or scanning.
6. Where can I see my past scans? — "Scan History" keeps a record of every item
   you've scanned, with the nutrition data attached.
7. What is the News tab for? — It surfaces nutrition and health articles curated
   for your profile.

Account & data:
8. How do I edit my profile? — Go to Settings → Profile Settings to update your
   name, avatar, and personal details.
9. Is my health data private? — Your data is stored securely and only used to
   power the app's features. See our Terms & Conditions for details.
10. How do I change app language or theme? — Settings → Appearance and Language
    let you switch between light/dark and English/Arabic instantly.

### Contact & feedback section
- "Contact Support" row → opens mailto to support address, empty body.
- "Send Feedback" row → opens small inline dialog (reuse `ConfirmationDialog`-style
  pattern or a simple `AlertDialog`-replacement already used in the codebase) with
  a multiline text field; "Send" builds mailto with that text as body.

### Credits footer
Centered text at the bottom of the scroll content, below contact section:
```
Made with care by the NutriScan Team
v{BuildConfig.VERSION_NAME}
```
Two `strings.xml` entries: `help_credits_team` ("Made with care by the NutriScan Team")
and `help_credits_version` ("v%1$s") formatted with `BuildConfig.VERSION_NAME`.

## Terms & Conditions screen

Static, scrollable `Column` under the existing header pattern (reuse
`AppSettingsHeader`-style header composable or a shared simple header — check
`SettingsActionRow`'s header component for reuse rather than duplicating).

Sections (each a heading string + body string in `strings.xml`, EN + AR):

1. **Acceptance of Terms** — By creating an account and using NutriScan, you
   agree to these Terms & Conditions. If you don't agree, please don't use the app.
2. **Use of the App** — NutriScan is provided for personal nutrition tracking.
   You agree to use it only for lawful purposes and to keep your account
   credentials secure.
3. **Not Medical Advice** — Nutrition information, calorie estimates, and
   NutriGPT responses are for informational purposes only and are not a
   substitute for professional medical or dietary advice. Consult a qualified
   professional for medical decisions.
4. **Scanning & Camera Data** — Photos taken for barcode/receipt scanning are
   processed to extract nutrition data and are not shared with third parties
   beyond what's required to provide this feature.
5. **AI Assistant (NutriGPT)** — Responses from NutriGPT are generated
   automatically and may occasionally be inaccurate. Use your judgment before
   acting on AI-provided suggestions.
6. **Account & Family Profiles** — You're responsible for the accuracy of
   information entered for yourself and any family profiles you manage under
   your account.
7. **Changes to These Terms** — We may update these Terms from time to time.
   Continued use of the app after changes means you accept the updated Terms.
8. **Contact** — Questions about these Terms? Reach us from the Help screen's
   Contact Support option.

## Gradle change

`presentation/build.gradle.kts`:
```kotlin
android {
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("String", "VERSION_NAME", "\"${/* from app version catalog or hardcoded sync */}\"")
    }
}
```

Version source of truth: `versionName` defined once in `gradle/libs.versions.toml`
(e.g. `appVersionName = "1.0.0"`), referenced from both `app/build.gradle.kts`
(`defaultConfig.versionName`) and `presentation/build.gradle.kts`
(`buildConfigField`), so they can never drift apart.

Support email: mailto target is `ahmedtayseer424@gmail.com`. Displayed label in
the UI (contact row text and credits) reads "NutriScan Support" / "NutriScan Team"
— never the raw address — so it reads as a real product support channel. The
`mailto:` intent's recipient is set to the real address via `Intent.putExtra` /
URI, invisible to the user until their email app opens.

## Testing

- `HelpViewModelTest`: initial state, `FaqItemClicked` toggles/collapses correctly
  (only one open at a time), `ContactSupportClicked` emits `OpenEmail` effect,
  feedback dialog open/close/text-change/submit transitions, `BackClicked` emits
  `NavigateBack`.
- Terms screen has no ViewModel — no test file needed (static content, matches
  codebase precedent of not testing stateless display-only composables).

## Strings

All FAQ questions/answers, section headings/bodies, contact labels, and credits
text go into `strings.xml` for both `values/` (English) and `values-ar/` (Arabic) —
zero hardcoded strings per project rule. Arabic translations to be written during
implementation (not drafted in this spec).
