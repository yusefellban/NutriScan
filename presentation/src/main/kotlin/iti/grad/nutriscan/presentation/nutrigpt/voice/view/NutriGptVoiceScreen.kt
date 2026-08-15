package iti.grad.nutriscan.presentation.nutrigpt.voice.view

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import iti.grad.nutriscan.presentation.common.components.AppBackButton
import iti.grad.nutriscan.presentation.common.components.BackButtonSurface
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.nutrigpt.chat.state.ChatLanguage
import iti.grad.nutriscan.presentation.nutrigpt.voice.state.NutriGptVoiceEffect
import iti.grad.nutriscan.presentation.nutrigpt.voice.state.NutriGptVoiceEvent
import iti.grad.nutriscan.presentation.nutrigpt.voice.view.components.VoiceWaveform
import iti.grad.nutriscan.presentation.nutrigpt.voice.viewmodel.NutriGptVoiceViewModel
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.util.rememberAudioPermissionRequester
import iti.grad.nutriscan.presentation.common.util.tick
import iti.grad.nutriscan.presentation.settings.app.view.components.SettingsSegmentedToggle
import iti.grad.presentation.R

@Composable
fun NutriGptVoiceScreen(
    onNavigateBack: () -> Unit,
    viewModel: NutriGptVoiceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    fun checkAudioPermission() = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    var hasAudioPermission by remember { mutableStateOf(checkAudioPermission()) }

    val requestAudioPermission = rememberAudioPermissionRequester(
        onGranted = {
            hasAudioPermission = true
            viewModel.onEvent(NutriGptVoiceEvent.SetListeningState(true))
        },
        onDenied = {
            Toast.makeText(context, "Microphone permission is required", Toast.LENGTH_SHORT).show()
        }
    )

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

    // Re-check on entry rather than auto-requesting — a placeholder card below lets the
    // user opt in via "Grant permission" instead of the system dialog firing immediately.
    LaunchedEffect(Unit) {
        hasAudioPermission = checkAudioPermission()
        if (hasAudioPermission) {
            viewModel.onEvent(NutriGptVoiceEvent.SetListeningState(true))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppTheme.colors.ChatScreenBackground,
                        AppTheme.colors.ChatScreenBackgroundEnd
                    )
                )
            )
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
                AppBackButton(
                    onClick = { viewModel.onNavigateBack() },
                    surface = BackButtonSurface.OnLight,
                )

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
                color = AppTheme.colors.PrimaryVariant,
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

            if (!hasAudioPermission) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.nutrigpt_voice_permission_denied),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    AppButton(
                        textResId = R.string.nutrigpt_voice_permission_retry,
                        isLoading = false,
                        onClick = { requestAudioPermission() },
                    )
                }
            } else {
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

                // Main Action Button (Mic / Fast Forward)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.ChatSendButtonBackground)
                        .clickable {
                            haptics.tick()
                            when {
                                state.isListening -> viewModel.onEvent(NutriGptVoiceEvent.SetListeningState(false))
                                state.isPlaying || state.isGenerating -> viewModel.onEvent(NutriGptVoiceEvent.StopPlaying)
                                else -> viewModel.onEvent(NutriGptVoiceEvent.SetListeningState(true))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isPlaying || state.isGenerating) Icons.Rounded.FastForward else Icons.Rounded.Mic,
                        contentDescription = "Action",
                        tint = AppTheme.colors.ChatSendButtonIcon,
                        modifier = Modifier.size(36.dp)
                    )
                }
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
