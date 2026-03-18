package com.sitepinapp.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
actual object PlatformFileHandler {
    private fun getCacheDir(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        return paths.firstOrNull() as? String ?: NSTemporaryDirectory()
    }

    actual suspend fun saveToCache(fileName: String, data: ByteArray): String {
        val path = "${getCacheDir()}/$fileName"
        val nsData = data.toNSData()
        nsData.writeToFile(path, atomically = true)
        return path
    }

    actual suspend fun saveToCacheString(fileName: String, content: String): String {
        val path = "${getCacheDir()}/$fileName"
        @Suppress("CAST_NEVER_SUCCEEDS")
        (content as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        return path
    }

    actual suspend fun readFileBytes(path: String): ByteArray? {
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        return data.toByteArray()
    }

    actual suspend fun shareFile(path: String, mimeType: String) {
        // On iOS, sharing is handled via UIActivityViewController
        val url = platform.Foundation.NSURL.fileURLWithPath(path)
        val activityVC = platform.UIKit.UIActivityViewController(
            activityItems = listOf(url),
            applicationActivities = null
        )
        val rootVC = platform.UIKit.UIApplication.sharedApplication.keyWindow?.rootViewController
        rootVC?.presentViewController(activityVC, animated = true, completion = null)
    }

    private fun ByteArray.toNSData(): NSData {
        return this.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        val size = this.length.toInt()
        val bytes = ByteArray(size)
        bytes.usePinned { pinned ->
            this.getBytes(pinned.addressOf(0), length = this.length)
        }
        return bytes
    }
}
