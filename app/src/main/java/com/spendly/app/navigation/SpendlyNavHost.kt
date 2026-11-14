package com.spendly.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.spendly.app.features.activity.ActivityScreen
import com.spendly.app.features.home.HomeScreen
import com.spendly.app.features.insights.InsightsScreen
import com.spendly.app.features.onboarding.OnboardingScreen
import com.spendly.app.features.settings.SettingsScreen

@Composable
fun SpendlyNavHost(
    navController: NavHostController,
    startDestination: String = TopLevelDestination.HOME.route,
    onNavigateToQuickEntry: (appName: String?, timestamp: Long?, sessionId: String?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(TopLevelDestination.HOME.route) {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable(TopLevelDestination.HOME.route) {
            HomeScreen(
                viewModel = hiltViewModel(),
                onNavigateToQuickEntry = onNavigateToQuickEntry,
                onNavigateToActivity = {
                    navController.navigate(TopLevelDestination.ACTIVITY.route)
                }
            )
        }
        composable(TopLevelDestination.INSIGHTS.route) {
            InsightsScreen(
                viewModel = hiltViewModel()
            )
        }
        composable(TopLevelDestination.ACTIVITY.route) {
            ActivityScreen(
                viewModel = hiltViewModel()
            )
        }
        composable(TopLevelDestination.SETTINGS.route) {
            SettingsScreen(
                viewModel = hiltViewModel()
            )
        }
    }
}
