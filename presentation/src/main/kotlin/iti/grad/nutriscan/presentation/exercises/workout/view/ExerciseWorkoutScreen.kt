package iti.grad.nutriscan.presentation.exercises.workout.view
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.BackHandler
import iti.grad.nutriscan.presentation.common.components.ActionConfirmAlert

import androidx.compose.foundation.Image
import coil3.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.style.TextAlign
import iti.grad.nutriscan.presentation.common.components.AppBackButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.Arrangement
import iti.grad.nutriscan.presentation.exercises.workout.viewmodel.ExerciseWorkoutViewModel
import androidx.compose.ui.text.input.KeyboardType
import iti.grad.nutriscan.presentation.common.theme.ExerciseWorkoutTypography
import iti.grad.nutriscan.presentation.exercises.workout.state.ExerciseWorkoutEffect
import iti.grad.nutriscan.presentation.common.components.AppButton
import androidx.compose.foundation.layout.Box
import iti.grad.nutriscan.presentation.exercises.workout.state.ExerciseWorkoutEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import iti.grad.nutriscan.presentation.common.model.ExerciseType

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
    var showBackWarningDialog by remember { mutableStateOf(false) }
    var showCancelWarningDialog by remember { mutableStateOf(false) }
    var showRestartWarningDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = state.hasStarted) {
        showBackWarningDialog = true
    }

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
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        color = AppTheme.colors.Teal1000
                    )
                }
                state.errorMessageRes != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(state.errorMessageRes!!),
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.onEvent(ExerciseWorkoutEvent.OnRetryInitClick) },
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.Primary)
                        ) {
                            Text(text = stringResource(id = R.string.action_retry))
                        }
                    }
                }
                else -> {
                    CircularProgressIndicator(
                        color = AppTheme.colors.Teal1000
                    )
                }
            }
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
                onClick = {
                    if (state.hasStarted) {
                        showBackWarningDialog = true
                    } else {
                        viewModel.onEvent(ExerciseWorkoutEvent.OnBackClick)
                    }
                },
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
            text = exercise.name,
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
            AsyncImage(
                model = exercise.gifUrl ?: exercise.imageUrl,
                contentDescription = null,
                placeholder = painterResource(id = R.drawable.dumbell),
                error = painterResource(id = R.drawable.dumbell),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(320.dp)
                    .padding(28.dp)
                    .clip(CircleShape)
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
                    modifier = Modifier.clickable {
                        if (state.hasStarted) {
                            showCancelWarningDialog = true
                        } else {
                            viewModel.onEvent(ExerciseWorkoutEvent.OnCancelClick)
                        }
                    }
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
                        onClick = {
                            if (state.hasStarted) {
                                showRestartWarningDialog = true
                            } else {
                                viewModel.onEvent(ExerciseWorkoutEvent.OnRestartClick)
                            }
                        },
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
                    modifier = Modifier.clickable {
                        if (state.hasStarted) {
                            showCancelWarningDialog = true
                        } else {
                            viewModel.onEvent(ExerciseWorkoutEvent.OnCancelClick)
                        }
                    }
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

    // Back Button Discard Warning Dialog
    if (showBackWarningDialog) {
        ActionConfirmAlert(
            title = stringResource(id = R.string.exercise_discard_confirm_title),
            message = stringResource(id = R.string.exercise_discard_confirm_message),
            confirmText = stringResource(id = R.string.exercise_discard_confirm_action),
            cancelText = stringResource(id = R.string.onboarding_back),
            onConfirm = {
                showBackWarningDialog = false
                viewModel.onEvent(ExerciseWorkoutEvent.OnBackClick)
            },
            onDismiss = { showBackWarningDialog = false }
        )
    }

    // Cancel Workout Warning Dialog
    if (showCancelWarningDialog) {
        ActionConfirmAlert(
            title = stringResource(id = R.string.exercise_discard_confirm_title),
            message = stringResource(id = R.string.exercise_discard_confirm_message),
            confirmText = stringResource(id = R.string.exercise_discard_confirm_action),
            cancelText = stringResource(id = R.string.onboarding_back),
            onConfirm = {
                showCancelWarningDialog = false
                viewModel.onEvent(ExerciseWorkoutEvent.OnCancelClick)
            },
            onDismiss = { showCancelWarningDialog = false }
        )
    }

    // Restart Workout Dialog
    if (showRestartWarningDialog) {
        ActionConfirmAlert(
            title = stringResource(id = R.string.exercise_restart_confirm_title),
            message = stringResource(id = R.string.exercise_restart_confirm_message),
            confirmText = stringResource(id = R.string.exercise_restart_confirm_action),
            cancelText = stringResource(id = R.string.onboarding_back),
            onConfirm = {
                showRestartWarningDialog = false
                viewModel.onEvent(ExerciseWorkoutEvent.OnRestartClick)
            },
            onDismiss = { showRestartWarningDialog = false }
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
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = ExerciseWorkoutTypography.setsRepsLabel,
            color = AppTheme.colors.ExerciseSetsRepsLabelColor,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable { onValueClick() }
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                    tint = AppTheme.colors.Teal1000,
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
                    tint = AppTheme.colors.Teal1000,
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
    val isDark = AppTheme.isDark
    val disabledContainerColor = if (isDark) {
        AppTheme.colors.Teal1000.copy(alpha = 0.15f)
    } else {
        AppTheme.colors.Teal1000.copy(alpha = 0.4f)
    }
    val disabledContentColor = if (isDark) {
        AppTheme.colors.ExerciseWorkoutPrimaryButtonText.copy(alpha = 0.3f)
    } else {
        AppTheme.colors.ExerciseWorkoutPrimaryButtonText.copy(alpha = 0.6f)
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colors.Teal1000,
            contentColor = AppTheme.colors.ExerciseWorkoutPrimaryButtonText,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
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
            containerColor = Color.Transparent,
            contentColor = AppTheme.colors.Teal1000,
            disabledContentColor = AppTheme.colors.Teal1000.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            width = 1.dp, 
            color = if (enabled) AppTheme.colors.Teal1000 else AppTheme.colors.Teal1000.copy(alpha = 0.4f)
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
    val context = LocalContext.current
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        CompositionLocalProvider(LocalContext provides context) {
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
                        .background(AppTheme.colors.Background)
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

                    val formattedCalories = String.format(LocalLocale.current.platformLocale, "%d", caloriesBurned)
                    Text(
                        text = stringResource(id = R.string.exercise_result_message, formattedCalories),
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
        containerColor = AppTheme.colors.Background
    )
}



