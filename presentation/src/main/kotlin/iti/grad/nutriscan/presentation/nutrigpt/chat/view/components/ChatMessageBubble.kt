package iti.grad.nutriscan.presentation.nutrigpt.chat.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.jeziellago.compose.markdowntext.MarkdownText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.domain.nutrigpt.model.NutriGptMessage
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.theme.AppTheme

@Composable
fun ChatMessageBubble(
    message: NutriGptMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.isFromUser
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        if (isUser) {
            Box(
                modifier = Modifier
                    .background(
                        color = AppTheme.colors.ChatUserBubble,
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .widthIn(max = 280.dp)
            ) {
                Text(
                    text = message.text,
                    style = AppTheme.typography.bodyLarge.copy(
                        color = AppTheme.colors.ChatUserBubbleText
                    )
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .background(
                        color = AppTheme.colors.ChatBotBubble,
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 20.dp
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = stringResource(id = R.string.nutrigpt_sparkle_content_description),
                        tint = AppTheme.colors.ChatDisclaimerIcon,
                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    MarkdownText(
                        markdown = message.text,
                        style = AppTheme.typography.bodyLarge,
                        color = AppTheme.colors.ChatBotBubbleText,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                
                if (message.sources.isNotEmpty()) {
                    ChatSourcesSection(
                        sources = message.sources,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}
