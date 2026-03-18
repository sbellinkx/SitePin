package com.sitepinapp.platform

expect object PlatformFileHandler {
    suspend fun saveToCache(fileName: String, data: ByteArray): String
    suspend fun saveToCacheString(fileName: String, content: String): String
    suspend fun readFileBytes(path: String): ByteArray?
    suspend fun shareFile(path: String, mimeType: String)
}
