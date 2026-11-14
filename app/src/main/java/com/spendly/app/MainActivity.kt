package com.spendly.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.spendly.app.core.designsystem.theme.SpendlyTheme
import com.spendly.app.core.security.AppLockManager
import com.spendly.app.core.security.BiometricAuthManager
import com.spendly.app.domain.repository.AppSettingsRepository
import com.spendly.app.navigation.SpendlyApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    @Inject
    lateinit var appSettingsRepository: AppSettingsRepository

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
    }
}
