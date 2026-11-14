package com.spendly.app.features.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.core.privacy.PrivacyManager
import com.spendly.app.core.security.AppLockManager
import com.spendly.app.core.security.BiometricAuthManager
import com.spendly.app.core.security.LocalBackupManager
import com.spendly.app.domain.model.AppSettings
import com.spendly.app.domain.model.ReminderPreference
import com.spendly.app.domain.repository.AppSettingsRepository
import com.spendly.app.domain.repository.ReminderPreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val reminderPreferences: List<ReminderPreference> = emptyList(),
    val isUsageAccessGranted: Boolean = false,
    val isNotificationAccessGranted: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val backupSuccessMessage: String? = null,
    val backupErrorMessage: String? = null,
    val isProcessingBackup: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettingsRepository: AppSettingsRepository,
    private val reminderPreferenceRepository: ReminderPreferenceRepository,
    private val privacyManager: PrivacyManager,
    private val appLockManager: AppLockManager,
    private val biometricAuthManager: BiometricAuthManager,
    private val localBackupManager: LocalBackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadData()
        checkPermissions()
    }

    fun checkPermissions() {
        val usage = privacyManager.isUsageAccessGranted()
        val notification = privacyManager.isNotificationAccessGranted()
        val biometric = biometricAuthManager.canAuthenticate()
        _uiState.update {
            it.copy(
                isUsageAccessGranted = usage,
                isNotificationAccessGranted = notification,
                isBiometricAvailable = biometric
            )
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            appSettingsRepository.getSettingsFlow().collect { settings ->
                if (settings != null) {
                    _uiState.update { it.copy(settings = settings) }
                }
            }
        }

        viewModelScope.launch {
            reminderPreferenceRepository.getAll().collect { prefs ->
                _uiState.update { it.copy(reminderPreferences = prefs) }
            }
        }
    }

    fun onCurrencyChanged(currency: String, symbol: String) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(
                currency = currency,
                currencySymbol = symbol,
                updatedAt = System.currentTimeMillis()
            )
            appSettingsRepository.saveSettings(updated)
        }
    }

    fun onThemeChanged(theme: String) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(
                theme = theme,
                updatedAt = System.currentTimeMillis()
            )
            appSettingsRepository.saveSettings(updated)
        }
    }

    fun onBiometricToggled(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(
                biometricEnabled = enabled,
                updatedAt = System.currentTimeMillis()
            )
            appSettingsRepository.saveSettings(updated)
        }
    }

    fun onAutoLockToggled(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(
                autoLockOnBackground = enabled,
                updatedAt = System.currentTimeMillis()
            )
            appSettingsRepository.saveSettings(updated)
        }
    }

    fun onSetPin(pin: String) {
        viewModelScope.launch {
            appLockManager.setPin(pin)
        }
    }

    fun onRemovePin() {
        viewModelScope.launch {
            appLockManager.clearPin()
        }
    }

    fun onUsageMonitoringToggled(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(
                usageMonitoringEnabled = enabled,
                updatedAt = System.currentTimeMillis()
            )
            appSettingsRepository.saveSettings(updated)
        }
    }

    fun onNotificationMonitoringToggled(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(
                notificationMonitoringEnabled = enabled,
                updatedAt = System.currentTimeMillis()
            )
            appSettingsRepository.saveSettings(updated)
        }
    }

    fun onReminderAppToggled(preference: ReminderPreference, enabled: Boolean) {
        viewModelScope.launch {
            reminderPreferenceRepository.update(
                preference.copy(
                    enabled = enabled,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun onCooldownChanged(preference: ReminderPreference, cooldownMinutes: Int) {
        viewModelScope.launch {
            reminderPreferenceRepository.update(
                preference.copy(
                    cooldownMinutes = cooldownMinutes,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun onQuietHoursChanged(preference: ReminderPreference, startMinutes: Int?, endMinutes: Int?) {
        viewModelScope.launch {
            reminderPreferenceRepository.update(
                preference.copy(
                    quietHoursStart = startMinutes,
                    quietHoursEnd = endMinutes,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun exportBackup(uri: Uri, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingBackup = true, backupSuccessMessage = null, backupErrorMessage = null) }
            val result = localBackupManager.exportBackup(uri, password.toCharArray())
            _uiState.update {
                it.copy(
                    isProcessingBackup = false,
                    backupSuccessMessage = if (result.isSuccess) "Encrypted backup exported successfully!" else null,
                    backupErrorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun restoreBackup(uri: Uri, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingBackup = true, backupSuccessMessage = null, backupErrorMessage = null) }
            val result = localBackupManager.restoreBackup(uri, password.toCharArray())
            _uiState.update {
                it.copy(
                    isProcessingBackup = false,
                    backupSuccessMessage = if (result.isSuccess) {
                        val summary = result.getOrNull()
                        "Restored ${summary?.transactionCount ?: 0} transactions successfully!"
                    } else null,
                    backupErrorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun clearAllFinancialData(onComplete: () -> Unit) {
        viewModelScope.launch {
            privacyManager.clearAllFinancialData()
            onComplete()
        }
    }

    fun dismissBackupMessages() {
        _uiState.update { it.copy(backupSuccessMessage = null, backupErrorMessage = null) }
    }
}
