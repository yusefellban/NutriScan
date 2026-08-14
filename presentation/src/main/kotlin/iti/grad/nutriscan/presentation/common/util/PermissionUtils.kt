package iti.grad.nutriscan.presentation.common.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
    )
}

/**
 * RECORD_AUDIO permission request that falls back to the app's settings page
 * when the OS denies without showing its own dialog (permanently denied).
 * Mirrors the camera permission flow in CameraScanScreen.
 */
@Composable
fun rememberAudioPermissionRequester(
    onGranted: () -> Unit,
    onDenied: () -> Unit = {},
): () -> Unit {
    val context = LocalContext.current
    val latestOnGranted by rememberUpdatedState(onGranted)
    val latestOnDenied by rememberUpdatedState(onDenied)
    var requestedAt by remember { mutableLongStateOf(0L) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            latestOnGranted()
        } else {
            latestOnDenied()
            if (System.currentTimeMillis() - requestedAt < 350) {
                openAppSettings(context)
            }
        }
    }

    return remember(launcher) {
        {
            requestedAt = System.currentTimeMillis()
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
