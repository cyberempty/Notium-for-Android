package com.notium.app.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

object BiometricHelper {
    private const val AUTH = BiometricManager.Authenticators.BIOMETRIC_STRONG

    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Mostra il BiometricPrompt legato a un Cipher del Keystore: la chiave si sblocca solo
     * dopo un'autenticazione biometrica riuscita.
     */
    fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        cipher: Cipher,
        negativeText: String,
        onSuccess: (Cipher) -> Unit,
        onFailure: (message: String, userCanceled: Boolean) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val c = result.cryptoObject?.cipher
                if (c != null) onSuccess(c) else onFailure("Autenticazione non valida.", false)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val canceled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                onFailure(errString.toString(), canceled)
            }
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeText)
            .setAllowedAuthenticators(AUTH)
            .setConfirmationRequired(false)
            .build()
        BiometricPrompt(activity, executor, callback)
            .authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    /** Autenticazione biometrica semplice (senza chiave): usata per il blocco grafico delle singole note. */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        negativeText: String,
        onSuccess: () -> Unit,
        onFallback: () -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFallback()
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText(negativeText)
            .setAllowedAuthenticators(AUTH)
            .setConfirmationRequired(false)
            .build()
        BiometricPrompt(activity, executor, callback).authenticate(info)
    }
}
