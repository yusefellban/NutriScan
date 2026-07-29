package iti.grad.nutriscan.presentation.settings.help.view

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.common.components.AppTopHeader
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.help.state.HelpEffect
import iti.grad.nutriscan.presentation.settings.help.state.HelpEvent
import iti.grad.nutriscan.presentation.settings.help.state.HelpState
import iti.grad.nutriscan.presentation.settings.help.view.components.FaqAccordionItem
import iti.grad.nutriscan.presentation.settings.help.view.components.FeedbackDialog
import iti.grad.nutriscan.presentation.settings.help.view.components.HelpContactSection
import iti.grad.nutriscan.presentation.settings.help.viewmodel.HelpViewModel
import iti.grad.presentation.BuildConfig
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HelpScreen(
    viewModel: HelpViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val contactSubject = stringResource(R.string.help_contact_support_subject)
    val feedbackSubject = stringResource(R.string.help_feedback_subject)

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is HelpEffect.NavigateBack -> onNavigateBack()
                is HelpEffect.OpenEmail -> {
                    val subject = if (effect.body.isEmpty()) contactSubject else feedbackSubject
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(effect.recipient))
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, effect.body)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }
            }
        }
    }

    HelpContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
private fun HelpContent(
    state: HelpState,
    onEvent: (HelpEvent) -> Unit,
) {
    Scaffold(containerColor = AppTheme.colors.Background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AppTheme.colors.Background)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            // Wrapped in a Teal1000 background box: AppTopHeader renders white text,
            // which needs a dark backdrop for contrast (matches AppSettingsHeader's pattern).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.colors.Teal1000),
            ) {
                AppTopHeader(
                    title = stringResource(R.string.app_settings_help),
                    onBackClick = { onEvent(HelpEvent.BackClicked) },
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.help_faq_section_title),
                        style = AppTheme.typography.titleMedium,
                        color = AppTheme.colors.AppSettingsRowLabel,
                    )
                    state.faqItems.forEach { item ->
                        FaqAccordionItem(
                            item = item,
                            expanded = state.expandedFaqId == item.id,
                            onClick = { onEvent(HelpEvent.FaqItemClicked(item.id)) },
                        )
                    }
                }

                HelpContactSection(
                    onContactSupportClick = { onEvent(HelpEvent.ContactSupportClicked) },
                    onSendFeedbackClick = { onEvent(HelpEvent.SendFeedbackClicked) },
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.help_credits_team),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.Gray500,
                    )
                    Text(
                        text = stringResource(R.string.help_credits_version, BuildConfig.VERSION_NAME),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.Gray500,
                    )
                }
            }
        }
    }

    if (state.showFeedbackDialog) {
        FeedbackDialog(
            feedbackText = state.feedbackText,
            onTextChanged = { onEvent(HelpEvent.FeedbackTextChanged(it)) },
            onSubmit = { onEvent(HelpEvent.FeedbackSubmitClicked) },
            onDismiss = { onEvent(HelpEvent.FeedbackDismissed) },
        )
    }
}
