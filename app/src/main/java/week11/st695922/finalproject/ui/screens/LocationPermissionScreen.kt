package week11.st695922.finalproject.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import week11.st695922.finalproject.ui.components.SecondaryButton
import week11.st695922.finalproject.ui.components.PrimaryButton
import week11.st695922.finalproject.ui.theme.GoGreen
import week11.st695922.finalproject.ui.theme.GoGreenContainer

/**
 * Matches the "Allow location all the time" mock, but the button underneath
 * only requests foreground ACCESS_FINE_LOCATION (Week 8, Slides 14, 23, 26).
 * Background location + the geofence triggers this copy describes are not
 * covered by the course material - see the scoping note in the build summary.
 */
@Composable
fun LocationPermissionScreen(
    onPermissionResolved: (granted: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onPermissionResolved(granted) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(GoGreenContainer),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(GoGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.White)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Allow location all the time",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "GOSpot draws a geofence around each GO lot. Crossing it checks you in and out automatically - no manual reporting.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        FeatureRow(
            title = "Background access is required",
            subtitle = "Geofence triggers must fire while the app is closed."
        )
        FeatureRow(
            title = "Battery friendly",
            subtitle = "The OS wakes the app only at lot boundaries, not continuously."
        )
        FeatureRow(
            title = "Only station events are stored",
            subtitle = "Firestore records arrival and departure - never your route."
        )

        Spacer(Modifier.height(24.dp))

        PrimaryButton(
            text = "Allow all the time",
            onClick = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
        )
        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            text = "Not now",
            onClick = { onPermissionResolved(false) }
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FeatureRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = GoGreen,
            modifier = Modifier.padding(end = 12.dp, top = 2.dp)
        )
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
