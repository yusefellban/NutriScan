package iti.grad.nutriscan.presentation.auth.profile_setup.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans

fun Modifier.dashedBorder(
    width: Dp,
    color: Color
) = drawBehind {
    val strokeWidthPx = width.toPx()
    val stroke = Stroke(
        width = strokeWidthPx,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
    )
    val radius = size.height / 2f
    drawRoundRect(
        color = color,
        style = stroke,
        cornerRadius = CornerRadius(radius, radius)
    )
}

@Composable
fun OtherInputChip(
    isEditing: Boolean,
    inputValue: String,
    onValueChange: (String) -> Unit,
    onStartEditing: () -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    
    val activeColor = AppTheme.colors.OtherChipActive
    val borderColor = AppTheme.colors.OtherChipBorder
    val textColor = AppTheme.colors.OtherChipText
    val backgroundColor = AppTheme.colors.OtherChipBackground
    val textUnselectedColor = AppTheme.colors.OtherChipTextUnselected
    
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }

    if (isEditing) {
        Row(
            modifier = modifier
                .clip(CircleShape)
                .background(backgroundColor)
                .dashedBorder(1.dp, borderColor)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = inputValue,
                onValueChange = onValueChange,
                modifier = Modifier
                    .widthIn(min = 80.dp, max = 150.dp)
                    .focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onSubmit() }
                ),
                textStyle = AppTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = textColor
                ),
                cursorBrush = SolidColor(activeColor),
                decorationBox = { innerTextField ->
                    if (inputValue.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = AppTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            ),
                            color = textColor.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Confirm",
                tint = activeColor,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onSubmit() }
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel",
                tint = textColor.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onCancel() }
            )
        }
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(Color.Transparent)
                .dashedBorder(1.dp, borderColor)
                .clickable(onClick = onStartEditing)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = placeholder,
                style = AppTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = textUnselectedColor
            )
        }
    }
}
