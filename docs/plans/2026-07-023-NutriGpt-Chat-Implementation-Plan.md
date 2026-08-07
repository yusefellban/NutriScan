# Implement NutriGPT Chat Logic (Data to Presentation)

This plan covers implementing the chat functionality for NutriGPT according to the Clean Architecture and MVI patterns defined in `AGENTS.md`. We will build the Data layer, Domain layer, and Presentation layer (excluding UI).

## Open Questions
- Auth token requirement: **Resolved (No token required for this endpoint)**. The API will be called without the standard AuthInterceptor or with an anonymous client if needed.

## Proposed Changes

### Domain Layer (Models & Abstractions)
#### [NEW] `domain/src/main/kotlin/iti/grad/nutriscan/domain/nutrigpt/model/NutriGptMessage.kt`
- Define `NutriGptMessage` (text, sender type, sources list) and `NutriGptSource` models.
#### [NEW] `domain/src/main/kotlin/iti/grad/nutriscan/domain/nutrigpt/repository/INutriGptRepository.kt`
- Interface `INutriGptRepository` with `suspend fun sendQuery(query: String): DataResult<NutriGptMessage>`.
#### [NEW] `domain/src/main/kotlin/iti/grad/nutriscan/domain/nutrigpt/usecase/SendNutriGptMessageUseCase.kt`
- Implement UseCase that calls the repository.

---

### Data Layer (API, DTOs, Repository)
#### [NEW] `data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/NutriGptRequestDto.kt`
- Define `NutriGptRequestDto` for the request body.
#### [NEW] `data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/NutriGptResponseDto.kt`
- Define `NutriGptResponseDto` and `NutriGptSourceDto` representing the API response.
#### [NEW] `data/src/main/kotlin/iti/grad/nutriscan/data/remote/api/NutriGptApiService.kt`
- Define Retrofit interface for POST `api/query`. We might need a separate Retrofit instance for the `nutri-scan-rag.vercel.app` base URL.
#### [NEW] `data/src/main/kotlin/iti/grad/nutriscan/data/remote/datasource/INutriGptRemoteDataSource.kt`
#### [NEW] `data/src/main/kotlin/iti/grad/nutriscan/data/remote/datasource/NutriGptRemoteDataSourceImpl.kt`
- Implementation of the remote data source handling the API call.
#### [NEW] `data/src/main/kotlin/iti/grad/nutriscan/data/repository/NutriGptRepositoryImpl.kt`
- Repository implementation handling mapping from DTO to Domain model using `runCatchingCancellable`.

---

### Presentation Layer (State, Event, Effect, ViewModel)
#### [NEW] `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/nutrigpt/chat/state/NutriGptState.kt`
- Define MVI State including `messages: ImmutableList<NutriGptMessage>`, `isLoading: Boolean`, and `currentQuery: String`.
#### [NEW] `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/nutrigpt/chat/state/NutriGptEvent.kt`
- Define Events like `SendMessage` and `UpdateQuery(text)`.
#### [NEW] `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/nutrigpt/chat/state/NutriGptEffect.kt`
- Define Effects like `ShowError`.
#### [NEW] `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/nutrigpt/chat/viewmodel/NutriGptViewModel.kt`
- ViewModel integrating the UseCase, managing the `StateFlow`, and exposing `FeatureEffect`.

---

### DI Binding (App / Data Modules)
#### [MODIFY] DI Modules
- Add bindings for `NutriGptApiService`, `INutriGptRemoteDataSource`, `INutriGptRepository`, and `SendNutriGptMessageUseCase` in their respective Hilt modules. We will inject a specific Retrofit instance for the `vercel.app` URL.

## Verification Plan
### Automated Tests
- The code will follow testable Clean Architecture patterns.
### Static Analysis / Compile
- Run `./gradlew assembleDebug` to ensure there are no compilation errors in the newly created classes.
