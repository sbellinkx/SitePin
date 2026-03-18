package com.sitepinapp

import com.sitepinapp.data.model.PinCategory
import com.sitepinapp.services.CSVExportService
import com.sitepinapp.services.CommentSnapshot
import com.sitepinapp.services.DocumentSnapshot
import com.sitepinapp.services.PinSnapshot
import com.sitepinapp.services.PhotoSnapshot
import com.sitepinapp.services.ProjectSnapshot
import kotlin.test.*

class CSVExportTest {

    @Test
    fun csvContainsBOM() {
        val csv = CSVExportService.buildCSV(createEmptySnapshot())
        assertEquals('\uFEFF', csv[0], "CSV should start with UTF-8 BOM")
    }

    @Test
    fun csvContainsHeader() {
        val csv = CSVExportService.buildCSV(createEmptySnapshot())
        assertTrue(csv.contains("Pin #,Title,Description,Category,Status,Author"))
    }

    @Test
    fun csvCategoryLabelsAreHumanReadable() {
        val snapshot = createSnapshotWithPins(
            listOf(
                createPin(1, category = "electrical"),
                createPin(2, category = "plumbing"),
                createPin(3, category = "structural"),
                createPin(4, category = "general")
            )
        )
        val csv = CSVExportService.buildCSV(snapshot)
        assertTrue(csv.contains("Electrical"), "Should contain 'Electrical' label, got: $csv")
        assertTrue(csv.contains("Plumbing"), "Should contain 'Plumbing' label")
        assertTrue(csv.contains("Structural"), "Should contain 'Structural' label")
        assertTrue(csv.contains("General"), "Should contain 'General' label")
    }

    @Test
    fun csvStatusLabelsAreHumanReadable() {
        val snapshot = createSnapshotWithPins(
            listOf(
                createPin(1, status = "open"),
                createPin(2, status = "in_progress"),
                createPin(3, status = "resolved")
            )
        )
        val csv = CSVExportService.buildCSV(snapshot)
        assertTrue(csv.contains("Open"), "Should contain 'Open' label")
        assertTrue(csv.contains("In Progress"), "Should contain 'In Progress' label")
        assertTrue(csv.contains("Resolved"), "Should contain 'Resolved' label")
    }

    @Test
    fun csvPinNumbersAreSequential() {
        val snapshot = createSnapshotWithPins(
            listOf(createPin(1), createPin(2), createPin(3))
        )
        val csv = CSVExportService.buildCSV(snapshot)
        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith('\uFEFF') && !it.contains("Pin #") }
        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("1,"))
        assertTrue(lines[1].startsWith("2,"))
        assertTrue(lines[2].startsWith("3,"))
    }

    @Test
    fun csvEscapesCommasInTitle() {
        val snapshot = createSnapshotWithPins(
            listOf(createPin(1, title = "Wall, Floor"))
        )
        val csv = CSVExportService.buildCSV(snapshot)
        assertTrue(csv.contains("\"Wall, Floor\""), "Commas in title should be quoted")
    }

    @Test
    fun csvEscapesQuotesInDescription() {
        val snapshot = createSnapshotWithPins(
            listOf(createPin(1, description = "Said \"hello\""))
        )
        val csv = CSVExportService.buildCSV(snapshot)
        assertTrue(csv.contains("\"Said \"\"hello\"\"\""), "Quotes in description should be escaped")
    }

    @Test
    fun csvPageNumberIsOneBased() {
        val snapshot = createSnapshotWithPins(
            listOf(createPin(1, pageIndex = 0))
        )
        val csv = CSVExportService.buildCSV(snapshot)
        val dataLine = csv.lines().last { it.startsWith("1,") }
        // pageIndex 0 should appear as page 1 in CSV
        assertTrue(dataLine.contains(",1,"), "Page index 0 should display as 1")
    }

    @Test
    fun csvIncludesPhotoAndCommentCounts() {
        val snapshot = createSnapshotWithPins(
            listOf(createPin(1, photoCount = 3, commentCount = 5))
        )
        val csv = CSVExportService.buildCSV(snapshot)
        val dataLine = csv.lines().last { it.startsWith("1,") }
        assertTrue(dataLine.contains(",3,"), "Should contain photo count 3")
        assertTrue(dataLine.contains(",5,"), "Should contain comment count 5")
    }

    @Test
    fun csvEmptySnapshotOnlyHasHeader() {
        val csv = CSVExportService.buildCSV(createEmptySnapshot())
        val lines = csv.lines().filter { it.isNotBlank() }
        assertEquals(1, lines.size, "Empty snapshot should only produce header line (with BOM)")
    }

    // ── PinCategory tests ──

    @Test
    fun pinCategoryFromStringCaseInsensitive() {
        assertEquals(PinCategory.ELECTRICAL, PinCategory.fromString("electrical"))
        assertEquals(PinCategory.ELECTRICAL, PinCategory.fromString("ELECTRICAL"))
        assertEquals(PinCategory.PLUMBING, PinCategory.fromString("plumbing"))
        assertEquals(PinCategory.STRUCTURAL, PinCategory.fromString("Structural"))
    }

    @Test
    fun pinCategoryFromStringUnknownDefaultsToGeneral() {
        assertEquals(PinCategory.GENERAL, PinCategory.fromString("unknown"))
        assertEquals(PinCategory.GENERAL, PinCategory.fromString(""))
    }

    @Test
    fun pinCategoryLabelsMatchExpected() {
        assertEquals("General", PinCategory.GENERAL.label)
        assertEquals("Electrical", PinCategory.ELECTRICAL.label)
        assertEquals("Plumbing", PinCategory.PLUMBING.label)
        assertEquals("Structural", PinCategory.STRUCTURAL.label)
        assertEquals("Finishing", PinCategory.FINISHING.label)
        assertEquals("HVAC", PinCategory.HVAC.label)
        assertEquals("Safety", PinCategory.SAFETY.label)
    }

    @Test
    fun allSevenCategoriesExist() {
        assertEquals(7, PinCategory.entries.size)
    }

    // ── Helpers ──

    private fun createEmptySnapshot() = ProjectSnapshot(
        name = "Test Project",
        documents = emptyList()
    )

    private fun createSnapshotWithPins(pins: List<PinSnapshot>) = ProjectSnapshot(
        name = "Test Project",
        documents = listOf(
            DocumentSnapshot(
                name = "Doc 1",
                fileData = byteArrayOf(1),
                fileType = "pdf",
                pageCount = 1,
                pins = pins
            )
        )
    )

    private fun createPin(
        number: Int,
        title: String = "Pin $number",
        description: String = "Desc $number",
        category: String = "general",
        status: String = "open",
        author: String = "Tester",
        location: String = "Room $number",
        pageIndex: Int = 0,
        photoCount: Int = 0,
        commentCount: Int = 0
    ) = PinSnapshot(
        number = number,
        title = title,
        description = description,
        category = category,
        status = status,
        author = author,
        location = location,
        height = "",
        width = "",
        pageIndex = pageIndex,
        documentName = "Doc 1",
        relativeX = 0.5,
        relativeY = 0.5,
        photoCount = photoCount,
        commentCount = commentCount,
        createdAt = 1700000000000L + (number * 1000L),
        photos = emptyList(),
        comments = emptyList()
    )
}
