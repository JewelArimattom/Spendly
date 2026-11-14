package com.spendly.app.domain.model

data class AppSettings(
    val id: String = "default",
    val currency: String = "INR",
    val currencySymbol: String = "₹",
    val theme: String = "system",
    val biometricEnabled: Boolean = false,
    val pinHash: String? = null,
    val autoLockOnBackground: Boolean = false,
    val usageMonitoringEnabled: Boolean = true,
    val notificationMonitoringEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val appLockEnabled: Boolean
        get() = autoLockOnBackground
}
