# NutriGPT AI Voice Screen Implementation Plan

This plan outlines the architecture and UI components for the new NutriGPT Voice Chat screen, following the guidelines in `AGENTS.md`.

## Proposed Architecture (MVI)
We will create a new package `presentation/nutrigpt/voice/` containing the MVI components and UI.

### 1. State, Events, and Effects
#### [NEW] `NutriGptVoiceState.kt`
- `chatLanguage`: AR or EN
- `isListening`: Boolean (User is speaking)
- `isGenerating`: Boolean (AI is generating/fetching response)
- `isPlaying`: Boolean (TTS is playing response)
- `sources`: List of official sources (to display the source icons)
- `currentStatusText`: The text shown above the mic (e.g., "جاري الرد...", "جاري الاستماع...")

#### [NEW] `NutriGptVoiceEvent.kt`
- `ToggleLanguage`
- `StartListening` / `StopListening`
- `SkipResponse` (Fast forward icon)
- `SwitchToTextChat` (Bottom button)

#### [NEW] `NutriGptVoiceEffect.kt`
- `NavigateBack` (Go back to text chat)

### 2. ViewModel
#### [NEW] `NutriGptVoiceViewModel.kt`
- Annotated with `@HiltViewModel`.
- Holds `_state: MutableStateFlow` and `_effect: Channel`.
- Injected with `SendNutriGptMessageUseCase` and possibly a Text-To-Speech use case if we have one.

### 3. UI Components & Screen
#### [NEW] `NutriGptVoiceScreen.kt`
- Main screen composable.
- Integrates `SpeechRecognizer` (reusing the logic we built for text chat, but adapting it for voice-first interactions).
- UI Layout:
  - **Top Row**: Language toggle (Left) and Back button (Right).
  - **Sources Header**: "مصادر من وثائق رسمية" and 3 circular icons.
  - **Waveform Animation**: A visual representation of sound using Canvas or an animated drawable.
  - **Status Text**: E.g., "جاري الرد...".
  - **Main Action Button**: Fast-forward/Mic button.
  - **Bottom Button**: "التبديل إلى الدردشة النصية" (Switch to Text Chat).

#### [NEW] `VoiceWaveform.kt` (Component)
- A custom Canvas composable to draw overlapping sine waves that animate horizontally.

### 4. Navigation & Integration
#### [MODIFY] `Route.kt`
- Add `@Serializable data object NutriGptVoice`

#### [MODIFY] `NavGraph.kt`
- Add `composable<Route.NutriGptVoice> { NutriGptVoiceScreen(...) }`

#### [MODIFY] `ChatTopBar.kt` & `NutriGptScreen.kt`
- Make the voice icon (next to EN/AR) navigate to `Route.NutriGptVoice` instead of just showing a toast.

### 5. String Resources & Colors
- Add strings for "التبديل إلى الدردشة النصية" and "مصادر من وثائق رسمية" in both `strings.xml` and `values-ar/strings.xml`.
- Add any missing color tokens to `AppColors.kt`.

## Open Questions for User
1. **TTS Engine**: Do we have an existing Text-to-Speech (TTS) utility in the domain/data layer, or should I create a basic one using Android's native `TextToSpeech` for the AI to "speak" the response back?
2. **Sources**: The screenshot shows 3 specific icons (Document, Leaf, MedKit). Should these be hardcoded placeholders for now, or do we have actual sources to map them to?

## Verification Plan
- Build and run the app.
- Open Text Chat, click the Voice icon -> Verify it navigates to the Voice Screen.
- Verify UI matches the provided screenshot (Arabic/English toggle, back button, sources, waveform, status text, bottom button).
- Check standard MVI state changes (e.g., clicking the language toggle changes the text).
