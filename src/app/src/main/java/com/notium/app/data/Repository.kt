package com.notium.app.data

import android.content.ContentValues
import android.database.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SQLiteDatabase

private fun Cursor.str(c: String) = getString(getColumnIndexOrThrow(c)) ?: ""
private fun Cursor.lng(c: String) = getLong(getColumnIndexOrThrow(c))
private fun Cursor.int(c: String) = getInt(getColumnIndexOrThrow(c))
private fun Cursor.lngOrNull(c: String): Long? {
    val i = getColumnIndexOrThrow(c)
    return if (isNull(i)) null else getLong(i)
}

/** Tutte le operazioni sui dati. Ogni scrittura e' transazionale. */
class Repository(private val helper: NotiumDatabase) {
    private val db: SQLiteDatabase get() = helper.writableDatabase

    fun close() = runCatching { helper.close() }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    private inline fun <T> tx(block: () -> T): T {
        db.beginTransaction()
        try {
            val r = block()
            db.setTransactionSuccessful()
            return r
        } finally {
            db.endTransaction()
        }
    }

    // ================================================================= NOTE

    suspend fun listNotes(f: NoteFilter): List<NoteSummary> = io {
        val args = ArrayList<String>()
        val where = StringBuilder()
        val q = f.query.trim()
        val cte = StringBuilder()

        when (val s = f.section) {
            Section.All -> where.append("n.deleted=0 AND n.archived=0")
            Section.Favorites -> where.append("n.deleted=0 AND n.favorite=1")
            Section.Pinned -> where.append("n.deleted=0 AND n.pinned=1")
            Section.Archive -> where.append("n.deleted=0 AND n.archived=1")
            Section.Trash -> where.append("n.deleted=1")
            is Section.InFolder -> where.append("n.deleted=0 AND n.archived=0 AND n.folder_id=${s.id}")
            is Section.WithTag -> where.append(
                "n.deleted=0 AND n.archived=0 AND EXISTS(SELECT 1 FROM note_tags x WHERE x.note_id=n.id AND x.tag_id=${s.id})"
            )
        }

        if (q.isNotEmpty()) {
            // Titoli, contenuti, parole, frasi e tag: indice FTS5. Cartelle e sottocartelle: nome della cartella.
            cte.append(
                "WITH RECURSIVE sub(id) AS (SELECT id FROM folders WHERE name LIKE ? ESCAPE '\\' " +
                    "UNION SELECT f.id FROM folders f JOIN sub s ON f.parent_id=s.id) "
            )
            args.add("%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%")
            where.append(
                " AND (n.id IN (SELECT rowid FROM notes_fts WHERE notes_fts MATCH ?) " +
                    "OR n.folder_id IN (SELECT id FROM sub))"
            )
            args.add(ftsQuery(q))
        }

        val order = when {
            f.section == Section.Trash -> "n.deleted_at DESC"
            else -> {
                val key = when (f.sort) {
                    SortOrder.MODIFIED -> "n.updated_at DESC"
                    SortOrder.CREATED -> "n.created_at DESC"
                    SortOrder.TITLE -> "n.title COLLATE NOCASE ASC"
                    SortOrder.FAVORITES -> "n.favorite DESC, n.updated_at DESC"
                    SortOrder.PINNED -> "n.pinned DESC, n.updated_at DESC"
                }
                "n.pinned DESC, $key"
            }
        }

        val sql = "$cte SELECT n.id, n.title, substr(n.content,1,400) AS snippet, n.folder_id, n.created_at, " +
            "n.updated_at, n.favorite, n.pinned, n.archived, n.deleted FROM notes n WHERE $where ORDER BY $order"

        val tagMap = tagsByNote()
        val out = ArrayList<NoteSummary>()
        db.rawQuery(sql, args.toTypedArray()).use { c ->
            while (c.moveToNext()) {
                val id = c.lng("id")
                out.add(
                    NoteSummary(
                        id, c.str("title"), c.str("snippet"), c.lngOrNull("folder_id"),
                        c.lng("created_at"), c.lng("updated_at"),
                        c.int("favorite") == 1, c.int("pinned") == 1,
                        c.int("archived") == 1, c.int("deleted") == 1,
                        tagMap[id].orEmpty(),
                    )
                )
            }
        }
        out
    }

    private fun tagsByNote(): Map<Long, List<Tag>> {
        val m = HashMap<Long, MutableList<Tag>>()
        db.rawQuery(
            "SELECT nt.note_id AS nid, t.id AS id, t.name AS name, t.color AS color " +
                "FROM note_tags nt JOIN tags t ON t.id=nt.tag_id ORDER BY t.name COLLATE NOCASE", null
        ).use { c ->
            while (c.moveToNext()) {
                m.getOrPut(c.lng("nid")) { ArrayList() }.add(Tag(c.lng("id"), c.str("name"), c.int("color")))
            }
        }
        return m
    }

    suspend fun getNote(id: Long): Note? = io {
        db.rawQuery("SELECT * FROM notes WHERE id=?", arrayOf(id.toString())).use { c ->
            if (!c.moveToFirst()) return@io null
            val tagIds = HashSet<Long>()
            db.rawQuery("SELECT tag_id FROM note_tags WHERE note_id=?", arrayOf(id.toString())).use { t ->
                while (t.moveToNext()) tagIds.add(t.getLong(0))
            }
            Note(
                id, c.str("title"), c.str("content"), c.lngOrNull("folder_id"),
                c.lng("created_at"), c.lng("updated_at"),
                c.int("favorite") == 1, c.int("pinned") == 1, c.int("archived") == 1, c.int("deleted") == 1,
                tagIds,
            )
        }
    }

    /** Crea o aggiorna una nota (con tag e indice di ricerca) in un'unica transazione. Restituisce l'id. */
    suspend fun saveNote(
        id: Long?, title: String, content: String, folderId: Long?, tagIds: Set<Long>,
        favorite: Boolean, pinned: Boolean, archived: Boolean, touch: Boolean = true,
    ): Long = io {
        tx {
            val now = System.currentTimeMillis()
            val v = ContentValues().apply {
                put("title", title); put("content", content)
                if (folderId == null) putNull("folder_id") else put("folder_id", folderId)
                put("favorite", favorite.toInt()); put("pinned", pinned.toInt()); put("archived", archived.toInt())
            }
            val noteId: Long
            if (id == null) {
                v.put("created_at", now); v.put("updated_at", now)
                noteId = db.insertOrThrow("notes", null, v)
            } else {
                if (touch) v.put("updated_at", now)
                db.update("notes", v, "id=?", arrayOf(id.toString()))
                noteId = id
            }
            db.delete("note_tags", "note_id=?", arrayOf(noteId.toString()))
            tagIds.forEach { t ->
                db.execSQL("INSERT OR IGNORE INTO note_tags(note_id, tag_id) VALUES(?,?)", arrayOf<Any>(noteId, t))
            }
            reindex(noteId)
            noteId
        }
    }

    private fun reindex(noteId: Long) {
        db.execSQL("DELETE FROM notes_fts WHERE rowid=?", arrayOf<Any>(noteId))
        db.execSQL(
            "INSERT INTO notes_fts(rowid, title, content, tags) " +
                "SELECT n.id, n.title, n.content, COALESCE((SELECT group_concat(t.name,' ') FROM note_tags nt " +
                "JOIN tags t ON t.id=nt.tag_id WHERE nt.note_id=n.id),'') FROM notes n WHERE n.id=?",
            arrayOf<Any>(noteId)
        )
    }

    private fun reindexAll() {
        db.execSQL("DELETE FROM notes_fts")
        db.execSQL(
            "INSERT INTO notes_fts(rowid, title, content, tags) " +
                "SELECT n.id, n.title, n.content, COALESCE((SELECT group_concat(t.name,' ') FROM note_tags nt " +
                "JOIN tags t ON t.id=nt.tag_id WHERE nt.note_id=n.id),'') FROM notes n"
        )
    }

    private fun setFlag(id: Long, col: String, value: Boolean) {
        db.execSQL("UPDATE notes SET $col=? WHERE id=?", arrayOf<Any>(value.toInt(), id))
    }

    suspend fun setFavorite(id: Long, v: Boolean) = io { setFlag(id, "favorite", v) }
    suspend fun setPinned(id: Long, v: Boolean) = io { setFlag(id, "pinned", v) }
    suspend fun setArchived(id: Long, v: Boolean) = io { setFlag(id, "archived", v) }

    suspend fun moveNote(id: Long, folderId: Long?) = io {
        db.execSQL("UPDATE notes SET folder_id=? WHERE id=?", arrayOf<Any?>(folderId, id))
    }

    suspend fun moveToTrash(id: Long) = io {
        db.execSQL(
            "UPDATE notes SET deleted=1, deleted_at=?, pinned=0 WHERE id=?",
            arrayOf<Any>(System.currentTimeMillis(), id)
        )
    }

    suspend fun restore(id: Long) = io {
        db.execSQL("UPDATE notes SET deleted=0, deleted_at=0, archived=0 WHERE id=?", arrayOf<Any>(id))
    }

    suspend fun deletePermanently(id: Long) = io {
        tx {
            db.execSQL("DELETE FROM notes_fts WHERE rowid=?", arrayOf<Any>(id))
            db.delete("notes", "id=?", arrayOf(id.toString()))
        }
        compact()
    }

    suspend fun emptyTrash() = io {
        tx {
            db.execSQL("DELETE FROM notes_fts WHERE rowid IN (SELECT id FROM notes WHERE deleted=1)")
            db.execSQL("DELETE FROM notes WHERE deleted=1")
        }
        compact()
    }

    /** Elimina in modo definitivo le note nel cestino da piu' di [days] giorni. */
    suspend fun purgeTrashOlderThan(days: Int) = io {
        if (days <= 0) return@io
        val limit = System.currentTimeMillis() - days * 86_400_000L
        tx {
            db.execSQL(
                "DELETE FROM notes_fts WHERE rowid IN (SELECT id FROM notes WHERE deleted=1 AND deleted_at<?)",
                arrayOf<Any>(limit)
            )
            db.execSQL("DELETE FROM notes WHERE deleted=1 AND deleted_at<?", arrayOf<Any>(limit))
        }
    }

    /** VACUUM ricostruisce il file: i dati rimossi non restano nelle pagine libere. */
    private fun compact() {
        runCatching { db.execSQL("VACUUM") }
    }

    // ============================================================== CARTELLE

    suspend fun listFolders(): List<Folder> = io {
        val out = ArrayList<Folder>()
        db.rawQuery(
            "SELECT f.id, f.name, f.parent_id, f.color, " +
                "(SELECT COUNT(*) FROM notes n WHERE n.folder_id=f.id AND n.deleted=0) AS cnt " +
                "FROM folders f ORDER BY f.name COLLATE NOCASE", null
        ).use { c ->
            while (c.moveToNext()) {
                out.add(Folder(c.lng("id"), c.str("name"), c.lngOrNull("parent_id"), c.int("color"), c.int("cnt")))
            }
        }
        out
    }

    suspend fun createFolder(name: String, parentId: Long?, color: Int): Long = io {
        val v = ContentValues().apply {
            put("name", name.trim())
            if (parentId == null) putNull("parent_id") else put("parent_id", parentId)
            put("color", color); put("created_at", System.currentTimeMillis())
        }
        db.insertOrThrow("folders", null, v)
    }

    suspend fun updateFolder(id: Long, name: String, color: Int) = io {
        db.execSQL("UPDATE folders SET name=?, color=? WHERE id=?", arrayOf<Any>(name.trim(), color, id))
    }

    /** Sposta una cartella. Restituisce false se creerebbe una gerarchia circolare. */
    suspend fun moveFolder(id: Long, newParentId: Long?): Boolean = io {
        if (newParentId != null && (newParentId == id || descendants(id).contains(newParentId))) {
            return@io false
        }
        db.execSQL("UPDATE folders SET parent_id=? WHERE id=?", arrayOf<Any?>(newParentId, id))
        true
    }

    private fun descendants(id: Long): Set<Long> {
        val out = HashSet<Long>()
        db.rawQuery(
            "WITH RECURSIVE sub(id) AS (SELECT id FROM folders WHERE parent_id=? " +
                "UNION SELECT f.id FROM folders f JOIN sub s ON f.parent_id=s.id) SELECT id FROM sub",
            arrayOf(id.toString())
        ).use { c -> while (c.moveToNext()) out.add(c.getLong(0)) }
        return out
    }

    /** Elenco degli id che non possono essere scelti come nuova cartella padre. */
    suspend fun forbiddenParents(id: Long): Set<Long> = io { descendants(id) + id }

    suspend fun deleteFolder(id: Long, mode: FolderDeleteMode) = io {
        tx {
            val parent: Long? = db.rawQuery("SELECT parent_id FROM folders WHERE id=?", arrayOf(id.toString()))
                .use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null }
            when (mode) {
                FolderDeleteMode.MoveNotesToRoot ->
                    db.execSQL("UPDATE notes SET folder_id=NULL WHERE folder_id=?", arrayOf<Any>(id))
                is FolderDeleteMode.MoveNotesTo ->
                    db.execSQL("UPDATE notes SET folder_id=? WHERE folder_id=?", arrayOf<Any>(mode.folderId, id))
                FolderDeleteMode.DeleteNotes ->
                    db.execSQL(
                        "UPDATE notes SET deleted=1, deleted_at=?, pinned=0, folder_id=NULL WHERE folder_id=? AND deleted=0",
                        arrayOf<Any>(System.currentTimeMillis(), id)
                    )
            }
            // Le sottocartelle salgono di un livello.
            db.execSQL("UPDATE folders SET parent_id=? WHERE parent_id=?", arrayOf<Any?>(parent, id))
            db.delete("folders", "id=?", arrayOf(id.toString()))
        }
    }

    // ================================================================== TAG

    suspend fun listTags(): List<Tag> = io {
        val out = ArrayList<Tag>()
        db.rawQuery(
            "SELECT t.id, t.name, t.color, (SELECT COUNT(*) FROM note_tags nt JOIN notes n ON n.id=nt.note_id " +
                "WHERE nt.tag_id=t.id AND n.deleted=0) AS cnt FROM tags t ORDER BY t.name COLLATE NOCASE", null
        ).use { c ->
            while (c.moveToNext()) out.add(Tag(c.lng("id"), c.str("name"), c.int("color"), c.int("cnt")))
        }
        out
    }

    /** Crea il tag; se esiste gia' (senza distinzione maiuscole/minuscole) restituisce quello esistente. */
    suspend fun createTag(name: String, color: Int): Long = io {
        val n = name.trim()
        db.rawQuery("SELECT id FROM tags WHERE name=?", arrayOf(n)).use { if (it.moveToFirst()) return@io it.getLong(0) }
        val v = ContentValues().apply { put("name", n); put("color", color) }
        db.insertOrThrow("tags", null, v)
    }

    suspend fun updateTag(id: Long, name: String, color: Int): Boolean = io {
        val n = name.trim()
        db.rawQuery("SELECT id FROM tags WHERE name=? AND id<>?", arrayOf(n, id.toString())).use {
            if (it.moveToFirst()) return@io false
        }
        tx {
            db.execSQL("UPDATE tags SET name=?, color=? WHERE id=?", arrayOf<Any>(n, color, id))
            db.rawQuery("SELECT note_id FROM note_tags WHERE tag_id=?", arrayOf(id.toString())).use { c ->
                while (c.moveToNext()) reindex(c.getLong(0))
            }
        }
        true
    }

    suspend fun deleteTag(id: Long) = io {
        tx {
            val ids = ArrayList<Long>()
            db.rawQuery("SELECT note_id FROM note_tags WHERE tag_id=?", arrayOf(id.toString())).use { c ->
                while (c.moveToNext()) ids.add(c.getLong(0))
            }
            db.delete("tags", "id=?", arrayOf(id.toString()))
            ids.forEach { reindex(it) }
        }
    }

    // ================================================================ BACKUP

    suspend fun exportData(): BackupData = io {
        val folders = ArrayList<Folder>()
        db.rawQuery("SELECT id, name, parent_id, color FROM folders", emptyArray<String>()).use { c ->
            while (c.moveToNext()) folders.add(Folder(c.lng("id"), c.str("name"), c.lngOrNull("parent_id"), c.int("color")))
        }
        val tags = ArrayList<Tag>()
        db.rawQuery("SELECT id, name, color FROM tags", emptyArray<String>()).use { c ->
            while (c.moveToNext()) tags.add(Tag(c.lng("id"), c.str("name"), c.int("color")))
        }
        val links = HashMap<Long, MutableList<Long>>()
        db.rawQuery("SELECT note_id, tag_id FROM note_tags", emptyArray<String>()).use { c ->
            while (c.moveToNext()) links.getOrPut(c.getLong(0)) { ArrayList() }.add(c.getLong(1))
        }
        val notes = ArrayList<BackupNote>()
        db.rawQuery("SELECT * FROM notes WHERE deleted=0", emptyArray<String>()).use { c ->
            while (c.moveToNext()) {
                val id = c.lng("id")
                notes.add(
                    BackupNote(
                        id, c.str("title"), c.str("content"), c.lngOrNull("folder_id"),
                        c.lng("created_at"), c.lng("updated_at"),
                        c.int("favorite") == 1, c.int("pinned") == 1, c.int("archived") == 1,
                        links[id].orEmpty(),
                    )
                )
            }
        }
        BackupData(System.currentTimeMillis(), folders, tags, notes)
    }

    /** Sostituisce tutti i dati con quelli del backup. Se qualcosa fallisce, la transazione annulla tutto. */
    suspend fun replaceAll(data: BackupData) = io {
        tx {
            db.execSQL("DELETE FROM note_tags")
            db.execSQL("DELETE FROM notes")
            db.execSQL("DELETE FROM tags")
            db.execSQL("DELETE FROM folders")
            // Le cartelle vanno inserite padre prima dei figli (FK).
            val pending = data.folders.toMutableList()
            val inserted = HashSet<Long>()
            var guard = pending.size + 1
            while (pending.isNotEmpty() && guard-- > 0) {
                val it = pending.iterator()
                while (it.hasNext()) {
                    val f = it.next()
                    if (f.parentId == null || f.parentId in inserted) {
                        db.execSQL(
                            "INSERT INTO folders(id,name,parent_id,color,created_at) VALUES(?,?,?,?,?)",
                            arrayOf<Any?>(f.id, f.name, f.parentId, f.color, data.createdAt)
                        )
                        inserted.add(f.id); it.remove()
                    }
                }
            }
            check(pending.isEmpty()) { "gerarchia cartelle non valida" }
            data.tags.forEach {
                db.execSQL("INSERT INTO tags(id,name,color) VALUES(?,?,?)", arrayOf<Any>(it.id, it.name, it.color))
            }
            data.notes.forEach { n ->
                db.execSQL(
                    "INSERT INTO notes(id,title,content,folder_id,created_at,updated_at,favorite,pinned,archived) " +
                        "VALUES(?,?,?,?,?,?,?,?,?)",
                    arrayOf<Any?>(
                        n.id, n.title, n.content, n.folderId, n.createdAt, n.updatedAt,
                        n.favorite.toInt(), n.pinned.toInt(), n.archived.toInt()
                    )
                )
                n.tagIds.forEach { t ->
                    db.execSQL("INSERT OR IGNORE INTO note_tags(note_id,tag_id) VALUES(?,?)", arrayOf<Any>(n.id, t))
                }
            }
            reindexAll()
        }
        compact()
    }

    private fun Boolean.toInt() = if (this) 1 else 0

    companion object {
        /** Costruisce una query FTS5 sicura: ogni parola tra virgolette, con prefisso, tutte obbligatorie. */
        fun ftsQuery(q: String): String =
            q.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                .joinToString(" ") { "\"" + it.replace("\"", "\"\"") + "\"*" }
    }
}
