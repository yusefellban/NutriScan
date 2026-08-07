package iti.grad.nutriscan.presentation.nutrigpt.chat.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.theme.AppTheme

@Composable
fun ChatInputBar(
    query: String,
    isLoading: Boolean = false,
    isListening: Boolean = false,
    onQueryChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicPress: () -> Unit,
    onMicRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.ChatScreenBackgroundEnd)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .border(
                    width = 1.dp,
                    color = if (isListening) Color.Red else AppTheme.colors.Accent,
                    shape = RoundedCornerShape(24.dp)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(if (isListening) Color.Red.copy(alpha = 0.05f) else AppTheme.colors.ChatInputBackground),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    textStyle = AppTheme.typography.bodyLarge.copy(
                        color = AppTheme.colors.TextPrimary
                    ),
                    cursorBrush = SolidColor(AppTheme.colors.TextPrimary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = if (isListening) stringResource(id = R.string.nutrigpt_listening_hint) else stringResource(id = R.string.nutrigpt_input_hint),
                                style = AppTheme.typography.bodyLarge.copy(
                                    color = AppTheme.colors.ChatInputPlaceholder
                                )
                            )
                        }
                        innerTextField()
                    }
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Box(
                    modifier = Modifier
                        .size(if (isListening) 40.dp else 32.dp)
                        .clip(CircleShape)
                        .background(if (isListening) Color.Red.copy(alpha = 0.2f) else Color.Transparent)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    if (!isLoading) {
                                        onMicPress()
                                        tryAwaitRelease()
                                        onMicRelease()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = stringResource(id = R.string.nutrigpt_mic_content_description),
                        tint = if (isListening) Color.Red else AppTheme.colors.Accent
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        IconButton(
            onClick = onSend,
            enabled = query.isNotBlank() && !isLoading,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (query.isNotBlank()) AppTheme.colors.Primary else AppTheme.colors.ChatSendButtonBackground
                )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = AppTheme.colors.ChatSendButtonIcon,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.ArrowUpward,
                    contentDescription = stringResource(id = R.string.nutrigpt_send_content_description),
                    tint = AppTheme.colors.ChatSendButtonIcon,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

