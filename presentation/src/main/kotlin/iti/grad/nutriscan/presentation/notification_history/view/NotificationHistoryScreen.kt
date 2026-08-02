package iti.grad.nutriscan.presentation.notification_history.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.hilt.navigation.compose.hiltViewModel
import iti.grad.nutriscan.presentation.common.components.AppSnackbar
import iti.grad.nutriscan.presentation.common.components.showAppSnackbar
import iti.grad.nutriscan.presentation.common.components.ConfirmationDialog
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.notification_history.state.NotificationHistoryEffect
import iti.grad.nutriscan.presentation.notification_history.state.NotificationHistoryEvent
import iti.grad.nutriscan.presentation.notification_history.state.NotificationHistoryItemUi
import iti.grad.nutriscan.presentation.notification_history.view.components.NotificationHistoryEmptyStateWidget
import iti.grad.nutriscan.presentation.notification_history.view.components.NotificationPermissionDeniedWidget
import iti.grad.nutriscan.presentation.notification_history.view.components.NotificationHistoryItemCard
import iti.grad.nutriscan.presentation.notification_history.viewmodel.NotificationHistoryViewModel
import iti.grad.nutriscan.presentation.settings.app.view.components.AppSettingsHeader
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun NotificationHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: NotificationHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
        }
    )

    LifecycleResumeEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NotificationHistoryEffect.NavigateBack -> onNavigateBack()
                is NotificationHistoryEffect.NavigateToSettings -> onNavigateToSettings()
                is NotificationHistoryEffect.ShowUndoSnackbar -> {
                    val message = context.applicationContext.getString(effect.messageResId)
                    scope.launch {
                        snackbarHostState.showAppSnackbar(
                            message = message,
                        )
                    }
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        ConfirmationDialog(
            title = stringResource(id = R.string.notification_history_confirm_clear_title),
            message = stringResource(id = R.string.notification_history_confirm_clear_message),
            confirmLabel = stringResource(id = R.string.notification_history_confirm_clear_yes),
            cancelLabel = stringResource(id = R.string.notification_history_confirm_clear_no),
            onConfirm = {
                viewModel.onEvent(NotificationHistoryEvent.ClearAll)
                showClearConfirmDialog = false
            },
            onDismiss = { showClearConfirmDialog = false },
        )
    }

    Scaffold(
        containerColor = AppTheme.colors.Background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                AppSnackbar(snackbarData = data)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box {
                    AppSettingsHeader(
                        title = stringResource(id = R.string.notification_history_title),
                        onBackClick = { viewModel.onEvent(NotificationHistoryEvent.BackClicked) },
                    )
                    
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 40.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!state.isEmpty) {
                            TextButton(onClick = { showClearConfirmDialog = true }) {
                                Text(
                                    text = stringResource(id = R.string.notification_history_clear_all),
                                    color = AppTheme.colors.Teal100,
                                    style = AppTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        IconButton(onClick = { viewModel.onEvent(NotificationHistoryEvent.NavigateToSettingsClicked) }) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(id = R.string.notification_history_settings),
                                tint = AppTheme.colors.Teal100
                            )
                        }
                    }
                }

                if (!hasNotificationPermission) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        NotificationPermissionDeniedWidget(
                            onGoToSettingsClick = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                } else if (state.isEmpty && !state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        NotificationHistoryEmptyStateWidget()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.notifications,
                            key = { it.id }
                        ) { item ->
                            SwipeToDeleteItem(
                                item = item,
                                onDelete = { viewModel.onEvent(NotificationHistoryEvent.DeleteNotification(item.id)) },
                                onClick = { viewModel.onEvent(NotificationHistoryEvent.NotificationClicked(item.id)) }
                            )
                        }
                    }
                }
            }

            if (state.isLoading && state.notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = AppTheme.colors.Teal500)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteItem(
    item: NotificationHistoryItemUi,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .background(AppTheme.colors.Error, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
        },
        content = {
            NotificationHistoryItemCard(
                item = item,
                onClick = onClick,
            )
        }
    )
}
