package com.sitepinapp

import com.sitepinapp.data.model.*
import kotlin.test.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ModelTest {

    // ── Project ──

    @Test
    fun projectDefaultValues() {
        val project = Project(name = "Test Project")
        assertEquals(0L, project.id)
        assertEquals("Test Project", project.name)
        assertTrue(project.createdAt > 0)
        assertTrue(project.syncID.isNotBlank())
    }

    @Test
    fun projectUniqueSyncIDs() {
        val p1 = Project(name = "A")
        val p2 = Project(name = "B")
        assertNotEquals(p1.syncID, p2.syncID)
    }

    // ── PlanDocument ──

    @Test
    fun planDocumentDefaultValues() {
        val doc = PlanDocument(projectId = 1, name = "Floor Plan", fileData = byteArrayOf(1, 2, 3), fileType = "application/pdf")
        assertEquals(0L, doc.id)
        assertEquals(1L, doc.projectId)
        assertEquals("Floor Plan", doc.name)
        assertEquals("application/pdf", doc.fileType)
        assertEquals(1, doc.pageCount)
        assertTrue(doc.createdAt > 0)
        assertTrue(doc.syncID.isNotBlank())
        assertContentEquals(byteArrayOf(1, 2, 3), doc.fileData)
    }

    @Test
    fun planDocumentEquality() {
        val data = byteArrayOf(10, 20, 30)
        val d1 = PlanDocument(id = 1, projectId = 1, name = "A", fileData = data, fileType = "pdf", syncID = "abc")
        val d2 = PlanDocument(id = 1, projectId = 1, name = "A", fileData = data, fileType = "pdf", syncID = "abc")
        assertEquals(d1, d2)
    }

    @Test
    fun planDocumentEqualityDifferentFileData() {
        val d1 = PlanDocument(id = 1, projectId = 1, name = "A", fileData = byteArrayOf(1), fileType = "pdf", syncID = "abc")
        val d2 = PlanDocument(id = 1, projectId = 1, name = "A", fileData = byteArrayOf(2), fileType = "pdf", syncID = "abc")
        assertNotEquals(d1, d2)
    }

    // ── Pin ──

    @Test
    fun pinDefaultValues() {
        val pin = Pin(documentId = 1, relativeX = 0.5, relativeY = 0.75)
        assertEquals(0L, pin.id)
        assertEquals(1L, pin.documentId)
        assertEquals(0.5, pin.relativeX)
        assertEquals(0.75, pin.relativeY)
        assertEquals(0, pin.pageIndex)
        assertEquals("", pin.title)
        assertEquals("", pin.pinDescription)
        assertEquals("open", pin.status)
        assertEquals("general", pin.category)
        assertTrue(pin.createdAt > 0)
        assertTrue(pin.modifiedAt > 0)
        assertTrue(pin.syncID.isNotBlank())
    }

    @Test
    fun pinAllFields() {
        val pin = Pin(
            id = 5, documentId = 2, relativeX = 0.1, relativeY = 0.9,
            pageIndex = 3, title = "Defect", pinDescription = "Crack in wall",
            location = "Room 101", height = "2m", width = "0.5m",
            status = "resolved", category = "structural", author = "John"
        )
        assertEquals("Defect", pin.title)
        assertEquals("Crack in wall", pin.pinDescription)
        assertEquals("Room 101", pin.location)
        assertEquals("resolved", pin.status)
        assertEquals("structural", pin.category)
        assertEquals("John", pin.author)
        assertEquals(3, pin.pageIndex)
    }

    // ── PinPhoto ──

    @Test
    fun pinPhotoDefaultValues() {
        val photo = PinPhoto(pinId = 1, imageData = byteArrayOf(0xFF.toByte(), 0xD8.toByte()))
        assertEquals(0L, photo.id)
        assertEquals(1L, photo.pinId)
        assertEquals("", photo.caption)
        assertTrue(photo.createdAt > 0)
        assertTrue(photo.syncID.isNotBlank())
    }

    @Test
    fun pinPhotoEquality() {
        val data = byteArrayOf(1, 2, 3)
        val p1 = PinPhoto(id = 1, pinId = 1, imageData = data)
        val p2 = PinPhoto(id = 1, pinId = 1, imageData = data)
        assertEquals(p1, p2)
    }

    @Test
    fun pinPhotoDifferentDataNotEqual() {
        val p1 = PinPhoto(id = 1, pinId = 1, imageData = byteArrayOf(1))
        val p2 = PinPhoto(id = 1, pinId = 1, imageData = byteArrayOf(2))
        assertNotEquals(p1, p2)
    }

    // ── PinComment ──

    @Test
    fun pinCommentDefaultValues() {
        val comment = PinComment(pinId = 1, text = "Fix this", author = "Jane")
        assertEquals(0L, comment.id)
        assertEquals(1L, comment.pinId)
        assertEquals("Fix this", comment.text)
        assertEquals("Jane", comment.author)
        assertTrue(comment.createdAt > 0)
        assertTrue(comment.syncID.isNotBlank())
    }

    @Test
    fun pinCommentUniqueSyncIDs() {
        val c1 = PinComment(pinId = 1, text = "A", author = "X")
        val c2 = PinComment(pinId = 1, text = "B", author = "Y")
        assertNotEquals(c1.syncID, c2.syncID)
    }
}
