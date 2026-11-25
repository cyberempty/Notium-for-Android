package com.notium.app.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.notium.app.AppViewModel
import com.notium.app.data.AutoTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant

enum class ExportFormat(val ext: String, val mime: String) {
    PDF("pdf", "application/pdf"), TXT("txt", "text/plain"), JSON("json", "application/json")
}

/** Tutto cio' che serve per esportare una nota. `content` e' in Markdown. */
data class ExportNote(
    val title: String, val content: String, val folder: String?, val tags: List<String>,
    val favorite: Boolean, val pinned: Boolean, val archived: Boolean,
    val createdAt: Long?, val updatedAt: Long?,
)

object NoteExporter {
    private val INLINE = listOf(Regex("\\*\\*(.+?)\\*\\*"), Regex("~~(.+?)~~"), Regex("==(.+?)=="), Regex("`([^`]+)`"))
    private val LINK = Regex("\\[([^\\]]*)\\]\\([^)]*\\)")
    private val HEADING = Regex("^\\s{0,3}#{1,6}\\s+")
    private val DONE = Regex("^(\\s*)[-*+] \\[[xX]\\] ")
    private val TODO = Regex("^(\\s*)[-*+] \\[ \\] ")
    private val ESCAPED = Regex("\\\\([*_`#>~=\\-.!\\[\\]])")

    fun titleOf(n: ExportNote, untitled: String): String = n.title.trim().ifBlank { AutoTitle.from(n.content) }.ifBlank { untitled }

    private fun plain(md: String): String {
        var inCode = false
        return md.split("\n").joinToString("\n") { raw ->
            if (raw.trimStart().startsWith("```")) { inCode = !inCode; return@joinToString "" }
            if (inCode) return@joinToString raw
            var l = HEADING.replace(raw, "")
            l = DONE.replace(l, "$1[x] ")
            l = TODO.replace(l, "$1[ ] ")
            l = LINK.replace(l, "$1")
            for (r in INLINE) l = r.replace(l, "$1")
            ESCAPED.replace(l, "$1")
        }.replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun iso(ms: Long?): String? = ms?.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).toString() }

    fun bytes(n: ExportNote, f: ExportFormat, untitled: String): ByteArray {
        val title = titleOf(n, untitled)
        return when (f) {
            ExportFormat.PDF -> ByteArrayOutputStream().also { PdfExport.write(it, title, n.content) }.toByteArray()
            ExportFormat.TXT -> (title + "\n\n" + plain(n.content) + "\n").toByteArray(Charsets.UTF_8)
            ExportFormat.JSON -> JSONObject().apply {
                put("app", "Notium"); put("formatVersion", 1)
                put("title", title); put("content", n.content)
                put("folder", n.folder ?: JSONObject.NULL)
                put("tags", JSONArray(n.tags))
                put("favorite", n.favorite); put("pinned", n.pinned); put("archived", n.archived)
                put("createdAt", iso(n.createdAt) ?: JSONObject.NULL)
                put("updatedAt", iso(n.updatedAt) ?: JSONObject.NULL)
            }.toString(2).plus("\n").toByteArray(Charsets.UTF_8)
        }
    }

    private fun safeName(title: String, untitled: String): String =
        title.replace(Regex("[\\\\/:*?\"<>|\\n\\r\\t]"), "_").trim().trim('.').take(60).ifBlank { untitled }

    /** Salva il file nella cartella Download pubblica del telefono. Restituisce il nome finale, o null se fallisce. */
    fun saveToDownloads(ctx: Context, n: ExportNote, f: ExportFormat, untitled: String): String? = runCatching {
        val base = safeName(titleOf(n, untitled), untitled)
        val data = bytes(n, f, untitled)
        val name = "$base.${f.ext}"
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = ctx.contentResolver
            val cv = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, f.mime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv) ?: error("insert")
            try {
                resolver.openOutputStream(uri)?.use { it.write(data) } ?: error("stream")
                val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
            } catch (t: Throwable) {
                runCatching { resolver.delete(uri, null, null) }
                throw t
            }
            // Android puo' aggiungere un numero se esiste gia' un file con lo stesso nome.
            resolver.query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: name
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            var file = File(dir, name)
            var i = 1
            while (file.exists()) { file = File(dir, "$base ($i).${f.ext}"); i++ }
            file.writeBytes(data)
            MediaScannerConnection.scanFile(ctx, arrayOf(file.absolutePath), arrayOf(f.mime), null)
            file.name
        }
    }.getOrNull()
}

/**
 * Restituisce la funzione "esporta nota": salva nella cartella Download e avvisa con la notifica in basso.
 * Su Android 8/9 chiede prima il permesso di scrittura (da Android 10 non serve).
 */
@Composable
fun rememberExporter(vm: AppViewModel): (List<ExportNote>, ExportFormat) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val untitled = I18n.tr("Nota", LocalLang.current)
    var pending by remember { mutableStateOf<Pair<List<ExportNote>, ExportFormat>?>(null) }
    // Un file separato per ogni nota: con piu' note si creano tanti file quante sono le note.
    fun run(list: List<ExportNote>, f: ExportFormat) {
        scope.launch {
            val names = withContext(Dispatchers.IO) { list.mapNotNull { NoteExporter.saveToDownloads(context, it, f, untitled) } }
            vm.notify(
                when {
                    names.isEmpty() -> "Esportazione non riuscita."
                    names.size == 1 -> "Salvato in Download: ${names[0]}"
                    else -> "Salvati ${names.size} file in Download."
                }
            )
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.pickerActive = false
        val p = pending; pending = null
        if (p != null) {
            if (granted) run(p.first, p.second) else vm.notify("Permesso negato: impossibile salvare in Download.")
        }
    }
    return { list, f ->
        val needs = Build.VERSION.SDK_INT < 29 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (needs) { pending = list to f; vm.pickerActive = true; permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) }
        else run(list, f)
    }
}

/** Voce "Esporta" da mettere in fondo al menu dei tre puntini: apre il sottomenu. */
@Composable
fun ColumnScope.ExportEntry(onOpen: () -> Unit) {
    HorizontalDivider()
    DropdownMenuItem(text = { Text("Esporta") }, leadingIcon = { Icon(Icons.Outlined.FileDownload, null) }, onClick = onOpen)
}

/** Sottomenu: sostituisce il contenuto del menu con le tre scelte di formato. */
@Composable
fun ColumnScope.ExportSubMenu(onBack: () -> Unit, onPick: (ExportFormat) -> Unit) {
    DropdownMenuItem(text = { Text("Indietro") }, leadingIcon = { Icon(Icons.Outlined.ChevronLeft, null) }, onClick = onBack)
    HorizontalDivider()
    DropdownMenuItem(text = { Text("Esporta in PDF") }, leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) }, onClick = { onPick(ExportFormat.PDF) })
    DropdownMenuItem(text = { Text("Esporta in TXT") }, leadingIcon = { Icon(Icons.Outlined.Description, null) }, onClick = { onPick(ExportFormat.TXT) })
    DropdownMenuItem(text = { Text("Esporta in JSON") }, leadingIcon = { Icon(Icons.Outlined.Code, null) }, onClick = { onPick(ExportFormat.JSON) })
}
