package iti.grad.nutriscan.presentation.settings.profile.edit.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TextButton
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import iti.grad.nutriscan.presentation.common.components.AppBackButton
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.components.ConfirmationDialog
import iti.grad.nutriscan.presentation.common.components.customShadow
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.profile.edit.state.EditProfileEffect
import iti.grad.nutriscan.presentation.settings.profile.edit.state.EditProfileEvent
import iti.grad.nutriscan.presentation.settings.profile.edit.state.EditProfileState
import iti.grad.nutriscan.presentation.settings.profile.edit.view.components.EditProfileInputField
import iti.grad.nutriscan.presentation.settings.profile.edit.view.components.EditProfileMeasurementField
import iti.grad.nutriscan.presentation.settings.profile.edit.viewmodel.EditProfileViewModel
import iti.grad.presentation.R

/**
 * Edit Profile Screen Composable.
 * Allows the authenticated user to modify their profile data and safety-critical health profile.
 */
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                EditProfileEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.onEvent(EditProfileEvent.SelectAvatar(uri.toString()))
            }
        }
    )

    EditProfileContent(
        state = state,
        onEvent = viewModel::onEvent,
        onSelectAvatarClick = {
            photoPickerLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileContent(
    state: EditProfileState,
    onEvent: (EditProfileEvent) -> Unit,
    onSelectAvatarClick: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Scaffold(
        containerColor = AppTheme.colors.Background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp)
            ) {
                AppButton(
                    textResId = if (state.isEditMode) R.string.action_save else R.string.action_edit,
                    isLoading = state.isLoading,
                    onClick = { 
                        if (state.isEditMode) onEvent(EditProfileEvent.SaveClicked)
                        else onEvent(EditProfileEvent.EditClicked)
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Scrollable fields container
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(56.dp))

                // Top row with Back Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppBackButton(
                        onClick = { onEvent(EditProfileEvent.BackClicked) },
                        iconTint = AppTheme.colors.Teal1000,
                        borderColor = AppTheme.colors.Teal1000
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Avatar and basic Info section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Picture with Edit badge overlay
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .customShadow(
                                shape = CircleShape,
                                color = AppTheme.colors.ShadowSelected.copy(alpha = 0.1f),
                                blurRadius = 8f
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer green border circle
                        Box(
                            modifier = Modifier
                                .size(98.dp)
                                .border(width = 1.dp, color = AppTheme.colors.Teal1000, shape = CircleShape)
                        )

                        // Inner avatar image / placeholder icon
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(AppTheme.colors.ProfileHeaderAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.avatarUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(state.avatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = stringResource(R.string.user_profile_avatar_description),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_person_solid),
                                    contentDescription = stringResource(R.string.user_profile_avatar_description),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Circular Pencil Button Overlay at Top-Right with White Border
                        if (state.isEditMode) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .border(width = 2.dp, color = Color.White, shape = CircleShape)
                                    .clip(CircleShape)
                                    .background(AppTheme.colors.Teal1000)
                                    .align(Alignment.TopEnd)
                                    .clickable { onSelectAvatarClick() }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_pen),
                                    contentDescription = stringResource(R.string.user_profile_edit_description),
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        val displayName = "${state.firstName} ${state.lastName}".trim()
                        Text(
                            text = if (displayName.isNotEmpty()) displayName else stringResource(R.string.edit_profile_first_name_hint),
                            style = AppTheme.typography.headlineLarge,
                            color = AppTheme.colors.Teal1000
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.email,
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.ProfileSetupSubtitle
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Input fields
                EditProfileInputField(
                    value = state.firstName,
                    onValueChange = { onEvent(EditProfileEvent.UpdateFirstName(it)) },
                    hint = stringResource(R.string.edit_profile_first_name_hint),
                    trailingIconRes = R.drawable.ic_edit,
                    isReadOnly = !state.isEditMode
                )

                Spacer(modifier = Modifier.height(12.dp))

                EditProfileInputField(
                    value = state.lastName,
                    onValueChange = { onEvent(EditProfileEvent.UpdateLastName(it)) },
                    hint = stringResource(R.string.edit_profile_last_name_hint),
                    trailingIconRes = R.drawable.ic_edit,
                    isReadOnly = !state.isEditMode
                )

                Spacer(modifier = Modifier.height(12.dp))

                EditProfileInputField(
                    value = state.dateOfBirth,
                    onValueChange = { onEvent(EditProfileEvent.UpdateDateOfBirth(it)) },
                    hint = stringResource(R.string.edit_profile_dob_hint),
                    trailingIconRes = if (state.isEditMode) R.drawable.ic_date else R.drawable.ic_lock,
                    isReadOnly = !state.isEditMode,
                    onClick = { if (state.isEditMode) showDatePicker = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EditProfileMeasurementField(
                        label = stringResource(R.string.edit_profile_height_label),
                        value = state.heightCm?.toString()?.removeSuffix(".0") ?: "",
                        onValueChange = {
                            val doubleValue = it.toDoubleOrNull()
                            if (it.isEmpty() || doubleValue != null) {
                                onEvent(EditProfileEvent.UpdateHeight(doubleValue))
                            }
                        },
                        unit = "cm",
                        isReadOnly = !state.isEditMode,
                        modifier = Modifier.weight(1f)
                    )

                    EditProfileMeasurementField(
                        label = stringResource(R.string.edit_profile_weight_label),
                        value = state.weightKg?.toString()?.removeSuffix(".0") ?: "",
                        onValueChange = {
                            val doubleValue = it.toDoubleOrNull()
                            if (it.isEmpty() || doubleValue != null) {
                                onEvent(EditProfileEvent.UpdateWeight(doubleValue))
                            }
                        },
                        unit = "kg",
                        isReadOnly = !state.isEditMode,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Chronic Conditions Section Title
                Text(
                    text = stringResource(R.string.profile_setup_chronic_conditions),
                    style = AppTheme.typography.headlineMedium,
                    color = AppTheme.colors.ProfileSetupSectionTitle
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Chronic Conditions FlowRow
                when {
                    state.isDiseasesLoading -> {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AppTheme.colors.Primary,
                            strokeWidth = 2.dp
                        )
                    }
                    state.diseasesErrorMessage != null -> {
                        Column {
                            Text(
                                text = stringResource(R.string.profile_setup_load_error),
                                style = AppTheme.typography.bodyMedium,
                                color = AppTheme.colors.Error
                            )
                            TextButton(onClick = { onEvent(EditProfileEvent.RetryLoadDiseases) }) {
                                Text(
                                    text = stringResource(R.string.action_retry),
                                    style = AppTheme.typography.labelLarge,
                                    color = AppTheme.colors.Primary
                                )
                            }
                        }
                    }
                    else -> {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            state.diseases.forEach { disease ->
                                val isSelected = state.selectedDiseaseIds.contains(disease.id)
                                if (state.isEditMode || isSelected) {
                                    iti.grad.nutriscan.presentation.profile_setup.view.components.SelectableChip(
                                        text = disease.name,
                                        isSelected = isSelected,
                                        enabled = state.isEditMode,
                                        onClick = { onEvent(EditProfileEvent.ToggleDisease(disease.id)) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Allergies Section Title
                Text(
                    text = stringResource(R.string.profile_setup_allergies),
                    style = AppTheme.typography.headlineMedium,
                    color = AppTheme.colors.ProfileSetupSectionTitle
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Allergies FlowRow
                when {
                    state.isAllergiesLoading -> {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AppTheme.colors.Primary,
                            strokeWidth = 2.dp
                        )
                    }
                    state.allergiesErrorMessage != null -> {
                        Column {
                            Text(
                                text = stringResource(R.string.profile_setup_load_error),
                                style = AppTheme.typography.bodyMedium,
                                color = AppTheme.colors.Error
                            )
                            TextButton(onClick = { onEvent(EditProfileEvent.RetryLoadAllergies) }) {
                                Text(
                                    text = stringResource(R.string.action_retry),
                                    style = AppTheme.typography.labelLarge,
                                    color = AppTheme.colors.Primary
                                )
                            }
                        }
                    }
                    else -> {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            state.allergies.forEach { allergy ->
                                val isSelected = state.selectedAllergyIds.contains(allergy.id)
                                if (state.isEditMode || isSelected) {
                                    iti.grad.nutriscan.presentation.profile_setup.view.components.SelectableChip(
                                        text = allergy.name,
                                        isSelected = isSelected,
                                        enabled = state.isEditMode,
                                        onClick = { onEvent(EditProfileEvent.ToggleAllergy(allergy.id)) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                        onEvent(EditProfileEvent.UpdateDateOfBirth(date))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Safety Confirmation Dialog on Save
    if (state.showSaveConfirmation) {
        ConfirmationDialog(
            title = stringResource(R.string.edit_profile_confirm_title),
            message = stringResource(R.string.edit_profile_confirm_message),
            confirmLabel = stringResource(R.string.action_save),
            cancelLabel = stringResource(R.string.action_cancel),
            onConfirm = { onEvent(EditProfileEvent.ConfirmSave) },
            onDismiss = { onEvent(EditProfileEvent.DismissSaveConfirmation) }
        )
    }
}

