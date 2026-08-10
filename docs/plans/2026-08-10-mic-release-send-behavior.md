# Plan: Mic Release Send Behavior (NutriGPT)

## 1. Feature Summary
Fix voice input flow so captured speech is sent only after user releases the microphone button, not when speech recognition emits an early final result while press-and-hold is still active.

## 2. Files to Create
- docs/plans/2026-08-10-mic-release-send-behavior.md: Implementation plan for this bug fix.

## 3. Files to Modify
- presentation/src/main/kotlin/iti/grad/nutriscan/presentation/nutrigpt/voice/viewmodel/NutriGptVoiceViewModel.kt: Remove early submit path tied to final-result callback and submit strictly on mic release.
- presentation/src/main/kotlin/iti/grad/nutriscan/presentation/nutrigpt/voice/state/NutriGptVoiceState.kt: Remove no-longer-needed state flag related to final-result submission timing.
- presentation/src/test/kotlin/iti/grad/nutriscan/presentation/nutrigpt/voice/viewmodel/NutriGptVoiceViewModelTest.kt: Update tests to validate release-driven submission.

## 4. Layer Breakdown
### Domain
- No domain model/use case/repository interface changes.

### Data
- No data-layer changes.

### Presentation
- State: remove internal final-result send gating flag.
- Events: keep existing events; SetListeningState(false) remains release trigger.
- Effects: no change.
- ViewModel logic:
  - Final speech result updates query only.
  - Query submission happens only after SetListeningState(false) (release path).

## 5. Navigation Changes
- No navigation changes.

## 6. Strings - MANDATORY (Zero Hardcoded Text)
No new user-facing strings required for this fix.

| Key (R.string.xxx) | English Value | Arabic Value |
|---|---|---|
| None | None | None |

## 7. Testing Plan
- Verify SetListeningState(true) starts listening.
- Verify SetListeningState(false) stops listening.
- Verify FinalResult alone does not send request before release.
- Verify FinalResult + release triggers send request.
- Verify existing error/effect behavior remains intact.

## 8. Edge Cases
- FinalResult arrives while user still holding mic -> do not submit yet.
- Release occurs after FinalResult -> submit once only.
- Release with blank query -> do not submit.

## 9. Definition of Done
- [ ] All listed files updated.
- [ ] ViewModel test cases for release-based sending pass.
- [ ] No hardcoded colors/strings added.
- [ ] Voice input submits only on release.
