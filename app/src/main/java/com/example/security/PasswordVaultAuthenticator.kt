package com.example.security

import android.app.Activity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

/**
 * Device-authentication gate for revealing saved passwords.
 *
 * The vault never receives the biometric result itself; the successful prompt
 * only unlocks the short-lived in-memory PasswordVaultSession.
 */class PasswordVaultAuthenticator(
    private val activity: Activity,
    private val session: PasswordVaultSession
) {
    fun canAuthenticate(): Boolean {
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        title: String = "Unlock password vault",
        subtitle: String = "Authenticate to reveal saved passwords",
        onResult: (Boolean) -> Unit
    ) {
        if (!canAuthenticate()) {
            onResult(false)
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    // WHY: Keep the vault unlocked only in memory; no biometric
                    // credential or token is stored by DixBrowser.
                    session.unlock()
                    onResult(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onResult(false)
                }

                override fun onAuthenticationFailed() {
                    // Keep the prompt active for another attempt.
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(info)
    }
}
