package iti.grad.nutriscan.presentation.nutrigpt.chat.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.common.components.AppSnackbar
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.nutrigpt.chat.state.ChatLanguage
import iti.grad.nutriscan.presentation.nutrigpt.chat.state.NutriGptEffect
import iti.grad.nutriscan.presentation.nutrigpt.chat.state.NutriGptEvent
import iti.grad.nutriscan.presentation.nutrigpt.chat.view.components.ChatInputBar
import iti.grad.nutriscan.presentation.nutrigpt.chat.view.components.ChatMessageBubble
import iti.grad.nutriscan.presentation.nutrigpt.chat.view.components.ChatTopBar
import iti.grad.nutriscan.presentation.nutrigpt.chat.view.components.ChatTypingBubble
import iti.grad.nutriscan.presentation.nutrigpt.chat.view.components.NutriGptEmptyState
import iti.grad.nutriscan.presentation.nutrigpt.chat.viewmodel.NutriGptViewModel

@Composable
fun NutriGptScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVoice: () -> Unit,
    viewModel: NutriGptViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                viewModel.onEvent(NutriGptEvent.SetListeningState(false))
            }
            override fun onError(error: Int) {
                viewModel.onEvent(NutriGptEvent.SetListeningState(false))
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    viewModel.onEvent(NutriGptEvent.UpdateQuery(text))
                }
                viewModel.onEvent(NutriGptEvent.SetListeningState(false))
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    viewModel.onEvent(NutriGptEvent.UpdateQuery(text))
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer?.setRecognitionListener(listener)
        
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val langCode = if (state.chatLanguage == ChatLanguage.AR) "ar-EG" else "en-US"
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
            }
            speechRecognizer?.startListening(intent)
            viewModel.onEvent(NutriGptEvent.SetListeningState(true))
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NutriGptEffect.NavigateBack -> onNavigateBack()
                is NutriGptEffect.ShowError -> {
                    // Show error somehow, maybe toast or snackbar
                }
            }
        }
    }
    
    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
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
            .padding(top = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding())
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatTopBar(
                currentLanguage = state.chatLanguage,
                onNavigateBack = viewModel::onNavigateBack,
                onToggleLanguage = { viewModel.onEvent(NutriGptEvent.ToggleLanguage) },
                onVoiceIconClick = onNavigateToVoice
            )
            
            if (state.messages.isEmpty() && !state.isLoading) {
                NutriGptEmptyState(
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
                ) {
                    items(
                        items = state.messages,
                        key = { it.id }
                    ) { message ->
                        ChatMessageBubble(message = message)
                    }
                    
                    if (state.isLoading) {
                        item(key = "loading_bubble") {
                            ChatTypingBubble()
                        }
                    }
                }
            }
            
            ChatInputBar(
                query = state.currentQuery,
                isLoading = state.isLoading,
                isListening = state.isListening,
                onQueryChange = { viewModel.onEvent(NutriGptEvent.UpdateQuery(it)) },
                onSend = {
                    if (state.currentQuery.isNotBlank()) {
                        viewModel.onEvent(NutriGptEvent.SendMessage(state.currentQuery))
                    }
                },
                onMicPress = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    
                    if (hasPermission) {
                        val langCode = if (state.chatLanguage == ChatLanguage.AR) "ar-EG" else "en-US"
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
                        }
                        speechRecognizer?.startListening(intent)
                        viewModel.onEvent(NutriGptEvent.SetListeningState(true))
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onMicRelease = {
                    speechRecognizer?.stopListening()
                    // The UI will return to normal immediately, but we let onResults send the message
                    viewModel.onEvent(NutriGptEvent.SetListeningState(false))
                }
            )
        }
    }
}
