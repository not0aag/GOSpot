package week11.st695922.finalproject.ui.screens

import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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


@Composable
fun LocationPermissionScreen(
    onPermissionResolved: (granted: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var awaitingBackgroundStep by remember { mutableStateOf(false) }

    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        when {
            !granted -> onPermissionResolved(false)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> awaitingBackgroundStep = true
            else -> onPermissionResolved(true)
        }
    }

    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> onPermissionResolved(true) }

    if (awaitingBackgroundStep) {
        BackgroundLocationStep(
            onAllow = { backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) },
            onNotNow = { onPermissionResolved(true) },
            modifier = modifier
        )
    } else {
        ForegroundLocationStep(
            onAllow = { foregroundLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
            onNotNow = { onPermissionResolved(false) },
            modifier = modifier
        )
    }
}

@Composable
private fun ForegroundLocationStep(
    onAllow: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    PermissionStepScaffold(
        title = "Allow location access",
        description = "GOSpot uses your location to find nearby GO stations and show " +
            "how far you are from each parking lot.",
        features = listOf(
            "See nearby stations" to "Distances and directions are based on where you are.",
            "Required to continue" to "The map and stations list need this to work."
        ),
        primaryLabel = "Allow while using the app",
        onAllow = onAllow,
        onNotNow = onNotNow,
        modifier = modifier
    )
}

@Composable
private fun BackgroundLocationStep(
    onAllow: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    PermissionStepScaffold(
        title = "Allow location all the time",
        description = "GOSpot draws a geofence around each GO lot. Crossing it checks you in and out automatically - no manual reporting.",
        features = listOf(
            "Background access is required" to "Geofence triggers must fire while the app is closed.",
            "Battery friendly" to "The OS wakes the app only at lot boundaries, not continuously.",
            "Only station events are stored" to "Firestore records arrival and departure - never your route."
        ),
        primaryLabel = "Allow all the time",
        onAllow = onAllow,
        onNotNow = onNotNow,
        modifier = modifier
    )
}

@Composable
private fun PermissionStepScaffold(
    title: String,
    description: String,
    features: List<Pair<String, String>>,
    primaryLabel: String,
    onAllow: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
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
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        features.forEach { (rowTitle, subtitle) ->
            FeatureRow(title = rowTitle, subtitle = subtitle)
        }

        Spacer(Modifier.height(24.dp))

        PrimaryButton(text = primaryLabel, onClick = onAllow)
        Spacer(Modifier.height(8.dp))
        SecondaryButton(text = "Not now", onClick = onNotNow)
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
