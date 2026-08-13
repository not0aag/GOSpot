package week11.st695922.finalproject.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
 * Screen shown after a successful check-in or check-out. This validates
 * that the user-initiated Firestore write was successful.
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
        Text(
            if (isCheckIn) {
                "Your check-in was recorded and the live parking availability was updated."
            } else {
                "Your checkout was recorded and the parking space was returned to the live count."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.85f)
        )
        Spacer(Modifier.height(24.dp))
        SecondaryButton(text = "Done", onClick = onDone)
    }
}
