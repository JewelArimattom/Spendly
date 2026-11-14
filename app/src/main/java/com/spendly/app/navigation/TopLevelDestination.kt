package com.spendly.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    HOME(
        route = "home",
        icon = Icons.Filled.Home,
        label = "Home"
    ),
    INSIGHTS(
        route = "insights",
        icon = Icons.Filled.Insights,
        label = "Insights"
    ),
    ACTIVITY(
        route = "activity",
        icon = Icons.Filled.ReceiptLong,
        label = "Activity"
    ),
    SETTINGS(
        route = "settings",
        icon = Icons.Filled.Settings,
        label = "Settings"
    )
}
