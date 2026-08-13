package week11.st695922.finalproject.ui.screens

import android.location.Location
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import week11.st695922.finalproject.model.Station
import week11.st695922.finalproject.ui.components.OccupancyBar
import week11.st695922.finalproject.ui.components.occupancyColor
import week11.st695922.finalproject.ui.state.UiState
import week11.st695922.finalproject.ui.theme.GoAmber
import week11.st695922.finalproject.ui.theme.GoGreen
import week11.st695922.finalproject.ui.theme.GoRed
import week11.st695922.finalproject.viewmodel.MapViewModel

@Composable
fun MapScreen(
    mapViewModel: MapViewModel,
    homeStation: Station?,
    userLocation: Location?,
    modifier: Modifier = Modifier
) {
    val stationsState by mapViewModel.stationsState.collectAsState()
    val selectedStation by mapViewModel.selectedStation.collectAsState()
    val stations = (stationsState as? UiState.Success)?.data ?: emptyList()
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = mapViewModel.cameraPositionState,
            onMapClick = { mapViewModel.selectStation(null) }
        ) {
            stations.forEach { station ->
                val markerState = remember(station.id) {
                    MarkerState(position = LatLng(station.lat, station.lng))
                }
                Marker(
                    state = markerState,
                    title = station.name,
                    icon = BitmapDescriptorFactory.defaultMarker(
                        when (occupancyColor(station.percentFull)) {
                            GoRed -> BitmapDescriptorFactory.HUE_RED
                            GoAmber -> BitmapDescriptorFactory.HUE_ORANGE
                            else -> BitmapDescriptorFactory.HUE_GREEN
                        }
                    ),
                    onClick = {
                        mapViewModel.selectStation(station)
                        true
                    }
                )
            }
        }

        // Header Chips
        Column(modifier = Modifier.padding(16.dp)) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lakeshore West · ${stations.size} stations", style = MaterialTheme.typography.bodyMedium)
                    Text("● LIVE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Info Card or Nearest Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            val stationToShow = selectedStation ?: homeStation
            
            if (stationToShow != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(stationToShow.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    if (selectedStation != null) "Selected Station" else "Your home station",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { mapViewModel.navigateToStation(context, stationToShow) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null)
                                Spacer(Modifier.padding(4.dp))
                                Text("Route")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "${stationToShow.spacesFree}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                " spaces left",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${stationToShow.percentFull}% full",
                                color = occupancyColor(stationToShow.percentFull),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OccupancyBar(percentFull = stationToShow.percentFull)
                    }
                }
            } else if (stations.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        mapViewModel.getNearestAvailableStation(userLocation)?.let {
                            mapViewModel.selectStation(it)
                        }
                    },
                    icon = { Icon(Icons.Default.Navigation, contentDescription = null) },
                    text = { Text("Route to nearest") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
