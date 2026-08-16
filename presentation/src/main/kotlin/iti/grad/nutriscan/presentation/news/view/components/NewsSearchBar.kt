package iti.grad.nutriscan.presentation.news.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

private val searchBarShape = RoundedCornerShape(50)
private val searchBarHeight = 56.dp
private val searchButtonSize = 48.dp

/**
 * Matches the iOS `CustomSearchBar`: a pill-shaped outlined field (no leading icon,
 * teal border) followed by a separate circular teal search button.
 */
@Composable
fun NewsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(searchBarHeight),
            placeholder = {
                Text(
                    text = stringResource(id = R.string.news_search_placeholder),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.NewsSearchIconTint,
                )
            },
            shape = searchBarShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppTheme.colors.Background,
                unfocusedContainerColor = AppTheme.colors.Background,
                disabledContainerColor = AppTheme.colors.Background,
                focusedBorderColor = AppTheme.colors.NewsSearchBarBorder,
                unfocusedBorderColor = AppTheme.colors.NewsSearchBarBorder,
                cursorColor = AppTheme.colors.TextPrimary,
            ),
            singleLine = true,
            textStyle = AppTheme.typography.bodyMedium.copy(color = AppTheme.colors.TextPrimary),
        )

        Box(
            modifier = Modifier
                .size(searchButtonSize)
                .clip(CircleShape)
                .background(AppTheme.colors.NewsChipSelectedBg)
                .clickable(onClick = onFilterClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = stringResource(id = R.string.news_search_placeholder),
                tint = AppTheme.colors.NewsChipSelectedText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
