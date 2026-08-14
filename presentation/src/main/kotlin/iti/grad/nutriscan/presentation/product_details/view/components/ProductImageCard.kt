package iti.grad.nutriscan.presentation.product_details.view.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.components.AdaptiveAsyncImage
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

@Composable
fun ProductImageCard(
    imageUrl: String?,
    productName: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 24.dp),
        shape = RoundedCornerShape(16.dp),
        color = AppTheme.colors.ScreenSurfaceBackground,
        shadowElevation = 4.dp,
        border = BorderStroke(3.dp, AppTheme.colors.Teal1000),
    ) {
        AdaptiveAsyncImage(
            model = imageUrl,
            contentDescription = productName,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp)),
            error = painterResource(id = R.drawable.ic_scanner),
            placeholder = painterResource(id = R.drawable.ic_scanner),
        )
    }
}
