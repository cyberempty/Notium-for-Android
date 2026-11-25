package com.notium.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.foundation.layout.height
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.sp
import com.notium.app.data.AnimStyle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.notium.app.AppViewModel
import com.notium.app.data.CardStyle
import com.notium.app.data.DateStyle
import com.notium.app.data.EditorFont
import com.notium.app.data.FabPlace
import com.notium.app.data.FabStyle
import com.notium.app.data.StartSection
import com.notium.app.data.ListStyle
import com.notium.app.data.ThemeMode
import com.notium.app.security.BiometricHelper
import com.notium.app.security.PasswordStrength
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface Dlg {
    data object ChangePassword : Dlg
    data object SetPin : Dlg
    data object DisablePin : Dlg
    data object BackupPassword : Dlg
    data class RestorePassword(val uri: android.net.Uri) : Dlg
    data object Wipe : Dlg
    data object EmptyTrash : Dlg
    data object Privacy : Dlg
    data class ResetCat(val c: Cat) : Dlg
    data object HomeTitle : Dlg
}

private enum class Cat(val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SECURITY("Sicurezza", "Password, PIN, biometria, blocco automatico", Icons.Outlined.Lock),
    PRIVACY("Privacy", "Anteprime, appunti, informativa", Icons.Outlined.VisibilityOff),
    BACKUP("Backup", "Crea e ripristina backup cifrati", Icons.Outlined.Backup),
    THEME("Tema e colori", "Tema, accento, carattere, angoli", Icons.Outlined.Palette),
    ANIM("Animazioni", "Transizioni, velocità, stile", Icons.Outlined.Animation),
    HOME("Schermata principale", "Apertura, colonne, pulsante Nuova nota", Icons.Outlined.Home),
    LIST("Elenco note", "Schede, anteprime, data, tag", Icons.Outlined.ViewList),
    EDITOR("Editor", "Carattere, margini, salvataggio, tastiera", Icons.Outlined.Edit),
    DATA("Gestione dati", "Cestino ed eliminazione dei dati", Icons.Outlined.Storage),
    ABOUT("Informazioni", "Versione e dettagli", Icons.Outlined.Info),
    LANGUAGE("Lingua", "Scegli la lingua dell'app", Icons.Outlined.Language),
}

private fun resettable(c: Cat) = c != Cat.BACKUP && c != Cat.ABOUT && c != Cat.LANGUAGE

private fun resetCat(c: Cat, s: com.notium.app.data.Settings): com.notium.app.data.Settings {
    val d = com.notium.app.data.Settings()
    return when (c) {
        Cat.THEME -> s.copy(theme = d.theme, accent = d.accent, uiFont = d.uiFont, fontScale = d.fontScale, cornerRadius = d.cornerRadius,
            bgColor = d.bgColor, cardColor = d.cardColor, highContrast = d.highContrast)
        Cat.ANIM -> s.copy(animations = d.animations, animSpeed = d.animSpeed, animStyle = d.animStyle, animPress = d.animPress,
            animList = d.animList, pressScale = d.pressScale)
        Cat.HOME -> s.copy(startSection = d.startSection, gridColumns = d.gridColumns, showTopLogo = d.showTopLogo, fabStyle = d.fabStyle,
            fabPlace = d.fabPlace, showSearch = d.showSearch, drawerFolders = d.drawerFolders, drawerTags = d.drawerTags,
            screenPadding = d.screenPadding, homeTitle = d.homeTitle)
        Cat.LIST -> s.copy(listStyle = d.listStyle, cardStyle = d.cardStyle, previewLines = d.previewLines, sort = d.sort, titleLines = d.titleLines,
            listTitleSize = d.listTitleSize, listPreviewSize = d.listPreviewSize, cardPadding = d.cardPadding, boldTitles = d.boldTitles,
            borderWidth = d.borderWidth, dateStyle = d.dateStyle, showDates = d.showDates, showTags = d.showTags,
            showFolderInList = d.showFolderInList, cardGap = d.cardGap, showPinIcon = d.showPinIcon, showFavIcon = d.showFavIcon)
        Cat.EDITOR -> s.copy(editorFont = d.editorFont, editorFontSize = d.editorFontSize, lineSpacing = d.lineSpacing, editorMargin = d.editorMargin,
            titleSize = d.titleSize, autosaveDelayMs = d.autosaveDelayMs, autoCorrect = d.autoCorrect, autoCapitalize = d.autoCapitalize,
            keepScreenOn = d.keepScreenOn, showToolbar = d.showToolbar, showCounters = d.showCounters, openInPreview = d.openInPreview,
            editorLetterSpacing = d.editorLetterSpacing, editorTitleBold = d.editorTitleBold,
            autoList = d.autoList, liveStyle = d.liveStyle)
        Cat.SECURITY -> s.copy(autoLockSeconds = d.autoLockSeconds, shuffleKeypad = d.shuffleKeypad, haptics = d.haptics)
        Cat.PRIVACY -> s.copy(secureScreen = d.secureScreen, clearClipboard = d.clearClipboard)
        Cat.DATA -> s.copy(trashDays = d.trashDays)
        else -> s
    }
}

@Composable
private fun CategoryRow(c: Cat, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(c.icon, null, tint = MaterialTheme.colorScheme.onSurface)
        Column(Modifier.weight(1f)) {
            Text(c.title, style = MaterialTheme.typography.titleMedium)
            Text(c.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val BgChoices: List<Pair<String, Int>> = listOf(
    "Predefinito" to 0, "Nero" to 0xFF000000.toInt(), "Grafite" to 0xFF121212.toInt(), "Antracite" to 0xFF1C1C1E.toInt(),
    "Blu notte" to 0xFF0B1220.toInt(), "Verde bosco" to 0xFF0B1410.toInt(), "Bordeaux" to 0xFF1A0B0F.toInt(), "Carta" to 0xFFF5F1E8.toInt(),
)
private val CardChoices: List<Pair<String, Int>> = listOf(
    "Predefinito" to 0, "Nero" to 0xFF000000.toInt(), "Grigio scuro" to 0xFF1B1B1D.toInt(), "Grigio medio" to 0xFF2A2A2D.toInt(),
    "Blu scuro" to 0xFF141C2B.toInt(), "Verde scuro" to 0xFF14231C.toInt(),
)

private const val PRIVACY_TEXT =
    "Notium non ha permessi di rete: non può inviare né ricevere dati.\n\n" +
        "• Nessun account, e-mail, numero di telefono o registrazione.\n" +
        "• Nessuna pubblicità, statistica, diagnostica o analisi dell'uso.\n" +
        "• Nessun accesso a posizione, contatti o altre app.\n" +
        "• Le note sono salvate in un database cifrato (SQLCipher, AES-256) sul tuo dispositivo.\n" +
        "• La chiave del database è casuale e protetta dalla tua password (Argon2id), dal PIN e dall'Android Keystore.\n" +
        "• Nessun backup automatico Android o cloud. I backup sono file cifrati che crei e sposti tu.\n" +
        "• Nessun dato personale nei log e nessun contenuto nelle notifiche.\n\n" +
        "Disinstallando l'app, Android rimuove i dati privati. I backup che hai copiato altrove restano dove li hai salvati."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, activity: FragmentActivity) {
    val s by vm.settings.collectAsState()
    val opts by vm.authOptions.collectAsState()
    val restore by vm.pendingRestore.collectAsState()
    val busy by vm.busy.collectAsState()
    var dlg by remember { mutableStateOf<Dlg?>(null) }
    var cat by rememberSaveable { mutableStateOf<Cat?>(null) }
    BackHandler(enabled = cat != null) { cat = null }
    var pendingBackupPw by remember { mutableStateOf<CharArray?>(null) }
    val context = LocalContext.current

    val createLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        vm.pickerActive = false
        val pw = pendingBackupPw; pendingBackupPw = null
        if (uri != null && pw != null) vm.createBackup(uri, pw)
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        vm.pickerActive = false
        if (uri != null) dlg = Dlg.RestorePassword(uri)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(cat?.title ?: "Impostazioni") },
                navigationIcon = { IconButton(onClick = { if (cat != null) cat = null else vm.back() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Indietro") } },
                actions = {
                    val cc = cat
                    if (cc != null && resettable(cc)) IconButton(onClick = { dlg = Dlg.ResetCat(cc) }) { Icon(Icons.Outlined.RestartAlt, "Ripristina predefiniti") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            val anim = LocalAnim.current
            androidx.compose.animation.AnimatedContent(
                targetState = cat,
                transitionSpec = {
                    val d = anim.ms(260)
                    val deeper = targetState != null
                    (androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(d)) { w -> if (deeper) w / 5 else -w / 5 } +
                        androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(d))) togetherWith
                        (androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(d)) { w -> if (deeper) -w / 5 else w / 5 } +
                            androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(d)))
                },
                label = "settings-cat",
            ) { c ->
            Column(Modifier.fillMaxWidth()) {
            if (c == null) {
                Cat.entries.forEach { k -> CategoryRow(k) { cat = k } }
            } else {
                when (c) {
                    Cat.SECURITY -> {
            if (!opts.pinEnabled && !opts.biometricEnabled) {
                Text(
                    "Senza PIN né impronta digitale l'app si apre direttamente, senza chiedere la password. Attiva uno dei due per proteggere l'accesso.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            Item("Cambia password principale", "Il database non viene ricifrato: cambia solo la protezione della chiave.") { dlg = Dlg.ChangePassword }
            if (opts.pinEnabled) {
                Item("Modifica PIN", "Sblocco rapido, minimo 6 cifre") { dlg = Dlg.SetPin }
                Item("Disattiva PIN", null) { dlg = Dlg.DisablePin }
            } else {
                Item("Attiva PIN", "Sblocco rapido. Dopo 5 errori scatta un blocco temporaneo.") { dlg = Dlg.SetPin }
            }
            SwitchItem(
                "Impronta digitale",
                if (opts.biometricAvailable) "Sblocca la chiave con la biometria del dispositivo" else "Non disponibile su questo dispositivo",
                opts.biometricEnabled, enabled = opts.biometricAvailable
            ) { on ->
                if (!on) vm.disableBiometric()
                else {
                    val c = vm.biometricEnrollCipher()
                    if (c == null) vm.notify("Impossibile attivare la biometria.")
                    else BiometricHelper.prompt(
                        activity, com.notium.app.ui.I18n.tr("Attiva l'impronta digitale", s.language), com.notium.app.ui.I18n.tr("Conferma per proteggere la chiave con la biometria", s.language), c,
                        com.notium.app.ui.I18n.tr("Annulla", s.language),
                        onSuccess = vm::biometricEnrollSuccess,
                        onFailure = { msg, canceled -> if (!canceled) vm.notify(msg) },
                    )
                }
            }
            Choice("Blocco automatico", when (s.autoLockSeconds) {
                0 -> "Subito"; 30 -> "Dopo 30 secondi"; 60 -> "Dopo 1 minuto"; 300 -> "Dopo 5 minuti"
                900 -> "Dopo 15 minuti"; else -> "Mai (finché l'app resta in memoria)"
            }, listOf("Subito" to 0, "Dopo 30 secondi" to 30, "Dopo 1 minuto" to 60, "Dopo 5 minuti" to 300,
                "Dopo 15 minuti" to 900, "Mai (finché l'app resta in memoria)" to -1)) { v -> vm.updateSettings { it.copy(autoLockSeconds = v) } }
            SwitchItem("Tastierino PIN casuale", "Cambia la posizione dei numeri a ogni sblocco", s.shuffleKeypad) { v -> vm.updateSettings { it.copy(shuffleKeypad = v) } }
            SwitchItem("Vibrazione sui tasti del PIN", null, s.haptics) { v -> vm.updateSettings { it.copy(haptics = v) } }
                    }
                    Cat.PRIVACY -> {
            SwitchItem("Proteggi anteprime e schermate", "Nasconde il contenuto nelle app recenti e blocca gli screenshot", s.secureScreen) { v ->
                vm.updateSettings { it.copy(secureScreen = v) }
            }
            SwitchItem("Svuota gli appunti dopo 60 secondi", "Vale per il testo copiato da Notium", s.clearClipboard) { v ->
                vm.updateSettings { it.copy(clearClipboard = v) }
            }
            Item("Informativa sulla privacy", "Cosa fa e cosa non fa Notium con i tuoi dati") { dlg = Dlg.Privacy }
                    }
                    Cat.BACKUP -> {
            Item("Crea backup cifrato", "File .notium protetto da una password indipendente (AES-256-GCM + Argon2id)") { dlg = Dlg.BackupPassword }
            Item("Importa backup", "Sostituisce tutte le note con quelle del file. Prima verifica integrità e firma.") {
                vm.pickerActive = true
                openLauncher.launch(arrayOf("*/*"))
            }
                    }
                    Cat.THEME -> {
            Choice("Tema", s.theme.label, ThemeMode.entries.map { it.label to it }) { v -> vm.updateSettings { it.copy(theme = v) } }
            ColorItem("Colore d'accento", s.accent, AccentChoices, MaterialTheme.colorScheme.onSurface) { v -> vm.updateSettings { it.copy(accent = v) } }
            Choice("Carattere dell'interfaccia", s.uiFont.label, EditorFont.entries.map { it.label to it }) { v -> vm.updateSettings { it.copy(uiFont = v) } }
            SliderItem("Dimensione del testo", s.fontScale, 0.8f..1.5f, { "${(it * 100).toInt()}%" }, applyOnRelease = true, sample = true) { v ->
                vm.updateSettings { it.copy(fontScale = v) }
            }
            SliderItem("Angoli arrotondati", s.cornerRadius.toFloat(), 0f..28f, { "${it.toInt()} dp" }, applyOnRelease = true) { v ->
                vm.updateSettings { it.copy(cornerRadius = v.toInt()) }
            }
            SwitchItem("Alto contrasto", "Testi e bordi più marcati, per leggere meglio", s.highContrast) { v -> vm.updateSettings { it.copy(highContrast = v) } }
            ColorItem("Colore di sfondo", s.bgColor, BgChoices, MaterialTheme.colorScheme.background) { v -> vm.updateSettings { it.copy(bgColor = v) } }
            ColorItem("Colore delle schede", s.cardColor, CardChoices, MaterialTheme.colorScheme.surface) { v -> vm.updateSettings { it.copy(cardColor = v) } }
                    }
                    Cat.ANIM -> {
            SwitchItem("Animazioni", "Transizioni tra schermate, elenco note, pulsanti e menu", s.animations) { v -> vm.updateSettings { it.copy(animations = v) } }
            SwitchItem("Movimento dell'elenco", "Le schede scorrono e compaiono con dolcezza quando la lista cambia", s.animList) { v -> vm.updateSettings { it.copy(animList = v) } }
            SwitchItem("Effetto pressione", "Le schede si rimpiccioliscono leggermente quando le tocchi", s.animPress) { v -> vm.updateSettings { it.copy(animPress = v) } }
            SliderItem("Intensità dell'effetto pressione", s.pressScale, 0.88f..1.0f, { "${(it * 100).toInt()}%" }) { v -> vm.updateSettings { it.copy(pressScale = v) } }
            Choice("Stile delle transizioni", s.animStyle.label, AnimStyle.entries.map { it.label to it }) { v -> vm.updateSettings { it.copy(animStyle = v) } }
            SliderItem("Velocità delle animazioni", s.animSpeed, 0.5f..2f, { "${(it * 100).toInt()}%" }) { v ->
                vm.updateSettings { it.copy(animSpeed = (v * 20).toInt() / 20f) }
            }
                    }
                    Cat.HOME -> {
            Choice("Sezione all'apertura", s.startSection.label, StartSection.entries.map { it.label to it }) { v -> vm.updateSettings { it.copy(startSection = v) } }
            Choice("Colonne", "${s.gridColumns}", (1..3).map { "$it" to it }) { v -> vm.updateSettings { it.copy(gridColumns = v) } }
            Item("Nome nella barra superiore", s.homeTitle) { dlg = Dlg.HomeTitle }
            SliderItem("Margini laterali dell'elenco", s.screenPadding.toFloat(), 0f..32f, { "${it.toInt()} dp" }) { v -> vm.updateSettings { it.copy(screenPadding = v.toInt()) } }
            SwitchItem("Barra di ricerca", null, s.showSearch) { v -> vm.updateSettings { it.copy(showSearch = v) } }
            SwitchItem("Cartelle nel menu laterale", null, s.drawerFolders) { v -> vm.updateSettings { it.copy(drawerFolders = v) } }
            SwitchItem("Tag nel menu laterale", null, s.drawerTags) { v -> vm.updateSettings { it.copy(drawerTags = v) } }
            SwitchItem("Logo nella barra superiore", null, s.showTopLogo) { v -> vm.updateSettings { it.copy(showTopLogo = v) } }
            Choice("Pulsante Nuova nota", s.fabStyle.label, FabStyle.entries.map { it.label to it }) { v -> vm.updateSettings { it.copy(fabStyle = v) } }
            Choice("Posizione del pulsante", s.fabPlace.label, FabPlace.entries.map { it.label to it }) { v -> vm.updateSettings { it.copy(fabPlace = v) } }
                    }
                    Cat.LIST -> {
            Choice("Densità", s.listStyle.label, ListStyle.entries.map { it.label to it }) { v -> vm.updateSettings { it.copy(listStyle = v) } }
            Choice("Stile delle schede", s.cardStyle.label, CardStyle.entries.map { it.label to it }) { v -> vm.updateSettings { it.copy(cardStyle = v) } }
            Choice("Ordinamento predefinito", s.sort.label, com.notium.app.data.SortOrder.entries.map { it.label to it }) { v ->
                vm.updateSettings { it.copy(sort = v) }
                vm.setSort(v)
            }
            Choice("Righe del titolo", "${s.titleLines}", (1..3).map { "$it" to it }) { v -> vm.updateSettings { it.copy(titleLines = v) } }
            SliderItem("Dimensione del titolo", s.listTitleSize, 12f..24f, { "${it.toInt()} sp" }) { v -> vm.updateSettings { it.copy(listTitleSize = v) } }
            SliderItem("Spaziatura interna delle schede", s.cardPadding.toFloat(), 6f..28f, { "${it.toInt()} dp" }) { v -> vm.updateSettings { it.copy(cardPadding = v.toInt()) } }
            SliderItem("Distanza tra le schede", s.cardGap.toFloat(), 0f..28f, { "${it.toInt()} dp" }) { v -> vm.updateSettings { it.copy(cardGap = v.toInt()) } }
            SwitchItem("Icona di nota fissata", null, s.showPinIcon) { v -> vm.updateSettings { it.copy(showPinIcon = v) } }
            SwitchItem("Icona di nota preferita", null, s.showFavIcon) { v -> vm.updateSettings { it.copy(showFavIcon = v) } }
            SwitchItem("Titoli in grassetto", null, s.boldTitles) { v -> vm.updateSettings { it.copy(boldTitles = v) } }
            SliderItem("Spessore dei bordi", s.borderWidth.toFloat(), 0f..3f, { "${it.toInt()} dp" }) { v -> vm.updateSettings { it.copy(borderWidth = v.toInt()) } }
            Choice("Formato della data", s.dateStyle.label, DateStyle.entries.map { it.label to it }) { v -> vm.updateSettings { it.copy(dateStyle = v) } }
            SwitchItem("Mostra la data", null, s.showDates) { v -> vm.updateSettings { it.copy(showDates = v) } }
            SwitchItem("Mostra i tag", null, s.showTags) { v -> vm.updateSettings { it.copy(showTags = v) } }
            SwitchItem("Mostra la cartella", null, s.showFolderInList) { v -> vm.updateSettings { it.copy(showFolderInList = v) } }
                    }
                    Cat.EDITOR -> {
            Text(
                "Anteprima: così apparirà il testo delle tue note mentre scrivi.",
                fontSize = s.editorFontSize.sp, lineHeight = (s.editorFontSize * s.lineSpacing).sp, fontFamily = s.editorFont.family(),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            Choice("Carattere", s.editorFont.label, EditorFont.entries.map { it.label to it }) { v -> vm.updateSettings { it.copy(editorFont = v) } }
            SliderItem("Dimensione del testo", s.editorFontSize, 13f..26f, { "${it.toInt()} sp" }) { v ->
                vm.updateSettings { it.copy(editorFontSize = v) }
            }
            SliderItem("Interlinea", s.lineSpacing, 1.0f..2.2f, { String.format(Locale.getDefault(), "%.1f", it) }) { v ->
                vm.updateSettings { it.copy(lineSpacing = (v * 10).toInt() / 10f) }
            }
            SliderItem("Spaziatura tra le lettere", s.editorLetterSpacing, -0.5f..2.0f, { String.format(Locale.getDefault(), "%.1f sp", it) }) { v -> vm.updateSettings { it.copy(editorLetterSpacing = (v * 10).toInt() / 10f) } }
            SwitchItem("Titolo in grassetto", null, s.editorTitleBold) { v -> vm.updateSettings { it.copy(editorTitleBold = v) } }
            SliderItem("Margini laterali", s.editorMargin.toFloat(), 8f..48f, { "${it.toInt()} dp" }) { v -> vm.updateSettings { it.copy(editorMargin = v.toInt()) } }
            SliderItem("Dimensione del titolo", s.titleSize, 20f..40f, { "${it.toInt()} sp" }) { v -> vm.updateSettings { it.copy(titleSize = v) } }
            Choice("Salvataggio automatico", "dopo ${s.autosaveDelayMs / 1000.0} s", listOf("dopo 0.3 s" to 300, "dopo 0.7 s" to 700, "dopo 1.5 s" to 1500, "dopo 3 s" to 3000)) { v -> vm.updateSettings { it.copy(autosaveDelayMs = v) } }
            SwitchItem("Correzione automatica", null, s.autoCorrect) { v -> vm.updateSettings { it.copy(autoCorrect = v) } }
            SwitchItem("Maiuscola a inizio frase", null, s.autoCapitalize) { v -> vm.updateSettings { it.copy(autoCapitalize = v) } }
            SwitchItem("Mantieni lo schermo acceso mentre scrivi", null, s.keepScreenOn) { v -> vm.updateSettings { it.copy(keepScreenOn = v) } }
            SwitchItem("Continua gli elenchi con Invio", "Elenchi puntati, numerati e caselle si proseguono da soli", s.autoList) { v -> vm.updateSettings { it.copy(autoList = v) } }
            SwitchItem("Barra di formattazione", "Grassetto, corsivo, titoli, elenchi, caselle", s.showToolbar) { v -> vm.updateSettings { it.copy(showToolbar = v) } }
            SwitchItem("Contatore parole e caratteri", null, s.showCounters) { v -> vm.updateSettings { it.copy(showCounters = v) } }
                    }
                    Cat.DATA -> {
            Choice("Svuotamento automatico cestino", if (s.trashDays == 0) "Mai" else "Dopo ${s.trashDays} giorni",
                listOf("Mai" to 0, "Dopo 7 giorni" to 7, "Dopo 30 giorni" to 30, "Dopo 90 giorni" to 90)) { v -> vm.updateSettings { it.copy(trashDays = v) } }
            Item("Svuota il cestino ora", null) { dlg = Dlg.EmptyTrash }
            Item("Elimina tutti i dati", "Cancella note, chiavi e preferenze da questo dispositivo", danger = true) { dlg = Dlg.Wipe }
                    }
                    Cat.ABOUT -> {
            Item("Notium 1.0.0", "Your notes. Your privacy. · 100% offline · Android 8.0+") {}
                    }
                    Cat.LANGUAGE -> {
            listOf(com.notium.app.data.AppLang.EN, com.notium.app.data.AppLang.ES, com.notium.app.data.AppLang.IT, com.notium.app.data.AppLang.ZH).forEach { l ->
                Row(
                    Modifier.fillMaxWidth().clickable { vm.updateSettings { it.copy(language = l) } }.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    androidx.compose.material3.RadioButton(selected = s.language == l, onClick = null)
                    Text(l.label, style = MaterialTheme.typography.titleMedium)
                }
            }
                    }
                }
                if (resettable(c)) {
                    HorizontalDivider(Modifier.padding(top = 12.dp))
                    Item("Ripristina predefiniti", "Riporta solo le opzioni di «${c.title}» ai valori iniziali", danger = true) { dlg = Dlg.ResetCat(c) }
                }
            }
            }
            }
            Box(Modifier.padding(24.dp))
        }
    }

    when (val d = dlg) {
        null -> Unit
        Dlg.ChangePassword -> ChangePasswordDialog(busy, { dlg = null }) { old, new -> vm.changePassword(old, new) { ok -> if (ok) dlg = null } }
        Dlg.SetPin -> PinDialog({ dlg = null }) { vm.enablePin(it); dlg = null }
        Dlg.DisablePin -> ConfirmDialog("Disattivare il PIN?", "Potrai sbloccare Notium con la password principale.", "Disattiva",
            onDismiss = { dlg = null }, onConfirm = { vm.disablePin(); dlg = null })
        Dlg.BackupPassword -> NewPasswordDialog(
            "Password del backup",
            "Scegli una password per questo backup. È indipendente da quella dell'app: senza di essa il file non si può aprire.",
            "Continua", { dlg = null }
        ) { pw ->
            pendingBackupPw = pw
            dlg = null
            vm.pickerActive = true
            val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(Date())
            createLauncher.launch("Notium-backup-$stamp.notium")
        }
        is Dlg.RestorePassword -> PasswordPromptDialog("Password del backup", "Inserisci la password usata quando hai creato il backup.",
            "Verifica", { dlg = null }) { vm.prepareRestore(d.uri, it); dlg = null }
        Dlg.Wipe -> WipeDialog({ dlg = null }) { vm.wipeEverything(it); dlg = null }
        Dlg.EmptyTrash -> ConfirmDialog("Svuotare il cestino?", "Le note nel cestino verranno eliminate definitivamente.", "Svuota",
            danger = true, onDismiss = { dlg = null }, onConfirm = { vm.emptyTrash(); dlg = null })
        is Dlg.ResetCat -> ConfirmDialog(
            "Ripristinare «${d.c.title}»?", "Le opzioni di questa categoria tornano ai valori predefiniti. Note e dati non vengono toccati.",
            "Ripristina", danger = true, onDismiss = { dlg = null },
            onConfirm = { vm.updateSettings { st -> resetCat(d.c, st) }; dlg = null }
        )
        Dlg.HomeTitle -> {
            var text by remember { mutableStateOf(s.homeTitle) }
            AlertDialog(
                onDismissRequest = { dlg = null }, title = { Text("Nome nella barra superiore") },
                text = { OutlinedTextField(text, { if (it.length <= 24) text = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
                confirmButton = { TextButton(onClick = { vm.updateSettings { st -> st.copy(homeTitle = text.trim().ifBlank { "Notium" }) }; dlg = null }) { Text("Salva") } },
                dismissButton = { TextButton(onClick = { dlg = null }) { Text("Annulla") } },
            )
        }
        Dlg.Privacy -> AlertDialog(
            onDismissRequest = { dlg = null }, title = { Text("Informativa sulla privacy") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(PRIVACY_TEXT) } },
            confirmButton = { TextButton(onClick = { dlg = null }) { Text("Chiudi") } },
        )
    }

    restore?.let { p ->
        AlertDialog(
            onDismissRequest = vm::cancelRestore,
            title = { Text("Backup verificato") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Integrità e autenticità confermate.")
                    Text("Contiene ${p.notes} note, ${p.folders} cartelle e ${p.tags} tag (creato il ${SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(p.data.createdAt))}).")
                    Text("Il ripristino SOSTITUISCE tutte le note attuali. Se qualcosa va storto, i dati attuali restano invariati.")
                }
            },
            confirmButton = { TextButton(onClick = vm::confirmRestore, enabled = !busy) { Text("Ripristina", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = vm::cancelRestore) { Text("Annulla") } },
        )
    }
}

// ------------------------------------------------------------------ elementi di lista

@Composable
private fun SettingsSection(title: String) {
    Text(
        title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 6.dp)
    )
}

@Composable
private fun Item(title: String, subtitle: String?, danger: Boolean = false, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchItem(title: String, subtitle: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled) { onChange(!checked) }.padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

private fun confirmWord(l: com.notium.app.data.AppLang) = when (l) {
    com.notium.app.data.AppLang.EN -> "DELETE"
    com.notium.app.data.AppLang.ES -> "ELIMINAR"
    com.notium.app.data.AppLang.IT -> "ELIMINA"
    com.notium.app.data.AppLang.ZH -> "删除"
}

@Composable
private fun <T> Choice(title: String, current: String, options: List<Pair<String, T>>, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Item(title, current) { open = true }
        DropdownMenu(open, { open = false }, modifier = Modifier.padding(horizontal = 8.dp)) {
            options.forEach { (label, v) -> DropdownMenuItem(text = { Text(label) }, onClick = { onSelect(v); open = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderItem(
    title: String, current: Float, range: ClosedFloatingPointRange<Float>, format: (Float) -> String,
    applyOnRelease: Boolean = false, sample: Boolean = false, onChange: (Float) -> Unit,
) {
    // Il cursore e il valore seguono il dito in modo continuo (stato locale). Le opzioni che ridisegnano
    // tutta l'app (dimensione testo, angoli) vengono applicate al rilascio, con anteprima durante il trascinamento.
    var dragging by remember { mutableStateOf(false) }
    var live by remember { mutableStateOf(current) }
    androidx.compose.runtime.LaunchedEffect(current) { if (!dragging) live = current }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(format(live), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (sample) Text(
            "Anteprima del testo", fontSize = (16f * live).sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp).animateContentSize(),
        )
        val knob = MaterialTheme.colorScheme.onSurface
        val rail = MaterialTheme.colorScheme.outlineVariant
        Slider(
            value = live,
            onValueChange = { dragging = true; live = it; if (!applyOnRelease) onChange(it) },
            valueRange = range,
            onValueChangeFinished = { if (applyOnRelease) onChange(live); dragging = false },
            thumb = { Box(Modifier.size(22.dp).clip(CircleShape).background(knob)) },
            track = { st ->
                val f = ((st.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(rail)) {
                    Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(knob))
                }
            },
        )
    }
}

@Composable
private fun ColorItem(title: String, current: Int, choices: List<Pair<String, Int>>, zeroColor: androidx.compose.ui.graphics.Color, onSelect: (Int) -> Unit) {
    var hexDialog by remember { mutableStateOf(false) }
    val custom = choices.none { it.second == current }
    if (hexDialog) {
        var hex by remember { mutableStateOf(if (custom) String.format("%06X", current and 0xFFFFFF) else "") }
        val parsed = hex.takeIf { it.length == 6 }?.toIntOrNull(16)
        AlertDialog(
            onDismissRequest = { hexDialog = false }, title = { Text("Colore personalizzato") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Inserisci un colore esadecimale a 6 cifre (ad esempio 7FA6D9).")
                    OutlinedTextField(hex, { v -> if (v.length <= 6 && v.all { it.isLetterOrDigit() }) hex = v.uppercase() },
                        label = { Text("#RRGGBB") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (parsed != null) Box(Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(androidx.compose.ui.graphics.Color(0xFF000000.toInt() or parsed)))
                }
            },
            confirmButton = { TextButton(enabled = parsed != null, onClick = { onSelect(0xFF000000.toInt() or parsed!!); hexDialog = false }) { Text("Applica") } },
            dismissButton = { TextButton(onClick = { hexDialog = false }) { Text("Annulla") } },
        )
    }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(choices.firstOrNull { it.second == current }?.first ?: "Personalizzato",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            choices.forEach { (_, c) ->
                val shown = if (c == 0) zeroColor else androidx.compose.ui.graphics.Color(c)
                Box(
                    Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(shown)
                        .border(
                            if (c == current) 3.dp else 1.dp,
                            if (c == current) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                            androidx.compose.foundation.shape.CircleShape
                        )
                        .clickable { onSelect(c) }
                )
            }
            Box(
                Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (custom) androidx.compose.ui.graphics.Color(current) else androidx.compose.ui.graphics.Color.Transparent)
                    .border(if (custom) 3.dp else 1.dp, if (custom) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant, androidx.compose.foundation.shape.CircleShape)
                    .clickable { hexDialog = true },
                contentAlignment = Alignment.Center
            ) { if (!custom) Text("+", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

// ------------------------------------------------------------------------- dialoghi

@Composable
private fun ChangePasswordDialog(busy: Boolean, onDismiss: () -> Unit, onConfirm: (CharArray, CharArray) -> Unit) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var new2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Cambia password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PasswordField(old, { old = it; error = null }, "Password attuale")
                PasswordField(new, { new = it; error = null }, "Nuova password")
                StrengthBar(PasswordStrength.evaluate(new))
                PasswordField(new2, { new2 = it; error = null }, "Conferma nuova password", imeAction = ImeAction.Done)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && old.isNotEmpty(), onClick = {
                error = PasswordStrength.problem(new) ?: if (new != new2) "Le due password non coincidono." else null
                if (error == null) onConfirm(old.toCharArray(), new.toCharArray())
            }) { Text("Cambia") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
private fun NewPasswordDialog(title: String, info: String, confirm: String, onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit) {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(info, style = MaterialTheme.typography.bodyMedium)
                PasswordField(a, { a = it; error = null }, "Password")
                StrengthBar(PasswordStrength.evaluate(a))
                PasswordField(b, { b = it; error = null }, "Conferma password", imeAction = ImeAction.Done)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = PasswordStrength.problem(a) ?: if (a != b) "Le due password non coincidono." else null
                if (error == null) onConfirm(a.toCharArray())
            }) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
internal fun PasswordPromptDialog(title: String, info: String, confirm: String, onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit) {
    var a by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(info)
                PasswordField(a, { a = it }, "Password", imeAction = ImeAction.Done, onDone = { if (a.isNotEmpty()) onConfirm(a.toCharArray()) })
            }
        },
        confirmButton = { TextButton(enabled = a.isNotEmpty(), onClick = { onConfirm(a.toCharArray()) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

private fun weakPin(p: String): Boolean {
    if (p.toSet().size == 1) return true
    val d = p.map { it - '0' }
    return d.zipWithNext().all { (x, y) -> y - x == 1 } || d.zipWithNext().all { (x, y) -> x - y == 1 }
}

@Composable
private fun PinDialog(onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit) {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val kb = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("PIN di sblocco rapido") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Da 6 a 12 cifre. Non può sostituire la password: serve solo a sbloccare più in fretta.")
                OutlinedTextField(a, { if (it.length <= 12 && it.all(Char::isDigit)) { a = it; error = null } }, label = { Text("PIN") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = kb, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(b, { if (it.length <= 12 && it.all(Char::isDigit)) { b = it; error = null } }, label = { Text("Conferma PIN") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = kb, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    a.length < 6 -> "Il PIN deve avere almeno 6 cifre."
                    weakPin(a) -> "PIN troppo semplice (cifre uguali o in sequenza)."
                    a != b -> "I due PIN non coincidono."
                    else -> null
                }
                if (error == null) onConfirm(a.toCharArray())
            }) { Text("Attiva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
private fun WipeDialog(onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit) {
    var pw by remember { mutableStateOf("") }
    var word by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Eliminare tutti i dati?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Verranno cancellati database, chiavi e preferenze di questo dispositivo. Non è reversibile. " +
                        "Non viene garantita la cancellazione forense delle memorie flash né dei backup che hai copiato altrove."
                )
                PasswordField(pw, { pw = it }, "Password principale")
                OutlinedTextField(word, { word = it }, label = { Text("Scrivi ELIMINA per confermare") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = pw.isNotEmpty() && word.trim().equals(confirmWord(LocalLang.current), ignoreCase = true), onClick = { onConfirm(pw.toCharArray()) }) {
                Text("Elimina tutto", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
