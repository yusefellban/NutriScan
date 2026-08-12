package iti.grad.nutriscan.presentation.common.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Shared search bar used across screens (Saved, History, News, etc.).
 *
 * Supports optional autocomplete suggestions dropdown — pass a non-empty [suggestions]
 * list with [showSuggestions] = true to activate the dropdown.
 *
 * When suggestions are not needed, leave the autocomplete parameters at their defaults.
 */
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf

@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    height: Dp = 48.dp,
    textColor: Color = AppTheme.colors.TextPrimary,
    placeholderColor: Color = AppTheme.colors.ExerciseSearchPlaceholder,
    borderColor: Color = AppTheme.colors.SavedSearchBarBorder,
    // ── Autocomplete ──────────────────────────────────────────────
    suggestions: ImmutableList<String> = persistentListOf(),
    showSuggestions: Boolean = false,
    isSuggestionsLoading: Boolean = false,
    onSuggestionSelected: (String) -> Unit = {},
    onSearchSubmitted: () -> Unit = {},
    onClearClicked: () -> Unit = {},
) {
    var textFieldWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Input Row ─────────────────────────────────────────────
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .onGloballyPositioned { textFieldWidth = it.size.width }
                .border(1.dp, borderColor, RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp),
            textStyle = AppTheme.typography.bodyMedium.copy(color = textColor),
            singleLine = true,
            cursorBrush = SolidColor(textColor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchSubmitted() }),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = if (query.isNotBlank()) AppTheme.colors.Primary
                               else placeholderColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = hint,
                                style = AppTheme.typography.bodyMedium,
                                color = placeholderColor,
                            )
                        }
                        innerTextField()
                    }
                    if (isSuggestionsLoading) {
                        CircularProgressIndicator(
                            color = AppTheme.colors.Primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                    } else if (query.isNotBlank()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = null,
                            tint = placeholderColor,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onClearClicked,
                                ),
                        )
                    }
                }
            },
        )

        // ── Autocomplete Dropdown ─────────────────────────────────
        if (showSuggestions && suggestions.isNotEmpty()) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(density) { (height + 4.dp).roundToPx() }),
                properties = PopupProperties(focusable = false)
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column(
                        modifier = Modifier
                            .width(with(density) { textFieldWidth.toDp() })
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                            .background(AppTheme.colors.Surface)
                            .heightIn(max = 240.dp),
                    ) {
                        suggestions.forEachIndexed { index, suggestion ->
                            SuggestionItem(
                                suggestion = suggestion,
                                query = query,
                                textColor = textColor,
                                onClick = { onSuggestionSelected(suggestion) },
                            )
                            if (index < suggestions.lastIndex) {
                                HorizontalDivider(
                                    color = borderColor,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: String,
    query: String,
    textColor: Color,
    onClick: () -> Unit,
) {
    val annotatedText = buildAnnotatedString {
        val lowerSuggestion = suggestion.lowercase()
        val lowerQuery = query.lowercase().trim()
        val matchStart = lowerSuggestion.indexOf(lowerQuery)

        if (matchStart >= 0 && lowerQuery.isNotEmpty()) {
            append(suggestion.substring(0, matchStart))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AppTheme.colors.Primary)) {
                append(suggestion.substring(matchStart, matchStart + lowerQuery.length))
            }
            append(suggestion.substring(matchStart + lowerQuery.length))
        } else {
            append(suggestion)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = AppTheme.colors.ExerciseSearchPlaceholder,
            modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = annotatedText,
            style = AppTheme.typography.bodyMedium,
            color = textColor,
        )
    }
}
