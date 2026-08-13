package week11.st695922.finalproject.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import week11.st695922.finalproject.model.Station
import week11.st695922.finalproject.ui.components.ErrorBanner
import week11.st695922.finalproject.ui.components.OccupancyBar
import week11.st695922.finalproject.ui.components.PrimaryButton
import week11.st695922.finalproject.ui.components.SecondaryButton
import week11.st695922.finalproject.ui.navigation.StationNavigator
import week11.st695922.finalproject.ui.theme.GoGreen

@Composable
fun StationDetailScreen(
    station: Station,
    isCheckedIn: Boolean,
    onToggleCheckIn: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    isPending: Boolean = false,
    errorMessage: String? = null,
    onDismissError: () -> Unit = {}
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GoGreen)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                station.address,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    "${station.spacesFree}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    " / ${station.capacityTotal}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(Modifier.weight(1f))
                if (station.percentFull >= 95) {
                    Text("full", color = Color.White.copy(alpha = 0.85f))
                }
            }
            Text(
                "spaces available right now",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(8.dp))
            OccupancyBar(percentFull = station.percentFull)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (errorMessage != null) {
                ErrorBanner(message = errorMessage, onDismiss = onDismissError)
                Spacer(Modifier.height(8.dp))
            }

            SecondaryButton(
                text = when {
                    isPending -> "Working..."
                    isCheckedIn -> "Check out"
                    else -> "Check in"
                },
                onClick = onToggleCheckIn,
                enabled = !isPending
            )
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = "Navigate here",
                onClick = { StationNavigator.navigate(context, station) }
            )
        }
    }
}
