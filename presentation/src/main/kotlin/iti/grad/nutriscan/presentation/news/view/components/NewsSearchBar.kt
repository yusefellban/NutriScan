package iti.grad.nutriscan.presentation.news.view.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

@Composable
fun NewsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = {
            Text(
                text = stringResource(id = R.string.news_search_placeholder),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.NewsSearchIconTint,
            )
        },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = stringResource(id = R.string.news_search_placeholder),
                tint = AppTheme.colors.NewsSearchIconTint,
                modifier = Modifier.size(20.dp),
            )
        },
        trailingIcon = {
            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = stringResource(id = R.string.news_search_filter_content_description),
                    tint = AppTheme.colors.NewsSearchIconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AppTheme.colors.NewsSearchBarBg,
            unfocusedContainerColor = AppTheme.colors.NewsSearchBarBg,
            disabledContainerColor = AppTheme.colors.NewsSearchBarBg,
            focusedBorderColor = AppTheme.colors.NewsSearchBarBorder,
            unfocusedBorderColor = AppTheme.colors.NewsSearchBarBorder,
            cursorColor = AppTheme.colors.TextPrimary,
        ),
        singleLine = true,
        textStyle = AppTheme.typography.bodyMedium.copy(color = AppTheme.colors.TextPrimary),
    )
}
