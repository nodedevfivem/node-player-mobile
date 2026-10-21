package com.nodedevelopment.nodeplayermobile.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nodedevelopment.nodeplayermobile.BuildConfig
import com.nodedevelopment.nodeplayermobile.MainViewModel
import com.nodedevelopment.nodeplayermobile.R
import com.nodedevelopment.nodeplayermobile.data.Artwork
import com.nodedevelopment.nodeplayermobile.data.MusicPlaylist
import com.nodedevelopment.nodeplayermobile.data.Song
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import java.util.Locale

private val BG = Color(0xFF030914)
private val CARD = Color(0xFF101C2D)
private val CARD_2 = Color(0xFF0A1422)
private val CYAN = Color(0xFF10D8FE)
private val MUTED = Color(0xFF93A8BC)
private val WHITE = Color(0xFFF5F9FF)
private val BLUE = Color(0xFF0964DB)
private val PLAYER_GRAD = Brush.verticalGradient(listOf(Color(0xFF12375B), BG, BG))
private val pageShape = RoundedCornerShape(18.dp)
private val scheme = darkColorScheme(
    primary = CYAN, onPrimary = Color(0xFF00121F),
    background = BG, onBackground = WHITE,
    surface = CARD, onSurface = WHITE, surfaceVariant = CARD_2,
    onSurfaceVariant = MUTED, secondary = BLUE
)

@Composable
fun NodeApp(
    vm: MainViewModel,
    hasPermission: Boolean,
    requestPermission: () -> Unit,
    chooseFolder: () -> Unit,
    defaultFolder: () -> Unit,
    importFiles: () -> Unit,
    openIntent: (Intent) -> Unit
) {
    MaterialTheme(colorScheme = scheme) {
        val context = LocalContext.current
        val prefs = remember { context.getSharedPreferences("node_player", Context.MODE_PRIVATE) }
        var welcomed by remember { mutableStateOf(prefs.getBoolean("setup_done", false)) }
        val library by vm.state.collectAsStateWithLifecycle()
        val playback by vm.hub.state.collectAsStateWithLifecycle()
        val update by vm.update.collectAsStateWithLifecycle()
        val updating by vm.updating.collectAsStateWithLifecycle()
        val percent by vm.progress.collectAsStateWithLifecycle()
        val message by vm.message.collectAsStateWithLifecycle()
        var page by remember { mutableStateOf("Home") }
        var playerOpen by remember { mutableStateOf(false) }
        var playlistOpen by remember { mutableStateOf<MusicPlaylist?>(null) }
        var group by remember { mutableStateOf<Pair<String, String>?>(null) }
        var addSong by remember { mutableStateOf<Song?>(null) }
        var search by remember { mutableStateOf("") }
        val current = library.songs.firstOrNull { it.id == playback.mediaId }
        BackHandler(enabled = playerOpen || playlistOpen != null || group != null || page != "Home") {
            when {
                playerOpen -> playerOpen = false
                playlistOpen != null -> playlistOpen = null
                group != null -> group = null
                else -> page = "Home"
            }
        }

        if (!welcomed) {
            WelcomeScreen(onDefault = {
                defaultFolder(); welcomed = true; prefs.edit().putBoolean("setup_done", true).apply()
            }, onCustom = {
                chooseFolder(); welcomed = true; prefs.edit().putBoolean("setup_done", true).apply()
            })
        } else {
            Scaffold(
                containerColor = BG,
                bottomBar = {
                    if (!playerOpen) Column {
                        if (current != null) MiniPlayer(current, playback.playing,
                            onOpen = { playerOpen = true },
                            onToggle = { vm.hub.toggle() },
                            onNext = { vm.hub.next() })
                        NavigationBar(containerColor = Color(0xFF07111F), tonalElevation = 0.dp) {
                            listOf(
                                Triple("Home", Icons.Default.Home, "Dashboard"),
                                Triple("Library", Icons.Default.LibraryMusic, "All music"),
                                Triple("Playlists", Icons.Default.QueueMusic, "My playlists"),
                                Triple("Settings", Icons.Default.Settings, "Preferences")
                            ).forEach { (label, icon, desc) ->
                                NavigationBarItem(selected = page == label,
                                    onClick = { page = label; playlistOpen = null; group = null; search = "" },
                                    icon = { Icon(icon, contentDescription = desc) },
                                    label = { Text(label, fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CYAN, selectedTextColor = CYAN,
                                        unselectedIconColor = MUTED, unselectedTextColor = MUTED,
                                        indicatorColor = Color(0xFF112E4E)))
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    if (playerOpen) {
                        NowPlaying(current, playback, library.favorites,
                            onBack = { playerOpen = false },
                            onToggle = vm.hub::toggle, onNext = vm.hub::next,
                            onPrevious = vm.hub::previous, onShuffle = vm.hub::shuffle,
                            onRepeat = vm.hub::repeat, onSeek = vm.hub::seek,
                            onFavorite = { if (current != null) vm.favorite(current) })
                    } else when (page) {
                        "Home" -> HomeScreen(library.songs, library.recent, library.favorites, library.scanning,
                            importFiles = importFiles,
                            onRescan = vm::refresh,
                            onClick = { play -> vm.hub.play(library.songs, play) },
                            onFavorites = { page = "Library"; group = "Favorites" to "" },
                            onArtists = { page = "Library"; group = "Artists" to "" },
                            onAlbums = { page = "Library"; group = "Albums" to "" },
                            onAll = { page = "Library"; group = null })
                        "Library" -> LibraryScreen(library.songs, library.favorites,
                            group, search, onSearch = { search = it }, onGroup = { group = it },
                            onPlay = { songs, song -> vm.hub.play(songs, song) },
                            onFavorite = { vm.favorite(it) },
                            onAdd = { addSong = it }, onImport = importFiles,
                            onRefresh = vm::refresh,
                            allowPermission = hasPermission, requestPermission = requestPermission)
                        "Playlists" -> PlaylistScreen(vm, library.songs, library.playlists,
                            playlistOpen, onOpen = { playlistOpen = it },
                            onPlay = { songs, song -> vm.hub.play(songs, song) },
                            onAdd = { addSong = it }, onFavorite = { vm.favorite(it) },
                            favorites = library.favorites)
                        "Settings" -> SettingsScreen(vm,
                            folder = vm.repository.folder?.toString(),
                            onChooseFolder = chooseFolder, onDefaultFolder = defaultFolder,
                            onImport = importFiles, onUpdate = { vm.checkForUpdates(true) },
                            onPermission = requestPermission)
                    }
                    if (library.importing) {
                        Box(Modifier.fillMaxSize().background(Color(0xCC030914)), contentAlignment = Alignment.Center) {
                            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = CYAN)
                                Spacer(Modifier.height(16.dp))
                                Text("Importing to Music folder…", color = WHITE, fontWeight = FontWeight.Bold)
                                Text(library.importedProgress, color = MUTED)
                            }
                        }
                    }
                }
            }
        }
        if (addSong != null) {
            AlertDialog(onDismissRequest = { addSong = null },
                title = { Text("Add to Playlist") },
                text = {
                    Column {
                        if (library.playlists.isEmpty()) Text("Create a playlist on the Playlists tab first.")
                        library.playlists.forEach { playlist ->
                            TextButton(onClick = {
                                addSong?.let { vm.addToPlaylist(playlist.id, it) }; addSong = null
                            }) { Icon(Icons.Default.QueueMusic, null); Spacer(Modifier.width(12.dp)); Text(playlist.name) }
                        }
                    }
                }, confirmButton = { TextButton(onClick = { addSong = null }) { Text("Close") } })
        }
        if (update != null) {
            AlertDialog(onDismissRequest = { if (!updating) vm.dismissUpdate() },
                title = { Text("NODE Player update available", color = CYAN) },
                text = { Column {
                    Text("Version ${update!!.version} is ready", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text(update!!.notes.ifBlank { "Improvements and fixes." }.take(700), color = MUTED)
                    if (updating) { Spacer(Modifier.height(16.dp)); LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth()); Text("Downloading… $percent%") }
                } },
                confirmButton = { TextButton(enabled = !updating, onClick = { vm.installUpdate(openIntent) }) { Text("UPDATE NOW") } },
                dismissButton = { TextButton(enabled = !updating, onClick = vm::dismissUpdate) { Text("Maybe Later") } })
        }
        if (message != null) {
            AlertDialog(onDismissRequest = vm::clearMessage,
                title = { Text("NODE Player") }, text = { Text(message ?: "") },
                confirmButton = { TextButton(onClick = vm::clearMessage) { Text("OK") } })
        }
    }
}

@Composable
private fun WelcomeScreen(onDefault: () -> Unit, onCustom: () -> Unit) {
    Column(Modifier.fillMaxSize().background(BG).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Spacer(Modifier.height(45.dp))
        Image(painterResource(R.drawable.node_player_logo), contentDescription = "Official NODE Player logo",
            modifier = Modifier.fillMaxWidth(.75f).aspectRatio(1f))
        Text("WELCOME TO NODE", color = WHITE, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text("PLAYER MOBILE", color = CYAN, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
        Spacer(Modifier.height(22.dp))
        Text("Your music. Your phone. No internet needed.", color = MUTED)
        Spacer(Modifier.height(32.dp))
        PrimaryButton("Use Default Music Folder", Icons.Default.Folder, onDefault)
        Spacer(Modifier.height(14.dp))
        OutlinedButton(onClick = onCustom, modifier = Modifier.fillMaxWidth().height(55.dp), shape = pageShape) {
            Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Choose Another Folder")
        }
        Spacer(Modifier.height(12.dp))
        Text("Default: Internal Storage / Music", color = MUTED, fontSize = 12.sp)
        Spacer(Modifier.height(42.dp))
    }
}

@Composable
private fun Titlebar(title: String, subtitle: String? = null, end: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 19.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, color = WHITE)
            if (subtitle != null) Text(subtitle, color = MUTED, fontSize = 12.sp)
        }
        end()
    }
}

@Composable
private fun PrimaryButton(text: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CYAN, contentColor = BG)) {
        if (icon != null) { Icon(icon, null); Spacer(Modifier.width(10.dp)) }
        Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun HomeScreen(
    songs: List<Song>, recent: List<String>, favorites: Set<String>, scanning: Boolean,
    importFiles: () -> Unit, onRescan: () -> Unit, onClick: (Song) -> Unit,
    onFavorites: () -> Unit, onArtists: () -> Unit, onAlbums: () -> Unit, onAll: () -> Unit
) {
    val latest = remember(songs, recent) {
        val byId = songs.associateBy { it.id }
        val recentSongs = recent.mapNotNull(byId::get)
        (recentSongs + songs.sortedByDescending { it.dateAdded }).distinctBy { it.id }.take(7)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 22.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.node_player_logo), "NODE Player official logo",
                    Modifier.size(65.dp))
                Column(Modifier.weight(1f)) {
                    Text("NODE PLAYER", color = CYAN, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 1.sp)
                    Text("M O B I L E  •  YOUR MUSIC", color = MUTED, fontSize = 10.sp)
                }
                IconButton(onClick = onRescan) { Icon(Icons.Default.Refresh, "Refresh music library", tint = CYAN) }
            }
            Box(Modifier.padding(horizontal = 18.dp).fillMaxWidth().clip(pageShape)
                .background(Brush.horizontalGradient(listOf(Color(0xFF102E52), Color(0xFF071A32))))
                .padding(20.dp)) {
                Column {
                    Text("YOUR MUSIC. UNLIMITED.", color = WHITE, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("Completely offline, forever free.", color = MUTED, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("${songs.size} tracks ready to play", color = CYAN, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("YOUR LIBRARY", color = MUTED, fontSize = 11.sp, letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 19.dp, vertical = 8.dp))
            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    LibraryTile("All Songs", "${songs.size}", Icons.Default.MusicNote, Modifier.weight(1f), onAll)
                    LibraryTile("Artists", "${songs.map { it.artist }.distinct().size}", Icons.Default.Person, Modifier.weight(1f), onArtists)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    LibraryTile("Albums", "${songs.map { it.album to it.artist }.distinct().size}", Icons.Default.Album, Modifier.weight(1f), onAlbums)
                    LibraryTile("Favorites", "${favorites.size}", Icons.Default.Favorite, Modifier.weight(1f), onFavorites)
                }
            }
            Spacer(Modifier.height(19.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = importFiles, modifier = Modifier.weight(1f), shape = pageShape) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(5.dp)); Text("Import Music")
                }
                OutlinedButton(onClick = onRescan, shape = pageShape) { Icon(Icons.Default.Refresh, "Scan again") }
            }
            Row(Modifier.fillMaxWidth().padding(start = 19.dp, end = 19.dp, top = 22.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("RECENTLY ADDED / PLAYED", color = WHITE, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (scanning) CircularProgressIndicator(Modifier.size(16.dp), color = CYAN, strokeWidth = 2.dp)
            }
        }
        if (latest.isEmpty()) item { EmptyState("No music yet", "Import MP3 files or copy them into your Music folder.", Icons.Default.MusicOff) }
        items(latest, key = { it.id }) { song -> SongRow(song, onClick = { onClick(song) }) }
    }
}

@Composable
private fun LibraryTile(title: String, count: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(93.dp), shape = pageShape, color = CARD) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, tint = CYAN, modifier = Modifier.size(27.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(title, color = WHITE, modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(count, color = MUTED, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    songs: List<Song>, favorites: Set<String>, group: Pair<String, String>?, search: String,
    onSearch: (String) -> Unit, onGroup: (Pair<String, String>?) -> Unit,
    onPlay: (List<Song>, Song) -> Unit, onFavorite: (Song) -> Unit,
    onAdd: (Song) -> Unit, onImport: () -> Unit, onRefresh: () -> Unit,
    allowPermission: Boolean, requestPermission: () -> Unit
) {
    var sort by remember { mutableStateOf("Title") }
    var sortMenu by remember { mutableStateOf(false) }
    val matching = remember(songs, group, search, favorites, sort) {
        val filtered = songs.filter { song ->
            (group == null || when (group.first) {
                "Favorites" -> song.id in favorites
                "Artists" -> group.second == song.artist || group.second.isBlank()
                "Albums" -> group.second == "${song.album}—${song.artist}" || group.second.isBlank()
                else -> true
            }) && (search.isBlank() || song.title.contains(search, true) || song.artist.contains(search, true) || song.album.contains(search, true))
        }
        when (sort) {
            "Newest" -> filtered.sortedByDescending { it.dateAdded }
            "Artist" -> filtered.sortedBy { it.artist.lowercase(Locale.ROOT) }
            "Duration" -> filtered.sortedByDescending { it.durationMs }
            else -> filtered.sortedBy { it.title.lowercase(Locale.ROOT) }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Titlebar(group?.takeIf { it.second.isNotBlank() }?.second?.substringBefore('—') ?: group?.first ?: "All Songs",
            "${matching.size} tracks") {
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Rescan", tint = CYAN) }
            IconButton(onClick = onImport) { Icon(Icons.Default.Add, "Import songs", tint = CYAN) }
        }
        if (group != null) {
            TextButton(onClick = { onGroup(null) }, modifier = Modifier.padding(horizontal = 12.dp)) {
                Icon(Icons.Default.ArrowBack, null); Spacer(Modifier.width(5.dp)); Text("Back to all songs")
            }
        }
        SearchField(search, onSearch)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FilterChip(selected = group == null, onClick = { onGroup(null) }, label = { Text("Songs") })
            FilterChip(selected = group?.first == "Artists", onClick = { onGroup("Artists" to "") }, label = { Text("Artists") })
            FilterChip(selected = group?.first == "Albums", onClick = { onGroup("Albums" to "") }, label = { Text("Albums") })
            FilterChip(selected = group?.first == "Favorites", onClick = { onGroup("Favorites" to "") }, label = { Text("Favorites") })
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${matching.size} songs", color = MUTED, modifier = Modifier.weight(1f), fontSize = 12.sp)
            Box {
                TextButton(onClick = { sortMenu = true }) { Icon(Icons.Default.Sort, null); Spacer(Modifier.width(3.dp)); Text(sort) }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    listOf("Title", "Newest", "Artist", "Duration").forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { sort = option; sortMenu = false })
                    }
                }
            }
        }
        if (!allowPermission) {
            EmptyState("Library access required", "Allow music permission to read Internal Storage/Music.", Icons.Default.Folder)
            PrimaryButton("Allow Music Access", Icons.Default.Check, requestPermission)
        } else if (group?.first == "Artists" && group.second.isBlank()) {
            LazyColumn {
                items(songs.filter { it.artist.contains(search, true) }.groupBy { it.artist }.toSortedMap(String.CASE_INSENSITIVE_ORDER).toList()) { (artist, artistSongs) ->
                    GroupRow(artist, "${artistSongs.size} songs", Icons.Default.Person) { onGroup("Artists" to artist) }
                }
            }
        } else if (group?.first == "Albums" && group.second.isBlank()) {
            LazyColumn {
                items(songs.filter { it.album.contains(search, true) || it.artist.contains(search, true) }
                    .groupBy { "${it.album}—${it.artist}" }.toSortedMap(String.CASE_INSENSITIVE_ORDER).toList()) { (albumKey, albumSongs) ->
                    GroupRow(albumKey.substringBefore('—'), "${albumKey.substringAfter('—')} • ${albumSongs.size} songs", Icons.Default.Album) {
                        onGroup("Albums" to albumKey)
                    }
                }
            }
        } else if (matching.isEmpty()) {
            EmptyState("Nothing to play yet", "Add music with Import Music, or copy files into Music/.", Icons.Default.MusicOff)
        } else {
            LazyColumn {
                items(matching, key = { it.id }) { song ->
                    SongRow(song, onClick = { onPlay(matching, song) }, isFavorite = song.id in favorites,
                        onFavorite = { onFavorite(song) }, onAdd = { onAdd(song) })
                }
            }
        }
    }
}

@Composable
private fun GroupRow(name: String, detail: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, color = BG, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 19.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(CARD, pageShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = CYAN, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = WHITE, fontWeight = FontWeight.SemiBold)
                Text(detail, color = MUTED, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MUTED)
        }
    }
}

@Composable
private fun SearchField(text: String, onText: (String) -> Unit) {
    OutlinedTextField(value = text, onValueChange = onText,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        singleLine = true, label = { Text("Search title, artist or album") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = { if (text.isNotEmpty()) IconButton(onClick = { onText("") }) { Icon(Icons.Default.Close, "Clear") } },
        shape = pageShape)
}

@Composable
private fun SongArtwork(song: Song?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageFile by produceState<java.io.File?>(initialValue = null, song?.id) {
        value = song?.let { Artwork.file(context, it.uri) }
    }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(
        Brush.linearGradient(listOf(Color(0xFF175C95), Color(0xFF03101D), Color(0xFF0D2763)))),
        contentAlignment = Alignment.Center) {
        if (imageFile != null) AsyncImage(model = imageFile, contentDescription = "Album art", modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        else Icon(Icons.Default.MusicNote, null, modifier = Modifier.fillMaxSize(.48f), tint = CYAN)
    }
}

@Composable
private fun SongRow(
    song: Song, onClick: () -> Unit,
    isFavorite: Boolean = false, onFavorite: (() -> Unit)? = null, onAdd: (() -> Unit)? = null
) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        SongArtwork(song, Modifier.size(53.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = WHITE, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(song.artist, color = MUTED, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(timeText(song.durationMs), color = MUTED, fontSize = 11.sp)
        if (onFavorite != null || onAdd != null) {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Song actions", tint = MUTED) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (onFavorite != null) DropdownMenuItem(
                        text = { Text(if (isFavorite) "Remove Favorite" else "Add to Favorites") },
                        leadingIcon = { Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null) },
                        onClick = { menu = false; onFavorite() })
                    if (onAdd != null) DropdownMenuItem(
                        text = { Text("Add to Playlist") }, leadingIcon = { Icon(Icons.Default.QueueMusic, null) },
                        onClick = { menu = false; onAdd() })
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(song: Song, playing: Boolean, onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit) {
    Surface(color = Color(0xFF123256), modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 4.dp),
        shape = RoundedCornerShape(15.dp)) {
        Row(Modifier.height(67.dp).clickable(onClick = onOpen).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SongArtwork(song, Modifier.size(51.dp))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = MUTED, fontSize = 11.sp, maxLines = 1)
            }
            IconButton(onClick = onToggle) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Pause or play", tint = CYAN) }
            IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Next", tint = WHITE) }
        }
    }
}

@Composable
private fun NowPlaying(
    song: Song?, state: com.nodedevelopment.nodeplayermobile.playback.PlaybackState,
    favorites: Set<String>, onBack: () -> Unit,
    onToggle: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit,
    onShuffle: () -> Unit, onRepeat: () -> Unit, onSeek: (Long) -> Unit, onFavorite: () -> Unit
) {
    var slider by remember { mutableFloatStateOf(0f) }
    var moving by remember { mutableStateOf(false) }
    LaunchedEffect(state.position, state.duration, moving) {
        if (!moving) slider = if (state.duration > 0) (state.position.toFloat() / state.duration).coerceIn(0f, 1f) else 0f
    }
    Column(Modifier.fillMaxSize().background(PLAYER_GRAD).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, "Close now playing") }
            Text("NOW PLAYING", Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp)
            Spacer(Modifier.width(48.dp))
        }
        Spacer(Modifier.height(28.dp))
        SongArtwork(song, Modifier.fillMaxWidth(.92f).aspectRatio(1f).shadow(18.dp, pageShape))
        Spacer(Modifier.height(34.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song?.title ?: "Choose a song", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(song?.artist ?: "NODE Player", color = MUTED, fontSize = 14.sp)
            }
            IconButton(enabled = song != null, onClick = onFavorite) {
                Icon(if (song != null && song.id in favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    "Favorite", tint = if (song != null && song.id in favorites) CYAN else MUTED)
            }
        }
        Spacer(Modifier.height(19.dp))
        Slider(value = slider, onValueChange = { slider = it; moving = true },
            onValueChangeFinished = { onSeek((slider * state.duration).toLong()); moving = false },
            enabled = state.duration > 0, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(timeText(if (moving) (slider * state.duration).toLong() else state.position), color = MUTED, fontSize = 12.sp)
            Text(timeText(state.duration), color = MUTED, fontSize = 12.sp)
        }
        Spacer(Modifier.height(17.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onShuffle) { Icon(Icons.Default.Shuffle, "Shuffle", tint = if (state.shuffle) CYAN else MUTED) }
            IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(34.dp), tint = WHITE) }
            FilledIconButton(onClick = onToggle, modifier = Modifier.size(73.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = CYAN, contentColor = BG)) {
                Icon(if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play pause", Modifier.size(35.dp))
            }
            IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Next", Modifier.size(34.dp), tint = WHITE) }
            IconButton(onClick = onRepeat) { Icon(if (state.repeat == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                "Repeat mode", tint = if (state.repeat != Player.REPEAT_MODE_OFF) CYAN else MUTED) }
        }
        Spacer(Modifier.height(45.dp))
        Text("NODE PLAYER  •  THE CLASSIC MUSIC EXPERIENCE", color = MUTED, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

private fun timeText(time: Long): String {
    if (time <= 0) return "0:00"
    val seconds = time / 1000
    return "%d:%02d".format(Locale.ROOT, seconds / 60, seconds % 60)
}

@Composable
private fun PlaylistScreen(
    vm: MainViewModel,
    allSongs: List<Song>, playlists: List<MusicPlaylist>, selected: MusicPlaylist?,
    onOpen: (MusicPlaylist?) -> Unit, onPlay: (List<Song>, Song) -> Unit,
    onAdd: (Song) -> Unit, onFavorite: (Song) -> Unit, favorites: Set<String>
) {
    var createDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var selectedIds by remember(selected?.id, playlists) { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(selected?.id, playlists) {
        if (selected != null) selectedIds = vm.playlistSongs(selected.id)
    }
    val tracks = remember(allSongs, selectedIds) { allSongs.filter { it.id in selectedIds } }
    Column(Modifier.fillMaxSize()) {
        if (selected == null) {
            Titlebar("My Playlists", "Organize your music")
            Box(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                PrimaryButton("Create Playlist", Icons.Default.Add) { createDialog = true }
            }
            if (playlists.isEmpty()) EmptyState("Make your first playlist", "Create a playlist, then add songs from the Library.", Icons.Default.QueueMusic)
            else LazyColumn {
                items(playlists, key = { it.id }) { playlist ->
                    GroupRow(playlist.name, "${playlist.songCount} songs", Icons.Default.QueueMusic) { onOpen(playlist) }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onOpen(null) }) { Icon(Icons.Default.ArrowBack, "Back") }
                Column(Modifier.weight(1f)) {
                    Text(selected.name, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                    Text("${tracks.size} songs", color = MUTED, fontSize = 12.sp)
                }
                IconButton(onClick = { deleteDialog = true }) { Icon(Icons.Default.Delete, "Delete playlist", tint = MUTED) }
            }
            Text("Choose a song to start playing. Add more from Library → ⋮ → Add to Playlist.",
                color = MUTED, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 19.dp, vertical = 10.dp))
            if (tracks.isEmpty()) EmptyState("Empty playlist", "Browse Library and use the song's menu to add it here.", Icons.Default.QueueMusic)
            else LazyColumn {
                items(tracks, key = { it.id }) { track ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            SongRow(track, onClick = { onPlay(tracks, track) },
                                isFavorite = track.id in favorites,
                                onFavorite = { onFavorite(track) }, onAdd = { onAdd(track) })
                        }
                        IconButton(onClick = {
                            vm.removeFromPlaylist(selected.id, track)
                            selectedIds = selectedIds - track.id
                        }) { Icon(Icons.Default.RemoveCircleOutline, "Remove from playlist", tint = MUTED) }
                    }
                }
            }
        }
    }
    if (createDialog) AlertDialog(
        onDismissRequest = { createDialog = false },
        title = { Text("New Playlist") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it.take(70) },
            label = { Text("Playlist name") }, singleLine = true) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = {
            vm.createPlaylist(name); name = ""; createDialog = false
        }) { Text("Create") } },
        dismissButton = { TextButton(onClick = { createDialog = false }) { Text("Cancel") } })
    if (deleteDialog) AlertDialog(onDismissRequest = { deleteDialog = false },
        title = { Text("Delete Playlist?") },
        text = { Text("This removes the playlist, not the original music files.") },
        confirmButton = { TextButton(onClick = {
            selected?.let { vm.deletePlaylist(it.id) }; onOpen(null); deleteDialog = false
        }) { Text("Delete", color = Color(0xFFFF7777)) } },
        dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("Keep") } })
}

@Composable
private fun SettingsScreen(
    vm: MainViewModel, folder: String?,
    onChooseFolder: () -> Unit, onDefaultFolder: () -> Unit,
    onImport: () -> Unit, onUpdate: () -> Unit, onPermission: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Titlebar("Settings", "Your player, your preferences")
        SettingsSection("MUSIC LIBRARY")
        SettingOption(Icons.Default.Folder, "Music Folder",
            if (folder == null) "Internal Storage / Music (default)" else "Custom folder selected",
            onChooseFolder)
        SettingOption(Icons.Default.Restore, "Restore Default Folder", "Use Android's Music folder", onDefaultFolder)
        SettingOption(Icons.Default.Add, "Import Music", "Copy downloaded audio into your Music library", onImport)
        SettingOption(Icons.Default.Refresh, "Rescan Library", "Find new songs", vm::refresh)
        SettingsSection("PERMISSIONS & UPDATES")
        SettingOption(Icons.Default.Security, "Music Permission", "Allow access to shared music files", onPermission)
        SettingOption(Icons.Default.SystemUpdate, "Check for Updates", "Updates are provided through GitHub Releases", onUpdate)
        SettingsSection("ABOUT")
        Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.node_player_logo), "Official NODE Player logo", Modifier.size(84.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text("NODE PLAYER MOBILE", fontWeight = FontWeight.ExtraBold, color = CYAN, fontSize = 16.sp)
                Text("Version ${BuildConfig.VERSION_NAME}", color = WHITE)
                Text("by NODE Development", color = MUTED, fontSize = 12.sp)
                Text("Offline • Free • No accounts", color = MUTED, fontSize = 12.sp)
            }
        }
        Text("Music stays on your device. Internet is used only to check and download optional app updates.",
            color = MUTED, fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 23.dp, vertical = 10.dp))
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(title, color = CYAN, letterSpacing = 1.3.sp, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 19.dp, top = 23.dp, bottom = 8.dp))
}

@Composable
private fun SettingOption(icon: ImageVector, title: String, detail: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = CARD_2, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 3.dp),
        shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = CYAN, modifier = Modifier.size(23.dp))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = WHITE, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(detail, color = MUTED, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MUTED)
        }
    }
}

@Composable
private fun EmptyState(title: String, detail: String, icon: ImageVector) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, modifier = Modifier.size(60.dp), tint = Color(0xFF2F729D))
        Spacer(Modifier.height(12.dp))
        Text(title, color = WHITE, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(5.dp))
        Text(detail, color = MUTED, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
