package com.nodedevelopment.nodeplayermobile.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.nodedevelopment.nodeplayermobile.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

/** GitHub Releases, not a remote database. Installation always requires Android user approval. */
data class AvailableUpdate(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val checksumUrl: String
)

class UpdateManager(private val context: Context) {
    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T =
        try { block(this) } finally { disconnect() }

    private fun connection(url: String): HttpURLConnection {
        val parsed = URL(url)
        require(parsed.protocol == "https") { "HTTPS is required" }
        require(parsed.host.lowercase(Locale.ROOT) in setOf(
            "api.github.com", "github.com", "objects.githubusercontent.com",
            "release-assets.githubusercontent.com"
        )) { "Untrusted update host" }
        return (parsed.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "NODE-Player-Mobile/${BuildConfig.VERSION_NAME}")
            setRequestProperty("Accept", "application/vnd.github+json")
        }
    }
    private fun openTrusted(url: String, maxRedirects: Int = 5): HttpURLConnection {
        var current = url
        repeat(maxRedirects + 1) { hop ->
            val conn = connection(current)
            val status = conn.responseCode
            if (status in 300..399) {
                val location = conn.getHeaderField("Location") ?: error("Missing redirect location")
                val dest = URL(URL(current), location)
                conn.disconnect()
                if (hop == maxRedirects) error("Too many redirects")
                current = dest.toExternalForm()
            } else {
                if (status !in 200..299) {
                    conn.disconnect()
                    error("GitHub returned HTTP $status")
                }
                return conn
            }
        }
        error("Too many redirects")
    }
    suspend fun check(): AvailableUpdate? = withContext(Dispatchers.IO) {
        val repo = BuildConfig.GITHUB_REPO
        val owner = BuildConfig.GITHUB_OWNER
        val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
        val release = openTrusted(url).use { it.inputStream.bufferedReader().readText() }
        val json = JSONObject(release)
        val tag = json.getString("tag_name").removePrefix("v")
        if (compareVersion(tag, BuildConfig.VERSION_NAME) <= 0) return@withContext null
        val assets = json.getJSONArray("assets")
        var apk: String? = null
        var checksum: String? = null
        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            val name = asset.optString("name")
            if (name.endsWith(".apk", true)) apk = asset.getString("browser_download_url")
            if (name.endsWith(".apk.sha256", true)) checksum = asset.getString("browser_download_url")
        }
        // Do not offer unverifiable downloads.
        if (apk == null || checksum == null) error("New release is missing APK or .apk.sha256 asset")
        AvailableUpdate(tag, json.optString("body").take(2000), apk, checksum)
    }
    private fun compareVersion(a: String, b: String): Int {
        val av = a.split('.').map { it.toIntOrNull() ?: return 0 }
        val bv = b.split('.').map { it.toIntOrNull() ?: return 0 }
        for (i in 0 until maxOf(av.size, bv.size)) {
            val diff = (av.getOrElse(i) { 0 }).compareTo(bv.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }
    suspend fun download(update: AvailableUpdate, progress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val sha = openTrusted(update.checksumUrl).use { conn ->
            conn.inputStream.bufferedReader().use { it.readText().trim().split(Regex("\\s+"))[0] }
        }
        require(sha.matches(Regex("[a-fA-F0-9]{64}"))) { "Invalid release SHA-256" }
        val updates = File(context.cacheDir, "updates").apply { mkdirs() }
        val apk = File(updates, "NODE-Player-Mobile-${update.version}.apk")
        val partial = File(updates, "${apk.name}.partial")
        try {
            openTrusted(update.apkUrl).use { conn ->
                val total = conn.contentLengthLong
                val digest = MessageDigest.getInstance("SHA-256")
                var readBytes = 0L
                conn.inputStream.use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val length = input.read(buffer)
                            if (length < 0) break
                            readBytes += length
                            require(readBytes <= 200L * 1024 * 1024) { "APK exceeds 200 MB" }
                            digest.update(buffer, 0, length)
                            output.write(buffer, 0, length)
                            if (total > 0) progress((readBytes * 100 / total).toInt().coerceIn(0, 100))
                        }
                    }
                }
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                require(actual.equals(sha, true)) { "SHA-256 mismatch: download rejected" }
            }
            if (apk.exists()) apk.delete()
            require(partial.renameTo(apk)) { "Could not finalize APK" }
            progress(100)
            apk
        } finally { partial.delete() }
    }
    /** User must allow unknown-app installation for GitHub sideloaded APKs. */
    fun startInstall(apk: File): Intent {
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            return Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"))
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
