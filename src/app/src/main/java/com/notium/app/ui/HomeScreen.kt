package com.notium.app.ui

import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.grid.LazyGridItemScope

import androidx.compose.animation.AnimatedVisibility
import com.notium.app.data.SwipeAction
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import com.notium.app.data.DateStyle
import com.notium.app.data.FabPlace
import com.notium.app.data.FabStyle
import com.notium.app.data.Settings
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notium.app.AppViewModel
import com.notium.app.Route
import com.notium.app.data.CardStyle
import com.notium.app.data.ListStyle
import com.notium.app.data.NoteSummary
import com.notium.app.data.Section
import com.notium.app.data.SortOrder
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppViewModel) {
    val filter by vm.filter.collectAsState()
    val notes by vm.notes.collectAsState()
    val folders by vm.folders.collectAsState()
    val tags by vm.tags.collectAsState()
    val settings by vm.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val exporter = rememberExporter(vm)
    val onExport: (NoteSummary, ExportFormat) -> Unit = { n, f -> scope.launch { vm.noteForExport(n.id)?.let { exporter(listOf(it), f) } } }
    var selection by remember { mutableStateOf(setOf<Long>()) }
    val selecting = selection.isNotEmpty()
    val selectedNotes = notes.filter { it.id in selection }
    val importBanner by vm.importBanner.collectAsState()
    BackHandler(enabled = selecting) { selection = emptySet() }

    var moveTarget by remember { mutableStateOf<NoteSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<NoteSummary?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }

    val folderById = remember(folders) { folders.associateBy { it.id } }
    val anim = LocalAnim.current
    val sec = filter.section
    val sectionTitle: String? = when {
        sec == Section.Favorites -> "Preferiti"
        sec == Section.Pinned -> "Fissate"
        sec == Section.Archive -> "Archivio"
        sec == Section.Trash -> "Cestino"
        sec is Section.InFolder -> folderById[sec.id]?.name ?: "Cartella"
        sec is Section.WithTag -> "#" + (tags.firstOrNull { it.id == sec.id }?.name ?: "tag")
        else -> null
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp

        val drawerBody: @Composable (closeDrawer: () -> Unit) -> Unit = { close ->
            DrawerBody(vm, filter.section, close)
        }

        val content: @Composable () -> Unit = {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                floatingActionButtonPosition = when (settings.fabPlace) { FabPlace.CENTER -> FabPosition.Center; FabPlace.START -> FabPosition.Start; else -> FabPosition.End },
                topBar = {
                    if (selecting) SelectionBar(
                        count = selectedNotes.size, total = notes.size,
                        allFavorite = selectedNotes.isNotEmpty() && selectedNotes.all { it.favorite },
                        allArchived = selectedNotes.isNotEmpty() && selectedNotes.all { it.archived },
                        onClose = { selection = emptySet() },
                        onAll = { selection = if (selection.size >= notes.size) emptySet() else notes.map { it.id }.toSet() },
                        onFavorite = { vm.favoriteMany(selectedNotes); selection = emptySet() },
                        onArchive = { vm.archiveMany(selectedNotes); selection = emptySet() },
                        onTrash = { vm.trashMany(selectedNotes); selection = emptySet() },
                        onExport = { f ->
                            val ids = selectedNotes.map { it.id }
                            selection = emptySet()
                            scope.launch { exporter(ids.mapNotNull { vm.noteForExport(it) }, f) }
                        },
                    ) else TopAppBar(
                        title = {
                            Crossfade(targetState = sectionTitle, animationSpec = tween(anim.ms(220)), label = "title") { t ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (t == null) {
                                        if (settings.showTopLogo) NotiumLogo(30.dp)
                                        Text("  " + settings.homeTitle.ifBlank { "Notium" }, fontWeight = FontWeight.Bold)
                                    } else Text(t, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        },
                        navigationIcon = {
                            if (!wide) IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Outlined.Menu, "Menu") }
                        },
                        actions = {
                            var sortOpen by remember { mutableStateOf(false) }
                            IconButton(onClick = { sortOpen = true }) { Icon(Icons.Outlined.Sort, "Ordina") }
                            DropdownMenu(sortOpen, { sortOpen = false }) {
                                SortOrder.entries.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.label) },
                                        leadingIcon = { if (filter.sort == s) Icon(Icons.Outlined.Check, null) else Spacer(Modifier.size(24.dp)) },
                                        onClick = { vm.setSort(s); sortOpen = false }
                                    )
                                }
                            }
                        },
                    )
                },
                floatingActionButton = {
                    AnimatedVisibility(
                        visible = filter.section != Section.Trash && !selecting,
                        enter = if (anim.enabled) scaleIn(spring(dampingRatio = 0.6f, stiffness = 380f)) + fadeIn(anim.smooth(220)) else EnterTransition.None,
                        exit = if (anim.enabled) scaleOut(anim.smooth(180)) + fadeOut(anim.smooth(180)) else ExitTransition.None,
                    ) {
                        when (settings.fabStyle) {
                            FabStyle.EXTENDED -> ExtendedFloatingActionButton(
                                onClick = vm::newNote,
                                icon = { Icon(Icons.Outlined.Add, null) },
                                text = { Text("Nuova nota") },
                            )
                            FabStyle.ICON -> FloatingActionButton(onClick = vm::newNote) { Icon(Icons.Outlined.Add, "Nuova nota") }
                            FabStyle.HIDDEN -> Unit
                        }
                    }
                },
            ) { padding ->
                Column(Modifier.padding(padding).fillMaxSize()) {
                    importBanner?.let { b ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp).fillMaxWidth()
                        ) {
                            Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
                                Box(Modifier.padding(top = 1.dp).size(24.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                }
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text("Le tue note sono qui", fontWeight = FontWeight.Medium)
                                    Text(
                                        "Importate ${b.notes} note, ${b.folders} cartelle e ${b.tags} tag dal backup.",
                                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                IconButton(onClick = vm::dismissImportBanner) { Icon(Icons.Outlined.Close, "Chiudi") }
                            }
                        }
                    }
                    if (settings.showSearch) OutlinedTextField(
                        value = filter.query, onValueChange = vm::setQuery, singleLine = true,
                        placeholder = { Text("Cerca in note, tag e cartelle") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        trailingIcon = {
                            if (filter.query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Outlined.Close, "Cancella") }
                        },
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = settings.screenPadding.dp, vertical = 4.dp),
                    )
                    if (filter.section == Section.Trash) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = settings.screenPadding.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (settings.trashDays > 0) "Eliminate dopo ${settings.trashDays} giorni" else "Nessuna eliminazione automatica",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (notes.isNotEmpty()) TextButton(onClick = { confirmEmpty = true }) { Text("Svuota cestino") }
                        }
                    }

                    if (notes.isEmpty()) {
                        EmptyState(filter.query.isNotBlank(), filter.section)
                    } else {
                        val pinnedNotes = remember(notes) { notes.filter { it.pinned } }
                        val otherNotes = remember(notes) { notes.filter { !it.pinned } }
                        val grouped = filter.section == Section.All && filter.query.isBlank() && pinnedNotes.isNotEmpty()
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(settings.gridColumns.coerceIn(1, 3)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = (settings.screenPadding + 6).dp, end = (settings.screenPadding + 6).dp, top = 8.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy((settings.cardGap * 0.6f).dp),
                            horizontalArrangement = Arrangement.spacedBy(settings.cardGap.dp),
                        ) {
                            if (grouped) {
                                item(key = "h-pinned", span = { GridItemSpan(maxLineSpan) }) { GroupHeader("Fissate") }
                                items(pinnedNotes, key = { it.id }) { n ->
                                    NoteRow(vm, n, folderById[n.folderId]?.name, filter.query, settings,
                                        onMove = { moveTarget = it }, onDelete = { deleteTarget = it },
                                        showPin = filter.section == Section.All, onExport = onExport,
                                        selecting = selecting, selected = n.id in selection,
                                        onSelect = { selection = if (n.id in selection) selection - n.id else selection + n.id },
                                        modifier = noteItemModifier(anim))
                                }
                                item(key = "h-others", span = { GridItemSpan(maxLineSpan) }) { GroupHeader("Altre note") }
                                items(otherNotes, key = { it.id }) { n ->
                                    NoteRow(vm, n, folderById[n.folderId]?.name, filter.query, settings,
                                        onMove = { moveTarget = it }, onDelete = { deleteTarget = it },
                                        showPin = filter.section == Section.All, onExport = onExport,
                                        selecting = selecting, selected = n.id in selection,
                                        onSelect = { selection = if (n.id in selection) selection - n.id else selection + n.id },
                                        modifier = noteItemModifier(anim))
                                }
                            } else {
                                items(notes, key = { it.id }) { n ->
                                    NoteRow(vm, n, folderById[n.folderId]?.name, filter.query, settings,
                                        onMove = { moveTarget = it }, onDelete = { deleteTarget = it },
                                        showPin = filter.section == Section.All, onExport = onExport,
                                        selecting = selecting, selected = n.id in selection,
                                        onSelect = { selection = if (n.id in selection) selection - n.id else selection + n.id },
                                        modifier = noteItemModifier(anim))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (wide) {
            PermanentNavigationDrawer(
                drawerContent = { PermanentDrawerSheet(Modifier.width(300.dp)) { drawerBody {} } },
                content = content,
            )
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = { ModalDrawerSheet { drawerBody { scope.launch { drawerState.close() } } } },
                content = content,
            )
        }
    }

    moveTarget?.let { n ->
        FolderPickerDialog("Sposta in cartella", folders,
            onCreate = { name -> vm.createFolderAndMove(n, name); moveTarget = null },
            onDismiss = { moveTarget = null }, onPick = { vm.moveNote(n, it); moveTarget = null })
    }
    deleteTarget?.let { n ->
        ConfirmDialog(
            "Eliminare definitivamente?", "La nota verrà rimossa dal database e non potrà essere recuperata.",
            "Elimina", danger = true, onDismiss = { deleteTarget = null }, onConfirm = { vm.deleteForever(n); deleteTarget = null }
        )
    }
    if (confirmEmpty) {
        ConfirmDialog(
            "Svuotare il cestino?", "Tutte le note nel cestino verranno eliminate definitivamente.",
            "Svuota", danger = true, onDismiss = { confirmEmpty = false }, onConfirm = { vm.emptyTrash(); confirmEmpty = false }
        )
    }
}

@Composable
private fun DrawerBody(vm: AppViewModel, selected: Section, close: () -> Unit) {
    val folders by vm.folders.collectAsState()
    val tags by vm.tags.collectAsState()
    val st by vm.settings.collectAsState()
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 16.dp)) {
        Row(Modifier.padding(start = 12.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            NotiumLogo(36.dp)
            Column(Modifier.padding(start = 12.dp)) {
                Text("Notium", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Your notes. Your privacy.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        @Composable
        fun item(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, section: Section, indent: Int = 0) {
            NavigationDrawerItem(
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }, icon = { Icon(icon, null) }, selected = selected == section,
                onClick = { vm.setSection(section); close() },
                modifier = Modifier.padding(start = (indent * 16).dp),
            )
        }
        item("Tutte le note", Icons.Outlined.Notes, Section.All)
        item("Preferiti", Icons.Outlined.Star, Section.Favorites)
        item("Fissate", Icons.Outlined.PushPin, Section.Pinned)
        item("Archivio", Icons.Outlined.Archive, Section.Archive)
        item("Cestino", Icons.Outlined.Delete, Section.Trash)

        if (st.drawerFolders) {
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            DrawerHeader("Cartelle") { close(); vm.go(Route.Folders) }
            if (folders.isEmpty()) Text("Nessuna cartella", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, bottom = 4.dp))
            flattenFolders(folders).forEach { (f, depth) -> item(f.name, Icons.Outlined.Folder, Section.InFolder(f.id), depth) }
        }
        if (st.drawerTags) {
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            DrawerHeader("Tag") { close(); vm.go(Route.Tags) }
            if (tags.isEmpty()) Text("Nessun tag", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, bottom = 4.dp))
            tags.forEach { t -> item(t.name, Icons.Outlined.Label, Section.WithTag(t.id)) }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        NavigationDrawerItem(label = { Text("Impostazioni") }, icon = { Icon(Icons.Outlined.Settings, null) }, selected = false,
            onClick = { close(); vm.go(Route.Settings) })
    }
}

@Composable
private fun DrawerHeader(title: String, onManage: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        TextButton(onClick = onManage) { Text("Gestisci") }
    }
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp)
    )
}

@Composable
private fun EmptyState(searching: Boolean, section: Section) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        NotiumLogo(64.dp)
        Text(
            when {
                searching -> "Nessun risultato"
                section == Section.Trash -> "Il cestino è vuoto"
                section == Section.Archive -> "Nessuna nota archiviata"
                section == Section.Favorites -> "Nessun preferito"
                section == Section.Pinned -> "Nessuna nota fissata"
                else -> "Nessuna nota"
            },
            style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)
        )
        if (!searching && section == Section.All) {
            Text("Tocca “Nuova nota” per iniziare. Tutto resta cifrato su questo dispositivo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

private val mdNoise = Regex("(?m)^\\s{0,3}(#{1,6}\\s+|[-*+]\\s+|>\\s*)|\\*\\*|\\*|`|\\[([^\\]]*)\\]\\([^)]*\\)")

private val lineBreaks = Regex("\\s*\\n+\\s*")

private fun plainPreview(s: String): String =
    s.replace(mdNoise) { it.groupValues.getOrNull(2)?.takeIf { g -> g.isNotEmpty() } ?: "" }
        .replace(lineBreaks, " ").trim()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionBar(
    count: Int, total: Int, allFavorite: Boolean, allArchived: Boolean,
    onClose: () -> Unit, onAll: () -> Unit, onFavorite: () -> Unit, onArchive: () -> Unit, onTrash: () -> Unit,
    onExport: (ExportFormat) -> Unit,
) {
    var exportMenu by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(if (count == 1) "1 selezionata" else "$count selezionate", fontWeight = FontWeight.Bold, maxLines = 1) },
        navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Annulla selezione") } },
        actions = {
            IconButton(onClick = onAll) { Icon(Icons.Outlined.SelectAll, if (count >= total) "Deseleziona tutto" else "Seleziona tutto") }
            IconButton(onClick = onFavorite) {
                Icon(if (allFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder, if (allFavorite) "Rimuovi dai preferiti" else "Aggiungi ai preferiti")
            }
            IconButton(onClick = onArchive) {
                Icon(if (allArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, if (allArchived) "Ripristina dall'archivio" else "Archivia")
            }
            Box {
                IconButton(onClick = { exportMenu = true }) { Icon(Icons.Outlined.FileDownload, "Esporta") }
                DropdownMenu(exportMenu, { exportMenu = false }) {
                    DropdownMenuItem(text = { Text("Esporta in PDF") }, leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) }, onClick = { exportMenu = false; onExport(ExportFormat.PDF) })
                    DropdownMenuItem(text = { Text("Esporta in TXT") }, leadingIcon = { Icon(Icons.Outlined.Description, null) }, onClick = { exportMenu = false; onExport(ExportFormat.TXT) })
                    DropdownMenuItem(text = { Text("Esporta in JSON") }, leadingIcon = { Icon(Icons.Outlined.Code, null) }, onClick = { exportMenu = false; onExport(ExportFormat.JSON) })
                }
            }
            IconButton(onClick = onTrash) { Icon(Icons.Outlined.Delete, "Sposta nel cestino") }
        },
    )
}

/** Scheda di una nota con i gesti di scorrimento personalizzabili (Impostazioni > Elenco note). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteRow(
    vm: AppViewModel, n: NoteSummary, folderName: String?, query: String, s: Settings,
    onMove: (NoteSummary) -> Unit, onDelete: (NoteSummary) -> Unit, showPin: Boolean = false, onExport: (NoteSummary, ExportFormat) -> Unit = { _, _ -> },
    selecting: Boolean = false, selected: Boolean = false, onSelect: () -> Unit = {}, modifier: Modifier = Modifier,
) {
    val left = s.swipeLeft
    val right = s.swipeRight
    val enabled = !selecting && !n.deleted && (left != SwipeAction.NONE || right != SwipeAction.NONE)
    if (!enabled) {
        NoteCard(vm, n, folderName, query, s, onMove, onDelete, showPin, onExport, selecting, selected, onSelect, modifier)
        return
    }
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
        when (v) {
            SwipeToDismissBoxValue.StartToEnd -> vm.swipeAction(right, n)
            SwipeToDismissBoxValue.EndToStart -> vm.swipeAction(left, n)
            else -> {}
        }
        false // la scheda torna al suo posto: se la nota sparisce dall'elenco lo fa la lista
    })
    SwipeToDismissBox(
        state = state, modifier = modifier,
        enableDismissFromStartToEnd = right != SwipeAction.NONE,
        enableDismissFromEndToStart = left != SwipeAction.NONE,
        backgroundContent = {
            val dir = state.dismissDirection
            val a = if (dir == SwipeToDismissBoxValue.StartToEnd) right else left
            val danger = a == SwipeAction.TRASH
            val end = dir == SwipeToDismissBoxValue.EndToStart
            if (dir != SwipeToDismissBoxValue.Settled) Box(
                Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium)
                    .background(if (danger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (end) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                val tint = if (danger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (end) Text(a.label, color = tint, style = MaterialTheme.typography.labelLarge)
                    Icon(
                        when (a) {
                            SwipeAction.TRASH -> Icons.Outlined.Delete
                            SwipeAction.ARCHIVE -> if (n.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive
                            SwipeAction.FAVORITE -> Icons.Outlined.Star
                            SwipeAction.PIN -> Icons.Outlined.PushPin
                            else -> if (n.id in s.lockedNotes) Icons.Outlined.LockOpen else Icons.Outlined.Lock
                        }, null, tint = tint,
                    )
                    if (!end) Text(a.label, color = tint, style = MaterialTheme.typography.labelLarge)
                }
            }
        },
    ) {
        NoteCard(vm, n, folderName, query, s, onMove, onDelete, showPin, onExport, selecting, selected, onSelect, Modifier)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    vm: AppViewModel, n: NoteSummary, folderName: String?, query: String, s: Settings,
    onMove: (NoteSummary) -> Unit, onDelete: (NoteSummary) -> Unit, showPin: Boolean = false, onExport: (NoteSummary, ExportFormat) -> Unit = { _, _ -> },
    selecting: Boolean = false, selected: Boolean = false, onSelect: () -> Unit = {}, modifier: Modifier = Modifier,
) {
    val anim = LocalAnim.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && anim.enabled && anim.press) anim.pressScale else 1f,
        spring(dampingRatio = 0.55f, stiffness = 450f), label = "press"
    )
    val hl = MaterialTheme.colorScheme.primaryContainer
    val style = s.listStyle
    val cardStyle = s.cardStyle
    val showDates = s.showDates
    val showTags = s.showTags
    val compact = style == ListStyle.COMPACT
    var menu by remember { mutableStateOf(false) }
    var exportSub by remember { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else if (s.cardColor != 0) androidx.compose.ui.graphics.Color(s.cardColor)
        else if (cardStyle == CardStyle.FILLED) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = if (cardStyle == CardStyle.OUTLINED && s.borderWidth > 0) BorderStroke(s.borderWidth.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        modifier = modifier.fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .combinedClickable(
                interactionSource = src, indication = LocalIndication.current,
                onClick = { if (selecting) onSelect() else vm.requestOpen(n.id) },
                onLongClick = { if (!selecting && !n.deleted) onSelect() },
            ),
    ) {
        val pad = (if (compact) s.cardPadding * 0.4f else s.cardPadding * 0.6f).dp
        Column(Modifier.padding(start = pad + 2.dp, end = 4.dp, top = pad, bottom = pad)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val title = n.title.ifBlank { I18n.tr("Senza titolo", LocalLang.current) }
                Text(
                    remember(title, query, hl) { highlight(title, query, hl) }, style = MaterialTheme.typography.titleMedium, fontSize = s.listTitleSize.sp,
                    fontWeight = if (s.boldTitles) FontWeight.SemiBold else FontWeight.Normal,
                    fontStyle = if (n.title.isBlank()) FontStyle.Italic else FontStyle.Normal,
                    maxLines = s.titleLines.coerceIn(1, 3), overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                if (n.id in s.lockedNotes) Icon(Icons.Outlined.Lock, "Nota bloccata", Modifier.size(16.dp).padding(end = 2.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!selecting) {
                if (!n.deleted && showPin) {
                    IconButton(onClick = { vm.togglePinned(n) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (n.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            if (n.pinned) "Rimuovi da fissate" else "Fissa in alto", Modifier.size(20.dp),
                            tint = if (n.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!n.deleted) {
                    IconButton(onClick = { vm.toggleFavorite(n) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (n.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            if (n.favorite) "Rimuovi dai preferiti" else "Aggiungi ai preferiti", Modifier.size(20.dp),
                            tint = if (n.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.MoreVert, "Altro", Modifier.size(20.dp)) }
                    DropdownMenu(menu, { menu = false; exportSub = false }) {
                        if (exportSub) {
                            ExportSubMenu(onBack = { exportSub = false }, onPick = { f -> menu = false; exportSub = false; onExport(n, f) })
                        } else {
                        if (!n.deleted) DropdownMenuItem(text = { Text("Seleziona") }, leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) },
                            onClick = { menu = false; onSelect() })
                        if (n.deleted) {
                            DropdownMenuItem(text = { Text("Ripristina") }, leadingIcon = { Icon(Icons.Outlined.Restore, null) },
                                onClick = { menu = false; vm.restore(n) })
                            DropdownMenuItem(text = { Text("Elimina definitivamente", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; onDelete(n) })
                        } else {
                            DropdownMenuItem(text = { Text(if (n.pinned) "Rimuovi da fissate" else "Fissa in alto") },
                                leadingIcon = { Icon(Icons.Outlined.PushPin, null) }, onClick = { menu = false; vm.togglePinned(n) })
                            DropdownMenuItem(text = { Text(if (n.favorite) "Rimuovi dai preferiti" else "Aggiungi ai preferiti") },
                                leadingIcon = { Icon(if (n.favorite) Icons.Outlined.StarBorder else Icons.Outlined.Star, null) },
                                onClick = { menu = false; vm.toggleFavorite(n) })
                            DropdownMenuItem(text = { Text(if (n.archived) "Ripristina dall'archivio" else "Archivia") },
                                leadingIcon = { Icon(if (n.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, null) },
                                onClick = { menu = false; vm.toggleArchived(n) })
                            DropdownMenuItem(text = { Text(if (n.id in s.lockedNotes) "Sblocca nota" else "Blocca nota") },
                                leadingIcon = { Icon(if (n.id in s.lockedNotes) Icons.Outlined.LockOpen else Icons.Outlined.Lock, null) },
                                onClick = { menu = false; vm.toggleNoteLock(n.id) })
                            DropdownMenuItem(text = { Text("Sposta in cartella") }, leadingIcon = { Icon(Icons.Outlined.DriveFileMove, null) },
                                onClick = { menu = false; onMove(n) })
                            DropdownMenuItem(text = { Text("Sposta nel cestino", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; vm.trash(n) })
                        }
                        ExportEntry { exportSub = true }
                        }
                    }
                }
                }
                if (selecting) Checkbox(checked = selected, onCheckedChange = null, modifier = Modifier.padding(end = 12.dp))
            }
            Row(
                Modifier.padding(top = 2.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (folderName != null) {
                    Icon(Icons.Outlined.Folder, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(folderName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                if (!compact && showTags) n.tags.take(3).forEach { t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorDot(t.color, 8.dp)
                        Text(" " + t.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (showDates) Text(
                    formatDate(n.updatedAt, s.dateStyle),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDate(ms: Long, style: DateStyle): String {
    val d = Date(ms)
    val loc = java.util.Locale.getDefault()
    return when (style) {
        DateStyle.SHORT -> java.text.SimpleDateFormat("d MMM", loc).format(d)
        DateStyle.MEDIUM -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(d)
        DateStyle.LONG -> DateFormat.getDateInstance(DateFormat.LONG).format(d)
        DateStyle.WITH_TIME -> DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(d)
        DateStyle.RELATIVE -> {
            val diff = (System.currentTimeMillis() - ms).coerceAtLeast(0) / 60_000
            when {
                diff < 1 -> "adesso"
                diff < 60 -> "$diff min fa"
                diff < 60 * 24 -> "${diff / 60} h fa"
                diff < 60 * 48 -> "ieri"
                diff < 60 * 24 * 30 -> "${diff / (60 * 24)} giorni fa"
                else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(d)
            }
        }
    }
}

/** Movimento fluido delle schede quando la lista cambia (inserimenti, spostamenti, rimozioni). */
private fun LazyGridItemScope.noteItemModifier(anim: AnimConfig): Modifier =
    if (anim.enabled && anim.list) Modifier.animateItem(
        fadeInSpec = anim.smooth(320),
        placementSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        fadeOutSpec = anim.smooth(200),
    ) else Modifier
