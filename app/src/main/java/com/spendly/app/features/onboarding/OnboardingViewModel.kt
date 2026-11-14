package com.spendly.app.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.core.privacy.PrivacyManager
import com.spendly.app.domain.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 0,
    val isUsageAccessGranted: Boolean = false,
    val isNotificationAccessGranted: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val privacyManager: PrivacyManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        val usage = privacyManager.isUsageAccessGranted()
        val notification = privacyManager.isNotificationAccessGranted()
        _uiState.update {
            it.copy(
                isUsageAccessGranted = usage,
                isNotificationAccessGranted = notification
            )
        }
    }

    fun nextStep() {
        _uiState.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(2)) }
    }

    fun prevStep() {
        _uiState.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }
    }

    fun completeOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            val settings = appSettingsRepository.getSettingsSync() ?: com.spendly.app.domain.model.AppSettings()
            appSettingsRepository.saveSettings(
                settings.copy(
                    onboardingCompleted = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
            onFinished()
        }
    }
}
