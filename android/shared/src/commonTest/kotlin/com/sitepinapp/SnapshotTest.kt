package com.sitepinapp

import com.sitepinapp.services.CommentSnapshot
import com.sitepinapp.services.DocumentSnapshot
import com.sitepinapp.services.PinSnapshot
import com.sitepinapp.services.PhotoSnapshot
import com.sitepinapp.services.ProjectSnapshot
import kotlin.test.*

class SnapshotTest {

    @Test
    fun projectSnapshotTotalPinCount() {
        val snapshot = ProjectSnapshot(
            name = "Test",
            documents = listOf(
                DocumentSnapshot("D1", byteArrayOf(), "pdf", 1, listOf(pin(1), pin(2))),
                DocumentSnapshot("D2", byteArrayOf(), "jpg", 1, listOf(pin(3)))
            )
        )
        assertEquals(3, snapshot.totalPinCount)
    }

    @Test
    fun projectSnapshotEmptyDocuments() {
        val snapshot = ProjectSnapshot(name = "Empty", documents = emptyList())
        assertEquals(0, snapshot.totalPinCount)
    }

    @Test
    fun documentSnapshotEquality() {
        val data = byteArrayOf(1, 2, 3)
        val d1 = DocumentSnapshot("A", data, "pdf", 1, emptyList())
        val d2 = DocumentSnapshot("A", data, "pdf", 1, emptyList())
        assertEquals(d1, d2)
    }

    @Test
    fun documentSnapshotInequalityDifferentData() {
        val d1 = DocumentSnapshot("A", byteArrayOf(1), "pdf", 1, emptyList())
        val d2 = DocumentSnapshot("A", byteArrayOf(2), "pdf", 1, emptyList())
        assertNotEquals(d1, d2)
    }

    @Test
    fun photoSnapshotEquality() {
        val data = byteArrayOf(10, 20)
        val p1 = PhotoSnapshot(data, "caption")
        val p2 = PhotoSnapshot(data, "caption")
        assertEquals(p1, p2)
    }

    @Test
    fun photoSnapshotInequalityDifferentCaption() {
        val data = byteArrayOf(10, 20)
        val p1 = PhotoSnapshot(data, "A")
        val p2 = PhotoSnapshot(data, "B")
        assertNotEquals(p1, p2)
    }

    @Test
    fun pinSnapshotFieldsPreserved() {
        val p = PinSnapshot(
            number = 5, title = "Test", description = "Desc",
            category = "electrical", status = "resolved",
            author = "Alice", location = "Room 1",
            height = "3m", width = "2m", pageIndex = 1,
            documentName = "Plan A", relativeX = 0.3, relativeY = 0.7,
            photoCount = 2, commentCount = 4,
            createdAt = 1700000000000L,
            photos = listOf(PhotoSnapshot(byteArrayOf(1), "photo")),
            comments = listOf(CommentSnapshot("text", "Bob", 1700000000000L))
        )
        assertEquals(5, p.number)
        assertEquals("electrical", p.category)
        assertEquals("resolved", p.status)
        assertEquals(1, p.photos.size)
        assertEquals(1, p.comments.size)
    }

    @Test
    fun commentSnapshotFields() {
        val c = CommentSnapshot("Hello", "Author", 1700000000000L)
        assertEquals("Hello", c.text)
        assertEquals("Author", c.author)
        assertEquals(1700000000000L, c.createdAt)
    }

    private fun pin(n: Int) = PinSnapshot(
        number = n, title = "P$n", description = "",
        category = "general", status = "open", author = "",
        location = "", height = "", width = "", pageIndex = 0,
        documentName = "D", relativeX = 0.0, relativeY = 0.0,
        photoCount = 0, commentCount = 0, createdAt = 0L,
        photos = emptyList(), comments = emptyList()
    )
}
