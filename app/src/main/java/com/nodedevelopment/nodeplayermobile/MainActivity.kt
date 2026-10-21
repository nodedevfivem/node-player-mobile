package com.nodedevelopment.nodeplayermobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nodedevelopment.nodeplayermobile.ui.NodeApp

class MainActivity : ComponentActivity() {
    private var observer: ContentObserver? = null
    private var activeViewModel: MainViewModel? = null
    private val scanHandler = Handler(Looper.getMainLooper())
    private val scanRunnable = Runnable { activeViewModel?.refresh() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(3, 9, 20)
        window.navigationBarColor = android.graphics.Color.rgb(3, 9, 20)
        setContent {
            val vm: MainViewModel = viewModel()
            SideEffect { activeViewModel = vm }
            val mediaPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
                else Manifest.permission.READ_EXTERNAL_STORAGE
            var granted by remember {
                mutableStateOf(ContextCompat.checkSelfPermission(this, mediaPermission) == PackageManager.PERMISSION_GRANTED)
            }
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
                granted = ok
                if (ok) vm.refresh()
            }
            val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folder ->
                if (folder != null) {
                    try { contentResolver.takePersistableUriPermission(folder,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
                    catch (_: SecurityException) {
                        try { contentResolver.takePersistableUriPermission(folder, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                        catch (_: SecurityException) { /* Provider may not offer persistence. */ }
                    }
                    vm.setFolder(folder)
                }
            }
            val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
                if (uris.isNotEmpty()) vm.import(uris)
            }
            LaunchedEffect(Unit) { vm.checkForUpdates() }
            NodeApp(
                vm = vm,
                hasPermission = granted || vm.repository.folder != null,
                requestPermission = { permissionLauncher.launch(mediaPermission) },
                chooseFolder = { folderLauncher.launch(null) },
                defaultFolder = { vm.setFolder(null); if (!granted) permissionLauncher.launch(mediaPermission) },
                importFiles = { importLauncher.launch(arrayOf("audio/*", "application/ogg", "application/octet-stream")) },
                openIntent = { intent -> try { startActivity(intent) } catch (e: Exception) { vm.say("Cannot launch installer: ${e.message}") } }
            )
        }
    }
    override fun onResume() {
        super.onResume()
        // Return from Android's "Install unknown apps" permission screen:
        // continue the verified download without making the user fetch it again.
        activeViewModel?.let { vm ->
            try {
                vm.updater.resumePendingInstall()?.let { startActivity(it) }
            } catch (e: Exception) {
                vm.say("Cannot launch update installer: ${e.message}")
            }
        }
    }
    override fun onStart() {
        super.onStart()
        val observerLocal = object : ContentObserver(scanHandler) {
            override fun onChange(selfChange: Boolean) {
                scanHandler.removeCallbacks(scanRunnable)
                scanHandler.postDelayed(scanRunnable, 900)
            }
        }
        observer = observerLocal
        activeViewModel?.refresh() // New files could have arrived while the app was backgrounded.
        try { contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observerLocal) }
        catch (_: SecurityException) { observer = null }
    }
    override fun onStop() {
        observer?.let { contentResolver.unregisterContentObserver(it) }
        observer = null
        scanHandler.removeCallbacks(scanRunnable)
        super.onStop()
    }
    override fun onDestroy() {
        activeViewModel = null
        super.onDestroy()
    }
}
