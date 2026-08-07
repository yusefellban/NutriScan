package iti.grad.nutriscan.presentation.product_details.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.scan.model.FlaggedIngredient
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

@Composable
fun FlaggedIngredientsRow(
    verdict: ProductVerdict,
    ingredients: List<FlaggedIngredient>,
    modifier: Modifier = Modifier,
) {
    if (ingredients.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        // Section title — adapts to verdict
        val titleResId = when (verdict) {
            ProductVerdict.UNSAFE -> R.string.product_details_why_unsafe
            ProductVerdict.CAUTION -> R.string.product_details_why_caution
            ProductVerdict.SAFE -> R.string.product_details_why_safe
        }

        Text(
            text = stringResource(titleResId),
            style = AppTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = AppTheme.colors.ProductDetailWhyNotSafeText,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontally scrollable ingredient cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ingredients.forEach { ingredient ->
                FlaggedIngredientCard(ingredient = ingredient)
            }
        }
    }
}
