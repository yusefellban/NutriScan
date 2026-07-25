package iti.grad.nutriscan.presentation.auth.register.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.auth.register.state.RegisterEffect
import iti.grad.nutriscan.presentation.auth.register.state.RegisterEvent
import iti.grad.nutriscan.presentation.auth.register.state.RegisterState
import iti.grad.nutriscan.presentation.auth.register.view.components.RegisterFormBody
import iti.grad.nutriscan.presentation.auth.register.viewmodel.RegisterViewModel
import iti.grad.nutriscan.presentation.common.components.AuthHeader
import iti.grad.nutriscan.presentation.common.components.ErrorAlert
import iti.grad.nutriscan.presentation.common.components.InternetAlert
import iti.grad.nutriscan.presentation.common.components.SuccessAlert
import iti.grad.nutriscan.presentation.common.components.WarningAlert
import iti.grad.nutriscan.presentation.common.state.AuthAlertState
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans
import iti.grad.presentation.R

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = hiltViewModel(),
    onNavigateToEmailVerification: (String) -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is RegisterEffect.NavigateToEmailVerification -> onNavigateToEmailVerification(effect.email)
                is RegisterEffect.NavigateToSignIn -> onNavigateToSignIn()
            }
        }
    }

    when (val alert = state.alertState) {
        is AuthAlertState.InternetError -> {
            InternetAlert(
                onRetry = { viewModel.onEvent(RegisterEvent.RetryAction) },
                onDismiss = { viewModel.onEvent(RegisterEvent.DismissAlert) }
            )
        }
        is AuthAlertState.Error -> {
            ErrorAlert(
                title = stringResource(id = R.string.alert_registration_failed_title),
                message = alert.message.asString(),
                onDismiss = { viewModel.onEvent(RegisterEvent.DismissAlert) }
            )
        }
        is AuthAlertState.Warning -> {
            WarningAlert(
                title = stringResource(id = R.string.alert_registration_failed_title),
                message = alert.message.asString(),
                onDismiss = { viewModel.onEvent(RegisterEvent.DismissAlert) }
            )
        }
        is AuthAlertState.Success -> {
            SuccessAlert(
                title = stringResource(id = R.string.alert_success_title),
                message = alert.message.asString(),
                onDismiss = { viewModel.onEvent(RegisterEvent.DismissAlert) }
            )
        }
        is AuthAlertState.None -> Unit
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AuthHeader(titleResId = R.string.signup_title)

            Spacer(modifier = Modifier.height(32.dp))

            RegisterFormBody(
                state = state,
                onEvent = viewModel::onEvent
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

