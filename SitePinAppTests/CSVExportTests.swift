import XCTest
import SwiftData
@testable import SitePinApp

final class CSVExportTests: XCTestCase {

    private var container: ModelContainer!
    private var context: ModelContext!

    override func setUp() {
        super.setUp()
        let config = ModelConfiguration(isStoredInMemoryOnly: true)
        container = try! ModelContainer(for: Project.self, PlanDocument.self, Pin.self, PinPhoto.self, PinComment.self, configurations: config)
        context = ModelContext(container)
    }

    override func tearDown() {
        context = nil
        container = nil
        super.tearDown()
    }

    private func makeTestProject() -> Project {
        let project = Project(name: "Test Project")
        context.insert(project)

        let doc = PlanDocument(name: "Floor Plan", fileData: Data(), fileType: "jpg")
        doc.project = project
        project.documents.append(doc)
        context.insert(doc)

        let pin = Pin(relativeX: 0.5, relativeY: 0.5)
        pin.title = "Test Pin"
        pin.pinDescription = "A description"
        pin.category = PinCategory.electrical.rawValue
        pin.status = PinStatus.inProgress.rawValue
        pin.author = "Tester"
        pin.location = "Room 101"
        pin.document = doc
        doc.pins.append(pin)
        context.insert(pin)

        return project
    }

    func testCSVExportContainsBOM() {
        let project = makeTestProject()
        let data = CSVExportService.exportProject(project)
        // UTF-8 BOM is the bytes EF BB BF
        XCTAssertGreaterThanOrEqual(data.count, 3, "Data should have at least 3 bytes")
        XCTAssertEqual(data[0], 0xEF, "First BOM byte")
        XCTAssertEqual(data[1], 0xBB, "Second BOM byte")
        XCTAssertEqual(data[2], 0xBF, "Third BOM byte")
    }

    func testCSVExportContainsHeaders() {
        let project = makeTestProject()
        let data = CSVExportService.exportProject(project)
        let csv = String(data: data, encoding: .utf8)!
        let firstLine = csv.components(separatedBy: "\n").first!
        XCTAssertTrue(firstLine.contains("Pin #"))
        XCTAssertTrue(firstLine.contains("Title"))
        XCTAssertTrue(firstLine.contains("Status"))
        XCTAssertTrue(firstLine.contains("Category"))
    }

    func testCSVExportContainsPinData() {
        let project = makeTestProject()
        let data = CSVExportService.exportProject(project)
        let csv = String(data: data, encoding: .utf8)!
        XCTAssertTrue(csv.contains("Test Pin"), "CSV should contain pin title")
        XCTAssertTrue(csv.contains("Electrical"), "CSV should contain category label")
        XCTAssertTrue(csv.contains("In Progress"), "CSV should contain status label")
        XCTAssertTrue(csv.contains("Room 101"), "CSV should contain location")
    }

    func testCSVExportEscapesCommas() {
        let project = makeTestProject()
        project.documents.first!.pins.first!.title = "Pin, with comma"
        let data = CSVExportService.exportProject(project)
        let csv = String(data: data, encoding: .utf8)!
        XCTAssertTrue(csv.contains("\"Pin, with comma\""))
    }

    func testCSVExportEscapesQuotes() {
        let project = makeTestProject()
        project.documents.first!.pins.first!.title = "Pin with \"quotes\""
        let data = CSVExportService.exportProject(project)
        let csv = String(data: data, encoding: .utf8)!
        XCTAssertTrue(csv.contains("\"Pin with \"\"quotes\"\"\""))
    }

    func testCSVExportEmptyProject() {
        let project = Project(name: "Empty")
        context.insert(project)
        let data = CSVExportService.exportProject(project)
        let csv = String(data: data, encoding: .utf8)!
        let lines = csv.components(separatedBy: "\n").filter { !$0.isEmpty }
        XCTAssertEqual(lines.count, 1, "Should only contain header line")
    }

    func testCSVExportUsesStatusEnum() {
        let project = makeTestProject()
        let pin = project.documents.first!.pins.first!
        for status in PinStatus.allCases {
            pin.status = status.rawValue
            let data = CSVExportService.exportProject(project)
            let csv = String(data: data, encoding: .utf8)!
            XCTAssertTrue(csv.contains(status.label), "CSV should contain '\(status.label)' for status '\(status.rawValue)'")
        }
    }
}
