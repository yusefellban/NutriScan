package iti.grad.nutriscan.presentation.onboarding.profile_setup.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.components.AppSnackbar
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans
import iti.grad.nutriscan.presentation.onboarding.profile_setup.state.HealthProfileSetupEffect
import iti.grad.nutriscan.presentation.onboarding.profile_setup.state.HealthProfileSetupEvent
import iti.grad.nutriscan.presentation.onboarding.profile_setup.state.HealthProfileSetupState
import iti.grad.nutriscan.presentation.onboarding.profile_setup.view.components.OtherInputChip
import iti.grad.nutriscan.presentation.onboarding.profile_setup.view.components.SelectableChip
import iti.grad.nutriscan.presentation.onboarding.profile_setup.viewmodel.HealthProfileSetupViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HealthProfileSetupScreen(
    viewModel: HealthProfileSetupViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                HealthProfileSetupEffect.NavigateToHome -> onNavigateToHome()
                is HealthProfileSetupEffect.ShowSnackbar -> {
                    val message = effect.messageStr
                        ?: effect.messageResId?.let { context.getString(it) }
                        ?: ""
                    snackbarHostState.showSnackbar(message = message)
                }
            }
        }
    }

    HealthProfileSetupScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState
    )
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HealthProfileSetupScreenContent(
    state: HealthProfileSetupState,
    onEvent: (HealthProfileSetupEvent) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val isDark = isSystemInDarkTheme()
    val edgeRes = if (isDark) R.drawable.edge_dark else R.drawable.edge_light
    val heartRes = if (isDark) R.drawable.heart_dark else R.drawable.hearts_light

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                AppSnackbar(message = data.visuals.message)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Background SVG Decorations
            Image(
                painter = painterResource(edgeRes),
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            Image(
                painter = painterResource(heartRes),
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomStart)
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(72.dp))

                // Title
                Text(
                    text = stringResource(R.string.profile_setup_title),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 35.sp,
                    color = AppTheme.colors.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subtitle
                Text(
                    text = stringResource(R.string.profile_setup_subtitle),
                    fontFamily = LexendDeca,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = AppTheme.colors.TextSecondary
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Chronic Conditions Title
                Text(
                    text = stringResource(R.string.profile_setup_chronic_conditions),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = AppTheme.colors.TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Chronic Conditions FlowRow
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    state.chronicConditions.forEach { condition ->
                        val isSelected = state.selectedChronicConditions.contains(condition)
                        SelectableChip(
                            text = getConditionDisplayName(condition),
                            isSelected = isSelected,
                            onClick = { onEvent(HealthProfileSetupEvent.ToggleCondition(condition)) }
                        )
                    }

                    OtherInputChip(
                        isEditing = state.isAddingCustomCondition,
                        inputValue = state.customConditionInput,
                        onValueChange = { onEvent(HealthProfileSetupEvent.UpdateCustomConditionInput(it)) },
                        onStartEditing = { onEvent(HealthProfileSetupEvent.StartAddCustomCondition) },
                        onSubmit = { onEvent(HealthProfileSetupEvent.SubmitCustomCondition) },
                        onCancel = { onEvent(HealthProfileSetupEvent.CancelAddCustomCondition) },
                        placeholder = stringResource(R.string.profile_setup_other)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Allergies Title
                Text(
                    text = stringResource(R.string.profile_setup_allergies),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = AppTheme.colors.TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Allergies FlowRow
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    state.allergies.forEach { allergy ->
                        val isSelected = state.selectedAllergies.contains(allergy)
                        SelectableChip(
                            text = getAllergyDisplayName(allergy),
                            isSelected = isSelected,
                            onClick = { onEvent(HealthProfileSetupEvent.ToggleAllergy(allergy)) }
                        )
                    }

                    OtherInputChip(
                        isEditing = state.isAddingCustomAllergy,
                        inputValue = state.customAllergyInput,
                        onValueChange = { onEvent(HealthProfileSetupEvent.UpdateCustomAllergyInput(it)) },
                        onStartEditing = { onEvent(HealthProfileSetupEvent.StartAddCustomAllergy) },
                        onSubmit = { onEvent(HealthProfileSetupEvent.SubmitCustomAllergy) },
                        onCancel = { onEvent(HealthProfileSetupEvent.CancelAddCustomAllergy) },
                        placeholder = stringResource(R.string.profile_setup_other)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Save Action Button
                AppButton(
                    textResId = R.string.action_save,
                    isLoading = state.isLoading,
                    onClick = { onEvent(HealthProfileSetupEvent.SaveProfile) }
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun getConditionDisplayName(condition: String): String {
    return when (condition) {
        "Diabetes" -> stringResource(R.string.profile_setup_diabetes)
        "Hypertension" -> stringResource(R.string.profile_setup_hypertension)
        "Celiac Disease" -> stringResource(R.string.profile_setup_celiac)
        else -> condition
    }
}

@Composable
private fun getAllergyDisplayName(allergy: String): String {
    return when (allergy) {
        "Peanuts" -> stringResource(R.string.profile_setup_peanuts)
        "Gluten" -> stringResource(R.string.profile_setup_gluten)
        "Dairy" -> stringResource(R.string.profile_setup_dairy)
        else -> allergy
    }
}

@Preview(name = "Health Profile Setup — Light", showBackground = true)
@Composable
private fun HealthProfileSetupLightPreview() {
    AppTheme(darkTheme = false) {
        HealthProfileSetupScreenContent(
            state = HealthProfileSetupState(),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "Health Profile Setup — Dark", showBackground = true)
@Composable
private fun HealthProfileSetupDarkPreview() {
    AppTheme(darkTheme = true) {
        HealthProfileSetupScreenContent(
            state = HealthProfileSetupState(),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

