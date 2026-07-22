package iti.grad.nutriscan.presentation.saved.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.components.ProductCard
import iti.grad.nutriscan.presentation.saved.state.SavedProductUiModel
import kotlinx.collections.immutable.ImmutableList

import androidx.compose.foundation.lazy.grid.GridItemSpan

@Composable
fun SavedProductGrid(
    products: ImmutableList<SavedProductUiModel>,
    onProductClick: (String) -> Unit,
    onSwipeToAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    header: (@Composable () -> Unit)? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (header != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                header()
            }
        }
        items(
            items = products,
            key = { it.id }
        ) { product ->
            ProductCard(
                imageUrl = product.imageUrl,
                productName = product.productName,
                verdict = product.verdict,
                calories = product.calories,
                swipeHintResId = R.string.product_card_swipe_hint,
                onClick = { onProductClick(product.id) },
                onSwipeToAdd = { onSwipeToAdd(product.id) }
            )
        }
    }
}
