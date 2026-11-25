package com.notium.app.data

data class Tag(val id: Long, val name: String, val color: Int, val noteCount: Int = 0)

data class Folder(val id: Long, val name: String, val parentId: Long?, val color: Int, val noteCount: Int = 0)

/** Nota completa (editor). */
data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val folderId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val favorite: Boolean,
    val pinned: Boolean,
    val archived: Boolean,
    val deleted: Boolean,
    val tagIds: Set<Long>,
)

/** Nota per gli elenchi: contenuto ridotto a un'anteprima per risparmiare memoria. */
data class NoteSummary(
    val id: Long,
    val title: String,
    val snippet: String,
    val folderId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val favorite: Boolean,
    val pinned: Boolean,
    val archived: Boolean,
    val deleted: Boolean,
    val tags: List<Tag>,
)

sealed interface Section {
    data object All : Section
    data object Favorites : Section
    data object Pinned : Section
    data object Archive : Section
    data object Trash : Section
    data class InFolder(val id: Long) : Section
    data class WithTag(val id: Long) : Section
}

enum class SortOrder(val label: String) {
    MODIFIED("Ultima modifica"),
    CREATED("Data di creazione"),
    TITLE("Titolo"),
    FAVORITES("Preferiti"),
    PINNED("Note fissate"),
}

data class NoteFilter(
    val section: Section = Section.All,
    val query: String = "",
    val sort: SortOrder = SortOrder.MODIFIED,
)

sealed interface FolderDeleteMode {
    data object MoveNotesToRoot : FolderDeleteMode
    data class MoveNotesTo(val folderId: Long) : FolderDeleteMode
    data object DeleteNotes : FolderDeleteMode
}

/** Palette per cartelle e tag (ARGB). */
val NotiumColors: List<Int> = listOf(
    0xFFF2F2F2.toInt(), 0xFFBDBDBD.toInt(), 0xFF8E8E8E.toInt(), 0xFF5F5F5F.toInt(),
    0xFF7FA6D9.toInt(), 0xFF7FC8A9.toInt(), 0xFFD98C7F.toInt(), 0xFFD9B97F.toInt(),
    0xFFA68FD9.toInt(), 0xFFD98FB8.toInt(),
)

// ---- Formato di backup (in memoria) ----

data class BackupNote(
    val id: Long, val title: String, val content: String, val folderId: Long?,
    val createdAt: Long, val updatedAt: Long,
    val favorite: Boolean, val pinned: Boolean, val archived: Boolean,
    val tagIds: List<Long>,
)

data class BackupData(
    val createdAt: Long,
    val folders: List<Folder>,
    val tags: List<Tag>,
    val notes: List<BackupNote>,
)
