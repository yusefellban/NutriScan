package iti.grad.nutriscan.presentation.settings.profile.edit.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import iti.grad.nutriscan.presentation.common.components.AppBackButton
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.components.ConfirmationDialog
import iti.grad.nutriscan.presentation.common.components.customShadow
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans
import iti.grad.nutriscan.presentation.profile_setup.view.components.OtherInputChip
import iti.grad.nutriscan.presentation.profile_setup.view.components.SelectableChip
import iti.grad.nutriscan.presentation.settings.profile.edit.state.EditProfileEffect
import iti.grad.nutriscan.presentation.settings.profile.edit.state.EditProfileEvent
import iti.grad.nutriscan.presentation.settings.profile.edit.state.EditProfileState
import iti.grad.nutriscan.presentation.settings.profile.edit.view.components.EditProfileInputField
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

    EditProfileContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileContent(
    state: EditProfileState,
    onEvent: (EditProfileEvent) -> Unit
) {
    Scaffold(
        containerColor = AppTheme.colors.Background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp)
            ) {
                AppButton(
                    textResId = R.string.action_save,
                    isLoading = state.isLoading,
                    onClick = { onEvent(EditProfileEvent.SaveClicked) }
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
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .border(width = 2.dp, color = Color.White, shape = CircleShape)
                                .clip(CircleShape)
                                .background(AppTheme.colors.Teal1000)
                                .align(Alignment.TopEnd)
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

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = if (state.name.isNotEmpty()) state.name else "Yousef Elban",
                            style = AppTheme.typography.headlineLarge,
                            color = AppTheme.colors.Teal1000
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (state.email.isNotEmpty()) state.email else "yousefelaban@gmail.com",
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.ProfileSetupSubtitle
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Input fields
                EditProfileInputField(
                    value = state.name,
                    onValueChange = { onEvent(EditProfileEvent.UpdateName(it)) },
                    hint = stringResource(R.string.edit_profile_name_hint),
                    trailingIconRes = R.drawable.ic_edit
                )

                Spacer(modifier = Modifier.height(12.dp))

                EditProfileInputField(
                    value = state.username,
                    onValueChange = { onEvent(EditProfileEvent.UpdateUsername(it)) },
                    hint = stringResource(R.string.edit_profile_username_hint),
                    trailingIconRes = R.drawable.ic_edit
                )

                Spacer(modifier = Modifier.height(12.dp))

                EditProfileInputField(
                    value = state.email,
                    onValueChange = { onEvent(EditProfileEvent.UpdateEmail(it)) },
                    hint = stringResource(R.string.edit_profile_email_hint),
                    trailingIconRes = R.drawable.ic_email
                )

                Spacer(modifier = Modifier.height(12.dp))

                EditProfileInputField(
                    value = state.password,
                    onValueChange = { onEvent(EditProfileEvent.UpdatePassword(it)) },
                    hint = stringResource(R.string.edit_profile_password_hint),
                    trailingIconRes = R.drawable.ic_lock,
                    isPassword = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Chronic Conditions Section Title
                Text(
                    text = stringResource(R.string.profile_setup_chronic_conditions),
                    style = AppTheme.typography.headlineMedium,
                    color = AppTheme.colors.ProfileSetupSectionTitle
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                            onClick = { onEvent(EditProfileEvent.ToggleCondition(condition)) }
                        )
                    }

                    OtherInputChip(
                        isEditing = state.isAddingCustomCondition,
                        inputValue = state.customConditionInput,
                        onValueChange = { onEvent(EditProfileEvent.UpdateCustomConditionInput(it)) },
                        onStartEditing = { onEvent(EditProfileEvent.StartAddCustomCondition) },
                        onSubmit = { onEvent(EditProfileEvent.SubmitCustomCondition) },
                        onCancel = { onEvent(EditProfileEvent.CancelAddCustomCondition) },
                        placeholder = stringResource(R.string.profile_setup_other)
                    )
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
                            onClick = { onEvent(EditProfileEvent.ToggleAllergy(allergy)) }
                        )
                    }

                    OtherInputChip(
                        isEditing = state.isAddingCustomAllergy,
                        inputValue = state.customAllergyInput,
                        onValueChange = { onEvent(EditProfileEvent.UpdateCustomAllergyInput(it)) },
                        onStartEditing = { onEvent(EditProfileEvent.StartAddCustomAllergy) },
                        onSubmit = { onEvent(EditProfileEvent.SubmitCustomAllergy) },
                        onCancel = { onEvent(EditProfileEvent.CancelAddCustomAllergy) },
                        placeholder = stringResource(R.string.profile_setup_other)
                    )
                }

                Spacer(modifier = Modifier.height(120.dp))
            }
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
