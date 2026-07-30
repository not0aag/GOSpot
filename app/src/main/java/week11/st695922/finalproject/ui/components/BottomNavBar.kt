package week11.st695922.finalproject.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import week11.st695922.finalproject.ui.navigation.Route

/**
 * The nav flow diagram calls these "Four bottom-tab siblings" that "switch
 * freely - no back-stack between them," which is exactly what this does:
 * tapping a tab replaces the current Route rather than pushing onto a stack.
 */
@Composable
fun GoSpotBottomNavBar(
    currentTab: Route.MainTab,
    onTabSelected: (Route.MainTab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentTab is Route.MainTab.Map,
            onClick = { onTabSelected(Route.MainTab.Map) },
            icon = { Icon(Icons.Filled.LocationOn, contentDescription = "Map") },
            label = { Text("Map") }
        )
        NavigationBarItem(
            selected = currentTab is Route.MainTab.Stations,
            onClick = { onTabSelected(Route.MainTab.Stations) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Stations") },
            label = { Text("Stations") }
        )
        NavigationBarItem(
            selected = currentTab is Route.MainTab.Alerts,
            onClick = { onTabSelected(Route.MainTab.Alerts) },
            icon = { Icon(Icons.Filled.Notifications, contentDescription = "Alerts") },
            label = { Text("Alerts") }
        )
        NavigationBarItem(
            selected = currentTab is Route.MainTab.Profile,
            onClick = { onTabSelected(Route.MainTab.Profile) },
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}
