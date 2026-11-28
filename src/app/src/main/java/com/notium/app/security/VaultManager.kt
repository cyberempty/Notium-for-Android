package com.notium.app.security

import android.content.Context
import android.security.keystore.KeyPermanentlyInvalidatedException
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.crypto.Cipher

sealed interface PinResult {
    data class Success(val masterKey: ByteArray) : PinResult
    data class Wrong(val attemptsBeforeLock: Int) : PinResult
    data class Locked(val untilMillis: Long) : PinResult
    data object Disabled : PinResult
}

/**
 * Gestisce la chiave principale del database.
 *
 * - La chiave principale (32 byte) e' generata casualmente e NON deriva dalla password.
 * - Viene conservata solo "avvolta" (AES-256-GCM) da tre chiavi indipendenti:
 *     1. password  -> Argon2id(password, salt)
 *     2. PIN       -> Argon2id(PIN, salt) + ulteriore cifratura con chiave dell'Android Keystore
 *     3. biometria -> chiave Keystore che richiede BiometricPrompt a ogni uso
 * - Cambiare password significa ri-avvolgere la chiave: il database non va ricifrato.
 */
class VaultManager(context: Context) {
    private val dir: File = context.noBackupFilesDir
    private val file = File(dir, "vault.json")

    fun exists(): Boolean = file.exists()

    private fun read(): JSONObject = JSONObject(file.readText())

    private fun write(o: JSONObject) {
        val tmp = File(dir, "vault.json.tmp")
        tmp.writeText(o.toString())
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    // ---------------------------------------------------------------- password

    /** Primo avvio: genera la chiave principale e la protegge con la password. */
    fun create(password: CharArray): ByteArray {
        val master = Crypto.randomBytes(Crypto.KEY_LEN)
        val o = JSONObject().put("v", 1)
        putPassword(o, master, password)
        o.put("pinEnabled", false).put("pinFails", 0).put("pinLockUntil", 0L).put("bioEnabled", false)
        write(o)
        return master
    }

    private fun putPassword(o: JSONObject, master: ByteArray, password: CharArray) {
        val salt = Crypto.randomBytes(Crypto.SALT_LEN)
        val pwBytes = password.toUtf8Bytes()
        val kek = Crypto.deriveKey(pwBytes, salt)
        pwBytes.wipe()
        o.put("pwLen", password.size)
        o.put("pwSalt", Crypto.b64(salt))
        o.put("pwWrapped", Crypto.b64(Crypto.seal(kek, master, "pw".toByteArray())))
        kek.wipe()
    }

    /** Restituisce la chiave principale oppure null se la password e' errata. */
    fun unlockWithPassword(password: CharArray): ByteArray? {
        val o = read()
        val salt = Crypto.unb64(o.getString("pwSalt"))
        val pwBytes = password.toUtf8Bytes()
        val kek = Crypto.deriveKey(pwBytes, salt)
        pwBytes.wipe()
        return try {
            Crypto.open(kek, Crypto.unb64(o.getString("pwWrapped")), "pw".toByteArray())
        } catch (e: Exception) {
            null
        } finally {
            kek.wipe()
        }
    }

    /** Cambio password sicuro: verifica la vecchia, ri-avvolge la chiave principale con la nuova. */
    fun changePassword(old: CharArray, new: CharArray): Boolean {
        val master = unlockWithPassword(old) ?: return false
        val o = read()
        putPassword(o, master, new)
        write(o)
        master.wipe()
        return true
    }

    // --------------------------------------------------------------------- PIN

    fun passwordLength(): Int = if (exists()) read().optInt("pwLen", 0) else 0
    fun pinLength(): Int = if (exists()) read().optInt("pinLen", 0) else 0
    fun rememberPasswordLength(n: Int) { if (exists() && passwordLength() != n) write(read().put("pwLen", n)) }
    fun rememberPinLength(n: Int) { if (exists() && pinLength() != n) write(read().put("pinLen", n)) }

    // ------------------------------------------- apertura diretta (nessun PIN ne' impronta)

    fun hasAutoUnlock(): Boolean = exists() && read().has("autoWrapped")

    /** Senza PIN ne' impronta l'app si apre da sola: la chiave resta protetta dal Keystore del dispositivo. */
    fun enableAutoUnlock(master: ByteArray) {
        val o = read()
        o.put("autoWrapped", Crypto.b64(KeystoreKeys.sealWithDevice(master)))
        write(o)
    }

    fun disableAutoUnlock() {
        if (!exists()) return
        val o = read()
        if (o.has("autoWrapped")) { o.remove("autoWrapped"); write(o) }
    }

    fun unlockAuto(): ByteArray? = try {
        KeystoreKeys.openWithDevice(Crypto.unb64(read().getString("autoWrapped")))
    } catch (e: Exception) { null }

    /** Se attivo, l'app chiede sempre la password principale anche senza PIN ne' impronta. */
    fun isPasswordLockEnabled(): Boolean = exists() && read().optBoolean("pwLock", false)
    fun setPasswordLock(on: Boolean) { if (exists()) write(read().put("pwLock", on).also { if (on) it.remove("autoWrapped") }) }

    fun isPinEnabled(): Boolean = exists() && read().optBoolean("pinEnabled", false)

    fun enablePin(master: ByteArray, pin: CharArray) {
        val o = read()
        val salt = Crypto.randomBytes(Crypto.SALT_LEN)
        val pinBytes = pin.toUtf8Bytes()
        val kek = Crypto.deriveKey(pinBytes, salt)
        pinBytes.wipe()
        val inner = Crypto.seal(kek, master, "pin".toByteArray())
        kek.wipe()
        o.put("pinSalt", Crypto.b64(salt))
        o.put("pinWrapped", Crypto.b64(KeystoreKeys.sealWithDevice(inner)))
        o.put("pinEnabled", true).put("pinFails", 0).put("pinLockUntil", 0L).put("pinLen", pin.size)
        o.remove("autoWrapped")
        write(o)
    }

    fun disablePin() {
        val o = read()
        o.remove("pinSalt"); o.remove("pinWrapped")
        o.put("pinEnabled", false).put("pinFails", 0).put("pinLockUntil", 0L)
        write(o)
    }

    /** Millisecondi di blocco residuo (0 se nessun blocco). */
    fun pinLockRemaining(now: Long = System.currentTimeMillis()): Long =
        if (!exists()) 0 else (read().optLong("pinLockUntil", 0L) - now).coerceAtLeast(0)

    fun unlockWithPin(pin: CharArray, now: Long = System.currentTimeMillis()): PinResult {
        val o = read()
        if (!o.optBoolean("pinEnabled", false)) return PinResult.Disabled
        val until = o.optLong("pinLockUntil", 0L)
        if (until > now) return PinResult.Locked(until)

        val salt = Crypto.unb64(o.getString("pinSalt"))
        val pinBytes = pin.toUtf8Bytes()
        val kek = Crypto.deriveKey(pinBytes, salt)
        pinBytes.wipe()
        try {
            val inner = KeystoreKeys.openWithDevice(Crypto.unb64(o.getString("pinWrapped")))
            val master = Crypto.open(kek, inner, "pin".toByteArray())
            o.put("pinFails", 0).put("pinLockUntil", 0L)
            write(o)
            return PinResult.Success(master)
        } catch (e: Exception) {
            val fails = o.optInt("pinFails", 0) + 1
            o.put("pinFails", fails)
            if (fails >= MAX_PIN_FAILS) {
                // Troppi tentativi: il PIN viene disattivato, resta solo la password principale.
                o.put("pinEnabled", false).remove("pinSalt")
                o.remove("pinWrapped")
                write(o)
                return PinResult.Disabled
            }
            val lockMs = lockDurationMs(fails)
            if (lockMs > 0) o.put("pinLockUntil", now + lockMs)
            write(o)
            return if (lockMs > 0) PinResult.Locked(now + lockMs)
            else PinResult.Wrong(FREE_ATTEMPTS - fails)
        } finally {
            kek.wipe()
        }
    }

    private fun lockDurationMs(fails: Int): Long = when (fails) {
        in 0 until FREE_ATTEMPTS -> 0
        5 -> 30_000
        6 -> 60_000
        7 -> 5 * 60_000
        8 -> 15 * 60_000
        else -> 60 * 60_000
    }

    // --------------------------------------------------------------- biometria

    fun isBiometricEnabled(): Boolean = exists() && read().optBoolean("bioEnabled", false)

    /** Cipher da autenticare con BiometricPrompt per attivare la biometria. */
    fun prepareBiometricEnroll(): Cipher = KeystoreKeys.bioEncryptCipher()

    fun finishBiometricEnroll(authenticated: Cipher, master: ByteArray) {
        val ct = authenticated.doFinal(master)
        val o = read()
        o.put("bioIv", Crypto.b64(authenticated.iv))
        o.put("bioWrapped", Crypto.b64(ct))
        o.put("bioEnabled", true)
        o.remove("autoWrapped")
        write(o)
    }

    /** Cipher per lo sblocco; null se la biometria e' stata invalidata (e viene disattivata). */
    fun prepareBiometricUnlock(): Cipher? {
        val o = read()
        if (!o.optBoolean("bioEnabled", false)) return null
        return try {
            KeystoreKeys.bioDecryptCipher(Crypto.unb64(o.getString("bioIv")))
        } catch (e: KeyPermanentlyInvalidatedException) {
            disableBiometric(); null
        } catch (e: Exception) {
            disableBiometric(); null
        }
    }

    fun finishBiometricUnlock(authenticated: Cipher): ByteArray? = try {
        authenticated.doFinal(Crypto.unb64(read().getString("bioWrapped")))
    } catch (e: Exception) {
        null
    }

    fun disableBiometric() {
        KeystoreKeys.deleteBioKey()
        if (!exists()) return
        val o = read()
        o.remove("bioIv"); o.remove("bioWrapped")
        o.put("bioEnabled", false)
        write(o)
    }

    // ------------------------------------------------------------------- reset

    /** Elimina il file delle chiavi e le chiavi nel Keystore. */
    fun destroy() {
        KeystoreKeys.deleteAll()
        runCatching {
            if (file.exists()) {
                file.writeBytes(Crypto.randomBytes(file.length().toInt().coerceAtLeast(64)))
            }
        }
        file.delete()
        File(dir, "vault.json.tmp").delete()
    }

    private companion object {
        const val FREE_ATTEMPTS = 5
        const val MAX_PIN_FAILS = 10
    }
}
