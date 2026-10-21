package com.nodedevelopment.nodeplayermobile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nodedevelopment.nodeplayermobile.data.LibraryStore
import com.nodedevelopment.nodeplayermobile.data.MusicPlaylist
import com.nodedevelopment.nodeplayermobile.data.MusicRepository
import com.nodedevelopment.nodeplayermobile.data.Song
import com.nodedevelopment.nodeplayermobile.playback.PlaybackHub
import com.nodedevelopment.nodeplayermobile.update.AvailableUpdate
import com.nodedevelopment.nodeplayermobile.update.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    val repository = MusicRepository(context)
    private val store = LibraryStore(context)
    val hub = PlaybackHub(context, viewModelScope).also { it.connect() }
    val updater = UpdateManager(context)
    private val stateMutable = MutableStateFlow(LibraryState())
    val state = stateMutable.asStateFlow()
    private val notifyMutable = MutableStateFlow<String?>(null)
    val message = notifyMutable.asStateFlow()
    private val updateMutable = MutableStateFlow<AvailableUpdate?>(null)
    val update = updateMutable.asStateFlow()
    private val updatingMutable = MutableStateFlow(false)
    val updating = updatingMutable.asStateFlow()
    private val progressMutable = MutableStateFlow(0)
    val progress = progressMutable.asStateFlow()
    private var libraryScan: Job? = null
    private var folderUsed = ""

    init {
        hub.onTransition = { uri -> viewModelScope.launch(Dispatchers.IO) { store.recordPlay(uri) } }
        refresh()
    }
    fun refresh() {
        libraryScan?.cancel()
        libraryScan = viewModelScope.launch {
            stateMutable.value = stateMutable.value.copy(scanning = true)
            val scanned = repository.scan()
            val extras = withContext(Dispatchers.IO) {
                LibraryExtras(store.favorites(), store.playlists(), store.recent(), store.customTitles())
            }
            val songs = scanned.map { song ->
                extras.titles[song.id]?.let { song.copy(title = it) } ?: song
            }
            hub.restoreIfEmpty(songs)
            stateMutable.value = LibraryState(
                songs = songs, favorites = extras.favorites, playlists = extras.playlists,
                recent = extras.recent, customTitleIds = extras.titles.keys, scanning = false
            )
        }
    }
    fun setFolder(uri: Uri?) { repository.selectFolder(uri); refresh() }

    /** Rename the title inside NODE Player only. Never alter or move the original music file. */
    fun renameSong(song: Song, input: String) {
        val clean = input.trim().replace(Regex("[\r\n\t]+"), " ").take(120)
        if (clean.isEmpty()) { say("Song title cannot be empty"); return }
        viewModelScope.launch(Dispatchers.IO) {
            store.setCustomTitle(song.id, clean)
            withContext(Dispatchers.Main) {
                stateMutable.value = stateMutable.value.copy(
                    songs = stateMutable.value.songs.map {
                        if (it.id == song.id) it.copy(title = clean) else it
                    },
                    customTitleIds = stateMutable.value.customTitleIds + song.id
                )
            }
        }
    }

    fun restoreOriginalSongTitle(song: Song) = viewModelScope.launch(Dispatchers.IO) {
        store.setCustomTitle(song.id, null)
        withContext(Dispatchers.Main) { refresh() }
    }
    fun favorite(song: Song) = viewModelScope.launch(Dispatchers.IO) {
        store.toggleFavorite(song.id)
        val fresh = store.favorites()
        withContext(Dispatchers.Main) { stateMutable.value = stateMutable.value.copy(favorites = fresh) }
    }
    fun createPlaylist(name: String) = viewModelScope.launch(Dispatchers.IO) {
        if (store.createPlaylist(name)) {
            val updated = store.playlists()
            withContext(Dispatchers.Main) { stateMutable.value = stateMutable.value.copy(playlists = updated) }
        }
    }
    fun deletePlaylist(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        store.deletePlaylist(id)
        val updated = store.playlists()
        withContext(Dispatchers.Main) { stateMutable.value = stateMutable.value.copy(playlists = updated) }
    }
    fun addToPlaylist(id: Long, song: Song) = viewModelScope.launch(Dispatchers.IO) {
        store.addToPlaylist(id, song.id)
        val updated = store.playlists()
        withContext(Dispatchers.Main) {
            stateMutable.value = stateMutable.value.copy(playlists = updated)
            say("Added to playlist")
        }
    }
    fun removeFromPlaylist(id: Long, song: Song) = viewModelScope.launch(Dispatchers.IO) {
        store.removeFromPlaylist(id, song.id)
        val updated = store.playlists()
        withContext(Dispatchers.Main) { stateMutable.value = stateMutable.value.copy(playlists = updated) }
    }
    suspend fun playlistSongs(id: Long): Set<String> = withContext(Dispatchers.IO) { store.tracksForPlaylist(id) }
    fun import(uris: List<Uri>) = viewModelScope.launch {
        stateMutable.value = stateMutable.value.copy(importing = true, importedProgress = "0/${uris.size}")
        val result = repository.importMusic(uris) { current, total ->
            stateMutable.value = stateMutable.value.copy(importedProgress = "$current/$total")
        }
        stateMutable.value = stateMutable.value.copy(importing = false, importedProgress = "")
        say("Imported ${result.imported} song(s)${if (result.failures.isNotEmpty()) "; ${result.failures.size} failed" else ""}")
        refresh()
    }
    fun checkForUpdates(manual: Boolean = false) = viewModelScope.launch {
        try {
            val next = updater.check()
            updateMutable.value = if (next != null && (!updater.isSnoozed(next.version) || manual)) next else null
            if (manual && next == null) say("You're using the latest version")
        } catch (e: Exception) { if (manual) say("Unable to check updates: ${e.message}") }
    }
    fun dismissUpdate() {
        updateMutable.value?.let { updater.remindLater(it.version) }
        updateMutable.value = null
    }
    fun installUpdate(onReady: (android.content.Intent) -> Unit) = viewModelScope.launch {
        val next = updateMutable.value ?: return@launch
        updatingMutable.value = true; progressMutable.value = 0
        try {
            val apk = updater.download(next) { progressMutable.value = it }
            onReady(updater.startInstall(apk))
            updateMutable.value = null
        } catch (e: Exception) { say("Update failed: ${e.message}") }
        finally { updatingMutable.value = false }
    }
    fun say(text: String) { notifyMutable.value = text }
    fun clearMessage() { notifyMutable.value = null }
    override fun onCleared() { hub.disconnect(); store.close(); super.onCleared() }
}

private data class LibraryExtras(
    val favorites: Set<String>,
    val playlists: List<MusicPlaylist>,
    val recent: List<String>,
    val titles: Map<String, String>
)

data class LibraryState(
    val songs: List<Song> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val playlists: List<MusicPlaylist> = emptyList(),
    val recent: List<String> = emptyList(),
    val customTitleIds: Set<String> = emptySet(),
    val scanning: Boolean = false,
    val importing: Boolean = false,
    val importedProgress: String = ""
)
