package week11.st695922.finalproject.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import week11.st695922.finalproject.model.Station
import week11.st695922.finalproject.ui.components.SecondaryButton
import week11.st695922.finalproject.ui.theme.GoGreen

/**
 * Stands in for the design's "Auto check-in" screen, which is triggered by a
 * GeofencingClient ENTER event - not covered by the course material. This
 * version shows the same confirmation layout, but reaches it from the
 * Stations list's manual "Check in" button instead of a background trigger.
 */
@Composable
fun CheckInConfirmationScreen(
    station: Station,
    isCheckIn: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(GoGreen)
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White)
        Text(
            text = if (isCheckIn) "Checked in at ${station.name}" else "Checked out of ${station.name}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 12.dp)
        )
        Spacer(Modifier.height(24.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("What just happened", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StepRow("Manual ${if (isCheckIn) "check-in" else "check-out"} tapped on Stations list")
                StepRow("Firestore write to stations/${station.id}.currentOccupancy")
                StepRow(
                    "Live occupancy now ${station.currentOccupancy} / ${station.capacityTotal}"
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "This screen replaces the design's automatic GeofencingClient ENTER/EXIT trigger, " +
                "which isn't covered by the course material - see the build's scoping notes.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f)
        )
        Spacer(Modifier.height(24.dp))
        SecondaryButton(text = "Done", onClick = onDone)
    }
}

@Composable
private fun StepRow(text: String) {
    Row(modifier = Modifier.padding(top = 8.dp)) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GoGreen, modifier = Modifier.padding(end = 8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
