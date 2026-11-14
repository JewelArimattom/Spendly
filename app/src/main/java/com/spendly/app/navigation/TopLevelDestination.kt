package com.spendly.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
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
    ACCOUNTS(
        route = "accounts",
        icon = Icons.Filled.AccountBalance,
        label = "Accounts"
    ),
    ANALYTICS(
        route = "analytics",
        icon = Icons.Filled.Analytics,
        label = "Analytics"
    ),
    SETTINGS(
        route = "settings",
        icon = Icons.Filled.Settings,
        label = "Settings"
    )
}
