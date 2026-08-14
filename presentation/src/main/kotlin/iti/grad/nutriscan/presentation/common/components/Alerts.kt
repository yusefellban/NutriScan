package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

@Composable
fun ActionConfirmAlert(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String,
    message: String,
    confirmText: String,
    cancelText: String = stringResource(id = R.string.alert_button_cancel)
) {
    CustomAlertDialog(
        title = title,
        message = message,
        icon = painterResource(id = R.drawable.warning_ic),

        onDismiss = onDismiss
    ) {
        AlertButton(
            text = cancelText,
            backgroundColor = AppTheme.colors.HeightUnselectedCardBackground,
            textColor = AppTheme.colors.TextPrimary,
            onClick = onDismiss
        )
        AlertButton(
            text = confirmText,
            backgroundColor = AppTheme.colors.Teal1000,
            textColor = AppTheme.colors.OnPrimary,
            onClick = onConfirm
        )
    }
}

@Composable
fun DeleteWarningAlert(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(id = R.string.alert_delete_warning_title),
    message: String = stringResource(id = R.string.alert_test_message),
    confirmText: String = stringResource(id = R.string.alert_button_delete),
    cancelText: String = stringResource(id = R.string.alert_button_cancel)
) {
    CustomAlertDialog(
        title = title,
        message = message,
        icon = painterResource(id = R.drawable.ic_trash),
        iconBackgroundColor = AppTheme.colors.AppSettingsLogoutAccent,
        iconContentColor = Color.White,
        onDismiss = onDismiss
    ) {
        AlertButton(
            text = cancelText,
            backgroundColor = AppTheme.colors.HeightUnselectedCardBackground,
            textColor = AppTheme.colors.TextPrimary,
            onClick = onDismiss
        )
        AlertButton(
            text = confirmText,
            backgroundColor = AppTheme.colors.AppSettingsLogoutAccent,
            textColor = Color.White,
            onClick = onConfirm
        )
    }
}

@Composable
fun LogoutAlert(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String,
    message: String,
    confirmText: String,
    cancelText: String = stringResource(id = R.string.alert_button_cancel)
) {
    CustomAlertDialog(
        title = title,
        message = message,
        icon = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.AutoMirrored.Filled.Logout),
        iconBackgroundColor = AppTheme.colors.AppSettingsLogoutAccent,
        iconContentColor = AppTheme.colors.Background,
        onDismiss = onDismiss
    ) {
        AlertButton(
            text = cancelText,
            backgroundColor = AppTheme.colors.HeightUnselectedCardBackground,
            textColor = AppTheme.colors.TextPrimary,
            onClick = onDismiss
        )
        AlertButton(
            text = confirmText,
            backgroundColor = AppTheme.colors.AppSettingsLogoutAccent,
            textColor = AppTheme.colors.Background,
            onClick = onConfirm
        )
    }
}

@Composable
fun SuccessAlert(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(id = R.string.alert_success_title),
    message: String = stringResource(id = R.string.alert_test_message),
    confirmText: String = stringResource(id = R.string.alert_button_ok),
    cancelText: String = stringResource(id = R.string.alert_button_cancel)
) {
    CustomAlertDialog(
        title = title,
        message = message,
        icon = painterResource(id = R.drawable.success_ic),

        onDismiss = onDismiss
    ) {
        AlertButton(
            text = cancelText,
            backgroundColor = AppTheme.colors.HeightUnselectedCardBackground,
            textColor = AppTheme.colors.TextPrimary,
            onClick = onDismiss
        )
        AlertButton(
            text = confirmText,
            backgroundColor = AppTheme.colors.Primary,
            textColor = AppTheme.colors.OnPrimary,
            onClick = onConfirm
        )
    }
}

@Composable
fun SuccessAlert(
    onDismiss: () -> Unit,
    title: String = stringResource(id = R.string.alert_success_title),
    message: String = stringResource(id = R.string.alert_test_message),
    confirmText: String = stringResource(id = R.string.alert_button_ok)
) {
    CustomAlertDialog(
        title = title,
        message = message,
        icon = painterResource(id = R.drawable.success_ic),

        onDismiss = onDismiss
    ) {
        AlertButton(
            text = confirmText,
            backgroundColor = AppTheme.colors.HeightUnselectedCardBackground,
            textColor = AppTheme.colors.TextPrimary,
            onClick = onDismiss
        )
    }
}

@Composable
fun ErrorAlert(
    onDismiss: () -> Unit,
    title: String = stringResource(id = R.string.alert_error_title),
    message: String = stringResource(id = R.string.alert_test_message),
    confirmText: String = stringResource(id = R.string.alert_button_ok)
) {
    CustomAlertDialog(
        title = title,
        message = message,
        icon = painterResource(id = R.drawable.error_ic),

        onDismiss = onDismiss
    ) {
        AlertButton(
            text = confirmText,
            backgroundColor = AppTheme.colors.HeightUnselectedCardBackground,
            textColor = AppTheme.colors.TextPrimary,
            onClick = onDismiss
        )
    }
}

@Composable
fun WarningAlert(
    onDismiss: () -> Unit,
    title: String = stringResource(id = R.string.alert_warning_title),
    message: String = stringResource(id = R.string.alert_test_message),
    confirmText: String = stringResource(id = R.string.alert_button_ok)
) {
    CustomAlertDialog(
        title = title,
        message = message,
        icon = painterResource(id = R.drawable.warning_ic),

        onDismiss = onDismiss
    ) {
        AlertButton(
            text = confirmText,
            backgroundColor = AppTheme.colors.HeightUnselectedCardBackground,
            textColor = AppTheme.colors.TextPrimary,
            onClick = onDismiss
        )
    }
}

@Composable
fun InternetAlert(
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(id = R.string.alert_internet_title),
    message: String = stringResource(id = R.string.alert_internet_message),
    retryText: String = stringResource(id = R.string.alert_button_retry)
) {
    CustomAlertDialog(
        title = title,
        message = message,
        icon = painterResource(id = R.drawable.internet_error_ic),

        onDismiss = onDismiss
    ) {
        AlertButton(
            text = retryText,
            backgroundColor = AppTheme.colors.HeightUnselectedCardBackground,
            textColor = AppTheme.colors.TextPrimary,
            onClick = onRetry
        )
    }
}

// Previews
@Preview(showBackground = true)
@Composable
private fun DeleteWarningAlertPreview() {
    AppTheme { DeleteWarningAlert({}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun DeleteWarningAlertDarkPreview() {
    AppTheme(darkTheme = true) { DeleteWarningAlert({}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun SuccessAlertPreview() {
    AppTheme { SuccessAlert({}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun SuccessAlertDarkPreview() {
    AppTheme(darkTheme = true) { SuccessAlert({}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun SuccessAlertSinglePreview() {
    AppTheme { SuccessAlert({}) }
}

@Preview(showBackground = true)
@Composable
private fun SuccessAlertSingleDarkPreview() {
    AppTheme(darkTheme = true) { SuccessAlert({}) }
}

@Preview(showBackground = true)
@Composable
private fun ErrorAlertPreview() {
    AppTheme { ErrorAlert({}) }
}

@Preview(showBackground = true)
@Composable
private fun ErrorAlertDarkPreview() {
    AppTheme(darkTheme = true) { ErrorAlert({}) }
}

@Preview(showBackground = true)
@Composable
private fun WarningAlertPreview() {
    AppTheme { WarningAlert({}) }
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
private fun WarningAlertDarkPreview() {
    AppTheme(darkTheme = true) { WarningAlert({}) }
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
private fun InternetAlertPreview() {
    AppTheme { InternetAlert({}, {}) }
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
private fun InternetAlertDarkPreview() {
    AppTheme(darkTheme = true) { InternetAlert({}, {}) }
}

