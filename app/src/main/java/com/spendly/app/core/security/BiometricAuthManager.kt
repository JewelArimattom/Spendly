package com.spendly.app.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val AUTHENTICATORS = Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL
    }

    private val biometricManager = BiometricManager.from(context)

    /**
     * Checks if biometric or device credential authentication is available on the device.
     * Checks BIOMETRIC_STRONG or DEVICE_CREDENTIAL.
     */
    fun canAuthenticate(): Boolean {
        return biometricManager.canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Returns the raw BiometricManager status code.
     */
    fun getAuthenticationStatus(): Int {
        return biometricManager.canAuthenticate(AUTHENTICATORS)
    }

    /**
     * Displays the system biometric prompt for authentication.
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(AUTHENTICATORS)

        if (subtitle.isNotBlank()) {
            promptInfoBuilder.setSubtitle(subtitle)
        }

        val promptInfo = promptInfoBuilder.build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Authentication failed (e.g. unrecognized fingerprint), prompt remains visible.
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }
}
