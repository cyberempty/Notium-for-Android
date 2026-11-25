package com.notium.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FormatIndentDecrease
import androidx.compose.material.icons.outlined.FormatIndentIncrease
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notium.app.AppViewModel
import com.notium.app.SaveStatus
import com.notium.app.data.EditorFont

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(vm: AppViewModel) {
    val state by vm.editor.collectAsState()
    val folders by vm.folders.collectAsState()
    val tags by vm.tags.collectAsState()
    val settings by vm.settings.collectAsState()
    val lang = LocalLang.current
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    val kbOpts = androidx.compose.foundation.text.KeyboardOptions(
        autoCorrectEnabled = settings.autoCorrect,
        capitalization = if (settings.autoCapitalize) androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
        else androidx.compose.ui.text.input.KeyboardCapitalization.None,
    )

    val e = state
    if (e == null) {
        LaunchedEffect(Unit) { vm.back() }
        return
    }

    // Il testo vive qui per mantenere cursore e selezione; la copia "ufficiale" e' nel ViewModel.
    val initial = remember { e }
    var title by remember { mutableStateOf(TextFieldValue(initial.title)) }
    var body by remember { mutableStateOf(RichCodec.load(initial.content)) }
    var pending by remember { mutableStateOf<Set<Fmt>?>(null) }
    var lastWord by remember { mutableStateOf<TextRange?>(null) }
    var lastWordAt by remember { mutableStateOf(0L) }
    var folderDialog by remember { mutableStateOf(false) }
    var tagDialog by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    // Annulla / Ripeti (come in Samsung Notes): le modifiche ravvicinate formano un unico passo.
    val undo = remember { mutableStateListOf<Rich>() }
    val redo = remember { mutableStateListOf<Rich>() }
    var lastEditAt by remember { mutableStateOf(0L) }
    fun md(b: Rich = body): String = RichCodec.toMarkdown(b.text, b.spans)
    fun push(t: TextFieldValue = title, b: Rich = body, force: Boolean = false) {
        if (b.text != body.text) {
            val now = System.currentTimeMillis()
            if (force || undo.isEmpty() || now - lastEditAt > 800) {
                undo.add(body)
                if (undo.size > 100) undo.removeAt(0)
            }
            lastEditAt = if (force) 0L else now
            redo.clear()
        }
        title = t; body = b
        pending = null
        vm.onEdit(t.text, md(b))
    }
    fun pushNow(b: Rich) = push(b = b, force = true)
    fun undoNow() {
        if (undo.isEmpty()) return
        redo.add(body); val prev = undo.removeAt(undo.lastIndex)
        body = prev; vm.onEdit(title.text, md(prev))
    }
    fun redoNow() {
        if (redo.isEmpty()) return
        undo.add(body); val nxt = redo.removeAt(redo.lastIndex)
        body = nxt; vm.onEdit(title.text, md(nxt))
    }

    // Cerca e sostituisci nella nota
    var findOpen by remember { mutableStateOf(false) }
    var findQ by remember { mutableStateOf("") }
    var replaceQ by remember { mutableStateOf("") }
    var findIndex by remember { mutableStateOf(0) }
    var styleMenu by remember { mutableStateOf(false) }
    var insertMenu by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var layoutRes by remember { mutableStateOf<TextLayoutResult?>(null) }
    var bodyTopPx by remember { mutableStateOf(0f) }
    val matches = remember(body.text, findQ, findOpen) { if (findOpen) findMatches(body.text, findQ) else emptyList() }
    fun goTo(i: Int) {
        if (matches.isEmpty()) return
        val k = ((i % matches.size) + matches.size) % matches.size
        findIndex = k
        val st = matches[k]
        body = body.copy(tf = body.tf.copy(selection = TextRange(st, st + findQ.length)))
        layoutRes?.let { lr ->
            val y = lr.getLineTop(lr.getLineForOffset(st))
            scope.launch { scroll.animateScrollTo((bodyTopPx + y - 160f).toInt().coerceAtLeast(0)) }
        }
    }
    fun replaceOne() {
        if (matches.isEmpty()) return
        val st = matches[findIndex.coerceIn(0, matches.size - 1)]
        pushNow(body.edit(st, findQ.length, replaceQ, body.activeAt(st, true)))
    }
    fun replaceAll() {
        if (matches.isEmpty()) return
        var r = body
        for (st in matches.sortedDescending()) r = r.edit(st, findQ.length, replaceQ, r.activeAt(st, true), TextRange(0))
        pushNow(r)
    }

    // Condivisione ed esportazione (nessun accesso alla rete: scegli tu dove salvare o con chi condividere)
    fun fileName(ext: String) = title.text.trim().ifBlank { com.notium.app.data.AutoTitle.from(md()) }.ifBlank { I18n.tr("Nota", settings.language) }.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(60) + ext
    fun asMarkdown(): String = (if (title.text.isNotBlank()) "# " + title.text.trim() + "\n\n" else "") + md()
    val mdLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        vm.pickerActive = false
        if (uri != null) scope.launch(Dispatchers.IO) {
            val ok = runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(asMarkdown().toByteArray(Charsets.UTF_8)) } }.isSuccess
            vm.notify(if (ok) "Nota esportata." else "Esportazione non riuscita.")
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        vm.pickerActive = false
        if (uri != null) scope.launch(Dispatchers.IO) {
            val ok = runCatching { context.contentResolver.openOutputStream(uri)?.use { PdfExport.write(it, title.text, md()) } }.isSuccess
            vm.notify(if (ok) "PDF creato." else "Creazione del PDF non riuscita.")
        }
    }
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { vm.pickerActive = false }
    val exporter = rememberExporter(vm)
    var exportSub by remember { mutableStateOf(false) }
    fun exportNow(f: ExportFormat) {
        scope.launch {
            val folderName = folders.firstOrNull { it.id == e.folderId }?.name
            val tagNames = tags.filter { it.id in e.tagIds }.map { it.name }
            exporter(listOf(ExportNote(title.text, md(), folderName, tagNames, e.favorite, e.pinned, e.archived, null, System.currentTimeMillis())), f)
        }
    }

    val family = settings.editorFont.family()
    val bodySize = settings.editorFontSize.sp
    val bodyLine = (settings.editorFontSize * settings.lineSpacing).sp
    val folder = folders.firstOrNull { it.id == e.folderId }
    val noteTags = tags.filter { it.id in e.tagIds }
    val readOnly = e.deleted

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.imePadding(),
        topBar = {
            Column {
            TopAppBar(
                navigationIcon = { IconButton(onClick = vm::closeEditor) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Indietro") } },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (e.status == SaveStatus.Saved) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        Text(
                            (if (e.status == SaveStatus.Saved) " " else "") + if (readOnly) "Nel cestino" else e.status.label,
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (!readOnly) {
                        IconButton(onClick = vm::editorToggleFavorite) {
                            Icon(if (e.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, "Preferita",
                                tint = if (e.favorite) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = vm::editorTogglePinned) {
                            Icon(Icons.Outlined.PushPin, "Fissa",
                                tint = if (e.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Altro") }
                        DropdownMenu(menu, { menu = false; exportSub = false }) {
                          if (exportSub) {
                            ExportSubMenu(onBack = { exportSub = false }, onPick = { f -> menu = false; exportSub = false; exportNow(f) })
                          } else {
                            DropdownMenuItem(text = { Text("Copia testo") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                                onClick = {
                                    menu = false
                                    SecureClipboard.copy(context, body.text, settings.clearClipboard)
                                    vm.notify(if (settings.clearClipboard) "Copiato. Gli appunti si svuotano dopo 60 secondi." else "Copiato.")
                                })
                            DropdownMenuItem(text = { Text("Cerca nella nota") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                                onClick = { menu = false; findOpen = true })
                            DropdownMenuItem(text = { Text("Condividi testo") }, leadingIcon = { Icon(Icons.Outlined.Share, null) },
                                onClick = {
                                    menu = false
                                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, title.text)
                                        putExtra(android.content.Intent.EXTRA_TEXT, (if (title.text.isNotBlank()) title.text.trim() + "\n\n" else "") + body.text)
                                    }
                                    vm.pickerActive = true
                                    shareLauncher.launch(android.content.Intent.createChooser(send, I18n.tr("Condividi nota", settings.language)))
                                })
                            DropdownMenuItem(text = { Text("Esporta come Markdown (.md)") }, leadingIcon = { Icon(Icons.Outlined.Description, null) },
                                onClick = { menu = false; vm.pickerActive = true; mdLauncher.launch(fileName(".md")) })
                            if (!readOnly) {
                                DropdownMenuItem(text = { Text("Sposta in cartella") }, leadingIcon = { Icon(Icons.Outlined.DriveFileMove, null) },
                                    onClick = { menu = false; folderDialog = true })
                                DropdownMenuItem(text = { Text("Gestisci tag") }, leadingIcon = { Icon(Icons.Outlined.Label, null) },
                                    onClick = { menu = false; tagDialog = true })
                                DropdownMenuItem(text = { Text(if (e.archived) "Ripristina dall'archivio" else "Archivia") },
                                    leadingIcon = { Icon(if (e.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, null) },
                                    onClick = { menu = false; vm.editorToggleArchived() })
                                DropdownMenuItem(text = { Text("Sposta nel cestino") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                    onClick = { menu = false; vm.editorTrash() })
                            }
                            ExportEntry { exportSub = true }
                          }
                        }
                    }
                },
            )
            if (findOpen) FindBar(
                findQ, { findQ = it; findIndex = 0 }, replaceQ, { replaceQ = it }, matches.size, findIndex,
                onPrev = { goTo(findIndex - 1) }, onNext = { goTo(findIndex + 1) },
                onReplaceOne = ::replaceOne, onReplaceAll = ::replaceAll,
                onClose = { findOpen = false; findQ = "" }, readOnly = readOnly,
            )
            }
        },
        bottomBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (!readOnly && settings.showToolbar) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
                        ToolButton(Icons.AutoMirrored.Outlined.Undo, "Annulla", enabled = undo.isNotEmpty()) { undoNow() }
                        ToolButton(Icons.AutoMirrored.Outlined.Redo, "Ripeti", enabled = redo.isNotEmpty()) { redoNow() }
                        Box {
                            ToolButton(Icons.Outlined.TextFields, "Stile del testo") { styleMenu = true }
                            DropdownMenu(styleMenu, { styleMenu = false }) {
                                DropdownMenuItem(text = { Text("Testo normale") }, onClick = { styleMenu = false; pushNow(body.heading(0)) })
                                DropdownMenuItem(text = { Text("Titolo 1", fontWeight = FontWeight.Bold, fontSize = 22.sp) }, onClick = { styleMenu = false; pushNow(body.heading(1)) })
                                DropdownMenuItem(text = { Text("Titolo 2", fontWeight = FontWeight.Bold, fontSize = 19.sp) }, onClick = { styleMenu = false; pushNow(body.heading(2)) })
                                DropdownMenuItem(text = { Text("Titolo 3", fontWeight = FontWeight.Bold, fontSize = 16.sp) }, onClick = { styleMenu = false; pushNow(body.heading(3)) })
                            }
                        }
                        val act = body.activeKinds(pending)
                        fun fmt(k: Fmt) {
                            if (body.tf.selection.collapsed) pending = if (k in act) act - k else act + k
                            else { body = body.toggle(k); vm.onEdit(title.text, md()) }
                        }
                        ToolButton(Icons.Outlined.FormatBold, "Grassetto", active = Fmt.BOLD in act) { fmt(Fmt.BOLD) }
                        ToolButton(Icons.Outlined.FormatItalic, "Corsivo", active = Fmt.ITALIC in act) { fmt(Fmt.ITALIC) }
                        ToolButton(Icons.Outlined.FormatStrikethrough, "Barrato", active = Fmt.STRIKE in act) { fmt(Fmt.STRIKE) }
                        ToolButton(Icons.Outlined.Highlight, "Evidenzia", active = Fmt.MARK in act) { fmt(Fmt.MARK) }
                        ToolButton(Icons.Outlined.Code, "Codice", active = Fmt.CODE in act) { fmt(Fmt.CODE) }
                        ToolButton(Icons.Outlined.FormatListBulleted, "Elenco puntato") { pushNow(body.bullets()) }
                        ToolButton(Icons.Outlined.FormatListNumbered, "Elenco numerato") { pushNow(body.numbered()) }
                        ToolButton(Icons.Outlined.Checklist, "Lista di controllo") { pushNow(body.checklist()) }
                        ToolButton(Icons.Outlined.CheckBox, "Spunta la casella") { pushNow(body.toggleCheck()) }
                        ToolButton(Icons.Outlined.FormatQuote, "Citazione") { pushNow(body.quote()) }
                        ToolButton(Icons.Outlined.FormatIndentIncrease, "Aumenta il rientro") { pushNow(body.indent(1)) }
                        ToolButton(Icons.Outlined.FormatIndentDecrease, "Riduci il rientro") { pushNow(body.indent(-1)) }
                        Box {
                            ToolButton(Icons.Outlined.AddCircleOutline, "Inserisci") { insertMenu = true }
                            DropdownMenu(insertMenu, { insertMenu = false }) {
                                DropdownMenuItem(text = { Text("Data e ora") }, leadingIcon = { Icon(Icons.Outlined.CalendarToday, null) },
                                    onClick = { insertMenu = false; pushNow(body.insertPlain(nowText())) })
                                DropdownMenuItem(text = { Text("Linea separatrice") }, leadingIcon = { Icon(Icons.Outlined.HorizontalRule, null) },
                                    onClick = { insertMenu = false; pushNow(body.insertDivider()) })
                                HorizontalDivider()
                                Text("MODELLI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                                NoteTemplates.all.forEach { (name, tpl) ->
                                    DropdownMenuItem(text = { Text(name) }, onClick = { insertMenu = false; pushNow(body.insertMarkdown(NoteTemplates.render(name, tpl, lang))) })
                                }
                            }
                        }
                    }
                }
                if (settings.showCounters) {
                    Text(
                        "${remember(body.text) { wordCount(body.text) }} parole · ${body.text.length} caratteri",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        },
    ) { padding ->
        var viewportPx by remember { mutableStateOf(0) }
        val density = androidx.compose.ui.platform.LocalDensity.current
        Column(Modifier.padding(padding).fillMaxSize().onSizeChanged { viewportPx = it.height }.verticalScroll(scroll).padding(horizontal = settings.editorMargin.dp)) {
            BasicTextField(
                value = title, onValueChange = { push(t = it) }, readOnly = readOnly, keyboardOptions = kbOpts,
                textStyle = TextStyle(
                    fontSize = settings.titleSize.sp, fontWeight = if (settings.editorTitleBold) FontWeight.Bold else FontWeight.Normal, fontFamily = family, color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box {
                        if (title.text.isEmpty()) Text("Titolo", fontSize = settings.titleSize.sp, fontWeight = if (settings.editorTitleBold) FontWeight.Bold else FontWeight.Normal, fontFamily = family,
                            color = MaterialTheme.colorScheme.outline)
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            // Cartella e tag
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { if (!readOnly) folderDialog = true },
                    label = { Text(folder?.name ?: "Nessuna cartella") },
                    leadingIcon = { Icon(Icons.Outlined.Folder, null, Modifier.size(18.dp)) },
                )
                noteTags.forEach { t ->
                    AssistChip(
                        onClick = { if (!readOnly) tagDialog = true }, label = { Text(t.name) },
                        leadingIcon = { ColorDot(t.color, 10.dp) },
                    )
                }
                if (!readOnly) AssistChip(onClick = { tagDialog = true }, label = { Text("+ Tag") })
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            run {
                val dim = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                val codeBg = MaterialTheme.colorScheme.surfaceVariant
                val styler = remember(body.spans, findQ, dim, codeBg, settings.editorFontSize) {
                    RichStyler(body.spans, settings.editorFontSize.toFloat(), dim, codeBg, Color(0x66FFD54F), findQ, Color(0x99FFB300))
                }
                BasicTextField(
                    value = body.tf,
                    onValueChange = valueChange@{ nv ->
                        val oldText = body.text
                        // Doppio tocco = parola (gestito dal campo); terzo tocco ravvicinato = intera frase.
                        val now = android.os.SystemClock.uptimeMillis()
                        val sel = nv.selection
                        val word = lastWord
                        var tripled = false
                        if (nv.text == oldText && word != null && now - lastWordAt < 600 && sel != word) {
                            val inside = if (sel.collapsed) sel.start in word.min..word.max else sel.min <= word.min && sel.max >= word.max
                            if (inside) {
                                val range = RichOps.sentenceAround(oldText, word.min)
                                body = body.copy(tf = nv.copy(selection = range)); pending = null
                                lastWord = null; tripled = true
                            }
                        }
                        if (!tripled && nv.text == oldText && !sel.collapsed && sel.length in 1..40) {
                            val piece = oldText.substring(sel.min, sel.max)
                            if (piece.none { it.isWhitespace() }) { lastWord = sel; lastWordAt = now }
                        }
                        if (tripled) return@valueChange
                        val tapped = if (!readOnly && nv.text == oldText && nv.selection.collapsed && nv.selection != body.tf.selection)
                            body.toggleCheckAt(nv.selection.start) else null
                        if (tapped != null) pushNow(tapped)
                        else {
                            var r = body.fromKeyboard(nv, pending)
                            if (settings.autoList) r = r.afterEnter(oldText.length).afterDelete(oldText)
                            push(b = r)
                        }
                    },
                    visualTransformation = styler,
                    onTextLayout = { layoutRes = it },
                    readOnly = readOnly, keyboardOptions = kbOpts,
                    textStyle = TextStyle(fontSize = bodySize, lineHeight = bodyLine, letterSpacing = settings.editorLetterSpacing.sp, fontFamily = family, color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        Box(Modifier.padding(vertical = 16.dp)) {
                            if (body.text.isEmpty()) Text("Scrivi qui…", fontSize = bodySize, fontFamily = family, color = MaterialTheme.colorScheme.outline)
                            inner()
                        }
                    },
                    // L'area di testo riempie tutto lo spazio sotto il titolo: un tocco ovunque porta il cursore li' (in fondo se sotto l'ultima riga).
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = with(density) { (viewportPx - bodyTopPx).coerceAtLeast(0f).toDp() })
                        .padding(bottom = 80.dp).onGloballyPositioned { bodyTopPx = it.positionInParent().y },
                )
            }
        }
    }

    if (folderDialog) {
        FolderPickerDialog("Cartella della nota", folders, rootLabel = "Nessuna cartella",
            onCreate = { name -> vm.createFolderForEditor(name); folderDialog = false },
            onDismiss = { folderDialog = false }, onPick = { vm.editorSetFolder(it); folderDialog = false })
    }
    if (tagDialog) {
        var newTag by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { tagDialog = false }, title = { Text("Tag") },
            text = {
                Column {
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { t ->
                            FilterChip(selected = t.id in e.tagIds, onClick = { vm.editorToggleTag(t.id) },
                                label = { Text(t.name) }, leadingIcon = { ColorDot(t.color, 10.dp) },
                                modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }
                    androidx.compose.material3.OutlinedTextField(
                        newTag, { newTag = it }, label = { Text("Nuovo tag") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        trailingIcon = {
                            TextButton(onClick = { vm.createTagForEditor(newTag); newTag = "" }, enabled = newTag.isNotBlank()) { Text("Aggiungi") }
                        }
                    )
                }
            },
            confirmButton = { TextButton(onClick = { tagDialog = false }) { Text("Fatto") } },
        )
    }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, enabled: Boolean = true, active: Boolean = false, onClick: () -> Unit) {
    IconButton(
        onClick = onClick, enabled = enabled,
        colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
            containerColor = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) { Icon(icon, desc) }
}

@Composable
private fun FindBar(
    query: String, onQuery: (String) -> Unit, replace: String, onReplace: (String) -> Unit, count: Int, index: Int,
    onPrev: () -> Unit, onNext: () -> Unit, onReplaceOne: () -> Unit, onReplaceAll: () -> Unit, onClose: () -> Unit, readOnly: Boolean,
) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.OutlinedTextField(
                query, onQuery, singleLine = true, label = { Text("Cerca nella nota") }, modifier = Modifier.weight(1f),
            )
            Text(
                if (query.isEmpty()) "" else if (count == 0) "0" else "${index + 1}/$count",
                style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp),
            )
            IconButton(onClick = onPrev, enabled = count > 0) { Icon(Icons.Outlined.KeyboardArrowUp, "Precedente") }
            IconButton(onClick = onNext, enabled = count > 0) { Icon(Icons.Outlined.KeyboardArrowDown, "Successivo") }
            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Chiudi la ricerca") }
        }
        if (!readOnly) Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.OutlinedTextField(
                replace, onReplace, singleLine = true, label = { Text("Sostituisci con") }, modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onReplaceOne, enabled = count > 0) { Text("Sostituisci") }
            TextButton(onClick = onReplaceAll, enabled = count > 0) { Text("Tutte") }
        }
    }
}
