package com.sitepinapp.data.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class ProjectExport(
    /** Format version for cross-platform compatibility. Increment on schema changes. */
    val formatVersion: Int = 2,
    val name: String,
    val syncID: String,
    val createdAt: String,
    /** Who exported this project (display name) */
    val exportedBy: String = "",
    /** When the export was created (ISO8601) */
    val exportedAt: String = "",
    val documents: List<DocumentExport>
)

@Serializable
data class DocumentExport(
    val name: String,
    val syncID: String,
    val fileType: String,
    val pageCount: Int,
    val createdAt: String,
    val fileData: String,
    val pins: List<PinExport>
)

@Serializable
data class PinExport(
    val syncID: String,
    val relativeX: Double,
    val relativeY: Double,
    val pageIndex: Int,
    val title: String,
    val pinDescription: String,
    val location: String,
    val height: String,
    val width: String,
    val status: String,
    val category: String,
    val author: String,
    val createdAt: String,
    val modifiedAt: String,
    val photos: List<PhotoExport>,
    val comments: List<CommentExport>
)

@Serializable
data class PhotoExport(
    val imageData: String,
    val caption: String,
    val createdAt: String,
    val syncID: String = ""
)

@Serializable
data class CommentExport(
    val text: String,
    val author: String,
    val createdAt: String,
    val syncID: String = ""
)

object DateUtils {
    fun toISO8601(epochMillis: Long): String {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        return instant.toString()
    }

    fun fromISO8601(dateString: String): Long {
        return try {
            Instant.parse(dateString).toEpochMilliseconds()
        } catch (e: Exception) {
            kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun encodeBase64(data: ByteArray): String = Base64.encode(data)

    @OptIn(ExperimentalEncodingApi::class)
    fun decodeBase64(encoded: String): ByteArray = Base64.decode(encoded)
}
