package iti.grad.nutriscan.presentation.profile_setup.view.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.profile_setup.state.ProfileSetupPagerEvent
import iti.grad.nutriscan.presentation.profile_setup.state.ProfileSetupPagerState
import iti.grad.presentation.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthProfileContent(
    state: ProfileSetupPagerState,
    onEvent: (ProfileSetupPagerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val edgeRes = if (isDark) R.drawable.edge_dark else R.drawable.edge_light
    val heartRes = if (isDark) R.drawable.heart_dark else R.drawable.hearts_light

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Heart background SVG decoration in top-left
        Image(
            painter = painterResource(heartRes),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .absolutePadding(left = 22.dp, top = 21.dp)
                .size(width = 150.dp, height = 228.dp)
        )

        // Edge background SVG decoration in top-right
        Image(
            painter = painterResource(edgeRes),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // Downward spacing shift to avoid top status bar overlap
            Spacer(modifier = Modifier.height(185.dp))

            // Title
            Text(
                text = stringResource(R.string.profile_setup_title),
                style = AppTheme.typography.displaySmall.copy(lineHeight = 35.sp),
                color = AppTheme.colors.ProfileSetupTitle
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = stringResource(R.string.profile_setup_subtitle),
                style = AppTheme.typography.bodyMedium.copy(lineHeight = 18.sp),
                color = AppTheme.colors.ProfileSetupSubtitle
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Chronic Conditions Section Title
            Text(
                text = stringResource(R.string.profile_setup_chronic_conditions),
                style = AppTheme.typography.headlineMedium.copy(lineHeight = 30.sp),
                color = AppTheme.colors.ProfileSetupSectionTitle
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
                        onClick = { onEvent(ProfileSetupPagerEvent.ToggleCondition(condition)) }
                    )
                }

                OtherInputChip(
                    isEditing = state.isAddingCustomCondition,
                    inputValue = state.customConditionInput,
                    onValueChange = { onEvent(ProfileSetupPagerEvent.UpdateCustomConditionInput(it)) },
                    onStartEditing = { onEvent(ProfileSetupPagerEvent.StartAddCustomCondition) },
                    onSubmit = { onEvent(ProfileSetupPagerEvent.SubmitCustomCondition) },
                    onCancel = { onEvent(ProfileSetupPagerEvent.CancelAddCustomCondition) },
                    placeholder = stringResource(R.string.profile_setup_other)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Allergies Section Title
            Text(
                text = stringResource(R.string.profile_setup_allergies),
                style = AppTheme.typography.headlineMedium.copy(lineHeight = 30.sp),
                color = AppTheme.colors.ProfileSetupSectionTitle
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
                        onClick = { onEvent(ProfileSetupPagerEvent.ToggleAllergy(allergy)) }
                    )
                }

                OtherInputChip(
                    isEditing = state.isAddingCustomAllergy,
                    inputValue = state.customAllergyInput,
                    onValueChange = { onEvent(ProfileSetupPagerEvent.UpdateCustomAllergyInput(it)) },
                    onStartEditing = { onEvent(ProfileSetupPagerEvent.StartAddCustomAllergy) },
                    onSubmit = { onEvent(ProfileSetupPagerEvent.SubmitCustomAllergy) },
                    onCancel = { onEvent(ProfileSetupPagerEvent.CancelAddCustomAllergy) },
                    placeholder = stringResource(R.string.profile_setup_other)
                )
            }

            // Spacing to keep content clear of the bottom Save button
            Spacer(modifier = Modifier.height(120.dp))
        }

        // Save Action Button container bottom-aligned
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        ) {
            AppButton(
                textResId = R.string.action_save,
                isLoading = state.isLoading,
                onClick = {
                    onEvent(ProfileSetupPagerEvent.SaveProfile)
                }
            )
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
