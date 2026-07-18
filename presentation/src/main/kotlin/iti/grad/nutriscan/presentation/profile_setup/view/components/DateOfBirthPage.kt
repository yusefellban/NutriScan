package iti.grad.nutriscan.presentation.profile_setup.view.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca
import iti.grad.nutriscan.presentation.profile_setup.state.ProfileSetupPagerEvent
import iti.grad.nutriscan.presentation.home.view.components.customShadow
import iti.grad.presentation.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateOfBirthPage(
    selectedDateMillis: Long?,
    currentPage: Int,
    pageCount: Int,
    onEvent: (ProfileSetupPagerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDatePickerVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(152.dp))

        // Title + Subtitle
        ProfileSetupHeader(
            prefixRes = R.string.profile_setup_dob_title_prefix,
            highlightRes = R.string.profile_setup_dob_title_highlight,
            subtitleRes = R.string.profile_setup_dob_subtitle,
            currentPage = currentPage,
            pageCount = pageCount
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Age Display Card
        val age = remember(selectedDateMillis) {
            selectedDateMillis?.let { millis ->
                val birthDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                val today = LocalDate.now()
                ChronoUnit.YEARS.between(birthDate, today).coerceAtLeast(0)
            }
        }

        val shadowColor = AppTheme.colors.ShadowDobActive
        val isDark = isSystemInDarkTheme()
        val shadowBlur = if (isDark) 20f else 80f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .customShadow(
                    shape = RoundedCornerShape(20.dp),
                    color = shadowColor,
                    blurRadius = shadowBlur,
                    offsetY = 0f
                )
                .clip(RoundedCornerShape(20.dp))
                .background(AppTheme.colors.DobCardBackground)
                .clickable { isDatePickerVisible = true },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Age Number Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppTheme.colors.DobAgeBadgeBackground),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = age?.toString() ?: "—",
                        label = "age_animation"
                    ) { targetAge ->
                        Text(
                            text = targetAge,
                            fontFamily = LexendDeca,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = AppTheme.colors.DobAgeBadgeText
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // "Years" label
                Text(
                    text = stringResource(R.string.profile_setup_dob_years),
                    fontFamily = LexendDeca,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = AppTheme.colors.DobCardText
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Date Input Field
        val dateString = remember(selectedDateMillis) {
            selectedDateMillis?.let { millis ->
                val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, AppTheme.colors.DobInputBorder, RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isDatePickerVisible = true
                }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateString ?: stringResource(R.string.profile_setup_dob_date_hint),
                    fontFamily = LexendDeca,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = if (dateString != null) AppTheme.colors.DobInputText else AppTheme.colors.TextSecondary
                )

                Icon(
                    painter = painterResource(R.drawable.ic_date),
                    contentDescription = null,
                    tint = AppTheme.colors.DobCalendarIcon,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    if (isDatePickerVisible) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis ?: Instant.now().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { isDatePickerVisible = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onEvent(ProfileSetupPagerEvent.SelectDateOfBirth(millis))
                    }
                    isDatePickerVisible = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { isDatePickerVisible = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
