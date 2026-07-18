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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
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
fun HeightSelectionPage(
    selectedHeightCm: Int,
    currentPage: Int,
    pageCount: Int,
    onEvent: (ProfileSetupPagerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Height range: 100cm to 250cm (151 options)
    val minHeight = 100
    val maxHeight = 250
    val totalSteps = maxHeight - minHeight + 1
    val initialPage = (selectedHeightCm - minHeight).coerceIn(0, totalSteps - 1)

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { totalSteps }
    )

    var isDraggingRuler by remember { mutableStateOf(false) }

    // Sync Pager swipes to state
    LaunchedEffect(pagerState.currentPage) {
        val newHeight = pagerState.currentPage + minHeight
        if (newHeight != selectedHeightCm) {
            onEvent(ProfileSetupPagerEvent.SelectHeight(newHeight))
        }
    }

    // Sync external state changes back to Pager, but only when not actively dragging/scrolling
    LaunchedEffect(selectedHeightCm) {
        val targetPage = selectedHeightCm - minHeight
        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress && !isDraggingRuler) {
            pagerState.scrollToPage(targetPage)
        }
    }

    // Low-latency SoundPool for click sound
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
    LaunchedEffect(selectedHeightCm) {
        if (isFirstLaunch) {
            isFirstLaunch = false
            return@LaunchedEffect
        }
        if (soundId != 0) {
            try {
                soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
            } catch (e: Exception) {
                // Ignore audio errors
            }
        }
    }

    // Measure screen width for dynamic centering of Pager
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

        // Title + Subtitle
        ProfileSetupHeader(
            prefixRes = R.string.profile_setup_height_title_prefix,
            highlightRes = R.string.profile_setup_height_title_highlight,
            suffixRes = R.string.profile_setup_height_title_suffix,
            isSuffixHighlighted = false,
            subtitleRes = R.string.profile_setup_height_subtitle,
            currentPage = currentPage,
            pageCount = pageCount,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Pointer Arrow pointing down to the selected center card
        Icon(
            painter = painterResource(R.drawable.ic_selected_arrow),
            contentDescription = null,
            tint = AppTheme.colors.HeightSelectedCardBackground,
            modifier = Modifier.size(width = 16.dp, height = 12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Cards Pager
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(cardWidth),
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val isSelected = pageOffset < 0.5f

            HeightCard(
                height = page + minHeight,
                pageOffset = pageOffset,
                isSelected = isSelected,
                cardWidth = cardWidth
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Custom Ruler Canvas
        val currentHeightDouble = pagerState.currentPage + minHeight + pagerState.currentPageOffsetFraction.toDouble()

        SoundwaveHeightRuler(
            currentHeightDouble = currentHeightDouble,
            minHeight = minHeight,
            maxHeight = maxHeight,
            tickSpacingPx = tickSpacingPx,
            gearRatio = gearRatio,
            isDraggingRuler = isDraggingRuler,
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
