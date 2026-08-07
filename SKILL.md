# AI Agent Development Skills & Best Practices

This document defines non-negotiable coding and testing guidelines for all AI agents working on **NutriScan AI**.

---

## 1. Feature Planning Standard
Before writing any code or making workspace edits for a new feature:
1. Conduct research on the requirements and codebase design.
2. Create a comprehensive implementation plan inside the `docs/plans/` folder.
   - Name the file using the pattern: `YYYY-MM-DD-feature-name.md`.
   - The plan must detail Goal Description, User Review Required, Proposed Changes (broken down by modules), and a Verification Plan (both automated and manual).

---

## 2. Localization & String Safety
- **No Hardcoded User-Facing Strings**: Under no circumstances should hardcoded string literals be used in Composable elements (e.g., `Text("Next")`).
- **Standard Implementation**:
  - Add English string keys to `presentation/src/main/res/values/strings.xml`.
  - Add Arabic translations to `presentation/src/main/res/values-ar/strings.xml`.
  - Access strings in Composables via `stringResource(id = R.string.key)`.
  - Ensure dynamic strings utilize string formatting placeholders (e.g. `Hello, %1$s!`).

---

## 3. Theming & Light/Dark Mode Compatibility
- **No Hardcoded Color Hexes**: Under no circumstances should custom colors be hardcoded in UI components (e.g., `Color(0xFF00FF00)`).
- **Theme Coupling**:
  - Always use `AppTheme.colors.Name` or `MaterialTheme.colorScheme.name` for colors.
  - Using semantic names ensures elements render beautifully and remain readable during theme switching.

---

## 4. ViewModel Unit Testing Pattern
All feature ViewModels must be thoroughly unit tested using **JUnit 5**, **Turbine**, and **Kotlin Coroutines Test**.

### Standard Test Template
```kotlin
package iti.grad.nutriscan.presentation.feature

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private lateinit var viewModel: FeatureViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = FeatureViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when action triggered, state is updated correctly`() = runTest(testDispatcher) {
        viewModel.onEvent(FeatureEvent.Action)
        assertEquals(ExpectedValue, viewModel.state.value.property)
    }

    @Test
    fun `when action triggered, flow effect is emitted`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(FeatureEvent.Action)
            assertEquals(FeatureEffect.ExpectedEffect, awaitItem())
        }
    }
}
```

### Guidelines for Writing Tests
1. **Dispatcher Scoping**: Use `StandardTestDispatcher(testScheduler)` and assign it as main dispatcher in `@BeforeEach` and reset in `@AfterEach`.
2. **Turbine Flow Collection**: Use `flow.test { ... }` block to assert flow emission events.
3. **Flat Assertions**: Avoid nesting `flow.test` inside other test flows to prevent variable type collision and compiler ambiguity. Check state properties directly via `viewModel.state.value` or in separate test cases.
