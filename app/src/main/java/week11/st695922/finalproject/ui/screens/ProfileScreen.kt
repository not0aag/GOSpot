package week11.st695922.finalproject.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import week11.st695922.finalproject.model.UserProfile
import week11.st695922.finalproject.ui.theme.GoGreen

@Composable
fun ProfileScreen(
    profile: UserProfile,
    checkInsCount: Int,
    alertsEnabled: Boolean,
    onAlertsEnabledChange: (Boolean) -> Unit,
    onChangeHomeStation: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Text(
            "Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
        )

        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GoGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials(profile.fullName), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(profile.fullName.ifBlank { profile.email }, fontWeight = FontWeight.Bold)
                    Text(
                        "Signed in with Firebase",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(value = "$checkInsCount", label = "Check-ins recorded", modifier = Modifier.weight(1f))
            StatCard(
                value = profile.homeStationName.ifBlank { "-" },
                label = "Home station",
                modifier = Modifier.weight(1f)
            )
        }

        SectionLabel("Parking")
        SettingsRow(
            title = "Home station",
            subtitle = profile.homeStationName.ifBlank { "Not set" },
            trailing = { TextButton(onClick = onChangeHomeStation) { Text("Change") } }
        )
        ToggleRow(
            title = "Automatic check-in",
            subtitle = "Automatic check-in/out via device location.",
            initialValue = false
        )
        ToggleRow(
            title = "Contribute to live counts",
            subtitle = "Your manual check-ins update the shared Firestore count",
            initialValue = true
        )

        SectionLabel("Alerts")
        ToggleRow(
            title = "Lot filling up alerts",
            subtitle = if (profile.homeStationName.isBlank()) {
                "Set a home station first."
            } else {
                "Get notified when ${profile.homeStationName} is near capacity."
            },
            checked = alertsEnabled,
            onCheckedChange = onAlertsEnabledChange,
            enabled = profile.homeStationId.isNotBlank()
        )

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onSignOut) {
            Text("Sign out", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(32.dp))
    }
}

private fun initials(name: String): String =
    name.trim().split(" ").filter { it.isNotBlank() }.take(2).map { it.first().uppercaseChar() }.joinToString("")

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsRow(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

/** Self-managed toggle, for rows that are not yet backed by stored state. */
@Composable
private fun ToggleRow(title: String, subtitle: String, initialValue: Boolean) {
    var checked by remember { mutableStateOf(initialValue) }
    ToggleRow(
        title = title,
        subtitle = subtitle,
        checked = checked,
        onCheckedChange = { checked = it }
    )
}

/** Controlled toggle, for rows whose state lives in a ViewModel. */
@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    SettingsRow(title = title, subtitle = subtitle) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
