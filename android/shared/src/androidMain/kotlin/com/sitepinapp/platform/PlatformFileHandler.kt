package com.sitepinapp.platform

import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual object PlatformFileHandler {
    private lateinit var context: android.content.Context

    fun init(context: android.content.Context) {
        this.context = context.applicationContext
    }

    actual suspend fun saveToCache(fileName: String, data: ByteArray): String = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, fileName)
        file.writeBytes(data)
        file.absolutePath
    }

    actual suspend fun saveToCacheString(fileName: String, content: String): String = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, fileName)
        file.writeText(content)
        file.absolutePath
    }

    actual suspend fun readFileBytes(path: String): ByteArray? = withContext(Dispatchers.IO) {
        try { File(path).readBytes() } catch (e: Exception) { null }
    }

    actual suspend fun shareFile(path: String, mimeType: String) {
        val file = File(path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
