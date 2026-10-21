package com.nodedevelopment.nodeplayermobile.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.util.Locale

/** A music entry is only a URI reference. We NEVER move/delete original music during scans. */
data class Song(
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val dateAdded: Long,
    val albumId: Long = -1,
    val fileName: String = ""
) {
    val id: String get() = uri.toString()
}

class MusicRepository(private val context: Context) {
    private val resolver get() = context.contentResolver
    private val prefs = context.getSharedPreferences("node_player", Context.MODE_PRIVATE)
    val folder: Uri? get() = prefs.getString("folder_uri", null)?.let(Uri::parse)

    fun selectFolder(uri: Uri?) {
        prefs.edit().apply { if (uri == null) remove("folder_uri") else putString("folder_uri", uri.toString()) }.apply()
    }

    suspend fun scan(): List<Song> = withContext(Dispatchers.IO) {
        val songs = if (folder != null) scanTree(folder!!) else scanMusicFolder()
        songs.distinctBy { it.id }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    }

    private fun scanMusicFolder(): List<Song> {
        val out = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cols = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.RELATIVE_PATH
        )
        try {
            resolver.query(collection, cols,
                "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?",
                arrayOf("${Environment.DIRECTORY_MUSIC}/%"),
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                fun str(k: String): String = cursor.getString(cursor.getColumnIndexOrThrow(k)) ?: ""
                fun num(k: String): Long = cursor.getLong(cursor.getColumnIndexOrThrow(k))
                while (cursor.moveToNext()) {
                    val file = str(MediaStore.Audio.Media.DISPLAY_NAME)
                    if (!isAudioFile(file)) continue
                    val uri = ContentUris.withAppendedId(collection, num(MediaStore.Audio.Media._ID))
                    val title = str(MediaStore.Audio.Media.TITLE).takeUnless { it.isBlank() || it == "<unknown>" }
                        ?: file.substringBeforeLast('.')
                    out.add(Song(uri, title, str(MediaStore.Audio.Media.ARTIST).cleanUnknown(),
                        str(MediaStore.Audio.Media.ALBUM).cleanUnknown(),
                        num(MediaStore.Audio.Media.DURATION), num(MediaStore.Audio.Media.DATE_ADDED),
                        num(MediaStore.Audio.Media.ALBUM_ID), file))
                }
            }
        } catch (_: SecurityException) { /* The permission UI handles this case. */ }
          catch (_: Exception) { /* A revoked volume must not crash the app. */ }
        return out
    }

    private fun scanTree(tree: Uri): List<Song> {
        val root = DocumentFile.fromTreeUri(context, tree) ?: return emptyList()
        val out = mutableListOf<Song>()
        val pending = ArrayDeque<DocumentFile>()
        pending.add(root)
        var visited = 0
        while (pending.isNotEmpty() && visited < 20_000) {
            val current = pending.removeFirst()
            val children = try { current.listFiles() } catch (_: Exception) { emptyArray() }
            for (child in children) {
                if (++visited > 20_000) break
                if (child.isDirectory) { pending.add(child); continue }
                val name = child.name ?: continue
                if (!child.isFile || !isAudioFile(name)) continue
                val title = name.substringBeforeLast('.')
                out.add(Song(child.uri, title, "Unknown Artist", "Unknown Album", 0,
                    child.lastModified() / 1000, -1, name))
            }
        }
        return out
    }

    /** Copies selected downloads into shared Music/ via MediaStore (Android 10+). */
    suspend fun importMusic(uris: List<Uri>, progress: (Int, Int) -> Unit): ImportResult = withContext(Dispatchers.IO) {
        var success = 0
        val failures = mutableListOf<String>()
        uris.distinct().forEachIndexed { index, source ->
            try {
                val original = resolver.query(source,
                    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                    ?: source.lastPathSegment?.substringAfterLast('/') ?: "Imported song.mp3"
                if (!isAudioFile(original)) throw IllegalArgumentException("Unsupported audio extension")
                val safe = original.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").take(160)
                val ext = safe.substringAfterLast('.', "mp3").lowercase(Locale.ROOT)
                val mime = when (ext) {
                    "mp3" -> "audio/mpeg"; "m4a", "mp4" -> "audio/mp4"
                    "flac" -> "audio/flac"; "ogg", "opus" -> "audio/ogg"
                    "wav" -> "audio/wav"; "aac" -> "audio/aac"; else -> "audio/*"
                }
                // A selected SAF folder is the user's library, so import THERE instead
                // of silently placing songs in the default Music folder.
                val customTree = folder
                if (customTree != null) {
                    val tree = DocumentFile.fromTreeUri(context, customTree)
                        ?: throw FileNotFoundException("Music folder no longer accessible")
                    val item = tree.createFile(mime, safe)
                        ?: throw FileNotFoundException("Could not create file in custom folder")
                    try {
                        resolver.openInputStream(source).use { input ->
                            if (input == null) throw FileNotFoundException("Source not found")
                            resolver.openOutputStream(item.uri, "w").use { output ->
                                if (output == null) throw FileNotFoundException("Destination not writable")
                                input.copyTo(output, 64 * 1024)
                            }
                        }
                        success++
                    } catch (e: Exception) { item.delete(); throw e }
                    progress(index + 1, uris.size)
                    return@forEachIndexed
                }
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safe)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val destination = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw FileNotFoundException("Unable to create destination")
                try {
                    resolver.openInputStream(source).use { input ->
                        if (input == null) throw FileNotFoundException("Source not found")
                        resolver.openOutputStream(destination, "w").use { output ->
                            if (output == null) throw FileNotFoundException("Destination not writable")
                            input.copyTo(output, 64 * 1024)
                        }
                    }
                    resolver.update(destination, ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }, null, null)
                    success++
                } catch (e: Exception) {
                    resolver.delete(destination, null, null)
                    throw e
                }
            } catch (e: Exception) { failures.add(e.message ?: "Import failed") }
            progress(index + 1, uris.size)
        }
        ImportResult(success, failures)
    }

    companion object {
        private val formats = setOf("mp3", "m4a", "mp4", "aac", "flac", "ogg", "opus", "wav")
        fun isAudioFile(name: String): Boolean = name.substringAfterLast('.', "").lowercase(Locale.ROOT) in formats
    }
}

data class ImportResult(val imported: Int, val failures: List<String>)
private fun String.cleanUnknown(): String = if (isBlank() || equals("<unknown>", true)) "Unknown" else this
