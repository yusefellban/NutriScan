# Help & Terms and Conditions Screens Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `PlaceholderScreen` placeholders behind `HelpRoute` and `TermsAndConditionsRoute` with real screens: an FAQ + contact/feedback Help screen, and a static Terms & Conditions screen.

**Architecture:** MVI screens following the existing `settings/app/{view,viewmodel,state}` module convention. Help gets a full ViewModel/State/Event/Effect stack (accordion FAQ, feedback dialog, mailto effects). Terms & Conditions is a stateless composable (no ViewModel — nothing mutates). A single `versionName` in `gradle/libs.versions.toml` feeds both `:app`'s and `:presentation`'s `BuildConfig.VERSION_NAME`.

**Tech Stack:** Jetpack Compose, Hilt (`@HiltViewModel`), Kotlin coroutines `Channel`-based effects (existing codebase pattern, see `AppSettingsViewModel`), `Intent.ACTION_SENDTO` for mailto (existing pattern, see `NewsScreen.kt` `ACTION_SEND`).

## Global Constraints

- **Commit budget: 6 commits maximum for this whole feature.** Each task below = exactly one commit; there are 5 tasks, leaving 1 commit in reserve for a fix-up if something breaks late. Do not split tasks into extra commits.
- Zero hardcoded user-facing strings — every string in `strings.xml` for both `values/` (English) and `values-ar/` (Arabic) before it's used in Kotlin/Compose.
- `AppTheme.colors` / `AppTheme.typography` / `AppTheme.shapes` only — never `Color(0xFF...)`, raw `sp`/`dp` text style, or ad-hoc corner radius.
- Strict layer boundaries: presentation → domain ← data. This feature touches presentation only.
- Every ViewModel needs a test file covering initial state, every Event → State transition, every Event → Effect, and error paths. `HelpViewModel` needs `HelpViewModelTest`. `TermsAndConditionsScreen` has no ViewModel, so no test file (matches codebase precedent for stateless display-only composables).
- No AI co-author trailer on any commit.
- Support email: mailto target is `ahmedtayseer424@gmail.com`. UI-visible label is always "NutriScan Support" / "NutriScan Team" — never the raw address in visible text.

---

### Task 1: Single-source app version + `:presentation` BuildConfig access

**Files:**
- Modify: `gradle/libs.versions.toml` (add `appVersionName = "1.0"` to `[versions]`)
- Modify: `app/build.gradle.kts:30` (`versionName = "1.0"` → `versionName = libs.versions.appVersionName.get()`)
- Modify: `presentation/build.gradle.kts` (enable `buildConfig` build feature, add `buildConfigField`)

**Interfaces:**
- Produces: `iti.grad.presentation.BuildConfig.VERSION_NAME: String`, consumed by Task 2's `HelpViewModel`.

- [ ] **Step 1: Add version catalog entry**

In `gradle/libs.versions.toml`, under `[versions]` (after the `agp = "9.2.1"` line):

```toml
appVersionName = "1.0"
```

- [ ] **Step 2: Point `:app`'s versionName at the catalog**

In `app/build.gradle.kts`, change line 30 from:

```kotlin
versionName = "1.0"
```

to:

```kotlin
versionName = libs.versions.appVersionName.get()
```

- [ ] **Step 3: Enable BuildConfig and add the field in `:presentation`**

In `presentation/build.gradle.kts`, inside the `android { ... }` block, change:

```kotlin
    buildFeatures {
        compose = true
    }
```

to:

```kotlin
    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "VERSION_NAME", "\"${libs.versions.appVersionName.get()}\"")
    }
```

Note: `presentation/build.gradle.kts` already has a `defaultConfig { minSdk = 30; testInstrumentationRunner = ... }` block (lines 17-21) — add `buildConfigField` inside that existing block rather than creating a second one.

- [ ] **Step 4: Verify it builds and the field is generated**

Run: `./gradlew :presentation:assembleDebug`
Expected: BUILD SUCCESSFUL. Generated file
`presentation/build/generated/source/buildConfig/debug/iti/grad/presentation/BuildConfig.java`
contains `public static final String VERSION_NAME = "1.0";`.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts presentation/build.gradle.kts
git commit -m "build: single-source app version, expose BuildConfig.VERSION_NAME in :presentation"
```

---

### Task 2: Help screen state, events, effects, ViewModel + strings + tests

**Files:**
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/state/HelpState.kt`
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/state/HelpEvent.kt`
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/state/HelpEffect.kt`
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/model/FaqItem.kt`
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/viewmodel/HelpViewModel.kt`
- Modify: `presentation/src/main/res/values/strings.xml` (add Help FAQ/contact/credits strings)
- Modify: `presentation/src/main/res/values-ar/strings.xml` (Arabic counterparts)
- Test: `presentation/src/test/java/iti/grad/nutriscan/presentation/settings/help/HelpViewModelTest.kt`

**Interfaces:**
- Produces: `HelpState(faqItems: List<FaqItem>, expandedFaqId: Int?, showFeedbackDialog: Boolean, feedbackText: String)`;
  `HelpEvent` sealed interface with `FaqItemClicked(id: Int)`, `ContactSupportClicked`, `SendFeedbackClicked`, `FeedbackTextChanged(text: String)`, `FeedbackSubmitClicked`, `FeedbackDismissed`, `BackClicked`;
  `HelpEffect` sealed interface with `NavigateBack`, `OpenEmail(recipient: String, subject: String, body: String)`;
  `FaqItem(id: Int, questionRes: Int, answerRes: Int)`;
  `HelpViewModel.state: StateFlow<HelpState>`, `HelpViewModel.effect: Flow<HelpEffect>`, `HelpViewModel.onEvent(event: HelpEvent)`.
- Consumes: nothing from other tasks (Task 1's `BuildConfig.VERSION_NAME` is read directly in the UI layer in Task 3, not in the ViewModel — version display is static, no need to route it through state).

- [ ] **Step 1: Add strings (English)**

In `presentation/src/main/res/values/strings.xml`, add after the existing `app_settings_help` line (currently line 212):

```xml
    <string name="help_faq_section_title">Frequently Asked Questions</string>
    <string name="help_faq_q_scan">How do I scan a food item?</string>
    <string name="help_faq_a_scan">Tap the scan icon on the Home screen, point your camera at the product\'s barcode or label, and NutriScan looks up its nutrition info automatically.</string>
    <string name="help_faq_q_receipt">Can I scan a grocery receipt instead of one item at a time?</string>
    <string name="help_faq_a_receipt">Yes. Use Receipt Capture to scan a whole receipt and log multiple items at once.</string>
    <string name="help_faq_q_nutrigpt">How does NutriGPT work?</string>
    <string name="help_faq_a_nutrigpt">NutriGPT is an in-app AI assistant you can chat with for nutrition questions and personalized suggestions based on your logged data.</string>
    <string name="help_faq_q_log">How do I log calories and exercises?</string>
    <string name="help_faq_a_log">Open the Calories or Exercises tab to record meals and workouts; your daily totals update automatically.</string>
    <string name="help_faq_q_family">Can I track nutrition for my family?</string>
    <string name="help_faq_a_family">Yes. Add family members under Manage Family and switch between profiles when logging or scanning.</string>
    <string name="help_faq_q_history">Where can I see my past scans?</string>
    <string name="help_faq_a_history">Scan History keeps a record of every item you\'ve scanned, with the nutrition data attached.</string>
    <string name="help_faq_q_news">What is the News tab for?</string>
    <string name="help_faq_a_news">It surfaces nutrition and health articles curated for your profile.</string>
    <string name="help_faq_q_profile">How do I edit my profile?</string>
    <string name="help_faq_a_profile">Go to Settings then Profile Settings to update your name, avatar, and personal details.</string>
    <string name="help_faq_q_privacy">Is my health data private?</string>
    <string name="help_faq_a_privacy">Your data is stored securely and only used to power the app\'s features. See Terms and Conditions for details.</string>
    <string name="help_faq_q_appearance">How do I change app language or theme?</string>
    <string name="help_faq_a_appearance">Settings then Appearance and Language let you switch between light/dark and English/Arabic instantly.</string>
    <string name="help_contact_section_title">Still need help?</string>
    <string name="help_contact_support">Contact Support</string>
    <string name="help_contact_support_subject">NutriScan Support Request</string>
    <string name="help_send_feedback">Send Feedback</string>
    <string name="help_feedback_dialog_title">Send Feedback</string>
    <string name="help_feedback_dialog_hint">Tell us what\'s on your mind</string>
    <string name="help_feedback_subject">NutriScan App Feedback</string>
    <string name="help_feedback_submit">Send</string>
    <string name="help_feedback_cancel">Cancel</string>
    <string name="help_credits_team">Made with care by the NutriScan Team</string>
    <string name="help_credits_version">v%1$s</string>
```

- [ ] **Step 2: Add strings (Arabic)**

In `presentation/src/main/res/values-ar/strings.xml`, add the matching Arabic block (same string names, translated values):

```xml
    <string name="help_faq_section_title">الأسئلة الشائعة</string>
    <string name="help_faq_q_scan">كيف أقوم بمسح عنصر غذائي؟</string>
    <string name="help_faq_a_scan">اضغط على أيقونة المسح في الشاشة الرئيسية، ووجّه الكاميرا نحو الباركود أو ملصق المنتج، وسيقوم NutriScan بجلب معلومات القيمة الغذائية تلقائيًا.</string>
    <string name="help_faq_q_receipt">هل يمكنني مسح إيصال بدلاً من عنصر واحد في كل مرة؟</string>
    <string name="help_faq_a_receipt">نعم. استخدم مسح الإيصال لمسح إيصال كامل وتسجيل عدة عناصر دفعة واحدة.</string>
    <string name="help_faq_q_nutrigpt">كيف يعمل NutriGPT؟</string>
    <string name="help_faq_a_nutrigpt">NutriGPT مساعد ذكاء اصطناعي داخل التطبيق يمكنك محادثته لأسئلة التغذية واقتراحات مخصصة بناءً على بياناتك المسجلة.</string>
    <string name="help_faq_q_log">كيف أسجل السعرات الحرارية والتمارين؟</string>
    <string name="help_faq_a_log">افتح تبويب السعرات الحرارية أو التمارين لتسجيل الوجبات والتمارين؛ يتم تحديث إجمالياتك اليومية تلقائيًا.</string>
    <string name="help_faq_q_family">هل يمكنني تتبع تغذية عائلتي؟</string>
    <string name="help_faq_a_family">نعم. أضف أفراد العائلة من إدارة العائلة وبدّل بين الملفات الشخصية عند التسجيل أو المسح.</string>
    <string name="help_faq_q_history">أين يمكنني رؤية عمليات المسح السابقة؟</string>
    <string name="help_faq_a_history">يحتفظ سجل المسح بسجل لكل عنصر قمت بمسحه، مع بيانات القيمة الغذائية المرفقة.</string>
    <string name="help_faq_q_news">ما الغرض من تبويب الأخبار؟</string>
    <string name="help_faq_a_news">يعرض مقالات عن التغذية والصحة مختارة خصيصًا لملفك الشخصي.</string>
    <string name="help_faq_q_profile">كيف أعدّل ملفي الشخصي؟</string>
    <string name="help_faq_a_profile">اذهب إلى الإعدادات ثم إعدادات الملف الشخصي لتحديث اسمك وصورتك وبياناتك الشخصية.</string>
    <string name="help_faq_q_privacy">هل بياناتي الصحية خاصة؟</string>
    <string name="help_faq_a_privacy">يتم تخزين بياناتك بأمان وتُستخدم فقط لتشغيل ميزات التطبيق. راجع الشروط والأحكام لمزيد من التفاصيل.</string>
    <string name="help_faq_q_appearance">كيف أغيّر لغة التطبيق أو المظهر؟</string>
    <string name="help_faq_a_appearance">الإعدادات ثم المظهر واللغة تتيح لك التبديل بين الوضع الفاتح/الداكن والعربية/الإنجليزية فورًا.</string>
    <string name="help_contact_section_title">ما زلت بحاجة إلى مساعدة؟</string>
    <string name="help_contact_support">تواصل مع الدعم</string>
    <string name="help_contact_support_subject">طلب دعم NutriScan</string>
    <string name="help_send_feedback">إرسال ملاحظات</string>
    <string name="help_feedback_dialog_title">إرسال ملاحظات</string>
    <string name="help_feedback_dialog_hint">أخبرنا بما يدور في ذهنك</string>
    <string name="help_feedback_subject">ملاحظات حول تطبيق NutriScan</string>
    <string name="help_feedback_submit">إرسال</string>
    <string name="help_feedback_cancel">إلغاء</string>
    <string name="help_credits_team">صُنع بعناية بواسطة فريق NutriScan</string>
    <string name="help_credits_version">الإصدار %1$s</string>
```

- [ ] **Step 3: Create `FaqItem` model**

Create `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/model/FaqItem.kt`:

```kotlin
package iti.grad.nutriscan.presentation.settings.help.model

import androidx.annotation.StringRes
import iti.grad.presentation.R

data class FaqItem(
    val id: Int,
    @StringRes val questionRes: Int,
    @StringRes val answerRes: Int,
) {
    companion object {
        fun default(): List<FaqItem> = listOf(
            FaqItem(1, R.string.help_faq_q_scan, R.string.help_faq_a_scan),
            FaqItem(2, R.string.help_faq_q_receipt, R.string.help_faq_a_receipt),
            FaqItem(3, R.string.help_faq_q_nutrigpt, R.string.help_faq_a_nutrigpt),
            FaqItem(4, R.string.help_faq_q_log, R.string.help_faq_a_log),
            FaqItem(5, R.string.help_faq_q_family, R.string.help_faq_a_family),
            FaqItem(6, R.string.help_faq_q_history, R.string.help_faq_a_history),
            FaqItem(7, R.string.help_faq_q_news, R.string.help_faq_a_news),
            FaqItem(8, R.string.help_faq_q_profile, R.string.help_faq_a_profile),
            FaqItem(9, R.string.help_faq_q_privacy, R.string.help_faq_a_privacy),
            FaqItem(10, R.string.help_faq_q_appearance, R.string.help_faq_a_appearance),
        )
    }
}
```

- [ ] **Step 4: Create `HelpState`**

Create `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/state/HelpState.kt`:

```kotlin
package iti.grad.nutriscan.presentation.settings.help.state

import iti.grad.nutriscan.presentation.settings.help.model.FaqItem

data class HelpState(
    val faqItems: List<FaqItem> = FaqItem.default(),
    val expandedFaqId: Int? = null,
    val showFeedbackDialog: Boolean = false,
    val feedbackText: String = "",
)
```

- [ ] **Step 5: Create `HelpEvent`**

Create `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/state/HelpEvent.kt`:

```kotlin
package iti.grad.nutriscan.presentation.settings.help.state

sealed interface HelpEvent {
    data object BackClicked : HelpEvent
    data class FaqItemClicked(val id: Int) : HelpEvent
    data object ContactSupportClicked : HelpEvent
    data object SendFeedbackClicked : HelpEvent
    data class FeedbackTextChanged(val text: String) : HelpEvent
    data object FeedbackSubmitClicked : HelpEvent
    data object FeedbackDismissed : HelpEvent
}
```

- [ ] **Step 6: Create `HelpEffect`**

Create `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/state/HelpEffect.kt`:

```kotlin
package iti.grad.nutriscan.presentation.settings.help.state

sealed interface HelpEffect {
    data object NavigateBack : HelpEffect
    data class OpenEmail(val recipient: String, val subject: String, val body: String) : HelpEffect
}
```

- [ ] **Step 7: Create `HelpViewModel`**

Create `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/viewmodel/HelpViewModel.kt`:

```kotlin
package iti.grad.nutriscan.presentation.settings.help.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.presentation.settings.help.state.HelpEffect
import iti.grad.nutriscan.presentation.settings.help.state.HelpEvent
import iti.grad.nutriscan.presentation.settings.help.state.HelpState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SUPPORT_EMAIL = "ahmedtayseer424@gmail.com"

@HiltViewModel
class HelpViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(HelpState())
    val state: StateFlow<HelpState> = _state.asStateFlow()

    private val _effect = Channel<HelpEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: HelpEvent) {
        when (event) {
            HelpEvent.BackClicked -> navigate(HelpEffect.NavigateBack)
            is HelpEvent.FaqItemClicked -> toggleFaq(event.id)
            HelpEvent.ContactSupportClicked -> navigate(
                HelpEffect.OpenEmail(recipient = SUPPORT_EMAIL, subject = "", body = "")
            )
            HelpEvent.SendFeedbackClicked -> _state.update { it.copy(showFeedbackDialog = true) }
            is HelpEvent.FeedbackTextChanged -> _state.update { it.copy(feedbackText = event.text) }
            HelpEvent.FeedbackSubmitClicked -> submitFeedback()
            HelpEvent.FeedbackDismissed -> _state.update {
                it.copy(showFeedbackDialog = false, feedbackText = "")
            }
        }
    }

    private fun toggleFaq(id: Int) {
        _state.update {
            it.copy(expandedFaqId = if (it.expandedFaqId == id) null else id)
        }
    }

    private fun submitFeedback() {
        val body = _state.value.feedbackText
        _state.update { it.copy(showFeedbackDialog = false, feedbackText = "") }
        navigate(HelpEffect.OpenEmail(recipient = SUPPORT_EMAIL, subject = "", body = body))
    }

    private fun navigate(effect: HelpEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
```

Note: `subject` is intentionally left blank here and filled in by the UI layer from
`R.string.help_contact_support_subject` / `R.string.help_feedback_subject` in Task 3 —
the ViewModel has no `Context`/`stringResource` access, matching how `AppSettingsViewModel`
keeps all string resolution in the Composable layer.

- [ ] **Step 8: Write `HelpViewModelTest`**

Create `presentation/src/test/java/iti/grad/nutriscan/presentation/settings/help/HelpViewModelTest.kt`.
This mirrors the `Dispatchers.setMain`/`StandardTestDispatcher` scaffolding used in
`AppSettingsViewModelTest` (`presentation/src/test/java/iti/grad/nutriscan/presentation/settings/app/AppSettingsViewModelTest.kt:32-50`).
`HelpViewModel` has no constructor dependencies, so no mockk setup is needed.

```kotlin
package iti.grad.nutriscan.presentation.settings.help

import app.cash.turbine.test
import iti.grad.nutriscan.presentation.settings.help.state.HelpEffect
import iti.grad.nutriscan.presentation.settings.help.state.HelpEvent
import iti.grad.nutriscan.presentation.settings.help.viewmodel.HelpViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HelpViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HelpViewModel()

    @Test
    fun `initial state has ten faq items and nothing expanded`() {
        val viewModel = createViewModel()
        Assertions.assertEquals(10, viewModel.state.value.faqItems.size)
        Assertions.assertNull(viewModel.state.value.expandedFaqId)
    }

    @Test
    fun `FaqItemClicked expands the clicked item`() {
        val viewModel = createViewModel()
        viewModel.onEvent(HelpEvent.FaqItemClicked(3))
        Assertions.assertEquals(3, viewModel.state.value.expandedFaqId)
    }

    @Test
    fun `FaqItemClicked on already-expanded item collapses it`() {
        val viewModel = createViewModel()
        viewModel.onEvent(HelpEvent.FaqItemClicked(3))
        viewModel.onEvent(HelpEvent.FaqItemClicked(3))
        Assertions.assertNull(viewModel.state.value.expandedFaqId)
    }

    @Test
    fun `FaqItemClicked on a different item switches which one is expanded`() {
        val viewModel = createViewModel()
        viewModel.onEvent(HelpEvent.FaqItemClicked(3))
        viewModel.onEvent(HelpEvent.FaqItemClicked(5))
        Assertions.assertEquals(5, viewModel.state.value.expandedFaqId)
    }

    @Test
    fun `ContactSupportClicked emits OpenEmail with support address and empty body`() = runTest {
        val viewModel = createViewModel()
        viewModel.effect.test {
            viewModel.onEvent(HelpEvent.ContactSupportClicked)
            val effect = awaitItem()
            Assertions.assertTrue(effect is HelpEffect.OpenEmail)
            effect as HelpEffect.OpenEmail
            Assertions.assertEquals("ahmedtayseer424@gmail.com", effect.recipient)
            Assertions.assertEquals("", effect.body)
        }
    }

    @Test
    fun `SendFeedbackClicked shows feedback dialog`() {
        val viewModel = createViewModel()
        viewModel.onEvent(HelpEvent.SendFeedbackClicked)
        Assertions.assertTrue(viewModel.state.value.showFeedbackDialog)
    }

    @Test
    fun `FeedbackTextChanged updates feedback text`() {
        val viewModel = createViewModel()
        viewModel.onEvent(HelpEvent.FeedbackTextChanged("great app"))
        Assertions.assertEquals("great app", viewModel.state.value.feedbackText)
    }

    @Test
    fun `FeedbackSubmitClicked emits OpenEmail with feedback text as body and closes dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(HelpEvent.SendFeedbackClicked)
        viewModel.onEvent(HelpEvent.FeedbackTextChanged("great app"))
        viewModel.effect.test {
            viewModel.onEvent(HelpEvent.FeedbackSubmitClicked)
            val effect = awaitItem()
            Assertions.assertTrue(effect is HelpEffect.OpenEmail)
            effect as HelpEffect.OpenEmail
            Assertions.assertEquals("great app", effect.body)
        }
        Assertions.assertFalse(viewModel.state.value.showFeedbackDialog)
        Assertions.assertEquals("", viewModel.state.value.feedbackText)
    }

    @Test
    fun `FeedbackDismissed hides dialog and clears text`() {
        val viewModel = createViewModel()
        viewModel.onEvent(HelpEvent.SendFeedbackClicked)
        viewModel.onEvent(HelpEvent.FeedbackTextChanged("draft"))
        viewModel.onEvent(HelpEvent.FeedbackDismissed)
        Assertions.assertFalse(viewModel.state.value.showFeedbackDialog)
        Assertions.assertEquals("", viewModel.state.value.feedbackText)
    }

    @Test
    fun `BackClicked emits NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.effect.test {
            viewModel.onEvent(HelpEvent.BackClicked)
            Assertions.assertTrue(awaitItem() is HelpEffect.NavigateBack)
        }
    }
}
```

- [ ] **Step 9: Run the new tests**

Run: `./gradlew :presentation:test --tests "*.HelpViewModelTest"`
Expected: all tests PASS (after copying the real dispatcher-rule boilerplate from
`AppSettingsViewModelTest` in Step 8 — this plan's snippet omits it because the exact
rule class name must be copied verbatim from that file, not guessed).

- [ ] **Step 10: Commit**

```bash
git add presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help presentation/src/main/res/values/strings.xml presentation/src/main/res/values-ar/strings.xml presentation/src/test/java/iti/grad/nutriscan/presentation/settings/help
git commit -m "feat(presentation): add Help screen state, viewmodel and FAQ content"
```

---

### Task 3: Help screen UI (FAQ accordion, contact/feedback, credits) + NavGraph wiring

**Files:**
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/view/components/FaqAccordionItem.kt`
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/view/components/HelpContactSection.kt`
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/view/components/FeedbackDialog.kt`
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/view/HelpScreen.kt`
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt:342-350` (replace `PlaceholderScreen` with `HelpScreen`)

**Interfaces:**
- Consumes: `HelpState`, `HelpEvent`, `HelpEffect`, `FaqItem` from Task 2; `AppTopHeader` from `presentation/common/components/AppTopHeader.kt`; `SettingsActionRow` from `presentation/settings/app/view/components/SettingsActionRow.kt` (reused as-is, package stays under `settings.app.view.components` — import it, don't move it); `iti.grad.presentation.BuildConfig.VERSION_NAME` from Task 1.
- Produces: `HelpScreen(viewModel: HelpViewModel = hiltViewModel(), onNavigateBack: () -> Unit = {})` composable consumed by `NavGraph.kt`.

- [ ] **Step 1: Create `FaqAccordionItem`**

Create `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/view/components/FaqAccordionItem.kt`:

```kotlin
package iti.grad.nutriscan.presentation.settings.help.view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.help.model.FaqItem

@Composable
fun FaqAccordionItem(
    item: FaqItem,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.AppSettingsCardBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(item.questionRes),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.AppSettingsRowLabel,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = AppTheme.colors.Gray500,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Text(
                text = stringResource(item.answerRes),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.AuthDialogSubtitle,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
```

- [ ] **Step 2: Create `HelpContactSection`**

Create `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/view/components/HelpContactSection.kt`:

```kotlin
package iti.grad.nutriscan.presentation.settings.help.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.app.view.components.SettingsActionRow
import iti.grad.presentation.R

@Composable
fun HelpContactSection(
    onContactSupportClick: () -> Unit,
    onSendFeedbackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.help_contact_section_title),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.AppSettingsRowLabel,
        )
        SettingsActionRow(
            icon = rememberVectorPainter(Icons.Filled.Email),
            label = stringResource(R.string.help_contact_support),
            onClick = onContactSupportClick,
        )
        SettingsActionRow(
            icon = rememberVectorPainter(Icons.Filled.Feedback),
            label = stringResource(R.string.help_send_feedback),
            onClick = onSendFeedbackClick,
        )
    }
}
```

- [ ] **Step 3: Create `FeedbackDialog`**

Create `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/view/components/FeedbackDialog.kt`:

```kotlin
package iti.grad.nutriscan.presentation.settings.help.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans
import iti.grad.presentation.R

@Composable
fun FeedbackDialog(
    feedbackText: String,
    onTextChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppTheme.colors.AuthDialogBackground)
                    .padding(24.dp),
            ) {
                Text(
                    text = stringResource(R.string.help_feedback_dialog_title),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    style = AppTheme.typography.headlineSmall,
                    color = AppTheme.colors.Teal1000,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = onTextChanged,
                    placeholder = { Text(stringResource(R.string.help_feedback_dialog_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.help_feedback_cancel),
                            color = AppTheme.colors.TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onSubmit) {
                        Text(
                            text = stringResource(R.string.help_feedback_submit),
                            color = AppTheme.colors.Teal1000,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Create `HelpScreen`**

Create `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/view/HelpScreen.kt`:

```kotlin
package iti.grad.nutriscan.presentation.settings.help.view

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.common.components.AppTopHeader
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.help.state.HelpEffect
import iti.grad.nutriscan.presentation.settings.help.state.HelpEvent
import iti.grad.nutriscan.presentation.settings.help.state.HelpState
import iti.grad.nutriscan.presentation.settings.help.view.components.FaqAccordionItem
import iti.grad.nutriscan.presentation.settings.help.view.components.FeedbackDialog
import iti.grad.nutriscan.presentation.settings.help.view.components.HelpContactSection
import iti.grad.nutriscan.presentation.settings.help.viewmodel.HelpViewModel
import iti.grad.presentation.BuildConfig
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HelpScreen(
    viewModel: HelpViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val contactSubject = stringResource(R.string.help_contact_support_subject)
    val feedbackSubject = stringResource(R.string.help_feedback_subject)

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is HelpEffect.NavigateBack -> onNavigateBack()
                is HelpEffect.OpenEmail -> {
                    val subject = if (effect.body.isEmpty()) contactSubject else feedbackSubject
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(effect.recipient))
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, effect.body)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }
            }
        }
    }

    HelpContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
private fun HelpContent(
    state: HelpState,
    onEvent: (HelpEvent) -> Unit,
) {
    Scaffold(containerColor = AppTheme.colors.Background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AppTheme.colors.Background)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            AppTopHeader(
                title = stringResource(R.string.app_settings_help),
                onBackClick = { onEvent(HelpEvent.BackClicked) },
            )

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.help_faq_section_title),
                        style = AppTheme.typography.titleMedium,
                        color = AppTheme.colors.AppSettingsRowLabel,
                    )
                    state.faqItems.forEach { item ->
                        FaqAccordionItem(
                            item = item,
                            expanded = state.expandedFaqId == item.id,
                            onClick = { onEvent(HelpEvent.FaqItemClicked(item.id)) },
                        )
                    }
                }

                HelpContactSection(
                    onContactSupportClick = { onEvent(HelpEvent.ContactSupportClicked) },
                    onSendFeedbackClick = { onEvent(HelpEvent.SendFeedbackClicked) },
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.help_credits_team),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.Gray500,
                    )
                    Text(
                        text = stringResource(R.string.help_credits_version, BuildConfig.VERSION_NAME),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.Gray500,
                    )
                }
            }
        }
    }

    if (state.showFeedbackDialog) {
        FeedbackDialog(
            feedbackText = state.feedbackText,
            onTextChanged = { onEvent(HelpEvent.FeedbackTextChanged(it)) },
            onSubmit = { onEvent(HelpEvent.FeedbackSubmitClicked) },
            onDismiss = { onEvent(HelpEvent.FeedbackDismissed) },
        )
    }
}
```

Note: `AppTopHeader` (Task 3 dependency) renders on a colored/dark background per its
own styling (`Color.White` text) — check it renders correctly against
`AppTheme.colors.Background` at the top of this scroll container when you build; if
contrast looks wrong, wrap just the header call in a `Box` with
`AppTheme.colors.Teal1000` background matching `AppSettingsHeader`'s pattern instead
of changing `AppTopHeader` itself (it's shared by other screens).

- [ ] **Step 5: Wire `HelpScreen` into NavGraph**

In `app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt`, replace lines 342-350:

```kotlin
        // 26. Help (Placeholder)
        composable<HelpRoute> {
            PlaceholderScreen(
                title = stringResource(R.string.app_settings_help),
                buttonText = stringResource(R.string.action_go_back),
            ) {
                navController.navigateUp()
            }
        }
```

with:

```kotlin
        // 26. Help
        composable<HelpRoute> {
            iti.grad.nutriscan.presentation.settings.help.view.HelpScreen(
                onNavigateBack = { navController.navigateUp() },
            )
        }
```

(Add a proper top-of-file `import iti.grad.nutriscan.presentation.settings.help.view.HelpScreen`
instead of the fully-qualified inline reference above, matching how `AppSettingsScreen`
is imported elsewhere in this file — the inline form here is just to pin the exact
insertion point unambiguously.)

- [ ] **Step 6: Build and manually verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Launch the app, navigate Settings → Help, confirm FAQ
items expand/collapse one at a time, Contact Support opens an email chooser with
"NutriScan Support" context, Send Feedback opens the dialog and submitting it opens
an email chooser with the typed text as body, and the credits footer shows the
version from Task 1.

- [ ] **Step 7: Commit**

```bash
git add presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/help/view app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt
git commit -m "feat(presentation): build Help screen UI and wire it into navigation"
```

---

### Task 4: Terms & Conditions screen + strings

**Files:**
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/terms/view/TermsAndConditionsScreen.kt`
- Create: `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/terms/view/components/TermsSection.kt`
- Modify: `presentation/src/main/res/values/strings.xml` (add Terms section strings)
- Modify: `presentation/src/main/res/values-ar/strings.xml` (Arabic counterparts)
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt:332-340` (replace `PlaceholderScreen` with `TermsAndConditionsScreen`)

**Interfaces:**
- Consumes: `AppTopHeader` (same as Task 3).
- Produces: `TermsAndConditionsScreen(onNavigateBack: () -> Unit = {})` consumed by `NavGraph.kt`.

- [ ] **Step 1: Add strings (English)**

In `presentation/src/main/res/values/strings.xml`, add after the Help strings added in Task 2:

```xml
    <string name="terms_section_1_title">Acceptance of Terms</string>
    <string name="terms_section_1_body">By creating an account and using NutriScan, you agree to these Terms and Conditions. If you don\'t agree, please don\'t use the app.</string>
    <string name="terms_section_2_title">Use of the App</string>
    <string name="terms_section_2_body">NutriScan is provided for personal nutrition tracking. You agree to use it only for lawful purposes and to keep your account credentials secure.</string>
    <string name="terms_section_3_title">Not Medical Advice</string>
    <string name="terms_section_3_body">Nutrition information, calorie estimates, and NutriGPT responses are for informational purposes only and are not a substitute for professional medical or dietary advice. Consult a qualified professional for medical decisions.</string>
    <string name="terms_section_4_title">Scanning and Camera Data</string>
    <string name="terms_section_4_body">Photos taken for barcode or receipt scanning are processed to extract nutrition data and are not shared with third parties beyond what\'s required to provide this feature.</string>
    <string name="terms_section_5_title">AI Assistant (NutriGPT)</string>
    <string name="terms_section_5_body">Responses from NutriGPT are generated automatically and may occasionally be inaccurate. Use your judgment before acting on AI-provided suggestions.</string>
    <string name="terms_section_6_title">Account and Family Profiles</string>
    <string name="terms_section_6_body">You\'re responsible for the accuracy of information entered for yourself and any family profiles you manage under your account.</string>
    <string name="terms_section_7_title">Changes to These Terms</string>
    <string name="terms_section_7_body">We may update these Terms from time to time. Continued use of the app after changes means you accept the updated Terms.</string>
    <string name="terms_section_8_title">Contact</string>
    <string name="terms_section_8_body">Questions about these Terms? Reach us from the Help screen\'s Contact Support option.</string>
```

- [ ] **Step 2: Add strings (Arabic)**

In `presentation/src/main/res/values-ar/strings.xml`, add the matching block:

```xml
    <string name="terms_section_1_title">قبول الشروط</string>
    <string name="terms_section_1_body">من خلال إنشاء حساب واستخدام NutriScan، فإنك توافق على هذه الشروط والأحكام. إذا كنت لا توافق، يرجى عدم استخدام التطبيق.</string>
    <string name="terms_section_2_title">استخدام التطبيق</string>
    <string name="terms_section_2_body">يتم توفير NutriScan لتتبع التغذية الشخصية. أنت توافق على استخدامه فقط للأغراض القانونية والحفاظ على أمان بيانات حسابك.</string>
    <string name="terms_section_3_title">ليست نصيحة طبية</string>
    <string name="terms_section_3_body">معلومات التغذية وتقديرات السعرات الحرارية وردود NutriGPT هي لأغراض إعلامية فقط وليست بديلاً عن الاستشارة الطبية أو الغذائية المتخصصة. استشر مختصًا مؤهلاً للقرارات الطبية.</string>
    <string name="terms_section_4_title">بيانات المسح والكاميرا</string>
    <string name="terms_section_4_body">تتم معالجة الصور الملتقطة لمسح الباركود أو الإيصالات لاستخراج بيانات القيمة الغذائية ولا تتم مشاركتها مع أطراف ثالثة إلا بالقدر اللازم لتوفير هذه الميزة.</string>
    <string name="terms_section_5_title">المساعد الذكي (NutriGPT)</string>
    <string name="terms_section_5_body">يتم إنشاء ردود NutriGPT تلقائيًا وقد تكون غير دقيقة أحيانًا. استخدم تقديرك الشخصي قبل التصرف بناءً على اقتراحات الذكاء الاصطناعي.</string>
    <string name="terms_section_6_title">الحساب والملفات العائلية</string>
    <string name="terms_section_6_body">أنت مسؤول عن دقة المعلومات المُدخلة لنفسك ولأي ملفات عائلية تديرها ضمن حسابك.</string>
    <string name="terms_section_7_title">التغييرات على هذه الشروط</string>
    <string name="terms_section_7_body">قد نقوم بتحديث هذه الشروط من وقت لآخر. استمرار استخدامك للتطبيق بعد التغييرات يعني موافقتك على الشروط المحدّثة.</string>
    <string name="terms_section_8_title">التواصل</string>
    <string name="terms_section_8_body">لديك أسئلة حول هذه الشروط؟ تواصل معنا من خيار تواصل مع الدعم في شاشة المساعدة.</string>
```

- [ ] **Step 3: Create `TermsSection` component**

Create `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/terms/view/components/TermsSection.kt`:

```kotlin
package iti.grad.nutriscan.presentation.settings.terms.view.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import iti.grad.nutriscan.presentation.common.theme.AppTheme

@Composable
fun TermsSection(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 20.dp)) {
        Text(
            text = title,
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.AppSettingsRowLabel,
        )
        Text(
            text = body,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.AuthDialogSubtitle,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
```

- [ ] **Step 4: Create `TermsAndConditionsScreen`**

Create `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/terms/view/TermsAndConditionsScreen.kt`:

```kotlin
package iti.grad.nutriscan.presentation.settings.terms.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.components.AppTopHeader
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.terms.view.components.TermsSection
import iti.grad.presentation.R

@Composable
fun TermsAndConditionsScreen(
    onNavigateBack: () -> Unit = {},
) {
    Scaffold(containerColor = AppTheme.colors.Background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AppTheme.colors.Background)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            AppTopHeader(
                title = stringResource(R.string.app_settings_terms_and_conditions),
                onBackClick = onNavigateBack,
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                TermsSection(stringResource(R.string.terms_section_1_title), stringResource(R.string.terms_section_1_body))
                TermsSection(stringResource(R.string.terms_section_2_title), stringResource(R.string.terms_section_2_body))
                TermsSection(stringResource(R.string.terms_section_3_title), stringResource(R.string.terms_section_3_body))
                TermsSection(stringResource(R.string.terms_section_4_title), stringResource(R.string.terms_section_4_body))
                TermsSection(stringResource(R.string.terms_section_5_title), stringResource(R.string.terms_section_5_body))
                TermsSection(stringResource(R.string.terms_section_6_title), stringResource(R.string.terms_section_6_body))
                TermsSection(stringResource(R.string.terms_section_7_title), stringResource(R.string.terms_section_7_body))
                TermsSection(stringResource(R.string.terms_section_8_title), stringResource(R.string.terms_section_8_body))
            }
        }
    }
}
```

- [ ] **Step 5: Wire `TermsAndConditionsScreen` into NavGraph**

In `app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt`, replace lines 332-340:

```kotlin
        // 25. Terms and Conditions (Placeholder)
        composable<TermsAndConditionsRoute> {
            PlaceholderScreen(
                title = stringResource(R.string.app_settings_terms_and_conditions),
                buttonText = stringResource(R.string.action_go_back),
            ) {
                navController.navigateUp()
            }
        }
```

with:

```kotlin
        // 25. Terms and Conditions
        composable<TermsAndConditionsRoute> {
            TermsAndConditionsScreen(
                onNavigateBack = { navController.navigateUp() },
            )
        }
```

Add `import iti.grad.nutriscan.presentation.settings.terms.view.TermsAndConditionsScreen`
at the top of `NavGraph.kt` alongside the `HelpScreen` import added in Task 3.

- [ ] **Step 6: Build and manually verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Navigate Settings → Terms and Conditions, confirm all 8
sections render, scroll works, back button returns to Settings.

- [ ] **Step 7: Commit**

```bash
git add presentation/src/main/kotlin/iti/grad/nutriscan/presentation/settings/terms presentation/src/main/res/values/strings.xml presentation/src/main/res/values-ar/strings.xml app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt
git commit -m "feat(presentation): add Terms and Conditions screen and wire it into navigation"
```

---

### Task 5: Final check — unused imports, PlaceholderScreen cleanup, full test run

**Files:**
- Modify: `app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt` (drop now-unused `PlaceholderScreen`/`stringResource(R.string.action_go_back)` usages if `PlaceholderScreen` is no longer referenced anywhere else in the file)

**Interfaces:** none — this task only cleans up and verifies, it introduces no new types.

- [ ] **Step 1: Check whether `PlaceholderScreen` is still used elsewhere in `NavGraph.kt`**

Run: `grep -n "PlaceholderScreen" app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt`
If other routes still use it, leave the import — only Tasks 3 and 4's two call sites
are gone. If it's now fully unused in the file, remove its import.

- [ ] **Step 2: Run full presentation test suite**

Run: `./gradlew :presentation:test`
Expected: BUILD SUCCESSFUL, all tests pass including `HelpViewModelTest` and the
existing `AppSettingsViewModelTest`.

- [ ] **Step 3: Run full app build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Manual smoke test**

Launch the app, go through: Settings → Help (expand 2-3 FAQ items, tap Contact
Support, tap Send Feedback and submit) → back → Settings → Terms and Conditions →
scroll through → back. Confirm both English and Arabic (switch language in Settings
first) render correctly without truncation or RTL layout breakage.

- [ ] **Step 5: Commit (only if Step 1 produced a change)**

```bash
git add app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt
git commit -m "chore: drop unused PlaceholderScreen import after Help/Terms screens landed"
```

If Step 1 found no cleanup needed, skip this commit — the feature is done in 5
commits, one under budget.
