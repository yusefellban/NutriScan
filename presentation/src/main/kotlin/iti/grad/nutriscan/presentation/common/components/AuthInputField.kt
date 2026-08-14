package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans
import iti.grad.presentation.R

// ─────────────────────────────────────────────────────────────────────────────
// Custom input field matching Figma: label + clean container row + error banner
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AuthInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String,
    leadingIconRes: Int,
    hasError: Boolean = false,
    errorResId: Int? = null,
    inputContainerBg: Color,
    inputLabelColor: Color,
    inputTextColor: Color,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    onVisibilityToggle: (() -> Unit)? = null
) {
    val errorColor = MaterialTheme.colorScheme.error
    val errorBannerBg = AppTheme.colors.ErrorBackground
    val errorBannerTextColor = MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Label
        Text(
            text = label,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            letterSpacing = (-0.14).sp,
            color = inputLabelColor
        )

        // Input container — glow wrapper when error
        val containerModifier = if (hasError) {
            Modifier
                .fillMaxWidth()
                // outer glow ring (rgba(250,77,94,0.25) at 4dp)
                .clip(RoundedCornerShape(16.dp))
                .background(errorColor.copy(alpha = 0.25f))
                .padding(4.dp)
        } else Modifier.fillMaxWidth()

        Box(modifier = containerModifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(inputContainerBg)
                    .then(
                        if (hasError) Modifier.border(1.dp, errorColor, RoundedCornerShape(12.dp))
                        else Modifier
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Leading icon
                Icon(
                    painter = painterResource(leadingIconRes),
                    contentDescription = null,
                    tint = inputTextColor,
                    modifier = Modifier.size(24.dp)
                )

                // Text input
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    visualTransformation = if (isPassword && !isPasswordVisible)
                        PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPassword) KeyboardType.Password else keyboardType
                    ),
                    textStyle = TextStyle(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        letterSpacing = (-0.16).sp,
                        color = inputTextColor
                    ),
                    cursorBrush = SolidColor(inputTextColor),
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(
                                text = hint,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                letterSpacing = (-0.16).sp,
                                color = inputTextColor.copy(alpha = 0.45f)
                            )
                        }
                        innerTextField()
                    }
                )

                // Password visibility toggle
                if (isPassword && onVisibilityToggle != null) {
                    IconButton(
                        onClick = onVisibilityToggle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible)
                                Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = inputTextColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Error banner
        if (hasError && errorResId != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(errorBannerBg)
                    .border(1.dp, errorColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_solid_warning),
                    contentDescription = null,
                    tint = errorColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(errorResId),
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    letterSpacing = (-0.14).sp,
                    color = errorBannerTextColor
                )
            }
        }
    }
}
