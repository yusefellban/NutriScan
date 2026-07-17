package iti.grad.nutriscan.presentation.profile_setup.view.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca
import iti.grad.nutriscan.presentation.profile_setup.state.Gender
import iti.grad.nutriscan.presentation.profile_setup.state.ProfileSetupPagerEvent
import iti.grad.nutriscan.presentation.home.view.components.customShadow
import iti.grad.presentation.R

@Composable
fun GenderSelectionPage(
    selectedGender: Gender?,
    currentPage: Int,
    pageCount: Int,
    onEvent: (ProfileSetupPagerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(170.dp))

        // Title + Subtitle
        ProfileSetupHeader(
            prefixRes = R.string.profile_setup_gender_title_prefix,
            highlightRes = R.string.profile_setup_gender_title_highlight,
            suffixRes = R.string.profile_setup_gender_title_suffix,
            subtitleRes = R.string.profile_setup_gender_subtitle,
            currentPage = currentPage,
            pageCount = pageCount
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Gender Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val femaleWeight by animateFloatAsState(
                targetValue = if (selectedGender == Gender.FEMALE) 1.15f else 1f,
                animationSpec = tween(300),
                label = "female_weight"
            )
            val maleWeight by animateFloatAsState(
                targetValue = if (selectedGender == Gender.MALE) 1.15f else 1f,
                animationSpec = tween(300),
                label = "male_weight"
            )

            // Female Card
            GenderCard(
                gender = Gender.FEMALE,
                isSelected = selectedGender == Gender.FEMALE,
                label = stringResource(R.string.profile_setup_gender_female),
                cardColor = AppTheme.colors.GenderFemaleCardBackground,
                labelColor = AppTheme.colors.GenderFemaleCardText,
                svgAssetPath = if (isSystemInDarkTheme()) "ic_female_dark.svg" else "ic_female.svg",
                onClick = { onEvent(ProfileSetupPagerEvent.SelectGender(Gender.FEMALE)) },
                modifier = Modifier.weight(femaleWeight)
            )

            // Male Card
            GenderCard(
                gender = Gender.MALE,
                isSelected = selectedGender == Gender.MALE,
                label = stringResource(R.string.profile_setup_gender_male),
                cardColor = AppTheme.colors.GenderMaleCardBackground,
                labelColor = AppTheme.colors.GenderMaleCardText,
                svgAssetPath = "ic_male.svg",
                onClick = { onEvent(ProfileSetupPagerEvent.SelectGender(Gender.MALE)) },
                modifier = Modifier.weight(maleWeight)
            )
        }
    }
}

@Composable
private fun GenderCard(
    gender: Gender,
    isSelected: Boolean,
    label: String,
    cardColor: Color,
    labelColor: Color,
    svgAssetPath: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Animate sizes on selection
    val cardHeight by animateDpAsState(
        targetValue = if (isSelected) 200.dp else 160.dp,
        animationSpec = tween(300),
        label = "card_height"
    )

    val imageWidth by animateDpAsState(
        targetValue = if (isSelected) 80.dp else 60.dp,
        animationSpec = tween(300),
        label = "image_width"
    )

    val imageHeight by animateDpAsState(
        targetValue = if (isSelected) 120.dp else 90.dp,
        animationSpec = tween(300),
        label = "image_height"
    )

    val textSize by animateFloatAsState(
        targetValue = if (isSelected) 24f else 18f,
        animationSpec = tween(300),
        label = "text_size"
    )

    val shadowColor = if (isSelected) {
        if (gender == Gender.FEMALE) AppTheme.colors.ShadowFemaleSelected else AppTheme.colors.ShadowMaleSelected
    } else {
        AppTheme.colors.ShadowUnselected
    }
    val isDark = isSystemInDarkTheme()
    val shadowBlur = if (isDark) {
        if (isSelected) 20f else 8f
    } else {
        if (isSelected) 80f else 32f
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Selection triangle indicator above the card
        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.ic_selected_arrow),
                contentDescription = null,
                tint = cardColor, // Match the card's background color
                modifier = Modifier.size(width = 16.dp, height = 12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Card body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .customShadow(
                    shape = RoundedCornerShape(20.dp),
                    color = shadowColor,
                    blurRadius = shadowBlur,
                    offsetY = 0f
                )
                .clip(RoundedCornerShape(20.dp))
                .background(cardColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // SVG illustration
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/$svgAssetPath")
                        .decoderFactory(SvgDecoder.Factory())
                        .build(),
                    contentDescription = label,
                    modifier = Modifier.size(width = imageWidth, height = imageHeight)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Label
                Text(
                    text = label,
                    fontFamily = LexendDeca,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = textSize.sp,
                    color = labelColor
                )
            }
        }
    }
}
