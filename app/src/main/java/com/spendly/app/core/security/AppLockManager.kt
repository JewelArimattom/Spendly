package com.spendly.app.core.security

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import com.spendly.app.MainActivity
import com.spendly.app.domain.model.AppSettings
import com.spendly.app.domain.repository.AppSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettingsRepository: AppSettingsRepository,
    private val biometricAuthManager: BiometricAuthManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    @Volatile
    private var currentSettings: AppSettings? = null

    @Volatile
    private var backgroundTimestamp: Long = 0L

    @Volatile
    private var lockTimeoutMillis: Long = 0L // 0ms = immediate lock on background

    private var currentMainActivity: WeakReference<Activity>? = null
    private var startedActivitiesCount = 0

    init {
        // Observe settings changes
        scope.launch {
            appSettingsRepository.getSettingsFlow().collect { settings ->
                currentSettings = settings
                updateWindowSecurity()
                // If app lock was just enabled and the app doesn't have an active unlock session, lock it.
                if (settings?.appLockEnabled == true && backgroundTimestamp == 0L && startedActivitiesCount > 0) {
                    // Initial startup with app lock enabled
                    _isLocked.value = true
                }
            }
        }

        // Register custom lifecycle observer via ActivityLifecycleCallbacks
        registerLifecycleCallbacks()
    }

    private fun registerLifecycleCallbacks() {
        val app = context.applicationContext as? Application ?: return
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is MainActivity) {
                    currentMainActivity = WeakReference(activity)
                    configureWindowSecurity(activity.window)
                }
            }

            override fun onActivityStarted(activity: Activity) {
                if (activity is MainActivity) {
                    currentMainActivity = WeakReference(activity)
                    configureWindowSecurity(activity.window)
                }

                val wasInBackground = startedActivitiesCount == 0
                startedActivitiesCount++

                if (wasInBackground) {
                    onAppForeground()
                }
            }

            override fun onActivityResumed(activity: Activity) {
                if (activity is MainActivity) {
                    currentMainActivity = WeakReference(activity)
                    configureWindowSecurity(activity.window)
                }
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                startedActivitiesCount--
                if (startedActivitiesCount <= 0) {
                    startedActivitiesCount = 0
                    onAppBackground()
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (currentMainActivity?.get() === activity) {
                    currentMainActivity = null
                }
            }
        })
    }

    private fun onAppBackground() {
        backgroundTimestamp = System.currentTimeMillis()
    }

    private fun onAppForeground() {
        val settings = currentSettings
        if (settings?.appLockEnabled == true) {
            val now = System.currentTimeMillis()
            val timeInBackground = if (backgroundTimestamp > 0) now - backgroundTimestamp else Long.MAX_VALUE
            if (timeInBackground >= lockTimeoutMillis) {
                _isLocked.value = true
            }
        }
        backgroundTimestamp = 0L
    }

    /**
     * Configures WindowManager.LayoutParams.FLAG_SECURE on the given window.
     * When app lock is enabled, sets FLAG_SECURE to prevent screen capture
     * and hide content in the Android recent apps preview.
     */
    fun configureWindowSecurity(window: Window) {
        val enabled = currentSettings?.appLockEnabled ?: false
        if (enabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    /**
     * Updates window security on the active MainActivity if present.
     */
    fun updateWindowSecurity() {
        currentMainActivity?.get()?.let { activity ->
            configureWindowSecurity(activity.window)
        }
    }

    /**
     * Sets the lock timeout in seconds (e.g., 0 for immediate, 30, 60).
     */
    fun setLockTimeoutSeconds(seconds: Long) {
        lockTimeoutMillis = seconds.coerceAtLeast(0L) * 1000L
    }

    fun getLockTimeoutSeconds(): Long = lockTimeoutMillis / 1000L

    /**
     * Unlocks the app.
     */
    fun unlock() {
        _isLocked.value = false
        backgroundTimestamp = 0L
    }

    /**
     * Manually locks the app.
     */
    fun lock() {
        _isLocked.value = true
    }

    /**
     * Generates a random 16-byte salt.
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Hashes a PIN using SHA-256 with the specified salt.
     * Returns a string in the format "saltHex:hashHex".
     */
    fun hashPin(pin: String, salt: ByteArray = generateSalt()): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val hash = md.digest(pin.toByteArray(Charsets.UTF_8))
        val saltHex = salt.joinToString("") { "%02x".format(it) }
        val hashHex = hash.joinToString("") { "%02x".format(it) }
        return "$saltHex:$hashHex"
    }

    /**
     * Verifies the provided PIN against the stored hash in AppSettings.
     */
    fun verifyPin(pin: String): Boolean {
        val storedHashWithSalt = currentSettings?.pinHash ?: return false
        val parts = storedHashWithSalt.split(":")
        if (parts.size != 2) return false

        val saltHex = parts[0]
        val expectedHashHex = parts[1]

        val salt = try {
            saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (_: Exception) {
            return false
        }

        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val computedHash = md.digest(pin.toByteArray(Charsets.UTF_8))
        val computedHashHex = computedHash.joinToString("") { "%02x".format(it) }

        return MessageDigest.isEqual(
            computedHashHex.toByteArray(Charsets.UTF_8),
            expectedHashHex.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Verifies PIN and unlocks the app if valid.
     */
    fun verifyAndUnlock(pin: String): Boolean {
        return if (verifyPin(pin)) {
            unlock()
            true
        } else {
            false
        }
    }

    /**
     * Sets a new PIN in AppSettings and enables app locking.
     */
    suspend fun setPin(pin: String) {
        val hashed = hashPin(pin)
        val current = currentSettings ?: appSettingsRepository.getSettingsSync() ?: AppSettings()
        val updated = current.copy(
            pinHash = hashed,
            autoLockOnBackground = true,
            updatedAt = System.currentTimeMillis()
        )
        appSettingsRepository.insertOrUpdate(updated)
        currentSettings = updated
        updateWindowSecurity()
    }

    /**
     * Clears the configured PIN from AppSettings.
     */
    suspend fun clearPin() {
        val current = currentSettings ?: appSettingsRepository.getSettingsSync() ?: return
        val updated = current.copy(
            pinHash = null,
            updatedAt = System.currentTimeMillis()
        )
        appSettingsRepository.insertOrUpdate(updated)
        currentSettings = updated
        updateWindowSecurity()
    }

    /**
     * Checks if a PIN has been configured.
     */
    fun isPinConfigured(): Boolean {
        return !currentSettings?.pinHash.isNullOrBlank()
    }

    /**
     * Enables or disables app locking in AppSettings.
     */
    suspend fun setAppLockEnabled(enabled: Boolean) {
        val current = currentSettings ?: appSettingsRepository.getSettingsSync() ?: AppSettings()
        val updated = current.copy(
            autoLockOnBackground = enabled,
            updatedAt = System.currentTimeMillis()
        )
        appSettingsRepository.insertOrUpdate(updated)
        currentSettings = updated
        updateWindowSecurity()
    }

    /**
     * Enables or disables biometric authentication in AppSettings.
     */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        val current = currentSettings ?: appSettingsRepository.getSettingsSync() ?: AppSettings()
        val updated = current.copy(
            biometricEnabled = enabled,
            updatedAt = System.currentTimeMillis()
        )
        appSettingsRepository.insertOrUpdate(updated)
        currentSettings = updated
    }

    /**
     * Prompts for biometric authentication and unlocks the app on success.
     */
    fun authenticateWithBiometrics(
        activity: FragmentActivity,
        title: String = "Unlock Spendly",
        subtitle: String = "Confirm your identity to access Spendly",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        biometricAuthManager.showBiometricPrompt(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = {
                unlock()
                onSuccess()
            },
            onError = onError
        )
    }
}
