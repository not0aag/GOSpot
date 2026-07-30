package week11.st695922.finalproject.ui.screens

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import week11.st695922.finalproject.model.Station
import week11.st695922.finalproject.ui.components.OccupancyBar
import week11.st695922.finalproject.ui.components.occupancyColor
import week11.st695922.finalproject.ui.theme.GoGreen

private enum class SortMode(val label: String) {
    NEAREST("Nearest"), MOST_SPACE("Most space"), HOME_FIRST("Home first")
}

@Composable
fun StationsListScreen(
    stations: List<Station>,
    checkedInStationIds: Set<String>,
    homeStationId: String,
    pendingStationId: String?,
    userLocation: Location?,
    onStationClick: (Station) -> Unit,
    onToggleCheckIn: (Station) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(SortMode.NEAREST) }

    fun distanceMetersTo(station: Station): Float? {
        val here = userLocation ?: return null
        val stationLocation = Location("station").apply {
            latitude = station.lat
            longitude = station.lng
        }
        return here.distanceTo(stationLocation)
    }

    val filtered = stations.filter { it.name.contains(query, ignoreCase = true) }
    val sorted = when (sortMode) {
        // "Nearest" here is straight-line distance from the one-time location
        // read (FusedLocationProviderClient, Week 8), not real drive-time
        // routing - Google Maps Directions isn't covered by the course
        // material. Falls back to Firestore document order with no location.
        SortMode.NEAREST -> if (userLocation != null) {
            filtered.sortedBy { distanceMetersTo(it) ?: Float.MAX_VALUE }
        } else {
            filtered
        }
        SortMode.MOST_SPACE -> filtered.sortedByDescending { it.spacesFree }
        SortMode.HOME_FIRST -> filtered.sortedByDescending { it.id == homeStationId }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text("Stations", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Lakeshore West · updating live",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search a station") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortMode.entries.forEach { mode ->
                    FilterChip(
                        selected = sortMode == mode,
                        onClick = { sortMode = mode },
                        label = { Text(mode.label) }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp)
        ) {
            items(sorted, key = { it.id }) { station ->
                val distanceKm = distanceMetersTo(station)?.let { it / 1000f }
                StationRow(
                    station = station,
                    isHome = station.id == homeStationId,
                    isCheckedIn = station.id in checkedInStationIds,
                    isPending = station.id == pendingStationId,
                    distanceKm = distanceKm,
                    onClick = { onStationClick(station) },
                    onToggleCheckIn = { onToggleCheckIn(station) }
                )
            }
        }
    }
}

@Composable
private fun StationRow(
    station: Station,
    isHome: Boolean,
    isCheckedIn: Boolean,
    isPending: Boolean,
    distanceKm: Float?,
    onClick: () -> Unit,
    onToggleCheckIn: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(),
        border = if (isHome) BorderStroke(2.dp, GoGreen) else null,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(occupancyColor(station.percentFull))
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(station.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${station.spacesFree} spaces free · ${station.percentFull}% full",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (distanceKm != null) {
                    Text(
                        "%.1f km".format(distanceKm),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OccupancyBar(percentFull = station.percentFull)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onToggleCheckIn, enabled = !isPending) {
                    Text(if (isCheckedIn) "Check out" else "Check in")
                }
            }
        }
    }
}
