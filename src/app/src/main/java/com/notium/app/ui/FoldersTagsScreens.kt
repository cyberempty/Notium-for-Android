package com.notium.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notium.app.AppViewModel
import com.notium.app.data.Folder
import com.notium.app.data.FolderDeleteMode
import com.notium.app.data.NotiumColors
import com.notium.app.data.Section
import com.notium.app.data.Tag

private sealed interface FolderDialog {
    data class Create(val parent: Long?) : FolderDialog
    data class Edit(val folder: Folder) : FolderDialog
    data class Move(val folder: Folder, val forbidden: Set<Long>) : FolderDialog
    data class Delete(val folder: Folder) : FolderDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(vm: AppViewModel) {
    val folders by vm.folders.collectAsState()
    var dialog by remember { mutableStateOf<FolderDialog?>(null) }
    var pending by remember { mutableStateOf<Folder?>(null) }

    // I divieti per lo spostamento (evitare cicli) vengono calcolati nel database.
    LaunchedEffect(pending) {
        pending?.let { dialog = FolderDialog.Move(it, vm.forbiddenParents(it)); pending = null }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Cartelle") },
                navigationIcon = { IconButton(onClick = vm::back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Indietro") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialog = FolderDialog.Create(null) }) { Icon(Icons.Outlined.CreateNewFolder, "Nuova cartella") }
        },
    ) { padding ->
        val flat = remember(folders) { flattenFolders(folders) }
        if (flat.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessuna cartella. Creane una con il pulsante in basso.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(32.dp))
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(flat, key = { it.first.id }) { (f, depth) ->
                    var menu by remember { mutableStateOf(false) }
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { vm.setSection(Section.InFolder(f.id)); vm.back() }
                            .padding(start = (16 + depth * 24).dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ColorDot(f.color, 16.dp)
                        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                            Text(f.name, style = MaterialTheme.typography.titleMedium)
                            Text("${f.noteCount} note", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Altro") }
                            DropdownMenu(menu, { menu = false }) {
                                DropdownMenuItem(text = { Text("Nuova sottocartella") }, leadingIcon = { Icon(Icons.Outlined.Add, null) },
                                    onClick = { menu = false; dialog = FolderDialog.Create(f.id) })
                                DropdownMenuItem(text = { Text("Rinomina e colore") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                    onClick = { menu = false; dialog = FolderDialog.Edit(f) })
                                DropdownMenuItem(text = { Text("Sposta") }, leadingIcon = { Icon(Icons.Outlined.DriveFileMove, null) },
                                    onClick = { menu = false; pending = f })
                                DropdownMenuItem(text = { Text("Elimina") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                    onClick = { menu = false; dialog = FolderDialog.Delete(f) })
                            }
                        }
                    }
                }
            }
        }
    }

    when (val d = dialog) {
        null -> Unit
        is FolderDialog.Create -> {
            var color by remember { mutableStateOf(NotiumColors[0]) }
            InputDialog(
                if (d.parent == null) "Nuova cartella" else "Nuova sottocartella", "Nome", confirm = "Crea",
                extra = { ColorPicker(color) { color = it } },
                onDismiss = { dialog = null }, onConfirm = { vm.createFolder(it, d.parent, color); dialog = null }
            )
        }
        is FolderDialog.Edit -> {
            var color by remember { mutableStateOf(d.folder.color) }
            InputDialog(
                "Modifica cartella", "Nome", d.folder.name,
                extra = { ColorPicker(color) { color = it } },
                onDismiss = { dialog = null }, onConfirm = { vm.updateFolder(d.folder, it, color); dialog = null }
            )
        }
        is FolderDialog.Move -> FolderPickerDialog(
            "Sposta “${d.folder.name}” in", folders, forbidden = d.forbidden,
            onDismiss = { dialog = null }, onPick = { vm.moveFolder(d.folder, it); dialog = null }
        )
        is FolderDialog.Delete -> DeleteFolderDialog(d.folder, folders, onDismiss = { dialog = null }) { mode ->
            vm.deleteFolder(d.folder, mode); dialog = null
        }
    }
}

@Composable
private fun DeleteFolderDialog(folder: Folder, all: List<Folder>, onDismiss: () -> Unit, onConfirm: (FolderDeleteMode) -> Unit) {
    var choice by remember { mutableStateOf(0) }
    var target by remember { mutableStateOf<Folder?>(null) }
    var pickOpen by remember { mutableStateOf(false) }
    val others = all.filter { it.id != folder.id }

    if (folder.noteCount == 0) {
        ConfirmDialog(
            "Eliminare la cartella?", "“${folder.name}” è vuota. Le sottocartelle salgono di un livello.",
            "Elimina", danger = true, onDismiss = onDismiss, onConfirm = { onConfirm(FolderDeleteMode.MoveNotesToRoot) }
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminare “${folder.name}”?") },
        text = {
            Column {
                Text("La cartella contiene ${folder.noteCount} note. Cosa vuoi fare?", modifier = Modifier.padding(bottom = 8.dp))
                listOf("Sposta le note nella cartella principale", "Sposta le note in un'altra cartella", "Elimina le note (vanno nel cestino)")
                    .forEachIndexed { i, label ->
                        Row(Modifier.fillMaxWidth().clickable { choice = i }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = choice == i, onClick = { choice = i }); Text(label)
                        }
                    }
                if (choice == 1) {
                    Box {
                        TextButton(onClick = { pickOpen = true }) { Text(target?.name ?: "Scegli la cartella…") }
                        DropdownMenu(pickOpen, { pickOpen = false }) {
                            others.forEach { f -> DropdownMenuItem(text = { Text(f.name) }, onClick = { target = f; pickOpen = false }) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = choice != 1 || target != null,
                onClick = {
                    onConfirm(
                        when (choice) {
                            0 -> FolderDeleteMode.MoveNotesToRoot
                            1 -> FolderDeleteMode.MoveNotesTo(target!!.id)
                            else -> FolderDeleteMode.DeleteNotes
                        }
                    )
                }
            ) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

private sealed interface TagDialog {
    data object Create : TagDialog
    data class Edit(val tag: Tag) : TagDialog
    data class Delete(val tag: Tag) : TagDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(vm: AppViewModel) {
    val tags by vm.tags.collectAsState()
    var dialog by remember { mutableStateOf<TagDialog?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Tag") },
                navigationIcon = { IconButton(onClick = vm::back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Indietro") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialog = TagDialog.Create }) { Icon(Icons.Outlined.Add, "Nuovo tag") }
        },
    ) { padding ->
        if (tags.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessun tag. I tag organizzano le note in modo trasversale alle cartelle.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(32.dp))
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(tags, key = { it.id }) { t ->
                    var menu by remember { mutableStateOf(false) }
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.setSection(Section.WithTag(t.id)); vm.back() }
                            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ColorDot(t.color, 16.dp)
                        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                            Text("#" + t.name, style = MaterialTheme.typography.titleMedium)
                            Text("${t.noteCount} note", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Altro") }
                            DropdownMenu(menu, { menu = false }) {
                                DropdownMenuItem(text = { Text("Rinomina e colore") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                    onClick = { menu = false; dialog = TagDialog.Edit(t) })
                                DropdownMenuItem(text = { Text("Elimina") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                    onClick = { menu = false; dialog = TagDialog.Delete(t) })
                            }
                        }
                    }
                }
            }
        }
    }

    when (val d = dialog) {
        null -> Unit
        TagDialog.Create -> {
            var color by remember { mutableStateOf(NotiumColors[(tags.size) % NotiumColors.size]) }
            InputDialog("Nuovo tag", "Nome", confirm = "Crea", extra = { ColorPicker(color) { color = it } },
                onDismiss = { dialog = null }, onConfirm = { vm.createTag(it, color); dialog = null })
        }
        is TagDialog.Edit -> {
            var color by remember { mutableStateOf(d.tag.color) }
            InputDialog("Modifica tag", "Nome", d.tag.name, extra = { ColorPicker(color) { color = it } },
                onDismiss = { dialog = null }, onConfirm = { vm.updateTag(d.tag, it, color); dialog = null })
        }
        is TagDialog.Delete -> ConfirmDialog(
            "Eliminare il tag?", "“${d.tag.name}” verrà rimosso da tutte le note. Le note non vengono eliminate.",
            "Elimina", danger = true, onDismiss = { dialog = null }, onConfirm = { vm.deleteTag(d.tag); dialog = null }
        )
    }
}
