package com.notium.app.backup

import com.notium.app.data.BackupData
import com.notium.app.data.BackupNote
import com.notium.app.data.Folder
import com.notium.app.data.Tag
import com.notium.app.security.Crypto
import com.notium.app.security.toUtf8Bytes
import com.notium.app.security.wipe
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class BackupException(val kind: Kind, message: String) : Exception(message) {
    enum class Kind { NOT_A_BACKUP, WRONG_PASSWORD_OR_CORRUPT, INVALID_CONTENT, UNSUPPORTED_VERSION }
}

/**
 * Formato file .notium
 *
 *   magic   "NOTIUMBK"        8 byte
 *   version 1                 1 byte
 *   t, m    parametri Argon2id 4 + 4 byte
 *   salt                      16 byte
 *   iv                        12 byte
 *   dati    AES-256-GCM( gzip( JSON ) )   con tag di autenticazione a 128 bit
 *
 * L'intestazione e' usata come "dati associati" (AAD) di GCM: qualunque modifica al file,
 * all'intestazione o ai dati, rende fallita la verifica di integrita' e autenticita'.
 * La chiave deriva dalla password del backup (indipendente da quella dell'app) con Argon2id.
 */
object BackupManager {
    private val MAGIC = "NOTIUMBK".toByteArray(Charsets.US_ASCII)
    private const val VERSION = 1
    private const val HEADER_LEN = 8 + 1 + 4 + 4 + Crypto.SALT_LEN

    fun encrypt(data: BackupData, password: CharArray): ByteArray {
        val json = toJson(data).toString().toByteArray(Charsets.UTF_8)
        val zipped = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(json) } }.toByteArray()
        val salt = Crypto.randomBytes(Crypto.SALT_LEN)
        val header = ByteBuffer.allocate(HEADER_LEN)
            .put(MAGIC).put(VERSION.toByte())
            .putInt(Crypto.DEFAULT_T).putInt(Crypto.DEFAULT_M_KIB)
            .put(salt).array()
        val pw = password.toUtf8Bytes()
        val key = Crypto.deriveKey(pw, salt)
        pw.wipe()
        val sealed = Crypto.seal(key, zipped, header)
        key.wipe()
        return header + sealed
    }

    fun decrypt(file: ByteArray, password: CharArray): BackupData {
        if (file.size < HEADER_LEN + Crypto.IV_LEN + 16 || !file.copyOfRange(0, 8).contentEquals(MAGIC)) {
            throw BackupException(BackupException.Kind.NOT_A_BACKUP, "File non riconosciuto")
        }
        val buf = ByteBuffer.wrap(file)
        buf.position(8)
        val version = buf.get().toInt()
        if (version != VERSION) throw BackupException(BackupException.Kind.UNSUPPORTED_VERSION, "Versione non supportata")
        val t = buf.int
        val m = buf.int
        if (t !in 1..10 || m !in 8 * 1024..512 * 1024) {
            throw BackupException(BackupException.Kind.INVALID_CONTENT, "Parametri non validi")
        }
        val salt = ByteArray(Crypto.SALT_LEN).also { buf.get(it) }
        val header = file.copyOfRange(0, HEADER_LEN)
        val pw = password.toUtf8Bytes()
        val key = Crypto.deriveKey(pw, salt, t, m)
        pw.wipe()
        val zipped = try {
            Crypto.open(key, file.copyOfRange(HEADER_LEN, file.size), header)
        } catch (e: Exception) {
            throw BackupException(
                BackupException.Kind.WRONG_PASSWORD_OR_CORRUPT, "Password errata oppure file danneggiato"
            )
        } finally {
            key.wipe()
        }
        return try {
            val json = GZIPInputStream(ByteArrayInputStream(zipped)).use { it.readBytes() }
            fromJson(JSONObject(String(json, Charsets.UTF_8)))
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException(BackupException.Kind.INVALID_CONTENT, "Contenuto del backup non valido")
        }
    }

    private fun toJson(d: BackupData) = JSONObject().apply {
        put("format", VERSION)
        put("createdAt", d.createdAt)
        put("folders", JSONArray().apply {
            d.folders.forEach {
                put(JSONObject().put("id", it.id).put("name", it.name).put("parentId", it.parentId ?: JSONObject.NULL).put("color", it.color))
            }
        })
        put("tags", JSONArray().apply {
            d.tags.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("color", it.color)) }
        })
        put("notes", JSONArray().apply {
            d.notes.forEach { n ->
                put(
                    JSONObject().put("id", n.id).put("title", n.title).put("content", n.content)
                        .put("folderId", n.folderId ?: JSONObject.NULL)
                        .put("createdAt", n.createdAt).put("updatedAt", n.updatedAt)
                        .put("favorite", n.favorite).put("pinned", n.pinned).put("archived", n.archived)
                        .put("tags", JSONArray(n.tagIds))
                )
            }
        })
    }

    private fun fromJson(o: JSONObject): BackupData {
        fun bad(): Nothing = throw BackupException(BackupException.Kind.INVALID_CONTENT, "Contenuto del backup non valido")
        if (o.optInt("format") != VERSION) bad()
        val folders = o.getJSONArray("folders").let { a ->
            List(a.length()) { i ->
                val f = a.getJSONObject(i)
                Folder(f.getLong("id"), f.getString("name"), if (f.isNull("parentId")) null else f.getLong("parentId"), f.getInt("color"))
            }
        }
        val tags = o.getJSONArray("tags").let { a ->
            List(a.length()) { i -> a.getJSONObject(i).let { Tag(it.getLong("id"), it.getString("name"), it.getInt("color")) } }
        }
        val notes = o.getJSONArray("notes").let { a ->
            List(a.length()) { i ->
                val n = a.getJSONObject(i)
                val ts = n.getJSONArray("tags")
                BackupNote(
                    n.getLong("id"), n.getString("title"), n.getString("content"),
                    if (n.isNull("folderId")) null else n.getLong("folderId"),
                    n.getLong("createdAt"), n.getLong("updatedAt"),
                    n.getBoolean("favorite"), n.getBoolean("pinned"), n.getBoolean("archived"),
                    List(ts.length()) { ts.getLong(it) },
                )
            }
        }
        // Controlli di coerenza: riferimenti validi e nessun id duplicato.
        val folderIds = folders.map { it.id }.toSet()
        val tagIds = tags.map { it.id }.toSet()
        if (folderIds.size != folders.size || tagIds.size != tags.size || notes.map { it.id }.toSet().size != notes.size) bad()
        if (folders.any { it.parentId != null && it.parentId !in folderIds }) bad()
        if (notes.any { (it.folderId != null && it.folderId !in folderIds) || it.tagIds.any { t -> t !in tagIds } }) bad()
        return BackupData(o.getLong("createdAt"), folders, tags, notes)
    }
}
