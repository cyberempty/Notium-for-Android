package com.notium.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.notium.app.AppViewModel
import com.notium.app.AuthState
import com.notium.app.security.PasswordStrength
import kotlinx.coroutines.delay
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import com.notium.app.data.AppLang

@Composable
fun AuthScreensHost(vm: AppViewModel, auth: AuthState, onBiometric: () -> Unit) {
    if (auth == AuthState.Setup) SetupScreen(vm) else LockScreen(vm, onBiometric)
}

/** Ingresso morbido: ogni blocco compare in sequenza (dissolvenza + leggero scorrimento verso l'alto). */
@Composable
fun Entrance(index: Int, content: @Composable () -> Unit) {
    val anim = LocalAnim.current
    val p = remember { Animatable(if (anim.enabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (p.value < 1f) { delay(index * 80L); p.animateTo(1f, tween(480, easing = FastOutSlowInEasing)) }
    }
    Box(Modifier.graphicsLayer { alpha = p.value; translationY = (1f - p.value) * 48f }) { content() }
}

/** Prima schermata: scelta della lingua (predefinita: inglese). */
@Composable
fun LanguageScreen(vm: AppViewModel) {
    val st by vm.settings.collectAsState()
    var open by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.systemBarsPadding().padding(28.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(64.dp))
            Entrance(0) { NotiumLogo(96.dp) }
            Entrance(1) {
                Text("Notium", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            }
            Entrance(2) {
                Text("Your notes. Your privacy.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 40.dp))
            }
            Entrance(3) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Scegli la lingua", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                            Icon(Icons.Outlined.Language, null, Modifier.size(20.dp))
                            Text(st.language.label, modifier = Modifier.weight(1f).padding(start = 12.dp), textAlign = TextAlign.Start)
                            Icon(Icons.Filled.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                            listOf(AppLang.EN, AppLang.ES, AppLang.IT, AppLang.ZH).forEach { l ->
                                DropdownMenuItem(
                                    text = { Text(l.label) },
                                    trailingIcon = { if (l == st.language) Icon(Icons.Outlined.Check, null) },
                                    onClick = { vm.chooseLanguage(l); open = false },
                                )
                            }
                        }
                    }
                }
            }
            Entrance(4) {
                Button(onClick = vm::confirmLanguage, modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(52.dp)) {
                    Text("Avanti")
                    Icon(Icons.Outlined.ArrowForward, null, Modifier.padding(start = 8.dp).size(18.dp))
                }
            }
            Text(
                "Puoi cambiare la lingua in qualsiasi momento da Impostazioni.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun PinDot(filled: Boolean) {
    val c by animateColorAsState(
        if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, tween(160), label = "dot",
    )
    val sc by animateFloatAsState(if (filled) 1.3f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "dotScale")
    Box(Modifier.size(14.dp).graphicsLayer { scaleX = sc; scaleY = sc }.clip(CircleShape).background(c))
}

/** Primo avvio: creazione della password principale. */
@Composable
fun SetupScreen(vm: AppViewModel) {
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    var shownError by remember { mutableStateOf<String?>(null) }
    val busy by vm.busy.collectAsState()
    val strength = PasswordStrength.evaluate(pw)
    val pending by vm.pendingRestore.collectAsState()
    var askBackupPw by remember { mutableStateOf<android.net.Uri?>(null) }
    val pickBackup = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        vm.pickerActive = false
        if (uri != null) vm.startRestore(uri) { askBackupPw = uri }
    }

    fun submit() {
        val problem = PasswordStrength.problem(pw)
        shownError = when {
            problem != null -> problem
            pw != pw2 -> "Le due password non coincidono."
            else -> null
        }
        if (shownError == null) vm.setupPassword(pw.toCharArray())
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            Entrance(0) { NotiumLogo(84.dp) }
            Entrance(1) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Benvenuto in Notium", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                    Text("Your notes. Your privacy.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    Text(
                        "Nessun account, nessuna connessione, nessun cloud. Le note restano cifrate su questo telefono.",
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
            }
            Entrance(2) {
                Column {
                    PasswordField(pw, { pw = it; shownError = null }, "Password principale")
                    StrengthBar(strength)
                    Spacer(Modifier.height(12.dp))
                    PasswordField(pw2, { pw2 = it; shownError = null }, "Conferma password", imeAction = ImeAction.Done, onDone = ::submit)
                    shownError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.padding(top = 20.dp).fillMaxWidth()
            ) {
                Text(
                    "Importante: la password non si può recuperare. Se la dimentichi, le note non sono più leggibili. " +
                        "Crea un backup cifrato dalle Impostazioni.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(14.dp)
                )
            }
            pending?.let { pv ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.padding(top = 20.dp).fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Column {
                            Text("Backup pronto", fontWeight = FontWeight.Medium)
                            Text(
                                "${pv.notes} note, ${pv.folders} cartelle, ${pv.tags} tag. Scegli la password principale per importarle.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            TextButton(onClick = { vm.cancelRestore(); vm.pickerActive = true; pickBackup.launch(arrayOf("*/*")) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                                Text("Scegli un altro file")
                            }
                        }
                    }
                }
            }
            Button(onClick = ::submit, enabled = !busy && pw.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(52.dp)) {
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(if (pending != null) "Crea e importa" else "Crea e proteggi")
            }
            if (pending == null) {
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f))
                    Text("oppure", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp))
                    HorizontalDivider(Modifier.weight(1f))
                }
                OutlinedButton(
                    onClick = { vm.pickerActive = true; pickBackup.launch(arrayOf("*/*")) }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(52.dp)
                ) {
                    Icon(Icons.Outlined.FileDownload, null, Modifier.size(18.dp))
                    Text("  Importa un backup")
                }
            }
            askBackupPw?.let { uri ->
                PasswordPromptDialog("Password del backup", "Inserisci la password usata quando hai creato il backup.",
                    "Verifica", { askBackupPw = null }) { vm.prepareRestore(uri, it); askBackupPw = null }
            }
            Text(
                "PIN e impronta digitale si attivano dopo, da Impostazioni › Sicurezza.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

/** Schermata di blocco: password, tastierino PIN, biometria. */
@Composable
fun LockScreen(vm: AppViewModel, onBiometric: () -> Unit) {
    val options by vm.authOptions.collectAsState()
    val message by vm.authMessage.collectAsState()
    val busy by vm.busy.collectAsState()
    val lockedUntil by vm.pinLockedUntil.collectAsState()
    val st by vm.settings.collectAsState()
    val digits = remember(st.shuffleKeypad, message) { if (st.shuffleKeypad) "0123456789".map { it.toString() }.shuffled() else "1234567890".map { it.toString() } }

    var usePassword by rememberSaveable { mutableStateOf(!options.pinEnabled) }
    var pin by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var autoTried by rememberSaveable { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Se il PIN viene disattivato (troppi tentativi) si passa alla password.
    LaunchedEffect(options.pinEnabled) { if (!options.pinEnabled) usePassword = true }
    // Sblocco biometrico proposto automaticamente una volta.
    LaunchedEffect(options.biometricEnabled) {
        if (options.biometricEnabled && !autoTried) { autoTried = true; onBiometric() }
    }
    // Conto alla rovescia del blocco temporaneo.
    LaunchedEffect(lockedUntil) {
        while (System.currentTimeMillis() < lockedUntil) { now = System.currentTimeMillis(); delay(500) }
        now = System.currentTimeMillis()
    }
    val maxPin = if (options.pinLen > 0) options.pinLen else 12
    val remainingSec = ((lockedUntil - now) / 1000).coerceAtLeast(0)
    val pinBlocked = remainingSec > 0

    // Sblocco automatico: appena il PIN o la password hanno la lunghezza giusta si prova subito, senza premere il tasto.
    LaunchedEffect(pin) {
        if (options.pinLen > 0 && pin.length == options.pinLen && !busy && !pinBlocked && !usePassword) {
            delay(140)
            vm.unlockWithPin(pin.toCharArray()); pin = ""
        }
    }
    LaunchedEffect(pw) {
        if (options.pwLen > 0 && pw.length == options.pwLen && !busy) {
            delay(300)
            vm.unlockWithPassword(pw.toCharArray()); pw = ""
        }
    }
    // Scossa quando il codice e' sbagliato.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(message) {
        if (message != null) {
            repeat(3) { shake.animateTo(16f, tween(50)); shake.animateTo(-16f, tween(50)) }
            shake.animateTo(0f, tween(50))
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Entrance(0) { NotiumLogo(72.dp) }
            Entrance(1) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Notium", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("  Bloccata", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            Entrance(2) { Column(Modifier.graphicsLayer { translationX = shake.value }, horizontalAlignment = Alignment.CenterHorizontally) {
            if (!usePassword && options.pinEnabled) {
                var pinShown by remember { mutableStateOf(false) } // occhio: mostra le cifre digitate
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.size(40.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (pinShown && pin.isNotEmpty()) Text(pin, style = MaterialTheme.typography.titleLarge, letterSpacing = 6.sp)
                        else repeat(if (options.pinLen > 0) options.pinLen else maxOf(6, pin.length)) { i -> PinDot(i < pin.length) }
                    }
                    IconButton(onClick = { pinShown = !pinShown }, Modifier.size(40.dp)) {
                        Icon(if (pinShown) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (pinShown) "Nascondi PIN" else "Mostra PIN")
                    }
                }
                Spacer(Modifier.height(16.dp))
                Keypad(
                    digits = digits, haptics = st.haptics,
                    enabled = !busy && !pinBlocked,
                    showBiometric = options.biometricEnabled,
                    onDigit = { if (pin.length <= maxPin - 1) pin += it },
                    onBackspace = { pin = pin.dropLast(1) },
                    onClear = { pin = "" },
                    onBiometric = onBiometric,
                )
                Button(
                    onClick = { vm.unlockWithPin(pin.toCharArray()); pin = "" },
                    enabled = pin.length >= 6 && !busy && !pinBlocked,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(52.dp),
                ) { Text("Sblocca") }
                TextButton(onClick = { usePassword = true }) { Text("Usa la password principale") }
            } else {
                PasswordField(
                    pw, { pw = it }, "Password principale", imeAction = ImeAction.Done,
                    onDone = { if (pw.isNotEmpty()) { vm.unlockWithPassword(pw.toCharArray()); pw = "" } },
                    isError = message != null,
                )
                Button(
                    onClick = { vm.unlockWithPassword(pw.toCharArray()); pw = "" },
                    enabled = pw.isNotEmpty() && !busy,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(52.dp),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Sblocca")
                }
                Row {
                    if (options.biometricEnabled) {
                        TextButton(onClick = onBiometric) {
                            Icon(Icons.Outlined.Fingerprint, null, Modifier.size(18.dp)); Text("  Impronta digitale")
                        }
                    }
                    if (options.pinEnabled) TextButton(onClick = { usePassword = false }) { Text("Usa il PIN") }
                }
            }

            } }

            if (pinBlocked && !usePassword) {
                Text(
                    "Troppi tentativi. Riprova tra $remainingSec s.",
                    color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)
                )
            }
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun Keypad(
    digits: List<String>, haptics: Boolean, enabled: Boolean, showBiometric: Boolean,
    onDigit: (String) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit, onBiometric: () -> Unit,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val tap: () -> Unit = { if (haptics) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        digits.take(9).chunked(3).forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) { r.forEach { d -> Key(d, enabled) { tap(); onDigit(d) } } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            if (showBiometric) IconKey(enabled, onBiometric) else Box(Modifier.size(72.dp))
            Key(digits[9], enabled) { tap(); onDigit(digits[9]) }
            Box(
                Modifier.size(72.dp).clip(CircleShape).combinedClickable(
                    enabled = enabled, onClick = { tap(); onBackspace() }, onLongClick = { tap(); onClear() }
                ),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Outlined.Backspace, "Cancella") }
        }
    }
}

@Composable
private fun IconKey(enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(72.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Outlined.Fingerprint, "Impronta digitale", tint = MaterialTheme.colorScheme.primary) }
}

@Composable
private fun Key(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, style = MaterialTheme.typography.headlineSmall) }
}
