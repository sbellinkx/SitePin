import XCTest
@testable import SitePinApp

final class ModelTests: XCTestCase {

    // MARK: - PinStatus

    func testPinStatusRawValues() {
        XCTAssertEqual(PinStatus.open.rawValue, "open")
        XCTAssertEqual(PinStatus.inProgress.rawValue, "in_progress")
        XCTAssertEqual(PinStatus.resolved.rawValue, "resolved")
    }

    func testPinStatusLabels() {
        XCTAssertEqual(PinStatus.open.label, "Open")
        XCTAssertEqual(PinStatus.inProgress.label, "In Progress")
        XCTAssertEqual(PinStatus.resolved.label, "Resolved")
    }

    func testPinStatusAllCases() {
        XCTAssertEqual(PinStatus.allCases.count, 3)
    }

    func testPinStatusFromRawValue() {
        XCTAssertEqual(PinStatus(rawValue: "open"), .open)
        XCTAssertEqual(PinStatus(rawValue: "in_progress"), .inProgress)
        XCTAssertEqual(PinStatus(rawValue: "resolved"), .resolved)
        XCTAssertNil(PinStatus(rawValue: "invalid"))
    }

    // MARK: - PinCategory

    func testPinCategoryAllCases() {
        XCTAssertEqual(PinCategory.allCases.count, 7)
    }

    func testPinCategoryLabels() {
        XCTAssertEqual(PinCategory.general.label, "General")
        XCTAssertEqual(PinCategory.electrical.label, "Electrical")
        XCTAssertEqual(PinCategory.plumbing.label, "Plumbing")
        XCTAssertEqual(PinCategory.structural.label, "Structural")
        XCTAssertEqual(PinCategory.finishing.label, "Finishing")
        XCTAssertEqual(PinCategory.hvac.label, "HVAC")
        XCTAssertEqual(PinCategory.safety.label, "Safety")
    }

    func testPinCategoryIcons() {
        for category in PinCategory.allCases {
            XCTAssertFalse(category.icon.isEmpty, "\(category.rawValue) should have an icon")
        }
    }

    // MARK: - SitePinConstants

    func testConstantsAreReasonable() {
        XCTAssertGreaterThan(SitePinConstants.maxDocumentSize, 0)
        XCTAssertGreaterThan(SitePinConstants.maxImportSize, SitePinConstants.maxDocumentSize)
        XCTAssertGreaterThan(SitePinConstants.maxPhotosPerPin, 0)
        XCTAssertGreaterThan(SitePinConstants.jpegCompressionQuality, 0)
        XCTAssertLessThanOrEqual(SitePinConstants.jpegCompressionQuality, 1.0)
    }
}
