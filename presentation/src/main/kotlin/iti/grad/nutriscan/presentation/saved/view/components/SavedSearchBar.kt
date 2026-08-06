package iti.grad.nutriscan.presentation.saved.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.theme.AppTheme

import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    textColor: Color = AppTheme.colors.TextPrimary,
    placeholderColor: Color = AppTheme.colors.ExerciseSearchPlaceholder,
    borderColor: Color = AppTheme.colors.SavedSearchBarBorder,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(height)
                .border(1.dp, borderColor, RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp),
            textStyle = AppTheme.typography.bodyMedium.copy(color = textColor),
            singleLine = true,
            cursorBrush = SolidColor(textColor),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.saved_search_hint),
                            style = AppTheme.typography.bodyMedium,
                            color = placeholderColor
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.width(12.dp))

        IconButton(
            onClick = { /* Search action usually handled by onQueryChange */ },
            modifier = Modifier
                .background(AppTheme.colors.Teal1000, CircleShape)
                .size(height)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = stringResource(id = R.string.saved_search_hint),
                tint = AppTheme.colors.Teal100,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


