package iti.grad.nutriscan.presentation.profile_setup.view.components

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.profile_setup.state.ProfileSetupPagerEvent
import iti.grad.presentation.R
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun WeightSelectionPage(
    selectedWeightKg: Int,
    currentPage: Int,
    pageCount: Int,
    onEvent: (ProfileSetupPagerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val minWeight = 30
    val maxWeight = 200
    val totalSteps = maxWeight - minWeight + 1
    val initialPage = (selectedWeightKg - minWeight).coerceIn(0, totalSteps - 1)

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { totalSteps }
    )

    var isDraggingRuler by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        val newWeight = pagerState.currentPage + minWeight
        if (newWeight != selectedWeightKg) {
            onEvent(ProfileSetupPagerEvent.SelectWeight(newWeight))
        }
    }

    LaunchedEffect(selectedWeightKg) {
        val targetPage = selectedWeightKg - minWeight
        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress && !isDraggingRuler) {
            pagerState.scrollToPage(targetPage)
        }
    }

    val soundPool = remember {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attrs)
            .build()
    }
    var soundId by remember { mutableStateOf(0) }
    LaunchedEffect(soundPool) {
        soundId = soundPool.load(context, R.raw.click, 1)
    }

    DisposableEffect(soundPool) {
        onDispose {
            soundPool.release()
        }
    }

    var isFirstLaunch by remember { mutableStateOf(true) }
    LaunchedEffect(selectedWeightKg) {
        if (isFirstLaunch) {
            isFirstLaunch = false
            return@LaunchedEffect
        }
        if (soundId != 0) {
            try {
                soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
            } catch (_: Exception) {
                // Ignore audio errors
            }
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val cardWidth = 130.dp
    val horizontalPadding = (screenWidth - cardWidth) / 2

    val pagerPageSizePx = with(LocalDensity.current) { cardWidth.toPx() }
    val tickSpacing = 18.dp
    val tickSpacingPx = with(LocalDensity.current) { tickSpacing.toPx() }
    val gearRatio = pagerPageSizePx / tickSpacingPx

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(AppTheme.colors.Background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(152.dp))

        ProfileSetupHeader(
            prefixRes = R.string.profile_setup_weight_title_prefix,
            highlightRes = R.string.profile_setup_weight_title_highlight,
            subtitleRes = R.string.profile_setup_weight_subtitle,
            currentPage = currentPage,
            pageCount = pageCount,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Icon(
            painter = painterResource(R.drawable.ic_selected_arrow),
            contentDescription = null,
            tint = AppTheme.colors.HeightSelectedCardBackground,
            modifier = Modifier.size(width = 16.dp, height = 12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(cardWidth),
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) { page ->
            val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).absoluteValue
            val isSelected = pageOffset < 0.5f

            WeightCard(
                weightKg = page + minWeight,
                pageOffset = pageOffset,
                isSelected = isSelected,
                cardWidth = cardWidth
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val currentWeightDouble = pagerState.currentPage + minWeight + pagerState.currentPageOffsetFraction.toDouble()

        Spacer(modifier = Modifier.height(20.dp))
        WeightArcRuler(
            currentWeightDouble = currentWeightDouble,
            minWeight = minWeight,
            maxWeight = maxWeight,
            tickSpacingPx = tickSpacingPx,
            gearRatio = gearRatio,
            onDraggingChanged = { isDraggingRuler = it },
            onDragScroll = { dragDelta ->
                coroutineScope.launch {
                    pagerState.scrollBy(dragDelta)
                }
            },
            onDragEnd = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage)
                }
            }
        )
    }
}
