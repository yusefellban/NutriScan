package iti.grad.nutriscan.presentation.settings.profile.edit.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans

/**
 * Custom text input field for measurements (Height/Weight).
 * Displays a label on the left and an editable value with a unit on the right.
 */
@Composable
fun EditProfileMeasurementField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    isReadOnly: Boolean = false,
    modifier: Modifier = Modifier,
    errorMessage: String? = null
) {
    val containerBg = AppTheme.colors.EditProfileInputBackground
    val borderColor = if (errorMessage != null) AppTheme.colors.Error else AppTheme.colors.EditProfileInputBorder
    val baseTextColor = AppTheme.colors.TextPrimary
    val hintColor = AppTheme.colors.ProfileSetupSubtitle
    val textColor = if (isReadOnly) hintColor else baseTextColor

    Column(modifier = modifier) {
    Row(
        modifier = Modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerBg)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label on the left
        Text(
            text = label,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            letterSpacing = (-0.16).sp,
            color = hintColor
        )

        // Editable value on the right
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            singleLine = true,
            enabled = !isReadOnly,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                letterSpacing = (-0.16).sp,
                color = textColor,
                textAlign = TextAlign.End
            ),
            cursorBrush = SolidColor(textColor)
        )

        if (value.isNotEmpty() || !isReadOnly) {
            Text(
                text = unit,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                letterSpacing = (-0.16).sp,
                color = hintColor
            )
        }
    }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = AppTheme.colors.Error,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}
