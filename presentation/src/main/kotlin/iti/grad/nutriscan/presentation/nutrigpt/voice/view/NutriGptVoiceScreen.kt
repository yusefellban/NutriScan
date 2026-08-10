package iti.grad.nutriscan.presentation.nutrigpt.voice.view

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.nutrigpt.chat.state.ChatLanguage
import iti.grad.nutriscan.presentation.nutrigpt.voice.state.NutriGptVoiceEffect
import iti.grad.nutriscan.presentation.nutrigpt.voice.state.NutriGptVoiceEvent
import iti.grad.nutriscan.presentation.nutrigpt.voice.view.components.VoiceWaveform
import iti.grad.nutriscan.presentation.nutrigpt.voice.viewmodel.NutriGptVoiceViewModel
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.settings.app.view.components.SettingsSegmentedToggle
import iti.grad.presentation.R

@Composable
fun NutriGptVoiceScreen(
    onNavigateBack: () -> Unit,
    viewModel: NutriGptVoiceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NutriGptVoiceEffect.NavigateBack -> onNavigateBack()
                is NutriGptVoiceEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                // Speak and StopSpeaking effects are no longer handled in UI
                else -> {}
            }
        }
    }

    // Request permission on startup if not granted
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.Background)
            .padding(
                top = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding(),
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, AppTheme.colors.Primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown()
                                viewModel.onNavigateBack()
                                do {
                                    val event = awaitPointerEvent()
                                } while (event.changes.any { it.pressed })
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = AppTheme.colors.Primary
                    )
                }

                // AR / EN Toggle
                SettingsSegmentedToggle(
                    options = listOf("En", "Ar"),
                    selectedIndex = if (state.chatLanguage == ChatLanguage.EN) 0 else 1,
                    onOptionSelected = { index ->
                        val selected = if (index == 0) ChatLanguage.EN else ChatLanguage.AR
                        if (state.chatLanguage != selected) {
                            viewModel.onEvent(NutriGptVoiceEvent.ToggleLanguage)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Official Sources Header
            Text(
                text = stringResource(id = R.string.nutrigpt_voice_official_sources),
                color = AppTheme.colors.Primary,
                style = AppTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Source Icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SourceImageIcon("world_health.png")
                SourceImageIcon("health_ministry.png")
                SourceImageIcon("food_authority.png")
            }

            Spacer(modifier = Modifier.weight(1f))

            // Waveform
            VoiceWaveform(
                isAnimating = state.isListening || state.isPlaying || state.isGenerating,
                isRtl = state.chatLanguage == ChatLanguage.AR
            )

            Spacer(modifier = Modifier.weight(1f))

            // Status Text
            val statusText = when {
                state.isGenerating -> stringResource(id = R.string.nutrigpt_voice_generating)
                state.isListening -> if (state.currentQuery.isNotBlank()) state.currentQuery else stringResource(id = R.string.nutrigpt_listening_hint)
                else -> ""
            }
            
            val isHintText = state.isGenerating || (state.isListening && state.currentQuery.isBlank())
            val textAlign = if (isHintText) {
                TextAlign.Center
            } else {
                TextAlign.Start
            }

            Text(
                text = statusText,
                color = AppTheme.colors.PrimaryVariant,
                style = AppTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                textAlign = textAlign,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            )

            // Main Action Button (Mic)
            val animatedScale by animateFloatAsState(
                targetValue = if (state.isListening) 1f + (state.speechVolume.coerceIn(0f, 10f) / 30f) else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "mic_scale"
            )

            Box(
                modifier = Modifier
                    .scale(animatedScale)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.Primary)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            if (hasPermission) {
                                if (state.isPlaying || state.isGenerating) {
                                    viewModel.onEvent(NutriGptVoiceEvent.StopPlaying)
                                }
                                viewModel.onEvent(NutriGptVoiceEvent.SetListeningState(true))
                                
                                do {
                                    val event = awaitPointerEvent()
                                } while (event.changes.any { it.pressed })
                                
                                viewModel.onEvent(NutriGptVoiceEvent.SetListeningState(false))
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                do {
                                    val event = awaitPointerEvent()
                                } while (event.changes.any { it.pressed })
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Action",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Bottom Button
            Box(modifier = Modifier.padding(vertical = 16.dp)) {
                AppButton(
                    textResId = R.string.nutrigpt_voice_switch_text,
                    isLoading = false,
                    onClick = { viewModel.onNavigateBack() }
                )
            }
        }
    }
}

@Composable
fun SourceImageIcon(assetName: String) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = "file:///android_asset/$assetName",
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}
