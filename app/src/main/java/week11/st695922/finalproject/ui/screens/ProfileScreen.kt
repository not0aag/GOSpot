package week11.st695922.finalproject.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import week11.st695922.finalproject.model.UserProfile
import week11.st695922.finalproject.ui.hasNotificationPermission

@Composable
fun ProfileScreen(
    profile: UserProfile,
    checkInsCount: Int,
    alertsEnabled: Boolean,
    onAlertsEnabledChange: (Boolean) -> Unit,
    automaticCheckInEnabled: Boolean,
    automaticCheckInBusy: Boolean,
    onAutomaticCheckInChange: (Boolean) -> Unit,
    onChangeHomeStation: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Text(
            "Profile",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp, bottom = 20.dp)
        )

        ProfileIdentityCard(profile)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                value = "$checkInsCount",
                label = "Check-ins recorded",
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            StatCard(
                value = profile.homeStationName.ifBlank { "Not set" },
                label = "Home station",
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        SectionLabel("Parking")
        SettingsCard {
            ActionSettingsRow(
                icon = Icons.Filled.Home,
                title = "Home station",
                subtitle = profile.homeStationName.ifBlank { "Not set" },
                actionLabel = "Change",
                onAction = onChangeHomeStation
            )
            SettingsDivider()
            AutomaticCheckInToggle(
                enabled = automaticCheckInEnabled,
                busy = automaticCheckInBusy,
                onEnabledChange = onAutomaticCheckInChange
            )
            SettingsDivider()
            ToggleRow(
                icon = Icons.Filled.DirectionsCar,
                title = "Contribute to live counts",
                subtitle = "Manual check-ins update the shared parking count.",
                initialValue = true
            )
        }

        SectionLabel("Alerts")
        SettingsCard {
            AlertsToggle(
                homeStationName = profile.homeStationName,
                hasHomeStation = profile.homeStationId.isNotBlank(),
                alertsEnabled = alertsEnabled,
                onAlertsEnabledChange = onAlertsEnabledChange
            )
        }

        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onSignOut,
            enabled = !automaticCheckInBusy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Sign out",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ProfileIdentityCard(profile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initials(profile.fullName.ifBlank { profile.email }),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.fullName.ifBlank { profile.email },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Signed in with Firebase",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.heightIn(min = 112.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 28.dp, bottom = 10.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(content = content)
    }
}

@Composable
private fun ActionSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon)
        Spacer(Modifier.width(12.dp))
        SettingsText(title, subtitle, Modifier.weight(1f))
        TextButton(onClick = onAction) {
            Text(actionLabel, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    initialValue: Boolean
) {
    var checked by rememberSaveable { mutableStateOf(initialValue) }
    ToggleRow(icon, title, subtitle, checked, { checked = it })
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon, enabled)
        Spacer(Modifier.width(12.dp))
        SettingsText(title, subtitle, Modifier.weight(1f), enabled)
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun SettingsText(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val alpha = if (enabled) 1f else 0.55f
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
        )
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector, enabled: Boolean = true) {
    val alpha = if (enabled) 1f else 0.55f
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    )
}

@Composable
private fun AutomaticCheckInToggle(
    enabled: Boolean,
    busy: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var fineLocationGranted by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION))
    }
    var backgroundLocationGranted by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION))
    }
    var pendingEnable by rememberSaveable { mutableStateOf(false) }
    var permissionAttempted by rememberSaveable { mutableStateOf(false) }

    fun refreshPermissions() {
        fineLocationGranted = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        backgroundLocationGranted = hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshPermissions()
        if (pendingEnable && fineLocationGranted && backgroundLocationGranted) {
            pendingEnable = false
            onEnabledChange(true)
        }
    }

    fun openAppSettings() {
        permissionAttempted = true
        pendingEnable = true
        settingsLauncher.launch(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
        )
    }

    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        fineLocationGranted = granted
        if (granted) openAppSettings() else permissionAttempted = true
    }

    LifecycleResumeEffect(Unit) {
        refreshPermissions()
        if (pendingEnable && fineLocationGranted && backgroundLocationGranted) {
            pendingEnable = false
            onEnabledChange(true)
        }
        onPauseOrDispose { }
    }

    val permissionsReady = fineLocationGranted && backgroundLocationGranted
    val subtitle = when {
        busy -> "Updating automatic check-in…"
        enabled && !permissionsReady ->
            "Background location is missing. Turn this off, then enable it again."
        else -> "Check in within 300 m and check out when you leave."
    }
    ToggleRow(
        icon = Icons.Filled.LocationOn,
        title = "Automatic check-in",
        subtitle = subtitle,
        checked = enabled,
        onCheckedChange = { wantsEnabled ->
            permissionAttempted = false
            when {
                !wantsEnabled -> {
                    pendingEnable = false
                    onEnabledChange(false)
                }
                permissionsReady -> onEnabledChange(true)
                !fineLocationGranted -> {
                    pendingEnable = true
                    foregroundLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                else -> openAppSettings()
            }
        },
        enabled = !busy
    )

    if ((enabled && !permissionsReady) || (permissionAttempted && !permissionsReady)) {
        Text(
            "Allow location all the time in Android Settings to use automatic check-in.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 68.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun AlertsToggle(
    homeStationName: String,
    hasHomeStation: Boolean,
    alertsEnabled: Boolean,
    onAlertsEnabledChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasNotificationPermission(context)) }
    var justDenied by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        hasPermission = hasNotificationPermission(context)
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        justDenied = !granted
        if (granted) onAlertsEnabledChange(true)
    }

    val effectivelyOn = alertsEnabled && hasPermission
    ToggleRow(
        icon = Icons.Filled.Notifications,
        title = "Lot filling up alerts",
        subtitle = if (!hasHomeStation) {
            "Set a home station first."
        } else {
            "Get notified when $homeStationName is near capacity."
        },
        checked = effectivelyOn,
        onCheckedChange = { wantsOn ->
            justDenied = false
            when {
                !wantsOn -> onAlertsEnabledChange(false)
                hasPermission -> onAlertsEnabledChange(true)
                else -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        enabled = hasHomeStation
    )

    if (hasHomeStation && !hasPermission && (justDenied || alertsEnabled)) {
        Text(
            "Notifications are turned off for GOSpot. Enable them in Settings to get lot alerts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 68.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

private fun hasPermission(context: android.content.Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun initials(name: String): String =
    name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifBlank { "GO" }
