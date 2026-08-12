package iti.grad.nutriscan.presentation.product_details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.common.components.AppTopHeader
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.components.DeleteWarningAlert
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.product_details.state.ProductDetailsEffect
import iti.grad.nutriscan.presentation.product_details.state.ProductDetailsEvent
import iti.grad.nutriscan.presentation.product_details.state.ProductDetailsState
import iti.grad.nutriscan.presentation.product_details.view.components.FlaggedIngredientsRow
import iti.grad.nutriscan.presentation.product_details.view.components.NutritionFactsRow

import iti.grad.nutriscan.presentation.product_details.view.components.ProductImageCard
import iti.grad.nutriscan.presentation.product_details.view.components.ProductInfoHeader
import iti.grad.nutriscan.presentation.product_details.viewmodel.ProductDetailsViewModel

@Composable
fun ProductDetailsScreen(
    viewModel: ProductDetailsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProductDetailsEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    ProductDetailsContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun ProductDetailsContent(
    state: ProductDetailsState,
    onEvent: (ProductDetailsEvent) -> Unit,
) {
    if (state.showDeleteDialog) {
        DeleteWarningAlert(
            title = stringResource(id = R.string.alert_remove_saved_title),
            message = stringResource(id = R.string.alert_remove_saved_message),
            confirmText = stringResource(id = R.string.action_remove),
            cancelText = stringResource(id = R.string.action_cancel),
            onConfirm = { onEvent(ProductDetailsEvent.ConfirmDeleteBookmark) },
            onDismiss = { onEvent(ProductDetailsEvent.DismissDeleteBookmark) },
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.Background)
            .verticalScroll(scrollState),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.Primary)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // ── Top Bar ──
                AppTopHeader(
                    title = stringResource(R.string.product_details_title),
                    onBackClick = { onEvent(ProductDetailsEvent.BackClicked) },
                    actionIconResId = if (state.productDetail?.isBookmarked == true) R.drawable.ic_bookmark_solid else R.drawable.ic_bookmark_outline,
                    actionIconContentDescription = stringResource(
                        if (state.productDetail?.isBookmarked == true) R.string.product_details_bookmark_remove else R.string.product_details_bookmark_add
                    ),
                    onActionClick = { onEvent(ProductDetailsEvent.BookmarkToggled) }
                )

                // ── White Body Container ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(AppTheme.colors.Background),
                ) {
                    when {
                        state.isLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = AppTheme.colors.Primary,
                                )
                            }
                        }

                        state.error != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.error,
                                    color = AppTheme.colors.Error,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }

                        state.productDetail != null -> {
                            val product = state.productDetail
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                // ── Product Image ──
                                ProductImageCard(
                                    imageUrl = product.imageUrl,
                                    productName = product.productName,
                                )
                                Spacer(modifier = Modifier.height(20.dp))

                                // ── Name + Verdict + Safety Text ──
                                ProductInfoHeader(
                                    productName = product.productName,
                                    status = product.status,
                                    verdict = product.verdict,
                                    scanDate = product.scanDate,
                                    safetyReasonText = product.safetyReasonText,
                                )
                                Spacer(modifier = Modifier.height(20.dp))

                                // ── Flagged Ingredients ──
                                if (product.flaggedIngredients.isNotEmpty()) {
                                    FlaggedIngredientsRow(
                                        verdict = product.verdict,
                                        ingredients = product.flaggedIngredients,
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                }

                                // ── Nutritional Info ──
                                NutritionFactsRow(
                                    calories = product.calories,
                                    servingSize = product.servingSize,
                                    sugar = product.sugar,
                                    fat = product.fat,
                                    saturatedFat = product.saturatedFat,
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
