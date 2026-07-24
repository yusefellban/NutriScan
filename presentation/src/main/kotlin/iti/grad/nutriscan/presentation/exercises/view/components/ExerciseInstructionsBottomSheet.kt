package iti.grad.nutriscan.presentation.exercises.view.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.model.ExerciseUiModel
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseInstructionsBottomSheet(
    exercise: ExerciseUiModel,
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    onReadMoreClick: () -> Unit,
    onStartWorkoutClick: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppTheme.colors.Surface,
        scrimColor = Color(0x660F474A),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 20.dp), // Reduced top padding
        ) {
            // Exercise header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_exercise_person),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(id = exercise.nameRes),
                        style = AppTheme.typography.titleSmall,
                        color = AppTheme.colors.ExerciseCardTitle
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            id = R.string.exercise_equipment_target,
                            stringResource(id = exercise.equipmentRes),
                            stringResource(id = exercise.targetRes)
                        ),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.ExerciseCardSubtitle
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Instructions title
            Text(
                text = stringResource(id = R.string.exercise_instructions_title),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.ExerciseInstructionsTitle
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Instructions body with bullet dot in a Row so text doesn't wrap under the dot
            val instructionsText = stringResource(id = exercise.instructionsRes)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "•",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.ExerciseInstructionsBullet
                )
                
                if (isExpanded) {
                    Text(
                        text = instructionsText,
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.ExerciseInstructionsBody
                    )
                } else {
                    val readMoreLabel = stringResource(id = R.string.action_read_more)
                    val truncatedText = if (instructionsText.length > 120) {
                        instructionsText.take(120) + "... "
                    } else {
                        instructionsText + " "
                    }
                    val annotated = buildAnnotatedString {
                        append(truncatedText)
                        withStyle(SpanStyle(color = AppTheme.colors.ExerciseReadMoreColor, fontWeight = FontWeight.Bold)) {
                            append(readMoreLabel)
                        }
                    }
                    Text(
                        text = annotated,
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.ExerciseInstructionsBody,
                        modifier = Modifier.clickable { onReadMoreClick() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Start Workout button
            AppButton(
                textResId = R.string.action_start_workout,
                isLoading = false,
                onClick = onStartWorkoutClick
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
