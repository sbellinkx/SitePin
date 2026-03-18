package com.sitepinapp

import com.sitepinapp.data.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*

class ExportFormatTest {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Test
    fun exportJsonContainsExpectedTopLevelFields() {
        val export = createMinimalExport()
        val jsonString = json.encodeToString(ProjectExport.serializer(), export)
        val obj = Json.parseToJsonElement(jsonString).jsonObject
        assertTrue("name" in obj)
        assertTrue("syncID" in obj)
        assertTrue("createdAt" in obj)
        assertTrue("documents" in obj)
    }

    @Test
    fun exportJsonDocumentHasRequiredFields() {
        val export = createMinimalExport()
        val jsonString = json.encodeToString(ProjectExport.serializer(), export)
        val obj = Json.parseToJsonElement(jsonString).jsonObject
        val doc = obj["documents"]!!.jsonArray.first().jsonObject
        assertTrue("name" in doc)
        assertTrue("syncID" in doc)
        assertTrue("fileType" in doc)
        assertTrue("pageCount" in doc)
        assertTrue("fileData" in doc)
        assertTrue("pins" in doc)
    }

    @Test
    fun exportJsonPinHasRequiredFields() {
        val export = createExportWithPin()
        val jsonString = json.encodeToString(ProjectExport.serializer(), export)
        val obj = Json.parseToJsonElement(jsonString).jsonObject
        val pin = obj["documents"]!!.jsonArray.first().jsonObject["pins"]!!.jsonArray.first().jsonObject

        val requiredFields = listOf(
            "syncID", "relativeX", "relativeY", "pageIndex",
            "title", "pinDescription", "location", "height", "width",
            "status", "category", "author", "createdAt", "modifiedAt",
            "photos", "comments"
        )
        for (field in requiredFields) {
            assertTrue(field in pin, "Pin missing field: $field")
        }
    }

    @Test
    fun exportJsonCommentHasSyncID() {
        val export = createExportWithComment()
        val jsonString = json.encodeToString(ProjectExport.serializer(), export)
        val obj = Json.parseToJsonElement(jsonString).jsonObject
        val comment = obj["documents"]!!.jsonArray.first().jsonObject["pins"]!!.jsonArray.first().jsonObject["comments"]!!.jsonArray.first().jsonObject
        assertTrue("syncID" in comment, "Comment must have syncID field")
        assertTrue("text" in comment)
        assertTrue("author" in comment)
        assertTrue("createdAt" in comment)
    }

    @Test
    fun exportJsonPhotoHasSyncID() {
        val export = createExportWithPhoto()
        val jsonString = json.encodeToString(ProjectExport.serializer(), export)
        val obj = Json.parseToJsonElement(jsonString).jsonObject
        val photo = obj["documents"]!!.jsonArray.first().jsonObject["pins"]!!.jsonArray.first().jsonObject["photos"]!!.jsonArray.first().jsonObject
        assertTrue("syncID" in photo, "Photo must have syncID field")
        assertTrue("imageData" in photo)
        assertTrue("caption" in photo)
        assertTrue("createdAt" in photo)
    }

    @Test
    fun base64FileDataIsValidString() {
        val export = createMinimalExport()
        val jsonString = json.encodeToString(ProjectExport.serializer(), export)
        val obj = Json.parseToJsonElement(jsonString).jsonObject
        val fileData = obj["documents"]!!.jsonArray.first().jsonObject["fileData"]!!.jsonPrimitive.content
        // Base64 should only contain valid chars
        assertTrue(fileData.all { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' })
    }

    @Test
    fun createdAtIsISO8601Format() {
        val export = createMinimalExport()
        val jsonString = json.encodeToString(ProjectExport.serializer(), export)
        val obj = Json.parseToJsonElement(jsonString).jsonObject
        val createdAt = obj["createdAt"]!!.jsonPrimitive.content
        assertTrue(createdAt.contains("T"), "createdAt should be ISO8601 with T separator")
        assertTrue(createdAt.contains("Z") || createdAt.contains("+"), "createdAt should have timezone")
    }

    @Test
    fun backwardCompatibilityWithoutSyncID() {
        // Simulate old .sitepin format without syncID on comments/photos
        val oldFormatJson = """
            {
                "text": "old comment",
                "author": "olduser",
                "createdAt": "2024-01-01T00:00:00Z"
            }
        """.trimIndent()
        val comment = json.decodeFromString(CommentExport.serializer(), oldFormatJson)
        assertEquals("old comment", comment.text)
        assertEquals("", comment.syncID, "Missing syncID should default to empty string")
    }

    @Test
    fun backwardCompatibilityPhotoWithoutSyncID() {
        val oldFormatJson = """
            {
                "imageData": "AQID",
                "caption": "old photo",
                "createdAt": "2024-01-01T00:00:00Z"
            }
        """.trimIndent()
        val photo = json.decodeFromString(PhotoExport.serializer(), oldFormatJson)
        assertEquals("old photo", photo.caption)
        assertEquals("", photo.syncID, "Missing syncID should default to empty string")
    }

    // ── Helpers ──

    private fun createMinimalExport() = ProjectExport(
        name = "Test", syncID = "s1", createdAt = "2024-01-01T00:00:00Z",
        documents = listOf(
            DocumentExport(
                name = "Doc", syncID = "d1", fileType = "application/pdf",
                pageCount = 1, createdAt = "2024-01-01T00:00:00Z",
                fileData = DateUtils.encodeBase64(byteArrayOf(1, 2, 3)),
                pins = emptyList()
            )
        )
    )

    private fun createExportWithPin() = ProjectExport(
        name = "Test", syncID = "s1", createdAt = "2024-01-01T00:00:00Z",
        documents = listOf(
            DocumentExport(
                name = "Doc", syncID = "d1", fileType = "pdf", pageCount = 1,
                createdAt = "2024-01-01T00:00:00Z",
                fileData = DateUtils.encodeBase64(byteArrayOf(1)),
                pins = listOf(
                    PinExport(
                        syncID = "p1", relativeX = 0.5, relativeY = 0.5,
                        pageIndex = 0, title = "T", pinDescription = "D",
                        location = "L", height = "H", width = "W",
                        status = "open", category = "general", author = "A",
                        createdAt = "2024-01-01T00:00:00Z", modifiedAt = "2024-01-01T00:00:00Z",
                        photos = emptyList(), comments = emptyList()
                    )
                )
            )
        )
    )

    private fun createExportWithComment() = ProjectExport(
        name = "Test", syncID = "s1", createdAt = "2024-01-01T00:00:00Z",
        documents = listOf(
            DocumentExport(
                name = "Doc", syncID = "d1", fileType = "pdf", pageCount = 1,
                createdAt = "2024-01-01T00:00:00Z",
                fileData = DateUtils.encodeBase64(byteArrayOf(1)),
                pins = listOf(
                    PinExport(
                        syncID = "p1", relativeX = 0.5, relativeY = 0.5,
                        pageIndex = 0, title = "T", pinDescription = "D",
                        location = "L", height = "H", width = "W",
                        status = "open", category = "general", author = "A",
                        createdAt = "2024-01-01T00:00:00Z", modifiedAt = "2024-01-01T00:00:00Z",
                        photos = emptyList(),
                        comments = listOf(
                            CommentExport(text = "Fix it", author = "Bob", createdAt = "2024-01-01T00:00:00Z", syncID = "c1")
                        )
                    )
                )
            )
        )
    )

    private fun createExportWithPhoto() = ProjectExport(
        name = "Test", syncID = "s1", createdAt = "2024-01-01T00:00:00Z",
        documents = listOf(
            DocumentExport(
                name = "Doc", syncID = "d1", fileType = "pdf", pageCount = 1,
                createdAt = "2024-01-01T00:00:00Z",
                fileData = DateUtils.encodeBase64(byteArrayOf(1)),
                pins = listOf(
                    PinExport(
                        syncID = "p1", relativeX = 0.5, relativeY = 0.5,
                        pageIndex = 0, title = "T", pinDescription = "D",
                        location = "L", height = "H", width = "W",
                        status = "open", category = "general", author = "A",
                        createdAt = "2024-01-01T00:00:00Z", modifiedAt = "2024-01-01T00:00:00Z",
                        comments = emptyList(),
                        photos = listOf(
                            PhotoExport(imageData = DateUtils.encodeBase64(byteArrayOf(1, 2)), caption = "Photo", createdAt = "2024-01-01T00:00:00Z", syncID = "ph1")
                        )
                    )
                )
            )
        )
    )
}
