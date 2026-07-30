package week11.st695922.finalproject.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import week11.st695922.finalproject.ui.theme.GoAmber
import week11.st695922.finalproject.ui.theme.GoGreen
import week11.st695922.finalproject.ui.theme.GoRed

/** Color thresholds approximated from the station list/detail screenshots. */
fun occupancyColor(percentFull: Int) = when {
    percentFull >= 85 -> GoRed
    percentFull >= 60 -> GoAmber
    else -> GoGreen
}

@Composable
fun OccupancyBar(
    percentFull: Int,
    modifier: Modifier = Modifier
) {
    LinearProgressIndicator(
        progress = { percentFull / 100f },
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        color = occupancyColor(percentFull),
        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )
}
