package iti.grad.nutriscan.presentation.exercises.workout.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.common.components.AppBackButton
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.model.ExerciseType
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.ExerciseWorkoutTypography
import iti.grad.nutriscan.presentation.exercises.workout.state.ExerciseWorkoutEffect
import iti.grad.nutriscan.presentation.exercises.workout.state.ExerciseWorkoutEvent
import iti.grad.nutriscan.presentation.exercises.workout.viewmodel.ExerciseWorkoutViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest

import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing

@Composable
fun ExerciseWorkoutScreen(
    exerciseId: String,
    viewModel: ExerciseWorkoutViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToCalories: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    }

    var showSetsDialog by remember { mutableStateOf(false) }
    var showRepsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(exerciseId) {
        viewModel.onEvent(ExerciseWorkoutEvent.InitExercise(exerciseId))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                ExerciseWorkoutEffect.NavigateBack -> onNavigateBack()
                ExerciseWorkoutEffect.NavigateToCaloriesDashboard -> onNavigateToCalories()
            }
        }
    }

    val exercise = state.exercise
    if (exercise == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.Background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Loading exercise...",
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colors.TextPrimary
            )
        }
        return
    }

    Scaffold(
        containerColor = AppTheme.colors.Background,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AppBackButton(
                onClick = { viewModel.onEvent(ExerciseWorkoutEvent.OnBackClick) },
                iconTint = AppTheme.colors.ExerciseBackButtonTint,
                borderColor = AppTheme.colors.ExerciseBackButtonTint
            )
            Text(
                text = stringResource(id = R.string.exercise_workout_title),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.ExerciseWorkoutHeaderTitle,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Exercise Name
        Text(
            text = stringResource(id = exercise.nameRes),
            style = ExerciseWorkoutTypography.exerciseName,
            color = AppTheme.colors.ExerciseWorkoutHeaderTitle,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Exercise circular preview - Big, close to screen width (340.dp container, 320.dp circle)
        // with internal padding on the image so it sits spacious and premium inside the card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
            contentAlignment = Alignment.Center
        ) {
            // Circular teal backdrop
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.ExerciseWorkoutImageBackground)
            )

            // Exercise pose image inside the circle with spacious internal padding to prevent clipping and look premium
            Image(
                painter = painterResource(id = R.drawable.img_exercise_person),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(320.dp)
                    .padding(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Timer Area
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.hasStarted && !state.isTimerRunning) {
                Text(
                    text = stringResource(id = R.string.exercise_total_time_label),
                    style = ExerciseWorkoutTypography.totalTimeLabel,
                    color = AppTheme.colors.ExerciseWorkoutTotalTimeLabel
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            // Sets/Reps and timer style differ depending on running vs paused (finish) state below
            Text(
                text = formatTime(state.secondsElapsed),
                style = ExerciseWorkoutTypography.timerText,
                color = AppTheme.colors.ExerciseWorkoutTimerText
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sets / Reps (only for normal workout, and only after Pause/Finish state is reached)
        if (exercise.type == ExerciseType.NORMAL_WORKOUT && (state.hasStarted && !state.isTimerRunning)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sets Card
                WorkoutControlCard(
                    label = stringResource(id = R.string.exercise_sets_label),
                    value = state.sets,
                    onIncrement = { viewModel.onEvent(ExerciseWorkoutEvent.OnSetIncrement) },
                    onDecrement = { viewModel.onEvent(ExerciseWorkoutEvent.OnSetDecrement) },
                    onValueClick = { showSetsDialog = true },
                    modifier = Modifier.weight(1f)
                )

                // Reps Card
                WorkoutControlCard(
                    label = stringResource(id = R.string.exercise_reps_label),
                    value = state.reps,
                    onIncrement = { viewModel.onEvent(ExerciseWorkoutEvent.OnRepIncrement) },
                    onDecrement = { viewModel.onEvent(ExerciseWorkoutEvent.OnRepDecrement) },
                    onValueClick = { showRepsDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }
            // 5- Make sets/reps very close to the finish button
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Action Buttons Column
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!state.hasStarted) {
                // Initial State: Two horizontal buttons: Start (active, outlined) and Pause (disabled, filled)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WorkoutSecondaryButton(
                        text = stringResource(id = R.string.exercise_start_button), // Named Start
                        onClick = { viewModel.onEvent(ExerciseWorkoutEvent.OnStartResumeClick) },
                        modifier = Modifier.weight(1f),
                        enabled = true
                    )
                    WorkoutPrimaryButton(
                        text = stringResource(id = R.string.exercise_pause_button),
                        onClick = { },
                        modifier = Modifier.weight(1f),
                        enabled = false // Disabled
                    )
                }
                Spacer(modifier = Modifier.height(24.dp)) // increased vertical space
                Text(
                    text = stringResource(id = R.string.exercise_cancel_workout),
                    style = ExerciseWorkoutTypography.controlButtonLabel,
                    color = AppTheme.colors.ExerciseCancelWorkoutText,
                    modifier = Modifier.clickable { viewModel.onEvent(ExerciseWorkoutEvent.OnCancelClick) }
                )
            } else if (state.isTimerRunning) {
                // Running State: Restart (outlined) and Pause (filled)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WorkoutSecondaryButton(
                        text = stringResource(id = R.string.exercise_restart_button), // Named Restart
                        onClick = { viewModel.onEvent(ExerciseWorkoutEvent.OnRestartClick) },
                        modifier = Modifier.weight(1f),
                        enabled = true
                    )
                    WorkoutPrimaryButton(
                        text = stringResource(id = R.string.exercise_pause_button),
                        onClick = { viewModel.onEvent(ExerciseWorkoutEvent.OnPauseClick) },
                        modifier = Modifier.weight(1f),
                        enabled = true
                    )
                }
                Spacer(modifier = Modifier.height(24.dp)) // increased vertical space
                Text(
                    text = stringResource(id = R.string.exercise_cancel_workout),
                    style = ExerciseWorkoutTypography.controlButtonLabel,
                    color = AppTheme.colors.ExerciseCancelWorkoutText,
                    modifier = Modifier.clickable { viewModel.onEvent(ExerciseWorkoutEvent.OnCancelClick) }
                )
            } else {
                // Paused / Finish State: restart, pause, and cancel buttons are REMOVED.
                // The Finish button appears in their position instead of down!
                AppButton(
                    textResId = R.string.exercise_finish_button,
                    isLoading = false,
                    onClick = { viewModel.onEvent(ExerciseWorkoutEvent.OnFinishClick) }
                )
            }
        }
    }
    }

    // Congratulations Dialog
    if (state.showCongratsDialog) {
        CongratsDialog(
            caloriesBurned = state.caloriesBurned,
            onConfirm = { viewModel.onEvent(ExerciseWorkoutEvent.OnCongratsDialogConfirm) }
        )
    }

    // Direct Input Dialogs
    if (showSetsDialog) {
        InputDialog(
            title = stringResource(id = R.string.exercise_sets_label),
            initialValue = state.sets.toString(),
            onDismiss = { showSetsDialog = false },
            onConfirm = {
                it.toIntOrNull()?.let { sets ->
                    viewModel.onEvent(ExerciseWorkoutEvent.OnSetChange(sets))
                }
                showSetsDialog = false
            }
        )
    }

    if (showRepsDialog) {
        InputDialog(
            title = stringResource(id = R.string.exercise_reps_label),
            initialValue = state.reps.toString(),
            onDismiss = { showRepsDialog = false },
            onConfirm = {
                it.toIntOrNull()?.let { reps ->
                    viewModel.onEvent(ExerciseWorkoutEvent.OnRepChange(reps))
                }
                showRepsDialog = false
            }
        )
    }
}

@Composable
fun WorkoutControlCard(
    label: String,
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onValueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AppTheme.colors.ExerciseSetsRepsCardBg)
            .border(width = 1.dp, color = AppTheme.colors.ExerciseSetsRepsCardBorder, shape = RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = ExerciseWorkoutTypography.setsRepsLabel,
            color = AppTheme.colors.ExerciseSetsRepsLabelColor,
            modifier = Modifier.clickable { onValueClick() }
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Minus Button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.ExerciseSetsRepsBtnBg)
                    .clickable { onDecrement() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_minus),
                    contentDescription = "Decrement",
                    tint = AppTheme.colors.ExerciseSetsRepsIconTint,
                    modifier = Modifier.size(12.dp)
                )
            }

            Text(
                text = value.toString(),
                style = AppTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = AppTheme.colors.ExerciseSetsRepsValueColor,
                modifier = Modifier.clickable { onValueClick() }
            )

            // Plus Button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.ExerciseSetsRepsBtnBg)
                    .clickable { onIncrement() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "Increment",
                    tint = AppTheme.colors.ExerciseSetsRepsIconTint,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun WorkoutPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colors.ExerciseWorkoutPrimaryButtonBg,
            contentColor = AppTheme.colors.ExerciseWorkoutPrimaryButtonText,
            disabledContainerColor = AppTheme.colors.ExerciseWorkoutPrimaryButtonBg.copy(alpha = 0.4f),
            disabledContentColor = AppTheme.colors.ExerciseWorkoutPrimaryButtonText.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(56.dp)
    ) {
        Text(
            text = text,
            style = ExerciseWorkoutTypography.controlButtonLabel
        )
    }
}

@Composable
fun WorkoutSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = AppTheme.colors.ExerciseWorkoutSecondaryButton,
            disabledContentColor = AppTheme.colors.ExerciseWorkoutSecondaryButton.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp, 
            color = if (enabled) AppTheme.colors.ExerciseWorkoutSecondaryButton else AppTheme.colors.ExerciseWorkoutSecondaryButton.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(56.dp)
    ) {
        Text(
            text = text,
            style = ExerciseWorkoutTypography.controlButtonLabel
        )
    }
}

@Composable
fun CongratsDialog(
    caloriesBurned: Int,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppTheme.colors.AuthDialogBackground)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_fire_solid),
                    contentDescription = null,
                    tint = AppTheme.colors.Warning,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.exercise_result_title),
                    style = AppTheme.typography.titleLarge,
                    color = AppTheme.colors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.exercise_result_message, caloriesBurned),
                    style = AppTheme.typography.bodyLarge,
                    color = AppTheme.colors.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                AppButton(
                    textResId = R.string.exercise_result_confirm,
                    isLoading = false,
                    onClick = onConfirm
                )
            }
        }
    }
}

@Composable
fun InputDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.TextPrimary
            )
        },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text(stringResource(id = R.string.exercise_enter_value)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(textValue) }
            ) {
                Text(
                    text = stringResource(id = R.string.exercise_result_confirm),
                    color = AppTheme.colors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.onboarding_back),
                    color = AppTheme.colors.TextSecondary
                )
            }
        },
        containerColor = AppTheme.colors.AuthDialogBackground
    )
}

