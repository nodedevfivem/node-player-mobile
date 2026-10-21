package com.nodedevelopment.nodeplayermobile.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.nodedevelopment.nodeplayermobile.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** UI commands are sent through the session controller, never through an Activity-owned player. */
class PlaybackHub(private val context: Context, private val scope: CoroutineScope) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var player: MediaController? = null
    private val mutable = MutableStateFlow(PlaybackState())
    val state = mutable.asStateFlow()
    private var clock: Job? = null
    private var pendingPlay: Pair<List<Song>, Song>? = null
    private var pendingRestore: List<Song>? = null
    private val prefs = context.getSharedPreferences("node_player", Context.MODE_PRIVATE)
    private var lastSavedAt = 0L
    var onTransition: ((String) -> Unit)? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { sync() }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (player?.isPlaying == true) mediaItem?.mediaId?.takeIf { it.isNotBlank() }
                ?.let { onTransition?.invoke(it) }
            sync()
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) player?.currentMediaItem?.mediaId?.let { onTransition?.invoke(it) }
            sync()
        }
    }
    fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync().also { future ->
            future.addListener({
                try {
                    player = future.get()
                    player?.addListener(listener)
                    val requested = pendingPlay
                    pendingPlay = null
                    if (requested != null) play(requested.first, requested.second)
                    else pendingRestore?.let(::restoreIfEmpty)
                    pendingRestore = null
                    sync()
                    clock = scope.launch {
                        while (isActive) { if (player?.isPlaying == true) sync(); delay(450) }
                    }
                } catch (_: Exception) { /* Can retry after re-entering app. */ }
            }, ContextCompat.getMainExecutor(context))
        }
    }
    private fun sync() {
        val p = player ?: return
        val id = p.currentMediaItem?.mediaId
        val now = System.currentTimeMillis()
        if (id != null && (now - lastSavedAt >= 8_000 || !p.isPlaying)) {
            prefs.edit().putString("last_song", id)
                .putLong("last_position", p.currentPosition.coerceAtLeast(0L)).apply()
            lastSavedAt = now
        }
        mutable.value = PlaybackState(
            mediaId = p.currentMediaItem?.mediaId,
            playing = p.isPlaying,
            duration = p.duration.takeIf { it > 0 && it != androidx.media3.common.C.TIME_UNSET } ?: 0,
            position = p.currentPosition.coerceAtLeast(0),
            shuffle = p.shuffleModeEnabled,
            repeat = p.repeatMode,
            hasNext = p.hasNextMediaItem(),
            hasPrev = p.hasPreviousMediaItem()
        )
    }
    private fun makeItems(songs: List<Song>): List<MediaItem> = songs.map { song ->
        MediaItem.Builder().setMediaId(song.id).setUri(song.uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(song.title)
                .setArtist(song.artist).setAlbumTitle(song.album).build()).build()
    }
    fun restoreIfEmpty(songs: List<Song>) {
        val p = player ?: run { pendingRestore = songs; return }
        if (p.mediaItemCount > 0 || songs.isEmpty()) return
        val id = prefs.getString("last_song", null) ?: return
        val index = songs.indexOfFirst { it.id == id }
        if (index < 0) return
        p.setMediaItems(makeItems(songs), index, prefs.getLong("last_position", 0L))
        p.prepare()
        p.pause() // Restore the queue; NEVER auto-play on app startup.
        sync()
    }
    fun play(songs: List<Song>, selected: Song) {
        val p = player ?: run { pendingPlay = songs to selected; return }
        if (songs.isEmpty()) return
        val at = songs.indexOfFirst { it.id == selected.id }.coerceAtLeast(0)
        p.setMediaItems(makeItems(songs), at, 0L)
        p.prepare(); p.play()
        sync()
    }
    fun toggle() { player?.let { if (it.isPlaying) it.pause() else it.play() }; sync() }
    fun next() { player?.seekToNextMediaItem(); sync() }
    fun previous() { player?.seekToPreviousMediaItem(); sync() }
    fun seek(ms: Long) { player?.seekTo(ms.coerceAtLeast(0)); sync() }
    fun shuffle() { player?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }; sync() }
    fun repeat() { player?.let { it.repeatMode = when (it.repeatMode) {
        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
        else -> Player.REPEAT_MODE_OFF
    } }; sync() }
    fun pause() { player?.pause(); sync() }
    fun disconnect() {
        clock?.cancel()
        player?.removeListener(listener)
        player = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }
}

data class PlaybackState(
    val mediaId: String? = null,
    val playing: Boolean = false,
    val duration: Long = 0L,
    val position: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: Int = Player.REPEAT_MODE_OFF,
    val hasNext: Boolean = false,
    val hasPrev: Boolean = false
)
