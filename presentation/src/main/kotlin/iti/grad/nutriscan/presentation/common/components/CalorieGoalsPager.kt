package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.onboarding.carousel.view.components.OnboardingPageIndicator

/**
 * The Calories screen's TDEE/BMI card pair as a two-page [SwipeableCardStack]
 * with a dot indicator underneath — see [CalorieGoalsCard] and [BmiGoalCard].
 */
@Composable
fun CalorieGoalsPager(
    tdee: Int,
    caloriesGained: Int,
    bmi: Double?,
    modifier: Modifier = Modifier,
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val pages = listOf<@Composable () -> Unit>(
        { CalorieGoalsCard(tdee = tdee, caloriesGained = caloriesGained) },
        { BmiGoalCard(bmi = bmi) },
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SwipeableCardStack(
            pages = pages,
            currentPage = currentPage,
            onPageChange = { currentPage = it },
        )
        Spacer(modifier = Modifier.height(10.dp))
        OnboardingPageIndicator(currentPage = currentPage, pageCount = pages.size)
    }
}
