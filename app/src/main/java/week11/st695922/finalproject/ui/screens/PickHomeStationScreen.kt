package week11.st695922.finalproject.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
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
import week11.st695922.finalproject.ui.components.PrimaryButton
import week11.st695922.finalproject.ui.components.SecondaryButton
import week11.st695922.finalproject.ui.theme.GoGreen
import week11.st695922.finalproject.ui.theme.GoGreenContainer

@Composable
fun PickHomeStationScreen(
    stations: List<Station>,
    onContinue: (Station) -> Unit,
    onSeedDemoStations: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf<Station?>(null) }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "Pick your home station",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = "We will watch this lot for you and alert you before it fills.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
        }

        if (stations.isEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    "No stations found in Firestore yet.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                SecondaryButton(text = "Load demo stations", onClick = onSeedDemoStations)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 4.dp)
            ) {
                items(stations) { station ->
                    val isSelected = selected?.id == station.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) GoGreen else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(if (isSelected) GoGreenContainer else MaterialTheme.colorScheme.surface)
                            .clickable { selected = station }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(station.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${station.capacityTotal} spaces",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GoGreen)
                        } else {
                            RadioButton(selected = false, onClick = { selected = station })
                        }
                    }
                }
            }
        }

        PrimaryButton(
            text = "Continue",
            onClick = { selected?.let(onContinue) },
            enabled = selected != null,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
    }
}
