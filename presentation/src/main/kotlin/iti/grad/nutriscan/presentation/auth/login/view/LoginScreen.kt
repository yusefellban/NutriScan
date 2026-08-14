package iti.grad.nutriscan.presentation.auth.login.view

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import iti.grad.nutriscan.domain.auth.model.AuthTokens
import iti.grad.nutriscan.presentation.auth.login.state.LoginEffect
import iti.grad.nutriscan.presentation.auth.login.state.LoginEvent
import iti.grad.nutriscan.presentation.auth.login.state.LoginState
import iti.grad.nutriscan.presentation.auth.login.view.components.LoginFormBody
import iti.grad.nutriscan.presentation.auth.login.viewmodel.LoginViewModel
import iti.grad.nutriscan.presentation.common.components.AuthHeader
import iti.grad.nutriscan.presentation.common.components.ErrorAlert
import iti.grad.nutriscan.presentation.common.components.InternetAlert
import iti.grad.nutriscan.presentation.common.components.SuccessAlert
import iti.grad.nutriscan.presentation.common.components.WarningAlert
import iti.grad.nutriscan.presentation.common.state.AuthAlertState
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToProfileSetup: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToAccountPendingDeletion: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val authService = remember { AuthorizationService(context) }
    
    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        val response = AuthorizationResponse.fromIntent(data)
        val ex = AuthorizationException.fromIntent(data)
        
        if (response != null) {
            authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, tokenEx ->
                if (tokenResponse != null) {
                    val authTokens = AuthTokens(
                        accessToken = tokenResponse.accessToken ?: "",
                        refreshToken = tokenResponse.refreshToken,
                        idToken = tokenResponse.idToken,
                        expiresIn = if (tokenResponse.accessTokenExpirationTime != null) {
                            (tokenResponse.accessTokenExpirationTime!! - System.currentTimeMillis()) / 1000
                        } else null,
                        refreshExpiresIn = null 
                    )
                    viewModel.onEvent(LoginEvent.GoogleLoginSuccess(authTokens))
                } else {
                    viewModel.onEvent(LoginEvent.GoogleLoginFailure(tokenEx?.errorDescription ?: tokenEx?.message ?: "Token exchange failed"))
                }
            }
        } else {
            viewModel.onEvent(LoginEvent.GoogleLoginFailure(ex?.errorDescription ?: ex?.message ?: "Authorization failed"))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is LoginEffect.NavigateToHome -> onNavigateToHome()
                is LoginEffect.NavigateToProfileSetup -> onNavigateToProfileSetup()
                is LoginEffect.NavigateToRegister -> onNavigateToRegister()
                is LoginEffect.NavigateToForgotPassword -> onNavigateToForgotPassword()
                is LoginEffect.NavigateToAccountPendingDeletion -> {
                    onNavigateToAccountPendingDeletion(effect.scheduledDeletionAt)
                }
                is LoginEffect.LaunchGoogleLogin -> {
                    val serviceConfig = AuthorizationServiceConfiguration(
                        Uri.parse(effect.config.authorizationEndpoint),
                        Uri.parse(effect.config.tokenEndpoint)
                    )
                    val authRequest = AuthorizationRequest.Builder(
                        serviceConfig,
                        effect.config.clientId,
                        ResponseTypeValues.CODE,
                        Uri.parse(effect.config.redirectUri)
                    ).setScopes("openid", "profile", "email")
                     .setAdditionalParameters(mapOf("kc_idp_hint" to "google"))
                     .build()
                     
                    val intent = authService.getAuthorizationRequestIntent(authRequest)
                    authLauncher.launch(intent)
                }
            }
        }
    }

    when (val alert = state.alertState) {
        is AuthAlertState.InternetError -> {
            InternetAlert(
                onRetry = { viewModel.onEvent(LoginEvent.RetryAction) },
                onDismiss = { viewModel.onEvent(LoginEvent.DismissAlert) }
            )
        }
        is AuthAlertState.Error -> {
            ErrorAlert(
                title = stringResource(id = R.string.alert_login_failed_title),
                message = alert.message.asString(),
                onDismiss = { viewModel.onEvent(LoginEvent.DismissAlert) }
            )
        }
        is AuthAlertState.Warning -> {
            WarningAlert(
                title = stringResource(id = R.string.alert_login_failed_title),
                message = alert.message.asString(),
                onDismiss = { viewModel.onEvent(LoginEvent.DismissAlert) }
            )
        }
        is AuthAlertState.Success -> {
            SuccessAlert(
                title = stringResource(id = R.string.alert_success_title),
                message = alert.message.asString(),
                onDismiss = { viewModel.onEvent(LoginEvent.DismissAlert) }
            )
        }
        is AuthAlertState.None -> Unit
    }

    LoginScreenContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun LoginScreenContent(
    state: LoginState,
    onEvent: (LoginEvent) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Teal header panel ──────────────────────────────────────────
            AuthHeader(titleResId = R.string.login_title)

            Spacer(modifier = Modifier.height(32.dp))

            // ── Form fields live directly on the page (no white card) ──────
            LoginFormBody(
                state = state,
                onEvent = onEvent
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
