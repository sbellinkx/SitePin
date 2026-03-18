import Foundation

struct CSVExportService {

    static func exportProject(_ project: Project) -> Data {
        var csv = "\u{FEFF}Pin #,Title,Description,Category,Status,Author,Location,Height,Width,Page,Document,Photos,Comments,Created\n"

        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd HH:mm"

        var counter = 1
        for doc in project.documents.sorted(by: { $0.createdAt < $1.createdAt }) {
            for pin in doc.pins.sorted(by: { $0.createdAt < $1.createdAt }) {
                let categoryLabel = PinCategory(rawValue: pin.category)?.label ?? pin.category
                let statusLabel = (PinStatus(rawValue: pin.status) ?? .open).label

                let row = [
                    "\(counter)",
                    escapeCSV(pin.title),
                    escapeCSV(pin.pinDescription),
                    escapeCSV(categoryLabel),
                    escapeCSV(statusLabel),
                    escapeCSV(pin.author),
                    escapeCSV(pin.location),
                    escapeCSV(pin.height),
                    escapeCSV(pin.width),
                    "\(pin.pageIndex + 1)",
                    escapeCSV(doc.name),
                    "\(pin.photos.count)",
                    "\(pin.comments.count)",
                    escapeCSV(dateFormatter.string(from: pin.createdAt)),
                ].joined(separator: ",")

                csv += row + "\n"
                counter += 1
            }
        }

        return csv.data(using: .utf8) ?? Data()
    }

    static func exportToFile(_ project: Project, fileName: String? = nil) -> URL? {
        let data = exportProject(project)
        let name = fileName ?? "\(project.name)_pins.csv"
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(name)
        do {
            try data.write(to: url)
            return url
        } catch {
            print("Failed to write CSV: \(error)")
            return nil
        }
    }

    private static func escapeCSV(_ value: String) -> String {
        if value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r") {
            return "\"" + value.replacingOccurrences(of: "\"", with: "\"\"") + "\""
        }
        return value
    }
}
