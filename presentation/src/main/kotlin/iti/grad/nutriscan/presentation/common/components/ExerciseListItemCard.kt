package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import coil3.compose.AsyncImage
import iti.grad.nutriscan.presentation.common.model.ExerciseUiModel
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

/**
 * Exercise list-item card matching the Figma "Exercises" screen.
 *
 * Shows: exercise thumbnail · name · "equipment • target" subtitle · chevron,
 * on a flat teal card surface.
 */
@Composable
fun ExerciseListItemCard(
    exercise: ExerciseUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.ExerciseCardBackground)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = exercise.imageUrl,
            contentDescription = null,
            placeholder = painterResource(id = R.drawable.dumbell),
            error = painterResource(id = R.drawable.dumbell),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = exercise.name,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.ExerciseCardTitle
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(
                    id = R.string.exercise_equipment_target,
                    exercise.equipment,
                    exercise.target
                ),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.ExerciseCardSubtitle
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = AppTheme.colors.ExerciseCardChevron,
            modifier = Modifier.size(20.dp)
        )
    }
}
