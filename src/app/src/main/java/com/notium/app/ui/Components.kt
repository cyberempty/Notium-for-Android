package com.notium.app.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.notium.app.data.Folder
import com.notium.app.data.NotiumColors
import com.notium.app.security.Strength

/** Logo Notium: foglio con lucchetto, disegnato in codice (nessuna immagine esterna). */
@Composable
fun NotiumLogo(size: Dp = 72.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.width
        drawRoundRect(Color(0xFF1A1A1A), size = Size(s, s), cornerRadius = CornerRadius(s * 0.24f))
        drawRoundRect(Color(0xFF3A3A3A), size = Size(s, s), cornerRadius = CornerRadius(s * 0.24f), style = Stroke(width = s * 0.02f))
        val sheet = Path().apply {
            moveTo(s * 0.30f, s * 0.22f); lineTo(s * 0.60f, s * 0.22f); lineTo(s * 0.74f, s * 0.36f)
            lineTo(s * 0.74f, s * 0.76f); lineTo(s * 0.30f, s * 0.76f); close()
        }
        drawPath(sheet, Color.White)
        drawPath(Path().apply {
            moveTo(s * 0.60f, s * 0.22f); lineTo(s * 0.74f, s * 0.36f); lineTo(s * 0.60f, s * 0.36f); close()
        }, Color(0xFFBDBDBD))
        drawRoundRect(Color(0xFF1A1A1A), Offset(s * 0.36f, s * 0.42f), Size(s * 0.22f, s * 0.035f), CornerRadius(4f))
        drawRoundRect(Color(0xFF1A1A1A), Offset(s * 0.36f, s * 0.50f), Size(s * 0.17f, s * 0.035f), CornerRadius(4f))
        val g = Color(0xFF8E8E8E)
        drawRoundRect(g, Offset(s * 0.52f, s * 0.60f), Size(s * 0.24f, s * 0.18f), CornerRadius(s * 0.03f))
        drawArc(
            g, 180f, 180f, false, Offset(s * 0.56f, s * 0.50f), Size(s * 0.16f, s * 0.20f),
            style = Stroke(width = s * 0.03f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun ColorDot(color: Int, size: Dp = 14.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(Color(color)))
}

@Composable
fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    // Le due file stanno in una Column: senza contenitore verticale si sovrapponevano nei dialoghi.
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NotiumColors.take(5).forEach { c -> ColorChoice(c, c == selected, onSelect) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NotiumColors.drop(5).forEach { c -> ColorChoice(c, c == selected, onSelect) }
        }
    }
}

@Composable
private fun ColorChoice(c: Int, selected: Boolean, onSelect: (Int) -> Unit) {
    Box(
        Modifier.size(34.dp).clip(CircleShape).background(Color(c)).clickable { onSelect(c) },
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(Icons.Outlined.Check, null, tint = Color.Black, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun PasswordField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    isError: Boolean = false,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, isError = isError,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction, autoCorrectEnabled = false),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone?.invoke() }, onGo = { onDone?.invoke() }),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Mostra/nascondi")
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun StrengthBar(strength: Strength) {
    if (strength == Strength.EMPTY) return
    val color = when (strength) {
        Strength.WEAK -> MaterialTheme.colorScheme.error
        Strength.FAIR -> Color(0xFFBDBDBD)
        else -> MaterialTheme.colorScheme.secondary
    }
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        LinearProgressIndicator(
            progress = { strength.fraction }, color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
        )
        Text("Robustezza: ${strength.label}", style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun InputDialog(
    title: String, label: String, initial: String = "", confirm: String = "Salva",
    extra: (@Composable () -> Unit)? = null,
    onDismiss: () -> Unit, onConfirm: (String) -> Unit,
) {
    var v by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(v, { v = it }, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (extra != null) Box(Modifier.padding(top = 16.dp)) { extra() }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(v) }, enabled = v.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
fun ConfirmDialog(
    title: String, message: String, confirm: String, danger: Boolean = false,
    onDismiss: () -> Unit, onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirm, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

/** Cartelle in ordine gerarchico, con il livello di profondita' per l'indentazione. */
fun flattenFolders(all: List<Folder>): List<Pair<Folder, Int>> {
    val byParent = all.groupBy { it.parentId }
    val ids = all.map { it.id }.toSet()
    val out = ArrayList<Pair<Folder, Int>>()
    fun walk(parent: Long?, depth: Int) {
        byParent[parent].orEmpty().sortedBy { it.name.lowercase() }.forEach { f ->
            out.add(f to depth); walk(f.id, depth + 1)
        }
    }
    walk(null, 0)
    // Eventuali cartelle con padre mancante vengono mostrate in radice.
    all.filter { it.parentId != null && it.parentId !in ids }.forEach { out.add(it to 0) }
    return out
}

@Composable
fun FolderPickerDialog(
    title: String, folders: List<Folder>, forbidden: Set<Long> = emptySet(),
    rootLabel: String = "Cartella principale", onCreate: ((String) -> Unit)? = null,
    onDismiss: () -> Unit, onPick: (Long?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (onCreate != null) {
                    val creating = remember { mutableStateOf(false) }
                    val newName = remember { mutableStateOf("") }
                    if (!creating.value) {
                        Row(
                            Modifier.fillMaxWidth().clickable { creating.value = true }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Outlined.CreateNewFolder, null)
                            Text("Nuova cartella…", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        OutlinedTextField(
                            newName.value, { newName.value = it }, label = { Text("Nome della nuova cartella") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            trailingIcon = { TextButton(onClick = { onCreate(newName.value) }, enabled = newName.value.isNotBlank()) { Text("Crea") } },
                        )
                    }
                }
                Text(
                    rootLabel,
                    Modifier.fillMaxWidth().clickable { onPick(null) }.padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary
                )
                val list = remember(folders, forbidden) { flattenFolders(folders).filter { it.first.id !in forbidden } }
                androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(list.size) { i ->
                        val (f, depth) = list[i]
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(f.id) }.padding(vertical = 12.dp, horizontal = (depth * 18).dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ColorDot(f.color); Text(f.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

/** Appunti: contenuto marcato come sensibile e cancellato dopo 60 secondi se ancora presente. */
object SecureClipboard {
    private val handler = Handler(Looper.getMainLooper())

    fun copy(context: Context, text: String, autoClear: Boolean) {
        val cm = context.getSystemService(ClipboardManager::class.java)
        val clip = ClipData.newPlainText("Notium", text)
        if (Build.VERSION.SDK_INT >= 33) {
            clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
        }
        cm.setPrimaryClip(clip)
        handler.removeCallbacksAndMessages(null)
        if (autoClear) handler.postDelayed({ clear(cm) }, 60_000)
    }

    private fun clear(cm: ClipboardManager) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 28) cm.clearPrimaryClip()
            else cm.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }
}
