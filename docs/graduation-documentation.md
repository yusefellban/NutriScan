# INFORMATION TECHNOLOGY INSTITUTE
## 9-MONTH PROGRAM – INTAKE MAD-46
### GRADUATION PROJECT

# NutriScan

**PREPARED BY**

//Team members

- Ahmed Tayseer
- Ashraf Sherif
- Noureldeen Osama
- Youssef El-Lban
- Narden Ayad

**SUPERVISED BY**

//SUPERVISOR

Project Code:

Date:

---

# Acknowledgment

We would like to express our sincere gratitude to the Information Technology
Institute (ITI) and the Ministry of Communications and Information Technology
for providing the environment, the equipment, and the nine months of intensive
training that made this project possible.

Our deepest thanks go to our supervisor, whose guidance shaped both the
technical direction and the engineering discipline of this work, and to the
instructors of the Native Mobile Application Development track, who taught us
not only Android and Kotlin but how to reason about architecture, testing, and
maintainability.

We also thank our QA colleagues, Mina Mofreh and Ahmed Abouelella, who tested
the application on real devices and produced the bug report that drove the
final hardening round, and the backend team whose REST services NutriScan
consumes.

Finally, we thank our families for their patience during the long weeks of
sprints, reviews, and late-night debugging.

---

# Table of Contents

| PART 1: Overview | |
|---|---|
| Abstract | 4 |
| Introduction | 5 |
| Objectives | 7 |
| **PART 2: Characteristics** | |
| Features | 9 |
| **PART 2.1: SDLC** | |
| Analysis | 14 |
| Design | 17 |
| Implementation | 22 |
| Tools | 28 |
| **PART 3: Illustrations** | |
| **PART 3.1: Diagrams** | |
| ERD | 30 |
| Use Case | 31 |
| Class Diagram | 32 |
| **PART 3.2: Screenshots** | 33 |
| References | 36 |

---

# Abstract

Food labels are written for regulators, not for people. A shopper with a peanut
allergy, a diabetic checking added sugar, or a parent buying a snack for a child
with celiac disease is expected to decode an ingredient list printed in six-point
type, in a language that may not be their own, while standing in a supermarket
aisle. The information required to make a safe decision exists — it is simply not
usable at the moment of the decision.

**NutriScan** is a native Android application that closes that gap. The user
points the camera at a product: either its barcode or its ingredient panel.
Within seconds the application returns a personalised verdict — **Safe**,
**Caution**, or **Unsafe** — computed against that specific user's recorded
allergies and chronic conditions, together with the exact ingredients that
triggered the verdict and a full nutrition breakdown.

Around this core, NutriScan provides the daily-health context that makes a single
scan meaningful: calorie and meal logging, water intake, automatic step counting,
a guided exercise library, a personalised nutrition news feed, a streak system,
a configurable notification engine, and **NutriGPT**, a retrieval-augmented
conversational assistant that answers nutrition questions in text or by voice
and cites its sources.

The application is written entirely in **Kotlin** with **Jetpack Compose**, and is
built on **Clean Architecture** across four Gradle modules (`:app`,
`:presentation`, `:domain`, `:data`) with a strict **MVI** presentation contract.
It is **offline-first**: a Room database is the single source of truth, and all
writes are replayed to the backend by a WorkManager sync engine when connectivity
returns. It is fully bilingual (English / Arabic) with complete right-to-left
support, and authenticates against a self-hosted Keycloak identity provider using
OpenID Connect.

The final release is version 1.1, comprising roughly 700 Kotlin source files,
98 automated test files, and 603 commits produced by a team of five over five
sprints between 13 July and 15 August 2026.

---

# Introduction

## The problem

Chronic diet-related disease and food allergy are both rising, and both place the
same demand on the individual: continuous, correct, per-product decisions.

- A person with a severe allergy must verify every packaged product they eat.
  A single missed derivative name — *casein* for milk, *semolina* for gluten,
  *E120* for an animal-derived colourant — is a clinical event, not an
  inconvenience.
- A person managing diabetes, hypertension, or kidney disease must track sugar,
  sodium, and protein against a target they cannot compute mentally from a
  per-100g table.
- Ingredient panels are dense, often bilingual, sometimes unreadable, and never
  personalised. They tell the reader what is *in* the product; they never tell
  the reader whether the product is safe *for them*.

Existing nutrition applications solve the adjacent problem — calorie counting —
and treat safety as a manual search task. The user is still the one who must read,
recognise, and decide.

## The idea

NutriScan inverts that relationship. The user tells the application, once, who
they are: their allergies, their chronic conditions, their body metrics, and the
same information for their family members. From that point on, every product the
user scans is evaluated *against that profile*, and the answer is delivered as a
verdict a person can act on in one glance, before the product goes into the
basket.

Two capture paths feed the same evaluation pipeline:

1. **Barcode scan** — ML Kit reads the barcode on-device; the product is resolved
   from the NutriScan backend, falling back to the OpenFoodFacts open database.
2. **Photo scan** — the user photographs the ingredient panel; the image is
   uploaded and analysed server-side, returning the same structured result.

Both return a `ScanResult`: a verdict, a summary, a list of flagged ingredients
with the reason each was flagged, and a normalised nutrition-facts block.

## Beyond the scan

A verdict is a moment. Health is a habit. NutriScan therefore wraps the scanner in
a daily-health loop: what the user ate (food log, fed directly from scan results),
what they drank (water tracking), how much they moved (automatic step counting
plus a guided exercise library with per-exercise calorie estimates), and how
consistently they did it (streaks and a notification engine that nudges without
nagging).

The result is an application where a single scan is not an isolated lookup but an
entry in a continuous personal health record — one that works on the aeroplane,
in the basement supermarket, and anywhere else the network does not.

## Document structure

Part 1 states the project's abstract, motivation, and objectives. Part 2 details
the delivered feature set and walks through the software development lifecycle:
requirements analysis, architectural and UI design, implementation of the key
technical subsystems, and the tooling used. Part 3 presents the system diagrams
(ERD, use case, class) and application screenshots, followed by references.

---

# Objectives

## Primary objective

Deliver a production-quality native Android application that turns a packaged
food product into a **personalised safety verdict** in under ten seconds, using
only the device camera and a health profile the user configures once.

## Product objectives

1. **Personalised safety, not generic data.** Every verdict is computed against
   the signed-in user's allergy and chronic-condition profile. The same product
   can be *Safe* for one account and *Unsafe* for another on the same device.
2. **Two capture paths, one result.** Barcode and ingredient-panel photo must
   produce the same structured `ScanResult` so that downstream features (history,
   saving, food logging) never need to know how a scan originated.
3. **Explainability.** The application never returns a bare verdict. It returns
   the specific flagged ingredients and why each was flagged, so the user can
   override an over-cautious result with informed judgement.
4. **A complete daily-health loop.** Calories, meals, water, steps, exercise, and
   streaks — so that a scan feeds a record rather than evaporating.
5. **Family coverage.** Allergy profiles are not only personal. A parent must be
   able to store family members with their own allergies and conditions.
6. **Assistance on demand.** NutriGPT answers free-form nutrition questions with
   cited sources, by keyboard or by voice, for users who cannot or prefer not to
   type.
7. **Full bilingual parity.** English and Arabic must be first-class: every
   string localised, full RTL layout mirroring, and an Arabic-appropriate
   typeface — not a translated afterthought.

## Engineering objectives

1. **Clean Architecture with enforced boundaries.** Four Gradle modules with a
   strict dependency rule (`presentation → domain ← data`) and a `:domain` module
   that is pure Kotlin with zero Android dependencies — making the business rules
   independently testable and framework-agnostic.
2. **A single, uniform presentation contract.** Every feature implements the same
   MVI triad (State / Event / Effect) with `StateFlow` for state and a `Channel`
   for one-shot effects, so that any team member can open any feature and already
   know its shape.
3. **Offline-first by default.** The Room database is the single source of truth.
   The UI reads from DAO `Flow`s and never blocks on the network; writes are
   queued with tombstone flags and replayed by a background sync engine.
4. **Testability as a delivery requirement.** Every ViewModel ships with a test
   file covering initial state, every Event → State transition, every
   Event → Effect emission, and error paths. Repositories and the sync engine are
   unit-tested against fakes.
5. **Secure authentication.** Standards-based OpenID Connect against a
   self-hosted Keycloak realm, with tokens held in encrypted storage and refreshed
   transparently at the HTTP layer rather than in feature code.
6. **Modern Android baseline.** 100% Kotlin, 100% Jetpack Compose, no XML layouts,
   no Fragments, type-safe navigation, `minSdk 31` / `targetSdk 36`.

---

# Features

This section documents the **implemented** feature set of NutriScan v1.1.

## 1. Onboarding and authentication

| Feature | Description |
|---|---|
| Splash | Branded splash using the AndroidX SplashScreen API; routes to onboarding, login, profile setup, or home based on session and profile state. |
| Onboarding carousel | Three-page introduction shown once; the "seen" flag persists in DataStore. |
| Register | Email/password registration against the NutriScan backend, with client-side validation and email-verification handoff. |
| Email verification | Verification-pending screen with resend support and rate-limit feedback. |
| Login | Email/password login, plus **Sign in with Google** via OpenID Connect through Keycloak (AppAuth). |
| Forgot password | Email-based password reset flow. |
| Profile setup pager | A six-step pager collecting gender, date of birth, height, weight, chronic conditions, and allergies. BMI and TDEE are derived on completion. |
| Account deletion / restoration | Soft deletion with a pending-deletion state and a restore path within the grace window. |

## 2. Scanning — the core feature

| Feature | Description |
|---|---|
| Unified camera screen | A single CameraX-backed screen handling both modes. ML Kit detects a barcode in the live preview automatically; the shutter captures the ingredient panel for photo analysis. |
| Barcode resolution | Barcode → NutriScan backend `POST /v1/scans/barcode`, falling back to the OpenFoodFacts public API. |
| Photo analysis | Image → `POST /v1/scans` (multipart) → structured `ScanResult`. |
| Verdict | Every scan resolves to **SAFE**, **CAUTION**, or **UNSAFE**, computed against the signed-in user's allergies and chronic conditions. |
| Product details | Verdict banner, product image and name, flagged-ingredient list with reasons, and a full nutrition-facts panel (calories, protein, carbohydrates, fat, fibre, sugar, sodium). |
| Add to food log | One tap adds the scanned product to today's meals with its calorie value. |
| Scan history | Full history with search plus server-backed autocomplete suggestions, verdict filters (Safe / Caution / Unsafe), date filtering, and swipe-to-delete with confirmation. |
| Saved scans | Favourite/unfavourite any scan; saved scans sync to the backend and remain readable offline. |

## 3. Calories and daily tracking

| Feature | Description |
|---|---|
| Calories dashboard | Daily calories consumed against the TDEE-derived target, with a macro breakdown and an animated mascot reflecting progress. |
| Food log | Today's meals, added from scans or manually; swipe-to-remove with a confirmation dialog. |
| Water tracking | Cup-based intake against an adjustable daily target. |
| Step counting | Automatic step counting from the hardware step-counter sensor via a foreground service, with calories-burned derivation. |
| Calories history | Per-day historical breakdown (consumed, water, exercise, steps) with date navigation. |
| Step history | Historical step data with daily detail. |
| Streaks | Consecutive-day goal-completion streak, synced with the backend. |

## 4. Exercises

| Feature | Description |
|---|---|
| Exercise library | Backend-served exercise catalogue browsable by category, body part, equipment, and target muscle, with search and paging. |
| Workout screen | Animated GIF demonstration, step-by-step instructions, targeted and secondary muscles, and per-repetition / per-minute calorie estimates. |
| Workout logging | Completed workouts add their calorie burn to the day's tracking record and mark the day in the workout log. |

## 5. News

| Feature | Description |
|---|---|
| News home | Curated nutrition and health headlines on the home screen. |
| News list | Full article list with topic filtering, sourced from NewsAPI. |
| News detail | In-app article reading, with Chrome Custom Tabs for the full source. |

## 6. NutriGPT assistant

| Feature | Description |
|---|---|
| Chat | Free-form nutrition Q&A against a retrieval-augmented backend (`POST /api/query`). Answers render as Markdown and list the sources they were drawn from. |
| Voice mode | Hands-free conversation: on-device speech recognition for input and text-to-speech playback of the answer, with an animated listening/speaking state. |

## 7. Profile, family, and settings

| Feature | Description |
|---|---|
| User profile | Personal details, body metrics, BMI, TDEE, allergies, and chronic conditions. |
| Edit profile | Full profile editing with avatar capture/crop and multipart upload. |
| Family members | Create, edit, and delete family members, each with their own allergy and chronic-condition sets and profile image. |
| App settings | Theme (light / dark / system) and language (English / Arabic), both persisted in DataStore and applied instantly. |
| Notification settings | Per-type toggles (water, steps, streak, workout, food, scan, news, quotes, breaks) plus quiet-hours windows. |
| Notification history | A persisted, readable log of every notification the application has raised, with read/unread state. |
| Help and Terms | In-app help content and terms & conditions. |

## 8. Cross-cutting features

- **Bilingual EN / AR** — approximately 607 localised strings per language, full
  RTL mirroring, and the Kosans typeface for Arabic.
- **Light and dark themes** — a single design-token system (`AppTheme.colors`,
  `AppTheme.typography`, `AppTheme.shapes`); no hard-coded colours or dimensions
  in any composable.
- **Offline-first** — the application opens, reads history, logs food, tracks
  water, and counts steps with no network; changes replay automatically.
- **Haptics and motion** — tactile feedback on key interactions and animated
  transitions between screens and bottom-navigation tabs.
- **Accessibility-conscious UI** — content descriptions, shimmer placeholders
  during loading, and standardised empty and error states across every screen.

---

# PART 2.1: SDLC

## 1.1 Analysis

### Development methodology

The project followed an **Agile / Scrum** process over five iterations —
a Sprint 0 setup iteration followed by Sprints 1 to 3 and a final stabilisation
and hardening phase — running from **13 July 2026 to 15 August 2026**. Work was
tracked on a Trello board (`NutriScan`, board `3j0IimeG`) with six lists modelling
the delivery pipeline:

| List | Purpose |
|---|---|
| Product Backlog | All identified work, unscheduled. |
| Sprint Backlog | Committed scope for the active sprint. |
| In Progress | Actively being developed. |
| Bugs | Defects raised by QA or the team. |
| Code Review | Awaiting peer review on a pull request. |
| Done | Merged and verified. |

Cards were prioritised with **MoSCoW** labels (`must have`, `should have`) and
tagged by iteration (`sprint 0`, `sprint 1`, `sprint 2`, `sprint 3`) plus
cross-cutting epics such as `Auth integration`. At delivery, **106 cards** were in
Done.

### Sprint breakdown

| Sprint | Focus | Representative delivered cards |
|---|---|---|
| **Sprint 0** (20 cards) | Foundations and skeleton | GitHub repository setup, modularisation, Hilt DI setup, Retrofit network layer, Room database setup, Navigation Component setup, splash, onboarding, login/register/forgot-password UI, profile-setup pager (gender, height, weight), home screen, bottom navigation, localisation (EN/AR), search infrastructure, engineering conventions (`AGENTS.md`). |
| **Sprint 1** (9 cards) | Authentication and profile integration | Login, registration, and forgot-password backend integration via Keycloak; profile integration; scan integration; health-profile setup (diseases); edit-profile screen; settings; saved-scans screen. |
| **Sprint 2** (9 cards) | Scanning and user data | Camera permission handling, product details, home/profile/settings data integration, profile refactor, exercises screen UI, application alert system. |
| **Sprint 3** (6 cards) | Tracking and content | Calories screen, news screen, exercises backend integration, BMI/TDEE on profile, registration and profile localisation fixes. |
| **Stabilisation** (62 cards) | Hardening and polish | Offline mode, barcode scanning, unit tests, push notifications and notification history, NutriChat AI, family-member CRUD and backend integration, RTL fixes, scan history, calories history, step details, empty states and shimmer placeholders, delete-user-data, UI unification, release v1. |

### Actors

| Actor | Description |
|---|---|
| **Guest** | An unauthenticated visitor. Can view onboarding, register, log in, and recover a password. |
| **Registered User** | The primary actor. Owns a health profile and performs every scanning, tracking, exercise, content, and assistant operation. |
| **Family Member** | A passive subject, not a login. A profile owned by a Registered User, carrying its own allergies and conditions. |
| **NutriScan Backend** | External system: scan analysis, product data, profile persistence, daily tracking, streaks. |
| **Keycloak** | External identity provider: OIDC authentication, token issue and refresh. |
| **Third-party services** | OpenFoodFacts (product fallback), NewsAPI (articles), Exercises API (catalogue), NutriGPT service (RAG answers). |

### Functional requirements

**Account** — register with email/password; verify email; log in with email/password
or Google; recover a forgotten password; complete a health profile; view and edit
the profile including avatar; manage family members; delete and restore the
account; log out.

**Scanning** — scan a barcode; photograph an ingredient panel; receive a
personalised verdict with flagged ingredients and nutrition facts; browse, search,
filter, and delete scan history; save and unsave scans; add a scanned product to
the food log.

**Tracking** — view daily calories against target; add and remove food-log
entries; track water intake against an adjustable target; count steps
automatically; view calories and step history by date; maintain a daily streak.

**Exercise** — browse and search the exercise catalogue by category; view exercise
instructions and demonstration; log a completed workout and its calorie burn.

**Content and assistance** — read nutrition news; ask NutriGPT a question by text
or voice and receive a cited answer.

**Settings and notifications** — switch theme; switch language; configure
notification types and quiet hours; review notification history.

### Non-functional requirements

| Requirement | Target and approach |
|---|---|
| **Offline availability** | Every read path is served from Room. The application is fully usable with no connectivity; writes queue and replay. |
| **Performance** | Barcode recognition on-device via ML Kit; images loaded and cached through Coil; immutable collections in state to preserve Compose stability and avoid needless recomposition. |
| **Security** | OIDC/OAuth2 with PKCE via AppAuth; tokens in encrypted DataStore (`security-crypto`); automatic refresh at the OkHttp `Authenticator` layer; no credentials in source (API keys are injected from `local.properties` into `BuildConfig`). |
| **Localisation** | 100% string externalisation, English and Arabic, with layout mirroring verified per screen. |
| **Maintainability** | Enforced layer boundaries, one uniform MVI contract, and module-scoped conventions documented in `AGENTS.md`. |
| **Testability** | 98 test files; a ViewModel change without a corresponding test update is treated as incomplete. |
| **Compatibility** | `minSdk 31` (Android 12) to `targetSdk 36`; phone form factors, portrait, light and dark. |
| **Resilience** | All repository calls wrapped in `runCatchingCancellable` on an injected IO dispatcher, mapped to typed `DomainException`s and surfaced as standardised error states. |

---

## 1.2 Design

### System architecture

NutriScan implements **Clean Architecture** across four Gradle modules:

```
                    ┌─────────────────┐
                    │      :app       │  DI wiring, navigation graph,
                    │                 │  WorkManager, notifications,
                    │                 │  steps foreground service
                    └────────┬────────┘
                             │
          ┌──────────────────┼──────────────────┐
          ▼                  ▼                  ▼
  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐
  │ :presentation │  │    :domain    │  │     :data     │
  │  Compose UI   │─▶│  Pure Kotlin  │◀─│  Room, Retrofit│
  │  ViewModels   │  │  Models,      │  │  DataStore,    │
  │  MVI states   │  │  UseCases,    │  │  Repository    │
  │               │  │  Repo ifaces  │  │  impls         │
  └───────────────┘  └───────────────┘  └───────────────┘
```

**The dependency rule:** `presentation → domain ← data`. The `:domain` module is a
plain `java-library` with **no Android dependencies at all** — no `Context`, no
`Uri`, no Compose. `:presentation` and `:data` both depend on `:domain` and never
on each other. `:app` is the only module that knows all three, and its sole job is
to bind interfaces to implementations and host the navigation graph.

This is what makes the business rules — verdict handling, TDEE derivation, sync
policy, notification eligibility — testable as plain JVM unit tests with no
emulator, and what allows the data layer to be replaced (a different backend, a
different local store) without touching a single composable.

### Presentation pattern: MVI

Every feature follows an identical contract:

```
feature/
├── state/
│   ├── FeatureState.kt     data class — everything the screen renders
│   ├── FeatureEvent.kt     sealed interface — everything the user can do
│   └── FeatureEffect.kt    sealed interface — one-shot side effects
├── viewmodel/
│   └── FeatureViewModel.kt StateFlow<State> + Channel<Effect>
└── view/
    ├── FeatureScreen.kt    stateless composable
    └── components/         screen-local composables
```

- **State** is a single immutable `data class` exposed as `StateFlow<State>`.
  Collections inside it use `kotlinx.collections.immutable.ImmutableList` so that
  Compose can treat them as stable and skip recomposition correctly.
- **Event** is the only way the UI talks to the ViewModel: a single
  `onEvent(event: FeatureEvent)` entry point. Screens have no other callbacks.
- **Effect** carries things that must happen exactly once — navigation, snackbars,
  opening the system settings — over a `Channel`, never over state, so that a
  configuration change cannot replay them.

The payoff is uniformity: five developers, thirty-plus features, and one shape.
Reviewing an unfamiliar feature requires no orientation.

### Data layer design: offline-first

Room is the **single source of truth**. No screen ever reads the network directly.

```
UI ──collect──▶ ViewModel ──▶ UseCase ──▶ Repository
                                              │
                                    ┌─────────┴─────────┐
                                    ▼                   ▼
                              Room DAO Flow      Remote DataSource
                             (emits first,       (fetched, written
                              always wins)        into Room)
```

The reference implementation is `data/repository/FoodLogRepositoryImpl.kt`. Each
read exposes the DAO `Flow` immediately and refreshes from the network inside a
`channelFlow`, so the UI paints from cache in the first frame and updates in place
when the remote response lands.

Writes never block on the network. They are committed locally with tombstone
flags — `pendingSync`, `deleted`, and `backendCreated` on `food_log` and
`saved_scans`, `syncedToBackend` on `daily_tracking` — and replayed later by
`DailyTrackingSyncEngine`, driven by a periodic WorkManager job every six hours
(`ExistingPeriodicWorkPolicy.KEEP`) plus opportunistic reconciliation when a
tracking screen opens.

Every repository call is wrapped in `runCatchingCancellable` and dispatched on an
injected `@IoDispatcher`, converting throwables into typed domain errors while
correctly propagating coroutine cancellation.

### Database design

`NutriScanDatabase` (schema **version 17**) contains eleven tables:

| Table | Role |
|---|---|
| `users` | The signed-in user's profile, metrics, allergy/disease ID sets, and family members. |
| `diseases`, `allergies` | Reference catalogues, seeded from the backend. |
| `daily_tracking` | One row per user per day: water, steps, calories burned, exercise minutes. |
| `food_log` | Individual logged meals with calories and verdict. |
| `saved_scans` | Scan results with verdict, flagged ingredients, and nutrition facts. |
| `exercises`, `exercise_categories` | Cached exercise catalogue. |
| `workout_log` | Per-day workout completion. |
| `streak` | Current consecutive-day streak per user. |
| `notification_history` | Every notification raised, with read state. |

Family members are stored as a serialised JSON list on the `users` row rather than
as their own table — a deliberate denormalisation, since a family member has no
independent lifecycle, is never queried on its own, and always loads with its
owner. The full ERD appears in Part 3.1.

### UI and visual design

The interface was designed in **Figma** before implementation and built with
**Material 3** on a custom design-token layer:

- **`AppTheme.colors`** — a semantic palette (teal/cyan primary, verdict colours
  for safe/caution/unsafe) defined once for light and once for dark. Raw
  `Color(0xFF…)` values are prohibited in composables.
- **`AppTheme.typography`** — a type scale with a Latin family and the **Kosans**
  family for Arabic; no raw `sp` values in feature code.
- **`AppTheme.shapes`** — the corner-radius scale; no ad-hoc rounding.

Reusable components are shared rather than forked: `ProductCard` accepts a
`ProductCardSwipeAction` (`Add` or `Remove`) so that the scan history, saved
scans, and food-log screens all use one card. Loading uses shimmer placeholders,
not spinners; empty and error states are standardised components, not per-screen
improvisations.

Navigation is **type-safe Compose Navigation**: routes are `@Serializable`
Kotlin objects and data classes, so an argument-type error is a compile error
rather than a runtime crash. The shell is a five-tab bottom bar — Home, Calories,
Scan (centre), Exercises/News, Profile — with animated tab and screen transitions.

**RTL** is a design constraint, not a post-process: layouts use start/end
directional modifiers throughout, and `RtlUtils` centralises the cases where
mirroring must be suppressed (numerals, charts, media controls).

---

## 1.3 Implementation

### Authentication

Authentication is standards-based OpenID Connect against a self-hosted
**Keycloak** realm (`nutriscan`):

- **Google sign-in** runs through Keycloak's identity brokering using **AppAuth**
  with an authorisation-code flow and a `nutriscan` redirect scheme. The
  application never handles Google credentials directly.
- **Email/password** registration, verification, and reset run against the
  NutriScan REST backend (`v1/auth/*`), with Keycloak issuing the tokens.
- **Token storage** is `TokenManager`, backed by DataStore with
  `androidx.security:security-crypto` encryption. Claims are parsed locally by
  `JwtDecoder`.
- **Token refresh is invisible to feature code.** `NutriScanAuthenticator`, an
  OkHttp `Authenticator`, intercepts a 401, exchanges the refresh token at
  `realms/nutriscan/protocol/openid-connect/token`, and transparently retries the
  original request. No ViewModel or use case contains refresh logic.

### The scan pipeline

```
CameraX preview
   │
   ├─ barcode detected (ML Kit, on-device)
   │     └─▶ POST /v1/scans/barcode ──┐
   │            └─ fallback: OpenFoodFacts /api/v2/product/{barcode}
   │                                  │
   └─ shutter → image captured        │
         └─▶ POST /v1/scans (multipart)
                                      │
                                      ▼
                              ScanResult
                    verdict · summary · flagged ingredients
                            · nutrition facts
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                 ▼
            Product details    saved_scans (Room)   food_log entry
```

Both entry points converge on one domain model, so scan history, saved scans, and
food logging never branch on capture mode. Verdict evaluation is server-side,
computed against the profile carried on the authenticated request, which keeps the
allergen rule set updatable without shipping an app release.

### Daily tracking and synchronisation

`daily_tracking` is keyed by `(userId, date)`. Water increments, step counts, and
exercise minutes are written locally and marked unsynced. `DailyTrackingSyncEngine`
reconciles local and remote state: it pushes pending local mutations, pulls the
authoritative remote snapshot, and merges. Food-log entries carry `pendingSync`,
`deleted`, and `backendCreated` flags so that a create-then-delete performed
entirely offline resolves correctly without ever reaching the server as two
operations.

Steps are read from the hardware step-counter sensor by `StepsForegroundService`
(a `FOREGROUND_SERVICE_HEALTH` service), which survives process death and
back-fills the day's count on reconnect. `StepsSyncWorker` uploads the totals.

### Notifications

Nine `@HiltWorker` WorkManager workers cover water, steps, streak, workout, food,
scan, news, quotes, and break reminders. Each consults the user's per-type
preference and the configured quiet-hours window before posting, and every posted
notification is recorded in `notification_history` so the user can review anything
they dismissed. The default `WorkManagerInitializer` is removed from the manifest
in favour of Hilt's `Configuration.Provider`, so workers can inject use cases.

### NutriGPT

The assistant posts the user's question to a retrieval-augmented backend
(`POST api/query`) and receives an answer plus its source list. Answers render
through `compose-markdown`, and sources are shown as tappable references.

Voice mode is built on the Android platform APIs — `SpeechRecognizer` for input
and `TextToSpeech` for output — wrapped behind a `VoiceManager` domain interface
so the ViewModel never touches an Android type. Playback runs at 1.5× speed, with
an animated state machine for idle, listening, thinking, and speaking.

### Localisation and RTL

Every user-facing string lives in `strings.xml` and `values-ar/strings.xml`
(~607 entries each) — a hard-coded string in Kotlin or Compose is a review
rejection. Language is a persisted user preference, applied to the composition
locally rather than requiring a process restart. RTL was verified screen by
screen, with dedicated fixes for the splash screen, charts, sliders, and the
bottom navigation.

### Testing and quality assurance

**Automated testing** — 98 test files across all four modules, on **JUnit 5**
(`useJUnitPlatform()`), with **MockK** for fakes and **Turbine** for `Flow` and
`Channel` assertions. Coverage spans ViewModels (state transitions, effects, and
error paths), repositories, the `DailyTrackingSyncEngine`, `NutriScanAuthenticator`,
`JwtDecoder`, notification slot logic, and Room DAOs as instrumented tests.

**Manual QA** — a dedicated testing round was carried out on physical devices
(Oppo Reno 6 5G / Android 11, Xiaomi Mi Note 10 / Android 12) and a Pixel 8
emulator (Android 16), producing a formal bug report of **23 defects**:

| Severity | Count |
|---|---|
| Critical | 10 |
| High | 10 |
| Medium | 1 |
| Low | 2 |

By type: 9 functional, 8 UI, 5 mixed UI/functional. **15 of the 23 were fixed**
for release; the remaining 8 were triaged as backend-owned, design-negotiable, or
out of the supported OS range. Representative defects and their resolutions:

| ID | Defect | Resolution |
|---|---|---|
| Bug-06 | All forgot-password recovery options (2FA, Authenticator, SMS) triggered the email flow. | Fixed — unsupported options removed. |
| Bug-07 | Password reset accepted a weak password with no validation. | Fixed at the backend validation layer. |
| Bug-08 / Bug-10 | Date of Birth accepted future dates on setup and on edit. | Fixed — forward-date validation on both paths. |
| Bug-16 / Bug-17 | Names accepted digits; height and weight accepted zero and negative values. | Fixed — field validation with localised error messages. |
| Bug-18 | Re-login after logout re-prompted for the full health profile. | Fixed — profile state restored from the server on login. |
| Bug-19 | "Grant Permission" did not open system settings after a camera-permission denial. | Fixed. |
| Bug-23 | The water target could be raised but never lowered. | Fixed — target is now bidirectional. |

Version control was **GitHub** with feature branches and pull-request review;
the final release comprises **603 commits** from five contributors, with releases
distributed to testers through **Firebase App Distribution**.

---

## 2. Tools

### Languages and platform

| Tool | Version | Purpose |
|---|---|---|
| Kotlin | 2.2.0 | Sole implementation language |
| Java | 11 | JVM target |
| Android Gradle Plugin | 9.2.1 | Build system |
| KSP | 2.2.0-2.0.2 | Annotation processing (Room, Hilt) |
| Android SDK | min 31 / target 36 / compile 37.1 | Platform baseline |

### Application frameworks and libraries

| Library | Version | Purpose |
|---|---|---|
| Jetpack Compose (BOM) | 2026.02.01 | Entire UI; Material 3 + extended icons |
| Navigation Compose | 2.8.3 | Type-safe navigation with serializable routes |
| Hilt | 2.59.2 | Dependency injection (+ `hilt-work`, `hilt-navigation-compose`) |
| Room | 2.8.4 | Local database, source of truth |
| Retrofit | 3.0.0 | REST client |
| OkHttp | 5.4.0 | HTTP, interceptors, token refresh |
| kotlinx.serialization | 1.11.0 | JSON and typed navigation routes |
| DataStore Preferences | 1.1.1 | Preferences (theme, language, onboarding, notifications) |
| security-crypto | 1.1.0-alpha06 | Encrypted token storage |
| WorkManager | 2.10.0 | Background sync and notification scheduling |
| CameraX | 1.4.2 | Camera preview and capture |
| ML Kit Barcode Scanning | 17.3.0 | On-device barcode recognition |
| Coil 3 | 3.0.4 | Image loading (Compose, OkHttp, SVG, GIF) |
| AppAuth | 0.11.1 | OpenID Connect / OAuth2 with PKCE |
| Firebase (BOM) | 33.7.0 | Analytics, App Distribution |
| androidx.browser | 1.9.0 | Chrome Custom Tabs |
| compose-markdown | 0.5.4 | Markdown rendering for NutriGPT answers |
| android-image-cropper | 4.6.0 | Avatar cropping |
| kotlinx-collections-immutable | 0.3.8 | Compose-stable collections in state |
| core-splashscreen | 1.0.1 | Splash screen API |
| Timber | 5.0.1 | Logging |

### Testing

| Tool | Version | Purpose |
|---|---|---|
| JUnit 5 | 5.10.2 | Test framework across all modules |
| MockK | 1.13.10 | Kotlin-native mocking |
| Turbine | 1.1.0 | Flow and Channel testing |
| kotlinx-coroutines-test | 1.8.1 | Coroutine test dispatchers |
| Espresso / Compose UI Test | — | Instrumented tests |

### External services and APIs

| Service | Use |
|---|---|
| NutriScan REST backend | Scans, profile, daily tracking, streaks, family members |
| Keycloak | Identity provider (OIDC, realm `nutriscan`) |
| OpenFoodFacts | Public product database fallback |
| NewsAPI | Nutrition and health articles |
| Exercises API | Exercise catalogue with demonstrations |
| NutriGPT service | Retrieval-augmented Q&A |

### Development and collaboration tools

| Tool | Use |
|---|---|
| Android Studio | Primary IDE |
| Git / GitHub | Version control, pull-request review, releases |
| Trello | Scrum board, backlog, sprint tracking |
| Figma | UI/UX design and design-system handoff |
| Postman | API exploration and contract verification |
| Firebase App Distribution | Test-build distribution to QA |
| Google Sheets | Bug reporting and QA tracking |

---

# PART 3: Illustrations

## 3.1 Diagrams

### 3.1.1 ERD

*[Diagram: ERD — `docs/diagrams/erd.png`]*

The entity-relationship diagram of the local Room schema (version 17). `users` is
the central entity; `daily_tracking`, `food_log`, `saved_scans`, and `streak` are
all keyed by `userId`. `diseases` and `allergies` are reference catalogues
referenced by ID sets on the user profile. `exercises` belongs to
`exercise_categories`; `workout_log` records per-day completion.

### 3.1.2 Use Case

*[Diagram: Use Case — `docs/diagrams/use-case.png`]*

The use-case diagram groups the application's capabilities into seven packages —
Account, Scanning, Tracking, Exercise, Content, Assistant, and Settings — against
the Guest and Registered User actors, with the NutriScan backend, Keycloak, and
the third-party APIs as external systems.

### 3.1.3 Class Diagram

*[Diagram: Class Diagram — `docs/diagrams/class-diagram.png`]*

The class diagram shows the layered structure: domain models and repository
interfaces at the centre, use cases depending only on those interfaces, data-layer
implementations satisfying them, and a representative MVI triad
(`State` / `Event` / `Effect` + `ViewModel`) on the presentation side.

---

## 3.2 Screenshots

*[Screenshots to be inserted]*

| # | Screen | Caption |
|---|---|---|
| 1 | Splash | Branded launch screen. |
| 2 | Onboarding | Three-page introduction to the application. |
| 3 | Login | Email/password sign-in with Google OIDC option. |
| 4 | Register | Account creation with validation. |
| 5 | Profile setup | Gender, date of birth, height, and weight collection. |
| 6 | Allergies / conditions | Health profile — the input to every verdict. |
| 7 | Home | Daily summary: calories, water, steps, streak, top news. |
| 8 | Scan — camera | Unified barcode and photo capture. |
| 9 | Product details — Safe | A product cleared against the user's profile. |
| 10 | Product details — Unsafe | Flagged ingredients with reasons. |
| 11 | Nutrition facts | Full macro and micro breakdown. |
| 12 | Scan history | Search, verdict filters, and date filtering. |
| 13 | Saved scans | Favourited products. |
| 14 | Calories | Daily target, macros, and the food log. |
| 15 | Calories history | Per-day breakdown over time. |
| 16 | Steps / step history | Automatic step counting and history. |
| 17 | Exercises | Catalogue by category with search. |
| 18 | Workout | Demonstration, instructions, calorie estimate. |
| 19 | News | Nutrition and health article feed. |
| 20 | NutriGPT chat | Cited answers rendered as Markdown. |
| 21 | NutriGPT voice | Hands-free assistant mode. |
| 22 | Profile | Metrics, BMI, TDEE, allergies, conditions. |
| 23 | Family members | Per-member allergy and condition profiles. |
| 24 | Notification settings | Per-type toggles and quiet hours. |
| 25 | App settings | Theme and language. |
| 26 | Arabic / RTL | The same screens fully mirrored. |
| 27 | Dark theme | Dark palette across key screens. |

---

# References

## Project resources

- Source repository — <https://github.com/yusefellban/NutriScan>
- Project board (Trello) — <https://trello.com/b/3j0IimeG/nutriscan>
- UI/UX design (Figma) — <https://www.figma.com/design/zs5EkmcWPyQJvwWY5awVQc/NutriScan>

## Platform and framework documentation

- Android Developers — <https://developer.android.com>
- Jetpack Compose — <https://developer.android.com/jetpack/compose>
- Guide to app architecture — <https://developer.android.com/topic/architecture>
- Room persistence library — <https://developer.android.com/training/data-storage/room>
- WorkManager — <https://developer.android.com/topic/libraries/architecture/workmanager>
- CameraX — <https://developer.android.com/training/camerax>
- Kotlin Coroutines — <https://kotlinlang.org/docs/coroutines-overview.html>
- Material Design 3 — <https://m3.material.io>

## Libraries and services

- Google ML Kit — Barcode Scanning — <https://developers.google.com/ml-kit/vision/barcode-scanning>
- Dagger Hilt — <https://dagger.dev/hilt>
- Retrofit — <https://square.github.io/retrofit>
- OkHttp — <https://square.github.io/okhttp>
- Coil — <https://coil-kt.github.io/coil>
- AppAuth for Android — <https://github.com/openid/AppAuth-Android>
- Keycloak — <https://www.keycloak.org/documentation>
- OpenFoodFacts API — <https://world.openfoodfacts.org/data>
- NewsAPI — <https://newsapi.org/docs>
- Firebase — <https://firebase.google.com/docs>

## Concepts

- Robert C. Martin, *Clean Architecture: A Craftsman's Guide to Software Structure
  and Design*, Prentice Hall, 2017.
- Model-View-Intent on Android — <https://developer.android.com/topic/architecture/ui-layer>
- Offline-first application design — <https://developer.android.com/topic/architecture/data-layer/offline-first>
- Mifflin-St Jeor equation (BMI / TDEE derivation) — *American Journal of Clinical
  Nutrition*, 51(2), 1990.
