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
            // We remove the hardcoded width(160.dp) to let it fill the staggered column
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

        if (ingredient.matchTag.isNotBlank() && ingredient.matchTag != "UNKNOWN") {
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .background(
                        color = AppTheme.colors.Teal1000,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = ingredient.matchTag,
                    style = AppTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = AppTheme.colors.Teal100
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Reason text
        Text(
            text = ingredient.reason,
            style = CaloriesTypography.badgeText,
            color = AppTheme.colors.ProductDetailIngredientReasonText,
        )
    }
}


