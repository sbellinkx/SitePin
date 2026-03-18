package com.sitepinapp

import com.sitepinapp.data.model.*
import com.sitepinapp.platform.ImportLimits
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ImportValidationTest {

    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    @Test
    fun testImportLimitsAreReasonable() {
        assertTrue(ImportLimits.MAX_DOCUMENT_SIZE_BYTES > 0)
        assertTrue(ImportLimits.MAX_IMPORT_SIZE_BYTES > ImportLimits.MAX_DOCUMENT_SIZE_BYTES)
        assertTrue(ImportLimits.MAX_PHOTOS_PER_PIN > 0)
    }

    @Test
    fun testImportLimitsValues() {
        assertEquals(50 * 1024 * 1024, ImportLimits.MAX_DOCUMENT_SIZE_BYTES)
        assertEquals(100 * 1024 * 1024, ImportLimits.MAX_IMPORT_SIZE_BYTES)
        assertEquals(20, ImportLimits.MAX_PHOTOS_PER_PIN)
    }

    @Test
    fun testProjectExportSerializesCorrectly() {
        val export = ProjectExport(
            name = "Test Project",
            syncID = "abc-123",
            createdAt = "2024-01-01T00:00:00Z",
            documents = emptyList()
        )
        val jsonStr = json.encodeToString(ProjectExport.serializer(), export)
        val decoded = json.decodeFromString(ProjectExport.serializer(), jsonStr)
        assertEquals("Test Project", decoded.name)
        assertEquals("abc-123", decoded.syncID)
        assertEquals(0, decoded.documents.size)
    }

    @Test
    fun testPinExportWithPhotosSerializes() {
        val pin = PinExport(
            syncID = "pin-1",
            relativeX = 0.5,
            relativeY = 0.5,
            pageIndex = 0,
            title = "Test Pin",
            pinDescription = "Description",
            location = "Room 101",
            height = "2.5m",
            width = "1.0m",
            status = "open",
            category = "general",
            author = "Tester",
            createdAt = "2024-01-01T00:00:00Z",
            modifiedAt = "2024-01-01T00:00:00Z",
            photos = listOf(
                PhotoExport(imageData = "dGVzdA==", caption = "Photo 1", createdAt = "2024-01-01T00:00:00Z", syncID = "photo-1")
            ),
            comments = listOf(
                CommentExport(text = "Hello", author = "Tester", createdAt = "2024-01-01T00:00:00Z", syncID = "comment-1")
            )
        )
        val jsonStr = json.encodeToString(PinExport.serializer(), pin)
        val decoded = json.decodeFromString(PinExport.serializer(), jsonStr)
        assertEquals(1, decoded.photos.size)
        assertEquals(1, decoded.comments.size)
        assertEquals("Photo 1", decoded.photos[0].caption)
        assertEquals("Hello", decoded.comments[0].text)
    }

    @Test
    fun testDateUtilsRoundTrip() {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val iso = DateUtils.toISO8601(now)
        val back = DateUtils.fromISO8601(iso)
        // Allow 1ms difference due to precision
        assertTrue(kotlin.math.abs(now - back) < 2, "Round trip should preserve epoch millis")
    }

    @Test
    fun testDateUtilsInvalidFallback() {
        val result = DateUtils.fromISO8601("not-a-date")
        assertTrue(result > 0, "Invalid date should fall back to current time")
    }

    @Test
    fun testBase64RoundTrip() {
        val original = byteArrayOf(1, 2, 3, 4, 5)
        val encoded = DateUtils.encodeBase64(original)
        val decoded = DateUtils.decodeBase64(encoded)
        assertTrue(original.contentEquals(decoded))
    }

    @Test
    fun testPhotoExportLimitConstant() {
        // Verify that the constant matches across both platforms
        assertEquals(20, ImportLimits.MAX_PHOTOS_PER_PIN)
    }
}
