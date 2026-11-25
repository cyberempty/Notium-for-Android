package com.notium.app.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Chiavi nell'Android Keystore: una "di dispositivo" (per il PIN) e una legata alla biometria. */
object KeystoreKeys {
    private const val PROVIDER = "AndroidKeyStore"
    private const val DEVICE_ALIAS = "notium_device_key"
    private const val BIO_ALIAS = "notium_bio_key"
    private const val TRANSFORM = "AES/GCM/NoPadding"

    private fun store(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    private fun generate(alias: String, requireAuth: Boolean): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .apply {
                if (requireAuth) {
                    setUserAuthenticationRequired(true)
                    setInvalidatedByBiometricEnrollment(true)
                }
            }
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
            .apply { init(spec) }.generateKey()
    }

    private fun existing(alias: String): SecretKey? =
        (store().getKey(alias, null) as? SecretKey)

    // ---- chiave di dispositivo (nessuna autenticazione): lega il PIN al telefono ----

    fun sealWithDevice(plain: ByteArray): ByteArray {
        val key = existing(DEVICE_ALIAS) ?: generate(DEVICE_ALIAS, false)
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.ENCRYPT_MODE, key)
        return c.iv + c.doFinal(plain)
    }

    fun openWithDevice(blob: ByteArray): ByteArray {
        val key = existing(DEVICE_ALIAS) ?: throw IllegalStateException("chiave di dispositivo assente")
        val iv = blob.copyOfRange(0, Crypto.IV_LEN)
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(Crypto.TAG_BITS, iv))
        return c.doFinal(blob, Crypto.IV_LEN, blob.size - Crypto.IV_LEN)
    }

    // ---- chiave biometrica (richiede autenticazione a ogni uso) ----

    /** Cipher di cifratura da passare al BiometricPrompt per attivare la biometria. */
    fun bioEncryptCipher(): Cipher {
        deleteBioKey()
        val key = generate(BIO_ALIAS, true)
        return Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key) }
    }

    /** Cipher di decifratura. Lancia KeyPermanentlyInvalidatedException se la biometria e' cambiata. */
    @Throws(KeyPermanentlyInvalidatedException::class)
    fun bioDecryptCipher(iv: ByteArray): Cipher {
        val key = existing(BIO_ALIAS) ?: throw KeyPermanentlyInvalidatedException()
        return Cipher.getInstance(TRANSFORM)
            .apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(Crypto.TAG_BITS, iv)) }
    }

    fun deleteBioKey() {
        runCatching { store().deleteEntry(BIO_ALIAS) }
    }

    fun deleteAll() {
        runCatching { store().deleteEntry(BIO_ALIAS) }
        runCatching { store().deleteEntry(DEVICE_ALIAS) }
    }
}
