package iti.grad.nutriscan.presentation.settings.terms.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.components.AppTopHeader
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.terms.view.components.TermsSection
import iti.grad.presentation.R

@Composable
fun TermsAndConditionsScreen(
    onNavigateBack: () -> Unit = {},
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.colors.Teal1000),
            ) {
                AppTopHeader(
                    title = stringResource(R.string.app_settings_terms_and_conditions),
                    onBackClick = onNavigateBack,
                )
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                TermsSection(stringResource(R.string.terms_section_1_title), stringResource(R.string.terms_section_1_body))
                TermsSection(stringResource(R.string.terms_section_2_title), stringResource(R.string.terms_section_2_body))
                TermsSection(stringResource(R.string.terms_section_3_title), stringResource(R.string.terms_section_3_body))
                TermsSection(stringResource(R.string.terms_section_4_title), stringResource(R.string.terms_section_4_body))
                TermsSection(stringResource(R.string.terms_section_5_title), stringResource(R.string.terms_section_5_body))
                TermsSection(stringResource(R.string.terms_section_6_title), stringResource(R.string.terms_section_6_body))
                TermsSection(stringResource(R.string.terms_section_7_title), stringResource(R.string.terms_section_7_body))
                TermsSection(stringResource(R.string.terms_section_8_title), stringResource(R.string.terms_section_8_body))
            }
        }
    }
}
