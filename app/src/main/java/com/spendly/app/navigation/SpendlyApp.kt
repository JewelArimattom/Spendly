package com.spendly.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spendly.app.core.security.AppLockManager
import com.spendly.app.core.security.BiometricAuthManager
import com.spendly.app.domain.repository.AppSettingsRepository
import com.spendly.app.features.lock.LockScreen
import com.spendly.app.features.quickentry.QuickExpenseEntrySheet
import com.spendly.app.features.quickentry.QuickExpenseEntryViewModel

data class QuickEntryParams(
    val appName: String? = null,
    val timestamp: Long? = null,
    val sessionId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendlyApp(
    appLockManager: AppLockManager,
    biometricAuthManager: BiometricAuthManager,
    appSettingsRepository: AppSettingsRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isLocked by appLockManager.isLocked.collectAsState()
    var quickEntryParams by remember { mutableStateOf<QuickEntryParams?>(null) }
    var isOnboardingCompleted by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        val settings = appSettingsRepository.getSettingsSync()
        isOnboardingCompleted = settings?.onboardingCompleted ?: false
    }

    if (isOnboardingCompleted == null) {
        // Wait for settings read
        return
    }

    val startDestination = if (isOnboardingCompleted == true) {
        TopLevelDestination.HOME.route
    } else {
        "onboarding"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val isTopLevelDestination = TopLevelDestination.entries.any { it.route == currentDestination?.route }

        Scaffold(
            bottomBar = {
                if (isTopLevelDestination) {
                    NavigationBar {
                        TopLevelDestination.entries.forEach { destination ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == destination.route
                            } == true

                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label
                                    )
                                },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            SpendlyNavHost(
                navController = navController,
                startDestination = startDestination,
                onNavigateToQuickEntry = { app, time, session ->
                    quickEntryParams = QuickEntryParams(app, time, session)
                },
                modifier = Modifier.padding(innerPadding)
            )
        }

        // Quick Expense Entry Bottom Sheet
        quickEntryParams?.let { params ->
            val quickEntryViewModel: QuickExpenseEntryViewModel = hiltViewModel()
            LaunchedEffect(params) {
                quickEntryViewModel.prefillData(
                    appName = params.appName,
                    timestamp = params.timestamp,
                    sessionId = params.sessionId
                )
            }
            QuickExpenseEntrySheet(
                viewModel = quickEntryViewModel,
                onDismiss = { quickEntryParams = null }
            )
        }

        // App Lock Overlay
        if (isLocked) {
            LockScreen(
                appLockManager = appLockManager,
                biometricAuthManager = biometricAuthManager,
                onUnlocked = {}
            )
        }
    }
}
