package iti.grad.nutriscan.presentation.product_details.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.CaloriesTypography

/**
 * Horizontal row of nutritional macro pills.
 *
 * Each pill shows a label (e.g. "Calories") and its value (e.g. "80 kcal")
 * in a teal rounded badge.
 */
@Composable
fun NutritionFactsRow(
    calories: String?,
    servingSize: String?,
    sugar: String?,
    fat: String?,
    saturatedFat: String?,
    modifier: Modifier = Modifier,
) {
    data class MacroItem(val label: String, val value: String)

    val items = buildList {
        if (!calories.isNullOrBlank()) add(MacroItem(stringResource(iti.grad.presentation.R.string.nutrition_calories), stringResource(iti.grad.presentation.R.string.unit_kcal, calories)))
        if (!servingSize.isNullOrBlank()) add(MacroItem(stringResource(iti.grad.presentation.R.string.nutrition_serving_size), stringResource(iti.grad.presentation.R.string.unit_serving, servingSize)))
        if (!sugar.isNullOrBlank()) add(MacroItem(stringResource(iti.grad.presentation.R.string.nutrition_sugar), stringResource(iti.grad.presentation.R.string.unit_g, sugar)))
        if (!fat.isNullOrBlank()) add(MacroItem(stringResource(iti.grad.presentation.R.string.nutrition_fat), stringResource(iti.grad.presentation.R.string.unit_g, fat)))
        if (!saturatedFat.isNullOrBlank()) add(MacroItem(stringResource(iti.grad.presentation.R.string.nutrition_saturated_fat), stringResource(iti.grad.presentation.R.string.unit_g, saturatedFat)))
    }

    if (items.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { macro ->
            NutritionPill(label = macro.label, value = macro.value)
        }
    }
}

@Composable
private fun NutritionPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            style = CaloriesTypography.badgeText,
            color = AppTheme.colors.ProductDetailNutritionLabelText,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppTheme.colors.ProductDetailNutritionCardBg,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )

        Text(
            text = value,
            textAlign = TextAlign.Center,
            style = CaloriesTypography.badgeText,
            color = AppTheme.colors.ProductDetailNutritionText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .background(
                    color = AppTheme.colors.ProductDetailNutritionPill,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
