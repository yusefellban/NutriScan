package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans

@Composable
fun AuthDivider(
    textColor: Color,
    dividerColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = dividerColor,
            thickness = 1.dp
        )
        Text(
            text = "OR",
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = textColor
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = dividerColor,
            thickness = 1.dp
        )
    }
}
