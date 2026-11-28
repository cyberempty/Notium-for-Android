package com.notium.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.fragment.app.FragmentActivity
import com.notium.app.NoteLockMode
import com.notium.app.NoteLockRequest
import com.notium.app.AppViewModel
import com.notium.app.security.BiometricHelper

/** Crea, verifica o rimuove il blocco grafico di una singola nota (PIN o password propri della nota). */
@Composable
fun NoteLockDialog(vm: AppViewModel, activity: FragmentActivity, req: NoteLockRequest, lang: com.notium.app.data.AppLang) {
    if (req.mode == NoteLockMode.CREATE) CreateLockDialog(vm, req) else AskLockDialog(vm, activity, req, lang)
}

@Composable
private fun CreateLockDialog(vm: AppViewModel, req: NoteLockRequest) {
    var isPin by remember { mutableStateOf(true) }
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val opts by vm.authOptions.collectAsState()
    var show by remember { mutableStateOf(false) } // occhio: mostra/nascondi PIN o password
    var bio by remember { mutableStateOf(true) } // sblocco con impronta: attivo di default
    AlertDialog(
        onDismissRequest = { vm.cancelNoteLock() },
        title = { Text("Blocca nota") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (isPin) "Crea un PIN per questa nota (almeno 4 cifre)." else "Crea una password per questa nota (almeno 4 caratteri).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                SecretField(a, { v -> a = if (isPin) v.filter { it.isDigit() }.take(12) else v; error = null },
                    if (isPin) "PIN" else "Password", show, { show = !show }, numeric = isPin)
                SecretField(b, { v -> b = if (isPin) v.filter { it.isDigit() }.take(12) else v; error = null },
                    if (isPin) "Conferma PIN" else "Conferma password", show, { show = !show }, numeric = isPin)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = { isPin = !isPin; a = ""; b = ""; error = null }) {
                    Text(if (isPin) "Usa una password" else "Usa un PIN")
                }
                if (opts.biometricAvailable) Row(
                    Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Sblocca anche con l'impronta digitale", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(bio, { bio = it })
                }
                Text(
                    "Il blocco vale solo su questo dispositivo e non è incluso nei backup.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    a.length < 4 -> error = if (isPin) "Il PIN deve avere almeno 4 cifre." else "La password deve avere almeno 4 caratteri."
                    a != b -> error = if (isPin) "I due PIN non coincidono." else "Le due password non coincidono."
                    else -> vm.createNoteLock(req.noteId, a.toCharArray(), isPin, bio && opts.biometricAvailable)
                }
            }) { Text("Blocca") }
        },
        dismissButton = { TextButton(onClick = { vm.cancelNoteLock() }) { Text("Annulla") } },
    )
}

@Composable
private fun AskLockDialog(vm: AppViewModel, activity: FragmentActivity, req: NoteLockRequest, lang: com.notium.app.data.AppLang) {
    val isPin = vm.noteLockIsPin(req.noteId)
    val opts by vm.authOptions.collectAsState()
    var a by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    val wrong by vm.noteLockError.collectAsState()
    androidx.compose.runtime.LaunchedEffect(req) { vm.clearNoteLockError() }
    val bioOn = vm.noteLockUsesBio(req.noteId) && opts.biometricAvailable
    fun askFingerprint() = BiometricHelper.authenticate(
        activity, I18n.tr("Nota protetta", lang), I18n.tr("Annulla", lang),
        onSuccess = { vm.noteAuthenticated(req) }, onFallback = {},
    )
    androidx.compose.runtime.LaunchedEffect(req) { if (bioOn) askFingerprint() }
    AlertDialog(
        onDismissRequest = { vm.cancelNoteLock() },
        title = { Text("Nota protetta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (isPin) "Inserisci il PIN di questa nota." else "Inserisci la password di questa nota.")
                SecretField(a, { v -> a = if (isPin) v.filter { it.isDigit() }.take(12) else v; vm.clearNoteLockError() },
                    if (isPin) "PIN" else "Password", show, { show = !show }, numeric = isPin, isError = wrong != null)
                wrong?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                if (bioOn) TextButton(onClick = { askFingerprint() }) { Text("Usa l'impronta digitale") }
            }
        },
        confirmButton = { TextButton(enabled = a.isNotEmpty(), onClick = { vm.verifyNoteSecret(a.toCharArray(), req) }) { Text("Continua") } },
        dismissButton = { TextButton(onClick = { vm.cancelNoteLock() }) { Text("Annulla") } },
    )
}
