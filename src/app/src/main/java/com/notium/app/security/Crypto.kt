package com.notium.app.security

import android.util.Base64
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Primitive crittografiche: Argon2id per la derivazione, AES-256-GCM per la cifratura. */
object Crypto {
    const val KEY_LEN = 32
    const val IV_LEN = 12
    const val SALT_LEN = 16
    const val TAG_BITS = 128

    const val DEFAULT_T = 3
    const val DEFAULT_M_KIB = 32 * 1024

    private val random = SecureRandom()
    private val argon by lazy { Argon2Kt() }

    fun randomBytes(n: Int): ByteArray = ByteArray(n).also { random.nextBytes(it) }

    /** Argon2id: deriva una chiave a 256 bit da una password/PIN e da un salt casuale. */
    fun deriveKey(
        secret: ByteArray,
        salt: ByteArray,
        t: Int = DEFAULT_T,
        mKib: Int = DEFAULT_M_KIB,
    ): ByteArray =
        argon.hash(Argon2Mode.ARGON2_ID, secret, salt, t, mKib, 1, KEY_LEN).rawHashAsByteArray()

    /** Cifra con AES-256-GCM. Risultato: IV (12 byte) + testo cifrato + tag. */
    fun seal(key: ByteArray, plain: ByteArray, aad: ByteArray? = null): ByteArray {
        val iv = randomBytes(IV_LEN)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        if (aad != null) cipher.updateAAD(aad)
        return iv + cipher.doFinal(plain)
    }

    /** Decifra e verifica autenticita'/integrita'. Lancia GeneralSecurityException se qualcosa non torna. */
    fun open(key: ByteArray, blob: ByteArray, aad: ByteArray? = null): ByteArray {
        require(blob.size > IV_LEN) { "blob troppo corto" }
        val iv = blob.copyOfRange(0, IV_LEN)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        if (aad != null) cipher.updateAAD(aad)
        return cipher.doFinal(blob, IV_LEN, blob.size - IV_LEN)
    }

    fun toHex(b: ByteArray): String = buildString(b.size * 2) { b.forEach { append("%02x".format(it)) } }
    fun b64(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP)
    fun unb64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)
}

fun CharArray.toUtf8Bytes(): ByteArray {
    val bb = Charsets.UTF_8.encode(java.nio.CharBuffer.wrap(this))
    return ByteArray(bb.remaining()).also { bb.get(it) }
}

fun ByteArray.wipe() = fill(0)
