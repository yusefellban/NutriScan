package iti.grad.nutriscan.presentation.saved.view.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.components.AppSearchBar
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

/**
 * Search bar for the Saved Products screen.
 * Thin wrapper around [AppSearchBar] — no autocomplete needed here.
 */
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
    AppSearchBar(
        query = query,
        onQueryChange = onQueryChange,
        hint = stringResource(R.string.saved_search_hint),
        height = height,
        borderColor = borderColor,
        placeholderColor = placeholderColor,
        textColor = textColor,
        modifier = modifier,
    )
}

