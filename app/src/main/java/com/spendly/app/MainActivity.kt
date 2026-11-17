package com.spendly.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.spendly.app.core.designsystem.theme.SpendlyTheme
import com.spendly.app.core.security.AppLockManager
import com.spendly.app.core.security.BiometricAuthManager
import com.spendly.app.domain.repository.AppSettingsRepository
import com.spendly.app.detection.reminders.SmartReminderScheduler
import com.spendly.app.detection.sessions.UpiAppSessionMonitor
import com.spendly.app.navigation.SpendlyApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    @Inject
    lateinit var appSettingsRepository: AppSettingsRepository

    @Inject
    lateinit var upiAppSessionMonitor: UpiAppSessionMonitor

    @Inject
    lateinit var smartReminderScheduler: SmartReminderScheduler

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val endedSessions = upiAppSessionMonitor.scanAndReconcileSessions(lookbackMillis = 2 * 60 * 60 * 1000L)
            endedSessions
                .filter { it.sessionStatus == "ENDED" && it.reminderStatus == "SCHEDULED" }
                .distinctBy { it.id }
                .forEach { smartReminderScheduler.scheduleReminderForSession(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SpendlyTheme {
                SpendlyApp(
                    appLockManager = appLockManager,
                    biometricAuthManager = biometricAuthManager,
                    appSettingsRepository = appSettingsRepository
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
