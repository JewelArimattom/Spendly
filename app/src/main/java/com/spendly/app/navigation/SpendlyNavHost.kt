package com.spendly.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.spendly.app.features.accounts.AccountsScreen
import com.spendly.app.features.analytics.AnalyticsScreen
import com.spendly.app.features.home.HomeScreen
import com.spendly.app.features.settings.SettingsScreen

@Composable
fun SpendlyNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.HOME.route,
        modifier = modifier
    ) {
        composable(TopLevelDestination.HOME.route) {
            HomeScreen()
        }
        composable(TopLevelDestination.ACCOUNTS.route) {
            AccountsScreen()
        }
        composable(TopLevelDestination.ANALYTICS.route) {
            AnalyticsScreen()
        }
        composable(TopLevelDestination.SETTINGS.route) {
            SettingsScreen()
        }
    }
}
