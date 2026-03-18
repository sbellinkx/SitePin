import Foundation
import SwiftData

struct ProjectSharingService {

    // MARK: - Export

    static func exportProject(_ project: Project) throws -> Data {
        let export = ProjectExport(
            name: project.name,
            syncID: project.syncID,
            exportedBy: UserProfileManager.shared.displayName,
            exportedAt: Date(),
            documents: project.documents.sorted(by: { $0.createdAt < $1.createdAt }).map { doc in
                DocumentExport(
                    name: doc.name,
                    syncID: doc.syncID,
                    fileData: doc.fileData,
                    fileType: doc.fileType,
                    pageCount: doc.pageCount,
                    createdAt: doc.createdAt,
                    pins: doc.pins.sorted(by: { $0.createdAt < $1.createdAt }).map { pin in
                        PinExport(
                            syncID: pin.syncID,
                            relativeX: pin.relativeX,
                            relativeY: pin.relativeY,
                            title: pin.title,
                            pinDescription: pin.pinDescription,
                            location: pin.location,
                            height: pin.height,
                            width: pin.width,
                            status: pin.status,
                            category: pin.category,
                            author: pin.author,
                            pageIndex: pin.pageIndex,
                            createdAt: pin.createdAt,
                            modifiedAt: pin.modifiedAt,
                            photos: pin.photos.map { photo in
                                PhotoExport(imageData: photo.imageData, caption: photo.caption, createdAt: photo.createdAt, syncID: photo.syncID)
                            },
                            comments: pin.comments.sorted(by: { $0.createdAt < $1.createdAt }).map { comment in
                                CommentExport(text: comment.text, author: comment.author, createdAt: comment.createdAt, syncID: comment.syncID)
                            }
                        )
                    }
                )
            }
        )

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return try encoder.encode(export)
    }

    static func exportToFile(_ project: Project) throws -> URL {
        let data = try exportProject(project)
        let safeName = sanitizeFileName(project.name)
        let fileName = "\(safeName).sitepin"
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
        try data.write(to: url)
        return url
    }

    // MARK: - Import

    enum ImportError: LocalizedError {
        case securityScopedAccessDenied
        case fileTooLarge(sizeMB: Int, maxMB: Int)
        case photoLimitExceeded(pinTitle: String, count: Int, max: Int)

        var errorDescription: String? {
            switch self {
            case .securityScopedAccessDenied:
                return "Could not access the selected file. Please try again."
            case .fileTooLarge(let sizeMB, let maxMB):
                return "File is too large (\(sizeMB) MB). Maximum allowed is \(maxMB) MB."
            case .photoLimitExceeded(let pinTitle, let count, let max):
                return "Pin \"\(pinTitle)\" has \(count) photos, exceeding the maximum of \(max)."
            }
        }
    }

    static func importProject(from url: URL, into context: ModelContext) throws -> Project {
        guard url.startAccessingSecurityScopedResource() else {
            throw ImportError.securityScopedAccessDenied
        }
        defer { url.stopAccessingSecurityScopedResource() }

        let data = try Data(contentsOf: url)
        return try importProject(from: data, into: context)
    }

    static func importProject(from data: Data, into context: ModelContext) throws -> Project {
        // A3/A4: Validate file size before parsing
        if data.count > SitePinConstants.maxImportSize {
            throw ImportError.fileTooLarge(sizeMB: data.count / (1024 * 1024), maxMB: SitePinConstants.maxImportSize / (1024 * 1024))
        }

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        let export = try decoder.decode(ProjectExport.self, from: data)

        // A3: Validate photo counts per pin before importing
        for doc in export.documents {
            for pin in doc.pins where pin.photos.count > SitePinConstants.maxPhotosPerPin {
                throw ImportError.photoLimitExceeded(
                    pinTitle: pin.title.isEmpty ? "Untitled" : pin.title,
                    count: pin.photos.count,
                    max: SitePinConstants.maxPhotosPerPin
                )
            }
        }

        let project = Project(name: sanitizeString(export.name))
        project.syncID = sanitizeSyncID(export.syncID)
        context.insert(project)

        for docExport in export.documents {
            let doc = PlanDocument(name: sanitizeString(docExport.name), fileData: docExport.fileData,
                                   fileType: docExport.fileType, pageCount: docExport.pageCount)
            doc.syncID = sanitizeSyncID(docExport.syncID)
            doc.createdAt = docExport.createdAt
            doc.project = project
            project.documents.append(doc)

            for pinExport in docExport.pins {
                let pin = Pin(relativeX: pinExport.relativeX, relativeY: pinExport.relativeY,
                              pageIndex: pinExport.pageIndex)
                pin.syncID = sanitizeSyncID(pinExport.syncID)
                applyPinExportToPin(pinExport, pin: pin)
                pin.createdAt = pinExport.createdAt
                pin.modifiedAt = pinExport.modifiedAt
                pin.document = doc
                doc.pins.append(pin)

                for photoExport in pinExport.photos {
                    let photo = PinPhoto(imageData: photoExport.imageData, caption: sanitizeString(photoExport.caption))
                    photo.createdAt = photoExport.createdAt
                    if let sid = photoExport.syncID, !sid.isEmpty { photo.syncID = sanitizeSyncID(sid) }
                    photo.pin = pin
                    pin.photos.append(photo)
                }

                for commentExport in pinExport.comments {
                    let comment = PinComment(text: sanitizeString(commentExport.text, maxLength: maxDescriptionLength),
                                             author: sanitizeString(commentExport.author), preserveEmptyAuthor: true)
                    comment.createdAt = commentExport.createdAt
                    if let sid = commentExport.syncID, !sid.isEmpty { comment.syncID = sanitizeSyncID(sid) }
                    comment.pin = pin
                    pin.comments.append(comment)
                }
            }
        }

        return project
    }

    // MARK: - Sync / Merge

    static func mergeProject(from url: URL, into existingProject: Project, context: ModelContext) throws {
        guard url.startAccessingSecurityScopedResource() else {
            throw ImportError.securityScopedAccessDenied
        }
        defer { url.stopAccessingSecurityScopedResource() }

        let data = try Data(contentsOf: url)

        // A4: Validate file size
        if data.count > SitePinConstants.maxImportSize {
            throw ImportError.fileTooLarge(sizeMB: data.count / (1024 * 1024), maxMB: SitePinConstants.maxImportSize / (1024 * 1024))
        }

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        let export = try decoder.decode(ProjectExport.self, from: data)

        for docExport in export.documents {
            if let existingDoc = existingProject.documents.first(where: { $0.syncID == docExport.syncID }) {
                for pinExport in docExport.pins {
                    if let existingPin = existingDoc.pins.first(where: { $0.syncID == pinExport.syncID }) {
                        let incomingModified = pinExport.modifiedAt
                        if incomingModified > existingPin.modifiedAt {
                            applyPinExportToPin(pinExport, pin: existingPin)
                            existingPin.modifiedAt = incomingModified
                        }

                        // Merge new comments (use syncID if available, fall back to text+author+timestamp)
                        for commentExport in pinExport.comments {
                            let alreadyExists: Bool
                            if let sid = commentExport.syncID, !sid.isEmpty {
                                alreadyExists = existingPin.comments.contains(where: { $0.syncID == sid })
                            } else {
                                alreadyExists = existingPin.comments.contains(where: {
                                    $0.text == commentExport.text && $0.author == commentExport.author &&
                                    abs($0.createdAt.timeIntervalSince(commentExport.createdAt)) < 1
                                })
                            }
                            if !alreadyExists {
                                let comment = PinComment(text: sanitizeString(commentExport.text, maxLength: maxDescriptionLength),
                                                         author: sanitizeString(commentExport.author), preserveEmptyAuthor: true)
                                comment.createdAt = commentExport.createdAt
                                if let sid = commentExport.syncID, !sid.isEmpty { comment.syncID = sanitizeSyncID(sid) }
                                comment.pin = existingPin
                                existingPin.comments.append(comment)
                            }
                        }

                        // Merge new photos (use syncID if available, fall back to caption+timestamp)
                        for photoExport in pinExport.photos {
                            let alreadyExists: Bool
                            if let sid = photoExport.syncID, !sid.isEmpty {
                                alreadyExists = existingPin.photos.contains(where: { $0.syncID == sid })
                            } else {
                                alreadyExists = existingPin.photos.contains(where: {
                                    $0.caption == photoExport.caption &&
                                    abs($0.createdAt.timeIntervalSince(photoExport.createdAt)) < 1
                                })
                            }
                            if !alreadyExists {
                                let photo = PinPhoto(imageData: photoExport.imageData, caption: sanitizeString(photoExport.caption))
                                photo.createdAt = photoExport.createdAt
                                if let sid = photoExport.syncID, !sid.isEmpty { photo.syncID = sanitizeSyncID(sid) }
                                photo.pin = existingPin
                                existingPin.photos.append(photo)
                            }
                        }
                    } else {
                        // New pin
                        addImportedPin(pinExport, to: existingDoc)
                    }
                }
            } else {
                // New document
                let doc = PlanDocument(name: sanitizeString(docExport.name), fileData: docExport.fileData,
                                       fileType: docExport.fileType, pageCount: docExport.pageCount)
                doc.syncID = sanitizeSyncID(docExport.syncID)
                doc.createdAt = docExport.createdAt
                doc.project = existingProject
                existingProject.documents.append(doc)

                for pinExport in docExport.pins {
                    addImportedPin(pinExport, to: doc)
                }
            }
        }

    }

    // MARK: - Input Sanitization

    private static let maxStringLength = 1000
    private static let maxDescriptionLength = 5000
    private static let validStatuses = Set(PinStatus.allCases.map(\.rawValue))
    private static let validCategories = Set(PinCategory.allCases.map(\.rawValue))

    private static func sanitizeString(_ value: String, maxLength: Int = maxStringLength) -> String {
        String(value.prefix(maxLength))
    }

    private static func sanitizeStatus(_ value: String) -> String {
        validStatuses.contains(value) ? value : PinStatus.open.rawValue
    }

    private static func sanitizeCategory(_ value: String) -> String {
        validCategories.contains(value) ? value : PinCategory.general.rawValue
    }

    private static func sanitizeSyncID(_ value: String) -> String {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? UUID().uuidString : String(trimmed.prefix(100))
    }

    private static func applyPinExportToPin(_ pinExport: PinExport, pin: Pin) {
        pin.title = sanitizeString(pinExport.title)
        pin.pinDescription = sanitizeString(pinExport.pinDescription, maxLength: maxDescriptionLength)
        pin.location = sanitizeString(pinExport.location)
        pin.height = sanitizeString(pinExport.height)
        pin.width = sanitizeString(pinExport.width)
        pin.status = sanitizeStatus(pinExport.status)
        pin.category = sanitizeCategory(pinExport.category)
        pin.author = sanitizeString(pinExport.author)
    }

    // MARK: - Helpers

    private static func addImportedPin(_ pinExport: PinExport, to doc: PlanDocument) {
        let pin = Pin(relativeX: pinExport.relativeX, relativeY: pinExport.relativeY,
                      pageIndex: pinExport.pageIndex)
        pin.syncID = sanitizeSyncID(pinExport.syncID)
        applyPinExportToPin(pinExport, pin: pin)
        pin.createdAt = pinExport.createdAt
        pin.modifiedAt = pinExport.modifiedAt
        pin.document = doc
        doc.pins.append(pin)

        for photoExport in pinExport.photos {
            let photo = PinPhoto(imageData: photoExport.imageData, caption: sanitizeString(photoExport.caption))
            photo.createdAt = photoExport.createdAt
            if let sid = photoExport.syncID, !sid.isEmpty { photo.syncID = sanitizeSyncID(sid) }
            photo.pin = pin
            pin.photos.append(photo)
        }
        for commentExport in pinExport.comments {
            let comment = PinComment(text: sanitizeString(commentExport.text, maxLength: maxDescriptionLength),
                                     author: sanitizeString(commentExport.author), preserveEmptyAuthor: true)
            comment.createdAt = commentExport.createdAt
            if let sid = commentExport.syncID, !sid.isEmpty { comment.syncID = sanitizeSyncID(sid) }
            comment.pin = pin
            pin.comments.append(comment)
        }
    }

    private static func sanitizeFileName(_ name: String) -> String {
        let sanitized = name.replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "\\", with: "_")
            .replacingOccurrences(of: ":", with: "_")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return sanitized.isEmpty ? "Untitled" : String(sanitized.prefix(100))
    }
}
