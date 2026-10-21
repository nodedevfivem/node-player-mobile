package com.nodedevelopment.nodeplayermobile.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** Lazily extract embedded artwork on IO, cache scaled 480px images on disk. */
object Artwork {
    private val absent = LruCache<String, Boolean>(1000)
    suspend fun file(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        val key = MessageDigest.getInstance("SHA-256").digest(uri.toString().toByteArray())
            .joinToString("") { "%02x".format(it) }
        val dir = File(context.cacheDir, "artwork").apply { mkdirs() }
        val output = File(dir, "$key.jpg")
        if (output.exists() && output.length() > 0) return@withContext output
        if (absent.get(key) == true) return@withContext null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: run { absent.put(key, true); return@withContext null }
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            var scale = 1
            while (opts.outWidth / scale > 480 || opts.outHeight / scale > 480) scale *= 2
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                BitmapFactory.Options().apply { inSampleSize = scale }) ?: return@withContext null
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            bitmap.recycle()
            output
        } catch (_: Exception) { absent.put(key, true); null }
        finally { runCatching { retriever.release() } }
    }
}
