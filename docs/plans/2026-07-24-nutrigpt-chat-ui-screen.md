# NutriGPT Chat UI Screen Implementation Plan

Implement the full Compose UI for the NutriGPT chat screen based on the provided Figma design screenshot. The domain, data, and ViewModel layers already exist — this plan covers **only** the UI layer (Compose Screen + components), the missing string resources, navigation wiring, theme colors, and any necessary State/Event/Effect additions.

## Design Reference (from screenshot)

The screen has a **dark teal gradient background** with the following sections:

1. **Top App Bar** — back button `<`, title "Nutrition AI", language toggle `EN`, and a sparkle/bot icon
2. **Chat messages area** — scrollable; user messages are right-aligned teal bubbles, bot messages are left-aligned dark bubbles with a sparkle "✦" prefix
3. **Sources section** — expandable/collapsible; shows RAG source documents as cards with file name, relevance score (%), and a snippet preview
4. **Arabic info banner** — a bottom inline note: "الوثائق المقدمة لا تحتوي على معلومات كافية..." (documents don't contain sufficient information disclaimer)
5. **Bottom input bar** — rounded text field with placeholder "Ask about nutrition...", a microphone icon 🎤, and a teal circular send button ➤

---

## Proposed Changes

### Theme & Colors

#### [MODIFY] [AppColors.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/theme/AppColors.kt)
Add NutriGPT chat-specific color tokens to `AppColorsExtension`:
- `ChatScreenBackground` — gradient start (dark teal ~`0xFF0A3D40`)
- `ChatScreenBackgroundEnd` — gradient end (darker teal ~`0xFF0D2E30`)
- `ChatUserBubble` — user message bubble (teal ~`0xFF13A4AB`)
- `ChatUserBubbleText` — white text on user bubble
- `ChatBotBubble` — bot message bubble (semi-transparent dark ~`0xFF0F3B3E`)
- `ChatBotBubbleText` — light text on bot bubble
- `ChatSourceCardBackground` — source document card bg
- `ChatSourceScoreBadge` — the relevance % badge
- `ChatInputBackground` — input field background
- `ChatInputText` — input text color
- `ChatInputPlaceholder` — placeholder text color
- `ChatSendButtonBackground` — circular send button bg
- `ChatSendButtonIcon` — send arrow icon tint
- `ChatDisclaimerBackground` — info banner background
- `ChatDisclaimerText` — info banner text color

---

### String Resources

#### [MODIFY] [strings.xml](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/res/values/strings.xml)
Add under a `<!-- NutriGPT Chat -->` comment:
```xml
<string name="nutrigpt_title">Nutrition AI</string>
<string name="nutrigpt_input_hint">Ask about nutrition…</string>
<string name="nutrigpt_sources_header">Sources</string>
<string name="nutrigpt_disclaimer">The provided documents may not contain sufficient information to answer your question. We recommend consulting additional sources.</string>
<string name="nutrigpt_send">Send</string>
<string name="nutrigpt_mic">Voice input</string>
<string name="nutrigpt_lang_toggle">EN</string>
<string name="nutrigpt_back">Go back</string>
<string name="nutrigpt_score_format">%1$d%%</string>
```

#### [MODIFY] [strings.xml (Arabic)](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/res/values-ar/strings.xml)
Add corresponding Arabic translations.

---

### Drawable Icons

#### [NEW] `ic_sparkle.xml`
A small sparkle/star icon for the bot message prefix and the top-bar icon (Material Symbols `auto_awesome` or custom vector).

#### [NEW] `ic_send_arrow.xml`
An upward-pointing arrow inside a circle for the send button.

#### [NEW] `ic_mic.xml`
A microphone icon for voice input.

> [!NOTE]
> Will use Material Icons composable fallbacks (`Icons.Rounded.Send`, `Icons.Rounded.Mic`, `Icons.Rounded.AutoAwesome`) where possible to avoid adding new XML drawables.

---

### MVI State / Event / Effect Additions

#### [MODIFY] [NutriGptState.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/nutrigpt/chat/state/NutriGptState.kt)
Add:
- `val areSourcesExpanded: Boolean = false` — tracks whether the sources section is expanded
- `val chatLanguage: ChatLanguage = ChatLanguage.EN` — the language used for NutriGPT queries only (EN / AR), independent of the app's UI language

#### [MODIFY] [NutriGptEvent.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/nutrigpt/chat/state/NutriGptEvent.kt)
Add:
- `data object ToggleSources : NutriGptEvent` — toggle sources visibility
- `data object ToggleLanguage : NutriGptEvent` — toggle chat language between EN ↔ AR (affects query language only)

#### [MODIFY] [NutriGptEffect.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/nutrigpt/chat/state/NutriGptEffect.kt)
Add:
- `data object NavigateBack : NutriGptEffect` — handle back navigation from the effect channel

#### [MODIFY] [NutriGptViewModel.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/nutrigpt/chat/viewmodel/NutriGptViewModel.kt)
- Handle the new `ToggleSources` event
- Handle the new `NavigateBack` event (emit effect)

---

### UI Components (new files in `presentation/nutrigpt/chat/view/`)

#### [NEW] [NutriGptScreen.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/nutrigpt/chat/view/NutriGptScreen.kt)
The main screen composable:
- `@Composable fun NutriGptScreen(onNavigateBack: () -> Unit)`
- Injects `NutriGptViewModel` via `hiltViewModel()`
- Collects `state` and `effect` flows
- Full-screen dark teal gradient background
- Scaffold-less layout: Column with TopBar → messages list → disclaimer → input bar

#### [NEW] `components/ChatTopBar.kt`
- Back button (uses `AppBackButton` with custom tint or custom Box)
- Title "Nutrition AI" centered
- Language toggle "EN" pill/button
- Sparkle/bot icon on the right

#### [NEW] `components/ChatMessageBubble.kt`
- Renders a single `NutriGptMessage`
- If `isFromUser` → right-aligned teal bubble with white text
- If not → left-aligned dark bubble with sparkle icon prefix, light text
- Smooth slide-in animation

#### [NEW] `components/ChatSourceCard.kt`
- Renders a single `NutriGptSource`
- Shows: document icon 📄, `fileName`, relevance `score` as a percentage badge, and truncated `snippet`
- Rounded card with semi-transparent dark background

#### [NEW] `components/ChatSourcesSection.kt`
- "Sources" header with expand/collapse toggle
- `AnimatedVisibility` for the sources list
- Uses `LazyColumn` or `Column` for source cards

#### [NEW] `components/ChatInputBar.kt`
- Rounded text field with teal-ish border
- Placeholder text "Ask about nutrition…"
- Microphone icon button (left or inside field)
- Circular teal send button with arrow icon
- Handles `NutriGptEvent.UpdateQuery` on text change and `NutriGptEvent.SendMessage` on send

#### [NEW] `components/ChatDisclaimerBanner.kt`
- Full-width semi-transparent banner
- Shows the "documents may not contain sufficient information" disclaimer text
- Icon + text layout

---

### Navigation Wiring

#### [MODIFY] [NavGraph.kt](file:///c:/Users/DELL/Documents/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt)
Replace the `ChatWithAiRoute` placeholder:
```kotlin
composable<ChatWithAiRoute> {
    NutriGptScreen(
        onNavigateBack = { navController.navigateUp() }
    )
}
```
Also replace the `NutriGptRoute` placeholder similarly.

---

## Open Questions

> [!NOTE]
> **Language Toggle behavior** *(Resolved)*: Tapping the "EN" / "AR" toggle changes **only the NutriGPT chat query language** sent to the backend — it does NOT change the app's UI language. A `ChatLanguage` enum (`EN`, `AR`) will be stored in `NutriGptState.chatLanguage` and toggled via `NutriGptEvent.ToggleLanguage`. The toggle pill text reflects the current chat language.

> [!IMPORTANT]
> **Microphone / Voice Input**: The screenshot shows a mic icon. Should this be functional (speech-to-text) or just a placeholder icon for now? I will implement it as a placeholder that shows a "Coming soon" toast.

---

## Verification Plan

### Automated Tests
- `./gradlew :presentation:compileDebugKotlin` — ensure new composables compile
- `./gradlew assembleDebug` — full project build

### Manual Verification
- Navigate from Home → "Chat with AI" → verify NutriGPT screen renders
- Send a message → verify user bubble appears → loading indicator → bot response with sources
- Verify dark/light theme colors
- Verify Arabic layout direction (RTL)
