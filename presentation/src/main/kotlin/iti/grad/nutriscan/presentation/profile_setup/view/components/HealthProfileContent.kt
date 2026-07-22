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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.domain.allergy.model.Allergy
import iti.grad.nutriscan.domain.disease.model.Disease
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

            // Chronic Conditions (Diseases) Section Title
            Text(
                text = stringResource(R.string.profile_setup_chronic_conditions),
                style = AppTheme.typography.headlineMedium.copy(lineHeight = 30.sp),
                color = AppTheme.colors.ProfileSetupSectionTitle
            )

            Spacer(modifier = Modifier.height(16.dp))

            DiseasesSection(
                diseases = state.diseases,
                selectedDiseaseIds = state.selectedDiseaseIds,
                isLoading = state.isDiseasesLoading,
                errorMessage = state.diseasesErrorMessage,
                onToggle = { onEvent(ProfileSetupPagerEvent.ToggleDisease(it)) },
                onRetry = { onEvent(ProfileSetupPagerEvent.RetryLoadDiseases) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Allergies Section Title
            Text(
                text = stringResource(R.string.profile_setup_allergies),
                style = AppTheme.typography.headlineMedium.copy(lineHeight = 30.sp),
                color = AppTheme.colors.ProfileSetupSectionTitle
            )

            Spacer(modifier = Modifier.height(16.dp))

            AllergiesSection(
                allergies = state.allergies,
                selectedAllergyIds = state.selectedAllergyIds,
                isLoading = state.isAllergiesLoading,
                errorMessage = state.allergiesErrorMessage,
                onToggle = { onEvent(ProfileSetupPagerEvent.ToggleAllergy(it)) },
                onRetry = { onEvent(ProfileSetupPagerEvent.RetryLoadAllergies) }
            )

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiseasesSection(
    diseases: List<Disease>,
    selectedDiseaseIds: List<Int>,
    isLoading: Boolean,
    errorMessage: String?,
    onToggle: (Int) -> Unit,
    onRetry: () -> Unit
) {
    when {
        isLoading -> LoadingRow()
        errorMessage != null -> ErrorRow(onRetry = onRetry)
        else -> FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            diseases.forEach { disease ->
                val isSelected = selectedDiseaseIds.contains(disease.id)
                SelectableChip(
                    text = disease.name,
                    isSelected = isSelected,
                    onClick = { onToggle(disease.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllergiesSection(
    allergies: List<Allergy>,
    selectedAllergyIds: List<Int>,
    isLoading: Boolean,
    errorMessage: String?,
    onToggle: (Int) -> Unit,
    onRetry: () -> Unit
) {
    when {
        isLoading -> LoadingRow()
        errorMessage != null -> ErrorRow(onRetry = onRetry)
        else -> FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            allergies.forEach { allergy ->
                val isSelected = selectedAllergyIds.contains(allergy.id)
                SelectableChip(
                    text = allergy.name,
                    isSelected = isSelected,
                    onClick = { onToggle(allergy.id) }
                )
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    CircularProgressIndicator(
        modifier = Modifier.size(24.dp),
        color = AppTheme.colors.Primary,
        strokeWidth = 2.dp
    )
}

@Composable
private fun ErrorRow(onRetry: () -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.profile_setup_load_error),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.Error
        )
        TextButton(onClick = onRetry) {
            Text(
                text = stringResource(R.string.action_retry),
                style = AppTheme.typography.labelLarge,
                color = AppTheme.colors.Primary
            )
        }
    }
}
