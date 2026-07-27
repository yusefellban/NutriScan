package iti.grad.nutriscan.presentation.settings.profile.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.components.ChipSelectionFlowRow
import iti.grad.nutriscan.presentation.common.components.ErrorAlert
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.profile.add_member.state.AddFamilyMemberEvent
import iti.grad.nutriscan.presentation.settings.profile.add_member.state.AddFamilyMemberState
import iti.grad.nutriscan.presentation.settings.profile.add_member.state.AddFamilyMemberEffect
import iti.grad.nutriscan.presentation.settings.profile.edit.view.components.EditProfileInputField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import iti.grad.presentation.R

/**
 * Bottom sheet for adding a family member: name + allergy/disease chip
 * selection, reusing [EditProfileInputField] (same visual family as the rest
 * of the Profile area) and the shared [ChipSelectionFlowRow] (also used by
 * `HealthProfileContent`). All colors come from [AppTheme.colors] — no
 * hardcoded hex, per SKILL.md §3 — including the new [AppTheme.colors.ScrimOverlay]
 * token instead of an ad-hoc `Color(0x66...)` scrim literal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFamilyMemberBottomSheet(
    state: AddFamilyMemberState,
    effectFlow: Flow<AddFamilyMemberEffect>,
    onEvent: (AddFamilyMemberEvent) -> Unit,
    onDismiss: () -> Unit,
    editingMemberId: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editingMemberId) {
        onEvent(AddFamilyMemberEvent.Initialize(editingMemberId))
    }

    LaunchedEffect(Unit) {
        effectFlow.collectLatest { effect ->
            when (effect) {
                AddFamilyMemberEffect.Dismiss -> onDismiss()
                is AddFamilyMemberEffect.ShowError -> errorMessage = effect.message
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppTheme.colors.Surface,
        scrimColor = AppTheme.colors.ScrimOverlay,
    ) {
        CompositionLocalProvider(LocalContext provides context) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
            Text(
                text = stringResource(
                    if (state.editingMemberId != null) R.string.edit_family_member_title
                    else R.string.add_family_member_title
                ),
                style = AppTheme.typography.headlineMedium,
                color = AppTheme.colors.Teal1000,
            )
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.ProfileAddMemberAvatarBackground)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_person_solid),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    EditProfileInputField(
                        value = state.name,
                        onValueChange = { onEvent(AddFamilyMemberEvent.NameChanged(it)) },
                        hint = stringResource(R.string.add_family_member_name_placeholder),
                    )
                    if (state.nameError != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(state.nameError),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.Error,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    EditProfileInputField(
                        value = state.relation,
                        onValueChange = { onEvent(AddFamilyMemberEvent.RelationChanged(it)) },
                        hint = stringResource(R.string.add_family_member_relation_placeholder),
                    )
                    if (state.relationError != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(state.relationError),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.Error,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.profile_setup_chronic_conditions),
                style = AppTheme.typography.headlineMedium,
                color = AppTheme.colors.ProfileSetupSectionTitle,
            )
            Spacer(Modifier.height(12.dp))
            ChipSelectionFlowRow(
                items = state.diseases,
                selectedIds = state.selectedDiseaseIds,
                isLoading = state.isDiseasesLoading,
                errorMessage = state.diseasesErrorMessage,
                idOf = { it.id },
                labelOf = { it.name },
                onToggle = { onEvent(AddFamilyMemberEvent.ToggleDisease(it)) },
                onRetry = { onEvent(AddFamilyMemberEvent.RetryLoadDiseases) },
            )
            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.profile_setup_allergies),
                style = AppTheme.typography.headlineMedium,
                color = AppTheme.colors.ProfileSetupSectionTitle,
            )
            Spacer(Modifier.height(12.dp))
            ChipSelectionFlowRow(
                items = state.allergies,
                selectedIds = state.selectedAllergyIds,
                isLoading = state.isAllergiesLoading,
                errorMessage = state.allergiesErrorMessage,
                idOf = { it.id },
                labelOf = { it.name },
                onToggle = { onEvent(AddFamilyMemberEvent.ToggleAllergy(it)) },
                onRetry = { onEvent(AddFamilyMemberEvent.RetryLoadAllergies) },
            )
            Spacer(Modifier.height(32.dp))

            AppButton(
                textResId = R.string.action_save,
                isLoading = state.isSaving,
                onClick = { onEvent(AddFamilyMemberEvent.SaveClicked) },
            )
            Spacer(Modifier.height(12.dp))
        }
        }

        errorMessage?.let { message ->
            ErrorAlert(
                title = stringResource(R.string.add_family_member_generic_error),
                message = message,
                onDismiss = { errorMessage = null },
            )
        }
    }
}
