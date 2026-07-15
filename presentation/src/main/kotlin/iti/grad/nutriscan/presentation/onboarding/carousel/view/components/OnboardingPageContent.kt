package iti.grad.nutriscan.presentation.onboarding.carousel.view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans

@Composable
fun OnboardingPageContent(
    imageRes: Int,
    titleRes: Int,
    descriptionRes: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    var animateImage by remember { mutableStateOf(false) }
    var animateText by remember { mutableStateOf(false) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            animateImage = true
            animateText = true
        } else {
            animateImage = false
            animateText = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = animateImage,
            enter = fadeIn(animationSpec = tween(durationMillis = 800)) +
                    slideInVertically(
                        animationSpec = tween(durationMillis = 800),
                        initialOffsetY = { it / 3 }
                    )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val colors = AppTheme.colors
        Text(
            text = stringResource(id = titleRes),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            textAlign = TextAlign.Center,
            color = colors.TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = animateText,
            enter = fadeIn(animationSpec = tween(durationMillis = 800, delayMillis = 200)) +
                    slideInVertically(
                        animationSpec = tween(durationMillis = 800, delayMillis = 200),
                        initialOffsetY = { it / 4 }
                    )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = stringResource(id = descriptionRes),
                    fontFamily = LexendDeca,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    color = colors.TextSecondary
                )
            }
        }
    }
}
