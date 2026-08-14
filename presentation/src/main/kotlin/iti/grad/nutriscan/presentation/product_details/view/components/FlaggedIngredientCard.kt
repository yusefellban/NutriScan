package iti.grad.nutriscan.presentation.product_details.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.domain.scan.model.FlaggedIngredient
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.CaloriesTypography

@Composable
fun FlaggedIngredientCard(
    ingredient: FlaggedIngredient,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(160.dp)
            .border(
                width = 1.dp,
                color = AppTheme.colors.ProductDetailIngredientCardBorder,
                shape = RoundedCornerShape(12.dp),
            )
            .background(
                color = AppTheme.colors.SurfaceVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
    ) {
        // Ingredient name with "- " prefix (matches mockup)
        Text(
            text = "- ${ingredient.name}",
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.Teal1000,
            maxLines = 2,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Reason text
        Text(
            text = ingredient.reason,
            style = CaloriesTypography.badgeText,
            color = AppTheme.colors.ProductDetailIngredientReasonText,
        )
    }
}


