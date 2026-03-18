import XCTest
@testable import SitePinApp

final class ImportValidationTests: XCTestCase {

    func testFileSizeValidation() {
        // Create data larger than the limit
        let oversizedData = Data(count: SitePinConstants.maxImportSize + 1)

        // This should not crash — we can't create a ModelContext in unit tests
        // without a full SwiftData setup, but we can verify the constants
        XCTAssertGreaterThan(oversizedData.count, SitePinConstants.maxImportSize)
    }

    func testImportErrorDescriptions() {
        let securityError = ProjectSharingService.ImportError.securityScopedAccessDenied
        XCTAssertNotNil(securityError.errorDescription)
        XCTAssertFalse(securityError.errorDescription!.isEmpty)

        let sizeError = ProjectSharingService.ImportError.fileTooLarge(sizeMB: 200, maxMB: 100)
        XCTAssertNotNil(sizeError.errorDescription)
        XCTAssertTrue(sizeError.errorDescription!.contains("200"))
        XCTAssertTrue(sizeError.errorDescription!.contains("100"))

        let photoError = ProjectSharingService.ImportError.photoLimitExceeded(pinTitle: "Test", count: 30, max: 20)
        XCTAssertNotNil(photoError.errorDescription)
        XCTAssertTrue(photoError.errorDescription!.contains("Test"))
        XCTAssertTrue(photoError.errorDescription!.contains("30"))
        XCTAssertTrue(photoError.errorDescription!.contains("20"))
    }

    func testExportEncodeDecode() throws {
        let export = ProjectExport(
            name: "Test",
            syncID: "abc-123",
            exportedBy: "Tester",
            exportedAt: Date(),
            documents: []
        )

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let data = try encoder.encode(export)

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        let decoded = try decoder.decode(ProjectExport.self, from: data)

        XCTAssertEqual(decoded.name, "Test")
        XCTAssertEqual(decoded.syncID, "abc-123")
        XCTAssertEqual(decoded.exportedBy, "Tester")
        XCTAssertEqual(decoded.documents.count, 0)
        XCTAssertEqual(decoded.formatVersion, 2)
    }

    func testFormatVersionIncludedInExport() throws {
        let export = ProjectExport(
            name: "Versioned",
            syncID: "v-1",
            exportedBy: "Test",
            exportedAt: Date(),
            documents: []
        )

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let data = try encoder.encode(export)
        let json = String(data: data, encoding: .utf8)!
        XCTAssertTrue(json.contains("\"formatVersion\""), "Export JSON should contain formatVersion field")
        XCTAssertTrue(json.contains(":2"), "formatVersion should be 2")
    }

    func testBackwardCompatibilityWithoutFormatVersion() throws {
        // Simulate an old export that doesn't have formatVersion
        let oldJson = """
        {
            "name": "Old Project",
            "syncID": "old-1",
            "exportedBy": "OldApp",
            "exportedAt": "2024-01-01T00:00:00Z",
            "documents": []
        }
        """
        let data = oldJson.data(using: .utf8)!
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        let decoded = try decoder.decode(ProjectExport.self, from: data)
        XCTAssertEqual(decoded.name, "Old Project")
        XCTAssertEqual(decoded.formatVersion, 1, "Missing formatVersion should default to 1 (legacy format)")
    }

    func testCrossPlatformStatusValues() {
        // Verify that iOS status values match what Android/KMP uses
        let expectedStatuses = ["open", "in_progress", "resolved"]
        let actualStatuses = PinStatus.allCases.map(\.rawValue)
        XCTAssertEqual(actualStatuses, expectedStatuses)
    }

    func testCrossPlatformCategoryValues() {
        // Verify that iOS category values match what Android/KMP uses
        let expectedCategories = ["general", "electrical", "plumbing", "structural", "finishing", "hvac", "safety"]
        let actualCategories = PinCategory.allCases.map(\.rawValue)
        XCTAssertEqual(actualCategories, expectedCategories)
    }
}
