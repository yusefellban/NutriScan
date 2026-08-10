# AI Voice Chat UX Improvement Plan

This plan details the changes required to update the Voice Chat UX to a push-to-talk behavior, as requested.

## User Review Required
Please review the changes below. Once approved, I will implement them.

## Open Questions
- When the user releases the mic button, the system stops listening and waits for the Speech Recognizer to return the final result before sending the message. Is this acceptable, or do you want to force-send the *current partial transcript* immediately on release, even if it's not the final result? (Usually, waiting for the final result is better for accuracy).

## Proposed Changes

### NutriGpt Voice Feature

#### [MODIFY] [NutriGptVoiceScreen.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/nutrigpt/voice/view/NutriGptVoiceScreen.kt)
- **Remove Auto-Listen on Startup**: In the `LaunchedEffect`, I will remove the call to `SetListeningState(true)` so the app only checks/requests microphone permission but waits for user input before listening.
- **Hold-to-Talk Implementation**: Replace the `.clickable` modifier on the Mic button with `.pointerInput` using `detectTapGestures`. 
  - `onPress`: 
    - If AI is speaking (`isPlaying` or `isGenerating`), stop it immediately by firing `StopPlaying`.
    - Start listening by firing `SetListeningState(true)`.
    - Wait for the user to release the button using `tryAwaitRelease()`.
  - `onRelease`: Fire `SetListeningState(false)` to stop recording and trigger the message sending.
- **Remove FastForward Button**: Update the Mic button's icon to always display `Icons.Rounded.Mic` instead of switching to a FastForward icon when the AI is speaking.

#### [MODIFY] [NutriGptVoiceViewModel.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/nutrigpt/voice/viewmodel/NutriGptVoiceViewModel.kt)
- The ViewModel already handles submitting the query when `VoiceState.FinalResult` is emitted. Calling `SetListeningState(false)` will stop the recognizer and trigger this flow naturally. No major structural changes are needed here, but I will ensure it smoothly integrates with the new `onPress`/`onRelease` logic.

## Verification Plan
### Manual Verification
- Open the Voice Chat screen and confirm it does not start listening immediately.
- Hold the mic button, speak, and release it. Verify the message is sent.
- While the AI is responding, verify the skip button is gone and the mic button is visible.
- Hold the mic button while the AI is responding; verify the AI stops talking immediately and the app starts listening again.
