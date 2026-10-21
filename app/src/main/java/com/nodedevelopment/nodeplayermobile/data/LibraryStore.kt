package com.nodedevelopment.nodeplayermobile.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Only local lightweight SQLite. The actual music files are never stored in this database. */
class LibraryStore(context: Context) : SQLiteOpenHelper(context, "node_library.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE favorites (song_uri TEXT PRIMARY KEY)")
        db.execSQL("CREATE TABLE playlists (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)")
        db.execSQL("CREATE TABLE playlist_songs (playlist_id INTEGER NOT NULL, song_uri TEXT NOT NULL, added_at INTEGER NOT NULL, PRIMARY KEY (playlist_id, song_uri))")
        db.execSQL("CREATE TABLE recent (song_uri TEXT PRIMARY KEY, last_played INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE song_titles (song_uri TEXT PRIMARY KEY, custom_title TEXT NOT NULL)")
    }
    // Additive migration: preserve all existing favorites, playlists, recent plays and tracks.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS song_titles (song_uri TEXT PRIMARY KEY, custom_title TEXT NOT NULL)")
        }
    }

    /** Song title overrides are local labels, keyed by the original song URI. */
    fun customTitles(): Map<String, String> = readableDatabase.rawQuery(
        "SELECT song_uri, custom_title FROM song_titles", null
    ).use { c -> buildMap { while (c.moveToNext()) put(c.getString(0), c.getString(1)) } }

    fun setCustomTitle(uri: String, customTitle: String?) {
        if (customTitle == null) {
            writableDatabase.execSQL("DELETE FROM song_titles WHERE song_uri=?", arrayOf(uri))
        } else {
            writableDatabase.execSQL(
                "INSERT OR REPLACE INTO song_titles (song_uri, custom_title) VALUES (?, ?)",
                arrayOf(uri, customTitle)
            )
        }
    }

    fun favorites(): Set<String> = readableDatabase.rawQuery("SELECT song_uri FROM favorites", null).use { c ->
        buildSet { while(c.moveToNext()) add(c.getString(0)) }
    }
    fun toggleFavorite(id: String): Boolean {
        val db = writableDatabase
        if (db.rawQuery("SELECT 1 FROM favorites WHERE song_uri=?", arrayOf(id)).use { it.moveToFirst() }) {
            db.execSQL("DELETE FROM favorites WHERE song_uri=?", arrayOf(id)); return false
        }
        db.execSQL("INSERT OR IGNORE INTO favorites (song_uri) VALUES (?)", arrayOf(id))
        return true
    }
    fun playlists(): List<MusicPlaylist> = readableDatabase.rawQuery(
        "SELECT p.id, p.name, COUNT(s.song_uri) FROM playlists p LEFT JOIN playlist_songs s ON p.id=s.playlist_id GROUP BY p.id ORDER BY p.name COLLATE NOCASE", null
    ).use { c -> buildList { while (c.moveToNext()) add(MusicPlaylist(c.getLong(0), c.getString(1), c.getInt(2))) } }

    fun createPlaylist(name: String): Boolean {
        val clean = name.trim().take(70)
        if (clean.isBlank()) return false
        writableDatabase.execSQL("INSERT INTO playlists (name) VALUES (?)", arrayOf(clean))
        return true
    }
    fun deletePlaylist(id: Long) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.execSQL("DELETE FROM playlist_songs WHERE playlist_id=?", arrayOf(id))
            writableDatabase.execSQL("DELETE FROM playlists WHERE id=?", arrayOf(id))
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }
    fun tracksForPlaylist(id: Long): Set<String> = readableDatabase.rawQuery(
        "SELECT song_uri FROM playlist_songs WHERE playlist_id=? ORDER BY added_at", arrayOf(id.toString())
    ).use { c -> buildSet { while(c.moveToNext()) add(c.getString(0)) } }
    fun addToPlaylist(playlist: Long, song: String) {
        writableDatabase.execSQL("INSERT OR IGNORE INTO playlist_songs VALUES (?, ?, ?)",
            arrayOf(playlist, song, System.currentTimeMillis()))
    }
    fun removeFromPlaylist(playlist: Long, song: String) {
        writableDatabase.execSQL("DELETE FROM playlist_songs WHERE playlist_id=? AND song_uri=?", arrayOf(playlist, song))
    }
    fun recordPlay(song: String) {
        writableDatabase.execSQL("INSERT OR REPLACE INTO recent VALUES (?, ?)",
            arrayOf(song, System.currentTimeMillis()))
    }
    fun recent(): List<String> = readableDatabase.rawQuery("SELECT song_uri FROM recent ORDER BY last_played DESC LIMIT 30", null)
        .use { c -> buildList { while(c.moveToNext()) add(c.getString(0)) } }
}

data class MusicPlaylist(val id: Long, val name: String, val songCount: Int)
