package com.notium.app.data

import android.content.Context
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteOpenHelper

/**
 * Database SQLite cifrato con SQLCipher. La chiave e' la chiave principale a 256 bit in forma
 * esadecimale "raw" (x'..'), quindi SQLCipher non esegue alcuna ulteriore derivazione.
 * Titoli, contenuti, cartelle, tag, stati, metadati e indice di ricerca FTS5 stanno tutti qui dentro.
 */
class NotiumDatabase(
    private val appContext: Context,
    keyHex: String,
) : SQLiteOpenHelper(appContext, DB_NAME, "x'$keyHex'", null, DB_VERSION, 0, null, null, false) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onOpen(db: SQLiteDatabase) {
        // Le pagine cancellate vengono sovrascritte: niente residui di note eliminate.
        db.rawQuery("PRAGMA secure_delete = ON", emptyArray<String>()).use { it.moveToFirst() }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE folders(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                parent_id INTEGER REFERENCES folders(id) ON DELETE SET NULL,
                color INTEGER NOT NULL,
                created_at INTEGER NOT NULL)"""
        )
        db.execSQL(
            """CREATE TABLE tags(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL COLLATE NOCASE UNIQUE,
                color INTEGER NOT NULL)"""
        )
        db.execSQL(
            """CREATE TABLE notes(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL DEFAULT '',
                content TEXT NOT NULL DEFAULT '',
                folder_id INTEGER REFERENCES folders(id) ON DELETE SET NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                favorite INTEGER NOT NULL DEFAULT 0,
                pinned INTEGER NOT NULL DEFAULT 0,
                archived INTEGER NOT NULL DEFAULT 0,
                deleted INTEGER NOT NULL DEFAULT 0,
                deleted_at INTEGER NOT NULL DEFAULT 0)"""
        )
        db.execSQL(
            """CREATE TABLE note_tags(
                note_id INTEGER NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
                tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
                PRIMARY KEY(note_id, tag_id))"""
        )
        db.execSQL("CREATE INDEX idx_notes_state ON notes(deleted, archived, pinned, updated_at)")
        db.execSQL("CREATE INDEX idx_notes_folder ON notes(folder_id)")
        db.execSQL("CREATE INDEX idx_note_tags_tag ON note_tags(tag_id)")
        db.execSQL("CREATE INDEX idx_folders_parent ON folders(parent_id)")
        // Ricerca full-text interna al database cifrato; rowid = id della nota.
        db.execSQL(
            "CREATE VIRTUAL TABLE notes_fts USING fts5(title, content, tags, tokenize='unicode61 remove_diacritics 2')"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migrazioni future: ogni passo va eseguito in ordine, dentro la transazione dell'helper.
        // Versione 1 e' la prima: nessuna migrazione necessaria.
    }

    companion object {
        const val DB_NAME = "notium.db"
        const val DB_VERSION = 1

        /** Elimina il file del database e i file ausiliari, sovrascrivendoli prima. */
        fun destroyFiles(context: Context) {
            val base = context.getDatabasePath(DB_NAME)
            listOf("", "-journal", "-wal", "-shm").forEach { suffix ->
                val f = java.io.File(base.path + suffix)
                if (f.exists()) {
                    runCatching {
                        java.io.RandomAccessFile(f, "rw").use { raf ->
                            val buf = ByteArray(64 * 1024)
                            var left = raf.length()
                            raf.seek(0)
                            while (left > 0) {
                                java.security.SecureRandom().nextBytes(buf)
                                val n = minOf(left, buf.size.toLong()).toInt()
                                raf.write(buf, 0, n)
                                left -= n
                            }
                            raf.fd.sync()
                        }
                    }
                    f.delete()
                }
            }
        }
    }
}
