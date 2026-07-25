package iti.grad.nutriscan.presentation.saved.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    height: Dp = 48.dp
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(height),
            placeholder = {
                Text(
                    text = stringResource(id = R.string.saved_search_hint),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.ExerciseSearchPlaceholder
                )
            },
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = AppTheme.colors.SavedSearchBarBorder,
                unfocusedIndicatorColor = AppTheme.colors.SavedSearchBarBorder,
            ),
            singleLine = true,
            textStyle = AppTheme.typography.bodyMedium.copy(color = AppTheme.colors.TextPrimary)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        IconButton(
            onClick = { /* Search action usually handled by onQueryChange */ },
            modifier = Modifier
                .background(AppTheme.colors.SavedSearchIconBackground, CircleShape)
                .size(height)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = stringResource(id = R.string.saved_search_hint),
                tint = AppTheme.colors.SavedSearchIconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
