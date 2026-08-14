package iti.grad.nutriscan.presentation.product_details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.common.components.AppBackButton
import iti.grad.nutriscan.presentation.common.components.BackButtonSurface
import iti.grad.nutriscan.presentation.common.components.HeroHeaderTitle
import iti.grad.nutriscan.presentation.common.components.SectionHeroHeader
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.components.DeleteWarningAlert
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.product_details.state.ProductDetailsEffect
import iti.grad.nutriscan.presentation.product_details.state.ProductDetailsEvent
import iti.grad.nutriscan.presentation.product_details.state.ProductDetailsState
import iti.grad.nutriscan.presentation.product_details.view.components.FlaggedIngredientsRow
import iti.grad.nutriscan.presentation.product_details.view.components.NutritionFactsRow
import iti.grad.nutriscan.presentation.common.components.NotFoundStateWidget
import iti.grad.nutriscan.presentation.common.components.AppErrorWidget
import iti.grad.nutriscan.presentation.common.components.OfflineStateWidget

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
            .background(AppTheme.colors.ProfileHeaderBackground),
    ) {
        // ── Top Bar ── same SectionHeroHeader shape/spacing as Home/Saved/Calories/Profile.
        SectionHeroHeader {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 56.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppBackButton(
                    onClick = { onEvent(ProductDetailsEvent.BackClicked) },
                    surface = BackButtonSurface.OnAccent,
                )

                Spacer(modifier = Modifier.width(12.dp))

                HeroHeaderTitle(
                    text = stringResource(R.string.product_details_title),
                    modifier = Modifier.weight(1f),
                )

                val isBookmarked = state.productDetail?.isBookmarked == true
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppTheme.colors.Teal700)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onEvent(ProductDetailsEvent.BookmarkToggled) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isBookmarked) R.drawable.ic_bookmark_solid else R.drawable.ic_bookmark_outline
                        ),
                        contentDescription = stringResource(
                            if (isBookmarked) R.string.product_details_bookmark_remove else R.string.product_details_bookmark_add
                        ),
                        tint = AppTheme.colors.Teal1600,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        // ── White Body Container ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(AppTheme.colors.Background)
                .verticalScroll(scrollState),
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

                        state.isNotFound -> {
                            NotFoundStateWidget(
                                onRetry = { onEvent(ProductDetailsEvent.RetryLoad) },
                                modifier = Modifier.padding(vertical = 48.dp),
                            )
                        }

                        state.errorMessageResId != null -> {
                            AppErrorWidget(
                                errorType = state.errorType,
                                onRetry = { onEvent(ProductDetailsEvent.RetryLoad) },
                                modifier = Modifier.padding(vertical = 48.dp),
                            )
                        }

                        state.productDetail != null -> {
                            val product = state.productDetail
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 20.dp),
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
                                    protein = product.protein,
                                    carbs = product.carbs,
                                    fat = product.fat,
                                    fiber = product.fiber,
                                    sugar = product.sugar,
                                    sodium = product.sodium,
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
        }
    }
}
