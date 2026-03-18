package com.sitepinapp

import com.sitepinapp.data.model.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class SerializationTest {

    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    // ── ProjectExport round-trip ──

    @Test
    fun projectExportSerializationRoundTrip() {
        val export = createSampleProjectExport()
        val jsonString = json.encodeToString(ProjectExport.serializer(), export)
        val decoded = json.decodeFromString(ProjectExport.serializer(), jsonString)

        assertEquals(export.name, decoded.name)
        assertEquals(export.syncID, decoded.syncID)
        assertEquals(export.createdAt, decoded.createdAt)
        assertEquals(export.documents.size, decoded.documents.size)
    }

    @Test
    fun documentExportPreservesAllFields() {
        val export = createSampleProjectExport()
        val jsonString = json.encodeToString(ProjectExport.serializer(), export)
        val decoded = json.decodeFromString(ProjectExport.serializer(), jsonString)

        val doc = decoded.documents.first()
        assertEquals("Floor Plan", doc.name)
        assertEquals("doc-sync-1", doc.syncID)
        assertEquals("application/pdf", doc.fileType)
        assertEquals(3, doc.pageCount)
    }

    @Test
    fun pinExportPreservesCoordinates() {
        val export = createSampleProjectExport()
        val jsonString = json.encodeToString(ProjectExport.serializer(), export)
        val decoded = json.decodeFromString(ProjectExport.serializer(), jsonString)

        val pin = decoded.documents.first().pins.first()
        assertEquals(0.25, pin.relativeX)
        assertEquals(0.75, pin.relativeY)
        assertEquals(2, pin.pageIndex)
        assertEquals("pin-sync-1", pin.syncID)
    }

    @Test
    fun commentExportPreservesFields() {
        val export = createSampleProjectExport()
        val jsonString = json.encodeToString(ProjectExport.serializer(), export)
        val decoded = json.decodeFromString(ProjectExport.serializer(), jsonString)

        val comment = decoded.documents.first().pins.first().comments.first()
        assertEquals("Fix the crack", comment.text)
        assertEquals("Jane", comment.author)
        assertEquals("comment-sync-1", comment.syncID)
    }

    @Test
    fun photoExportPreservesFields() {
        val export = createSampleProjectExport()
        val jsonString = json.encodeToString(ProjectExport.serializer(), export)
        val decoded = json.decodeFromString(ProjectExport.serializer(), jsonString)

        val photo = decoded.documents.first().pins.first().photos.first()
        assertEquals("Wall crack", photo.caption)
        assertEquals("photo-sync-1", photo.syncID)
        assertTrue(photo.imageData.isNotEmpty())
    }

    // ── Edge cases ──

    @Test
    fun emptyDocumentsList() {
        val export = ProjectExport(
            name = "Empty", syncID = "s1", createdAt = "2024-01-01T00:00:00Z",
            documents = emptyList()
        )
        val jsonString = json.encodeToString(ProjectExport.serializer(), export)
        val decoded = json.decodeFromString(ProjectExport.serializer(), jsonString)
        assertTrue(decoded.documents.isEmpty())
    }

    @Test
    fun emptyPinsAndCommentsAndPhotos() {
        val doc = DocumentExport(
            name = "Doc", syncID = "d1", fileType = "pdf", pageCount = 1,
            createdAt = "2024-01-01T00:00:00Z", fileData = "AQID",
            pins = emptyList()
        )
        val export = ProjectExport(
            name = "P", syncID = "p1", createdAt = "2024-01-01T00:00:00Z",
            documents = listOf(doc)
        )
        val jsonString = json.encodeToString(ProjectExport.serializer(), export)
        val decoded = json.decodeFromString(ProjectExport.serializer(), jsonString)
        assertTrue(decoded.documents.first().pins.isEmpty())
    }

    @Test
    fun commentExportDefaultSyncIDIsEmpty() {
        val comment = CommentExport(text = "test", author = "a", createdAt = "2024-01-01T00:00:00Z")
        assertEquals("", comment.syncID)
    }

    @Test
    fun photoExportDefaultSyncIDIsEmpty() {
        val photo = PhotoExport(imageData = "AQID", caption = "test", createdAt = "2024-01-01T00:00:00Z")
        assertEquals("", photo.syncID)
    }

    @Test
    fun unknownFieldsIgnored() {
        val jsonWithExtra = """
            {
                "name": "Test",
                "syncID": "s1",
                "createdAt": "2024-01-01T00:00:00Z",
                "documents": [],
                "unknownField": "should be ignored"
            }
        """.trimIndent()
        val decoded = json.decodeFromString(ProjectExport.serializer(), jsonWithExtra)
        assertEquals("Test", decoded.name)
    }

    @Test
    fun commentExportWithSyncIDSerialized() {
        val comment = CommentExport(text = "hello", author = "bob", createdAt = "2024-01-01T00:00:00Z", syncID = "abc-123")
        val jsonString = json.encodeToString(CommentExport.serializer(), comment)
        assertTrue(jsonString.contains("abc-123"))
        val decoded = json.decodeFromString(CommentExport.serializer(), jsonString)
        assertEquals("abc-123", decoded.syncID)
    }

    @Test
    fun photoExportWithSyncIDSerialized() {
        val photo = PhotoExport(imageData = "AQID", caption = "pic", createdAt = "2024-01-01T00:00:00Z", syncID = "xyz-456")
        val jsonString = json.encodeToString(PhotoExport.serializer(), photo)
        assertTrue(jsonString.contains("xyz-456"))
        val decoded = json.decodeFromString(PhotoExport.serializer(), jsonString)
        assertEquals("xyz-456", decoded.syncID)
    }

    // ── Helper ──

    private fun createSampleProjectExport(): ProjectExport {
        return ProjectExport(
            name = "Construction Site A",
            syncID = "proj-sync-1",
            createdAt = "2024-06-15T10:30:00Z",
            documents = listOf(
                DocumentExport(
                    name = "Floor Plan",
                    syncID = "doc-sync-1",
                    fileType = "application/pdf",
                    pageCount = 3,
                    createdAt = "2024-06-15T10:31:00Z",
                    fileData = DateUtils.encodeBase64(byteArrayOf(1, 2, 3, 4, 5)),
                    pins = listOf(
                        PinExport(
                            syncID = "pin-sync-1",
                            relativeX = 0.25, relativeY = 0.75,
                            pageIndex = 2, title = "Wall Defect",
                            pinDescription = "Visible crack", location = "Room 101",
                            height = "2m", width = "0.5m",
                            status = "open", category = "structural",
                            author = "John",
                            createdAt = "2024-06-15T11:00:00Z",
                            modifiedAt = "2024-06-15T12:00:00Z",
                            photos = listOf(
                                PhotoExport(
                                    imageData = DateUtils.encodeBase64(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())),
                                    caption = "Wall crack",
                                    createdAt = "2024-06-15T11:05:00Z",
                                    syncID = "photo-sync-1"
                                )
                            ),
                            comments = listOf(
                                CommentExport(
                                    text = "Fix the crack",
                                    author = "Jane",
                                    createdAt = "2024-06-15T11:10:00Z",
                                    syncID = "comment-sync-1"
                                )
                            )
                        )
                    )
                )
            )
        )
    }
}
