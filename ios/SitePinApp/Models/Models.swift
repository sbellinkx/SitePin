import Foundation
import SwiftData
import SwiftUI

// MARK: - Pin Status

enum PinStatus: String, CaseIterable, Identifiable {
    case open = "open"
    case inProgress = "in_progress"
    case resolved = "resolved"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .open: return "Open"
        case .inProgress: return "In Progress"
        case .resolved: return "Resolved"
        }
    }

    var icon: String {
        switch self {
        case .open: return "circle"
        case .inProgress: return "clock"
        case .resolved: return "checkmark.circle"
        }
    }

    var color: Color {
        switch self {
        case .open: return Color(red: 1.0, green: 0.341, blue: 0.133) // #FF5722
        case .inProgress: return Color(red: 1.0, green: 0.596, blue: 0.0) // #FF9800
        case .resolved: return Color(red: 0.298, green: 0.686, blue: 0.314) // #4CAF50
        }
    }

    var uiColor: UIColor {
        switch self {
        case .open: return UIColor(red: 1.0, green: 0.341, blue: 0.133, alpha: 1)
        case .inProgress: return UIColor(red: 1.0, green: 0.596, blue: 0.0, alpha: 1)
        case .resolved: return UIColor(red: 0.298, green: 0.686, blue: 0.314, alpha: 1)
        }
    }
}

// MARK: - Constants

enum SitePinConstants {
    /// JPEG compression quality for photos
    static let jpegCompressionQuality: CGFloat = 0.7
    /// Maximum image rendering dimension in pixels
    static let maxImageDimension: CGFloat = 4096
    /// PDF page width (A4)
    static let pdfPageWidth: CGFloat = 595.0
    /// PDF page height (A4)
    static let pdfPageHeight: CGFloat = 842.0
    /// PDF margin
    static let pdfMargin: CGFloat = 40.0
    /// Maximum document file size (50 MB)
    static let maxDocumentSize = 50 * 1024 * 1024
    /// Maximum import file size (100 MB)
    static let maxImportSize = 100 * 1024 * 1024
    /// Maximum photos per pin
    static let maxPhotosPerPin = 20
}

// MARK: - Pin Categories

enum PinCategory: String, CaseIterable, Identifiable {
    case general = "general"
    case electrical = "electrical"
    case plumbing = "plumbing"
    case structural = "structural"
    case finishing = "finishing"
    case hvac = "hvac"
    case safety = "safety"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .general: return "General"
        case .electrical: return "Electrical"
        case .plumbing: return "Plumbing"
        case .structural: return "Structural"
        case .finishing: return "Finishing"
        case .hvac: return "HVAC"
        case .safety: return "Safety"
        }
    }

    var icon: String {
        switch self {
        case .general: return "mappin"
        case .electrical: return "bolt.fill"
        case .plumbing: return "drop.fill"
        case .structural: return "building.2.fill"
        case .finishing: return "paintbrush.fill"
        case .hvac: return "fan.fill"
        case .safety: return "exclamationmark.triangle.fill"
        }
    }

    var color: Color {
        switch self {
        case .general: return Color(red: 0.376, green: 0.490, blue: 0.545) // #607D8B
        case .electrical: return Color(red: 1.0, green: 0.757, blue: 0.027) // #FFC107
        case .plumbing: return Color(red: 0.129, green: 0.588, blue: 0.953) // #2196F3
        case .structural: return Color(red: 0.475, green: 0.333, blue: 0.282) // #795548
        case .finishing: return Color(red: 0.612, green: 0.153, blue: 0.690) // #9C27B0
        case .hvac: return Color(red: 0.0, green: 0.737, blue: 0.831) // #00BCD4
        case .safety: return Color(red: 1.0, green: 0.341, blue: 0.133) // #FF5722
        }
    }

    var uiColor: UIColor {
        switch self {
        case .general: return UIColor(red: 0.376, green: 0.490, blue: 0.545, alpha: 1)
        case .electrical: return UIColor(red: 1.0, green: 0.757, blue: 0.027, alpha: 1)
        case .plumbing: return UIColor(red: 0.129, green: 0.588, blue: 0.953, alpha: 1)
        case .structural: return UIColor(red: 0.475, green: 0.333, blue: 0.282, alpha: 1)
        case .finishing: return UIColor(red: 0.612, green: 0.153, blue: 0.690, alpha: 1)
        case .hvac: return UIColor(red: 0.0, green: 0.737, blue: 0.831, alpha: 1)
        case .safety: return UIColor(red: 1.0, green: 0.341, blue: 0.133, alpha: 1)
        }
    }
}

// MARK: - User Profile

class UserProfileManager {
    static let shared = UserProfileManager()
    private let nameKey = "user_display_name"

    var displayName: String {
        get { UserDefaults.standard.string(forKey: nameKey) ?? "" }
        set { UserDefaults.standard.set(newValue.trimmingCharacters(in: .whitespacesAndNewlines), forKey: nameKey) }
    }

    var hasProfile: Bool { !displayName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
}

// MARK: - SwiftData Models

@Model
final class Project {
    var name: String = ""
    var createdAt: Date = Date()
    var syncID: String = ""
    @Relationship(deleteRule: .cascade) var documents: [PlanDocument] = []

    init(name: String) {
        self.name = name
        self.createdAt = Date()
        self.syncID = UUID().uuidString
        self.documents = []
    }
}

@Model
final class PlanDocument {
    var name: String = ""
    @Attribute(.externalStorage) var fileData: Data = Data()
    var fileType: String = "jpg"
    var pageCount: Int = 1
    var createdAt: Date = Date()
    var syncID: String = ""
    @Relationship(deleteRule: .cascade) var pins: [Pin] = []
    var project: Project?

    init(name: String, fileData: Data, fileType: String, pageCount: Int = 1) {
        self.name = name
        self.fileData = fileData
        self.fileType = fileType
        self.pageCount = pageCount
        self.createdAt = Date()
        self.syncID = UUID().uuidString
        self.pins = []
    }
}

@Model
final class Pin {
    var relativeX: Double = 0
    var relativeY: Double = 0
    var title: String = ""
    var pinDescription: String = ""
    var location: String = ""
    var height: String = ""
    var width: String = ""
    var status: String = "open"
    var category: String = "general"
    var author: String = ""
    var pageIndex: Int = 0
    var createdAt: Date = Date()
    var modifiedAt: Date = Date()
    var syncID: String = ""
    @Relationship(deleteRule: .cascade) var photos: [PinPhoto] = []
    @Relationship(deleteRule: .cascade) var comments: [PinComment] = []
    var document: PlanDocument?

    init(relativeX: Double, relativeY: Double, pageIndex: Int = 0) {
        self.relativeX = min(1, max(0, relativeX))
        self.relativeY = min(1, max(0, relativeY))
        self.title = ""
        self.pinDescription = ""
        self.location = ""
        self.height = ""
        self.width = ""
        self.status = "open"
        self.category = PinCategory.general.rawValue
        self.author = UserProfileManager.shared.displayName
        self.pageIndex = pageIndex
        self.createdAt = Date()
        self.modifiedAt = Date()
        self.syncID = UUID().uuidString
        self.photos = []
        self.comments = []
    }
}

@Model
final class PinComment {
    var text: String = ""
    var author: String = ""
    var createdAt: Date = Date()
    var syncID: String = ""
    var pin: Pin?

    init(text: String, author: String = "", preserveEmptyAuthor: Bool = false) {
        self.text = text
        if preserveEmptyAuthor {
            self.author = author
        } else {
            self.author = author.isEmpty ? UserProfileManager.shared.displayName : author
        }
        self.createdAt = Date()
        self.syncID = UUID().uuidString
    }
}

@Model
final class PinPhoto {
    @Attribute(.externalStorage) var imageData: Data = Data()
    var caption: String = ""
    var createdAt: Date = Date()
    var syncID: String = ""
    var pin: Pin?

    init(imageData: Data, caption: String = "") {
        self.imageData = imageData
        self.caption = caption
        self.createdAt = Date()
        self.syncID = UUID().uuidString
    }
}

// MARK: - Sharing / Export Codable Types

struct ProjectExport: Codable {
    /// Format version for cross-platform compatibility. Increment on schema changes.
    var formatVersion: Int
    var name: String
    var syncID: String
    var exportedBy: String
    var exportedAt: Date
    var documents: [DocumentExport]

    init(formatVersion: Int = 2, name: String, syncID: String, exportedBy: String, exportedAt: Date, documents: [DocumentExport]) {
        self.formatVersion = formatVersion
        self.name = name
        self.syncID = syncID
        self.exportedBy = exportedBy
        self.exportedAt = exportedAt
        self.documents = documents
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        formatVersion = try container.decodeIfPresent(Int.self, forKey: .formatVersion) ?? 1
        name = try container.decode(String.self, forKey: .name)
        syncID = try container.decode(String.self, forKey: .syncID)
        exportedBy = try container.decodeIfPresent(String.self, forKey: .exportedBy) ?? ""
        exportedAt = try container.decodeIfPresent(Date.self, forKey: .exportedAt) ?? Date()
        documents = try container.decode([DocumentExport].self, forKey: .documents)
    }
}

struct DocumentExport: Codable {
    var name: String
    var syncID: String
    var fileData: Data
    var fileType: String
    var pageCount: Int
    var createdAt: Date
    var pins: [PinExport]
}

struct PinExport: Codable {
    var syncID: String
    var relativeX: Double
    var relativeY: Double
    var title: String
    var pinDescription: String
    var location: String
    var height: String
    var width: String
    var status: String
    var category: String
    var author: String
    var pageIndex: Int
    var createdAt: Date
    var modifiedAt: Date
    var photos: [PhotoExport]
    var comments: [CommentExport]

    init(syncID: String, relativeX: Double, relativeY: Double, title: String, pinDescription: String,
         location: String, height: String, width: String, status: String, category: String,
         author: String, pageIndex: Int, createdAt: Date, modifiedAt: Date,
         photos: [PhotoExport], comments: [CommentExport]) {
        self.syncID = syncID; self.relativeX = relativeX; self.relativeY = relativeY
        self.title = title; self.pinDescription = pinDescription; self.location = location
        self.height = height; self.width = width; self.status = status; self.category = category
        self.author = author; self.pageIndex = pageIndex; self.createdAt = createdAt
        self.modifiedAt = modifiedAt; self.photos = photos; self.comments = comments
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        syncID = try container.decode(String.self, forKey: .syncID)
        relativeX = try container.decode(Double.self, forKey: .relativeX)
        relativeY = try container.decode(Double.self, forKey: .relativeY)
        title = try container.decode(String.self, forKey: .title)
        pinDescription = try container.decode(String.self, forKey: .pinDescription)
        location = try container.decode(String.self, forKey: .location)
        height = try container.decode(String.self, forKey: .height)
        width = try container.decode(String.self, forKey: .width)
        status = try container.decode(String.self, forKey: .status)
        category = try container.decode(String.self, forKey: .category)
        author = try container.decode(String.self, forKey: .author)
        pageIndex = try container.decode(Int.self, forKey: .pageIndex)
        createdAt = try container.decode(Date.self, forKey: .createdAt)
        modifiedAt = try container.decodeIfPresent(Date.self, forKey: .modifiedAt) ?? createdAt
        photos = try container.decode([PhotoExport].self, forKey: .photos)
        comments = try container.decode([CommentExport].self, forKey: .comments)
    }
}

struct PhotoExport: Codable {
    var imageData: Data
    var caption: String
    var createdAt: Date
    var syncID: String?
}

struct CommentExport: Codable {
    var text: String
    var author: String
    var createdAt: Date
    var syncID: String?
}
