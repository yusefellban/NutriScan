package iti.grad.nutriscan.presentation.settings.profile.edit.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import coil3.compose.AsyncImage
import iti.grad.nutriscan.presentation.common.components.customShadow
import iti.grad.nutriscan.presentation.common.components.rememberAvatarImageRequest
import iti.grad.nutriscan.presentation.settings.profile.edit.state.AvatarUploadState
import iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.Success
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Icon
import androidx.compose.ui.res.stringResource
import iti.grad.nutriscan.presentation.settings.profile.edit.state.EditProfileEffect
import iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.Warning
import androidx.compose.material3.DatePicker
import androidx.compose.foundation.layout.FlowRow
import iti.grad.nutriscan.presentation.profile_setup.view.components.SelectableChip
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.InternetError
import iti.grad.presentation.R
import androidx.compose.material3.DatePickerDialog
import androidx.compose.foundation.layout.Spacer
import iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.None
import iti.grad.nutriscan.presentation.settings.profile.edit.state.EditProfileState
import androidx.compose.ui.Alignment
import iti.grad.nutriscan.presentation.common.components.SuccessAlert
import iti.grad.nutriscan.presentation.common.components.AppBackButton
import androidx.activity.result.contract.ActivityResultContracts
import iti.grad.nutriscan.presentation.settings.profile.edit.state.EditProfileEvent
import iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.Error
import iti.grad.nutriscan.presentation.common.components.ConfirmationDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import iti.grad.nutriscan.presentation.common.components.ErrorAlert
import androidx.compose.ui.graphics.Color
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Image
import iti.grad.nutriscan.presentation.common.components.InternetAlert
import androidx.compose.foundation.layout.Arrangement
import iti.grad.nutriscan.presentation.settings.profile.edit.viewmodel.EditProfileViewModel
import iti.grad.nutriscan.presentation.common.components.AppButton
import androidx.compose.runtime.Composable
import iti.grad.nutriscan.presentation.common.components.ActionConfirmAlert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import iti.grad.nutriscan.presentation.settings.profile.edit.view.components.EditProfileMeasurementField
import iti.grad.nutriscan.presentation.settings.profile.edit.view.components.EditProfileInputField
import iti.grad.nutriscan.presentation.common.components.WarningAlert
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text

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
                viewModel.onEvent(EditProfileEvent.SelectAvatar(uri))
            }
        }
    )

    EditProfileContent(
        state = state,
        onEvent = viewModel::onEvent,
        onSelectAvatarClick = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(
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
                            val avatarRequest = rememberAvatarImageRequest(
                                avatarUrl = state.avatarUrl,
                                avatarUpdatedAt = state.avatarUpdatedAt
                            )
                            if (avatarRequest != null) {
                                AsyncImage(
                                    model = avatarRequest,
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

                            // Uploading overlay — shown while a newly picked photo is being sent to the server.
                            if (state.avatarUploadState is AvatarUploadState.Uploading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }

                        // Circular Pencil Button Overlay at Top-Right with White Border
                        if (state.isEditMode) {
                            val isUploading = state.avatarUploadState is AvatarUploadState.Uploading
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .border(width = 2.dp, color = Color.White, shape = CircleShape)
                                    .clip(CircleShape)
                                    .background(if (isUploading) AppTheme.colors.Teal1000.copy(alpha = 0.5f) else AppTheme.colors.Teal1000)
                                    .align(Alignment.TopEnd)
                                    .clickable(enabled = !isUploading) { onSelectAvatarClick() }
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
                        CircularProgressIndicator(
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
                                    SelectableChip(
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
                        CircularProgressIndicator(
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
                                    SelectableChip(
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
        ActionConfirmAlert(
            title = stringResource(R.string.edit_profile_confirm_title),
            message = stringResource(R.string.edit_profile_confirm_message),
            confirmText = stringResource(R.string.action_save),
            cancelText = stringResource(R.string.action_cancel),
            onConfirm = { onEvent(EditProfileEvent.ConfirmSave) },
            onDismiss = { onEvent(EditProfileEvent.DismissSaveConfirmation) }
        )
    }

    when (val alert = state.alertState) {
        is InternetError -> {
            InternetAlert(
                onRetry = { onEvent(EditProfileEvent.RetryAction) },
                onDismiss = { onEvent(EditProfileEvent.DismissAlert) }
            )
        }
        is Error -> {
            ErrorAlert(
                title = stringResource(id = R.string.alert_error_title),
                message = alert.messageStr ?: alert.messageResId?.let { stringResource(id = it) } ?: "",
                onDismiss = { onEvent(EditProfileEvent.DismissAlert) }
            )
        }
        is Warning -> {
            WarningAlert(
                title = stringResource(id = R.string.alert_warning_title),
                message = alert.messageStr ?: alert.messageResId?.let { stringResource(id = it) } ?: "",
                onDismiss = { onEvent(EditProfileEvent.DismissAlert) }
            )
        }
        is Success -> {
            SuccessAlert(
                title = stringResource(id = R.string.alert_success_title),
                message = alert.messageStr ?: alert.messageResId?.let { stringResource(id = it) } ?: "",
                onDismiss = { onEvent(EditProfileEvent.DismissAlert) }
            )
        }
        is None -> Unit
    }

    // Avatar upload failure — kept separate from the alertState above since it can
    // surface independently of the Save flow (upload starts as soon as a photo is picked).
    if (state.avatarUploadState is AvatarUploadState.Error) {
        InternetAlert(
            title = stringResource(id = R.string.alert_error_title),
            message = stringResource(id = R.string.edit_profile_avatar_upload_error),
            onRetry = { onEvent(EditProfileEvent.RetryAvatarUpload) },
            onDismiss = { onEvent(EditProfileEvent.DismissAvatarUploadError) }
        )
    }
}

