import SwiftUI
import SwiftData
import UIKit
import PDFKit
import UniformTypeIdentifiers

enum ImportMode {
    case plan
    case sync
}

struct DocumentListView: View {
    @Environment(\.modelContext) private var modelContext
    let project: Project
    @State private var showingFilePicker = false
    @State private var importMode: ImportMode = .plan
    @State private var showingShareSheet = false
    @State private var shareItems: [Any] = []
    @State private var isExporting = false
    @State private var syncMessage = ""
    @State private var showingSyncAlert = false
    @State private var showingDashboard = false
    @State private var exportError = ""
    @State private var showingExportError = false
    @State private var errorMessage: String?

    private var importContentTypes: [UTType] {
        switch importMode {
        case .plan: return [.pdf, .jpeg, .png, .image]
        case .sync: return [.json, .data]
        }
    }

    private var allPins: [Pin] {
        project.documents.flatMap(\.pins)
    }

    private var openPins: [Pin] {
        allPins.filter { $0.status == PinStatus.open.rawValue || $0.status == PinStatus.inProgress.rawValue }
    }

    private var resolvedPins: [Pin] {
        allPins.filter { $0.status == PinStatus.resolved.rawValue }
    }

    var body: some View {
        List {
            // Pin dashboard summary
            if !allPins.isEmpty {
                Section {
                    Button(action: { showingDashboard = true }) {
                        HStack(spacing: 16) {
                            VStack {
                                Text("\(allPins.filter { $0.status == PinStatus.open.rawValue }.count)")
                                    .font(.title2.bold())
                                    .foregroundStyle(PinStatus.open.color)
                                Text(PinStatus.open.label)
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                            .frame(maxWidth: .infinity)

                            Divider()
                                .frame(height: 30)

                            VStack {
                                Text("\(allPins.filter { $0.status == PinStatus.inProgress.rawValue }.count)")
                                    .font(.title2.bold())
                                    .foregroundStyle(PinStatus.inProgress.color)
                                Text(PinStatus.inProgress.label)
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                            .frame(maxWidth: .infinity)

                            Divider()
                                .frame(height: 30)

                            VStack {
                                Text("\(resolvedPins.count)")
                                    .font(.title2.bold())
                                    .foregroundStyle(PinStatus.resolved.color)
                                Text("Done")
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                            .frame(maxWidth: .infinity)
                        }
                        .padding(.vertical, 4)
                    }
                    .buttonStyle(.plain)
                }
            }

            // Documents
            Section("Plans") {
                ForEach(sortedDocuments) { doc in
                    NavigationLink(destination: PlanAnnotationView(document: doc)) {
                        HStack {
                            Image(systemName: doc.fileType == "pdf" ? "doc.richtext" : "photo")
                                .foregroundStyle(.blue)
                                .frame(width: 30)
                            VStack(alignment: .leading) {
                                Text(doc.name)
                                    .font(.headline)
                                HStack(spacing: 4) {
                                    Text("\(doc.pins.count) pin(s)")
                                    if doc.pageCount > 1 {
                                        Text("- \(doc.pageCount) pages")
                                    }
                                    Text("- \(doc.fileType.uppercased())")
                                }
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
                .onDelete(perform: deleteDocuments)
            }
        }
        .navigationTitle(project.name)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                HStack(spacing: 12) {
                    Menu {
                        Button(action: { exportPDFReport() }) {
                            Label("Export PDF Report", systemImage: "doc.richtext")
                        }
                        .disabled(project.documents.isEmpty || isExporting)
                        Button(action: { exportCSV() }) {
                            Label("Export CSV / Excel", systemImage: "tablecells")
                        }
                        .disabled(project.documents.isEmpty)
                        Button(action: { shareProject() }) {
                            Label("Share Project (.sitepin)", systemImage: "square.and.arrow.up")
                        }
                        .disabled(project.documents.isEmpty)
                        Divider()
                        Button(action: {
                            importMode = .sync
                            showingFilePicker = true
                        }) {
                            Label("Sync / Import .sitepin", systemImage: "arrow.triangle.2.circlepath")
                        }
                    } label: {
                        if isExporting {
                            ProgressView()
                        } else {
                            Image(systemName: "ellipsis.circle")
                        }
                    }

                    Button(action: {
                        importMode = .plan
                        showingFilePicker = true
                    }) {
                        Image(systemName: "doc.badge.plus")
                    }
                }
            }
        }
        .sheet(isPresented: $showingShareSheet) {
            ShareSheet(activityItems: shareItems)
        }
        .sheet(isPresented: $showingDashboard) {
            PinDashboardView(project: project)
        }
        .fileImporter(
            isPresented: $showingFilePicker,
            allowedContentTypes: importContentTypes,
            allowsMultipleSelection: importMode == .plan
        ) { result in
            switch importMode {
            case .plan:
                handleFileImport(result)
            case .sync:
                handleSyncImport(result)
            }
        }
        .alert("Sync Result", isPresented: $showingSyncAlert) {
            Button("OK") {}
        } message: {
            Text(syncMessage)
        }
        .alert("Export Error", isPresented: $showingExportError) {
            Button("OK") {}
        } message: {
            Text(exportError)
        }
        .alert("Import Error", isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button("OK") { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "")
        }
        .overlay {
            if project.documents.isEmpty {
                ContentUnavailableView("No Plans",
                    systemImage: "doc.badge.plus",
                    description: Text("Tap + to import a PDF or image file"))
            }
        }
    }

    // Stable sorted list used by both ForEach and onDelete
    private var sortedDocuments: [PlanDocument] {
        project.documents.sorted(by: { ($0.createdAt, $0.syncID) > ($1.createdAt, $1.syncID) })
    }

    private static let maxDocumentSize = SitePinConstants.maxDocumentSize

    private func handleFileImport(_ result: Result<[URL], Error>) {
        switch result {
        case .failure(let error):
            errorMessage = "Failed to open file: \(error.localizedDescription)"
            return
        case .success(let urls):
            for url in urls {
                guard url.startAccessingSecurityScopedResource() else { continue }
                defer { url.stopAccessingSecurityScopedResource() }

                do {
                    let data = try Data(contentsOf: url)

                    // A4: Validate file size before loading into memory-intensive operations
                    if data.count > Self.maxDocumentSize {
                        let sizeMB = data.count / (1024 * 1024)
                        let maxMB = Self.maxDocumentSize / (1024 * 1024)
                        errorMessage = "\(url.lastPathComponent) is too large (\(sizeMB) MB). Maximum allowed is \(maxMB) MB."
                        return
                    }

                    let ext = url.pathExtension.lowercased()
                    let fileType: String
                    switch ext {
                    case "pdf": fileType = "pdf"
                    case "png": fileType = "png"
                    default: fileType = "jpg"
                    }
                    let name = url.deletingPathExtension().lastPathComponent

                    var pageCount = 1
                    if fileType == "pdf", let pdfDoc = PDFDocument(data: data) {
                        pageCount = pdfDoc.pageCount
                    }

                    let doc = PlanDocument(name: name, fileData: data, fileType: fileType, pageCount: pageCount)
                    doc.project = project
                    project.documents.append(doc)
                } catch {
                    errorMessage = "Failed to import \(url.lastPathComponent): \(error.localizedDescription)"
                }
            }
        }
    }

    private func exportPDFReport() {
        isExporting = true
        // Gather all data on main thread (SwiftData is not thread-safe)
        let snapshot = PDFExportService.snapshotProject(project)
        let projectName = sanitizeFileName(project.name)

        DispatchQueue.global(qos: .userInitiated).async {
            let pdfData = PDFExportService.exportFromSnapshot(snapshot)
            let fileName = "\(projectName)_report.pdf"
            let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
            do {
                try pdfData.write(to: tempURL)
                DispatchQueue.main.async {
                    shareItems = [tempURL]
                    showingShareSheet = true
                    isExporting = false
                }
            } catch {
                DispatchQueue.main.async {
                    exportError = "Failed to save PDF report: \(error.localizedDescription)"
                    showingExportError = true
                    isExporting = false
                }
            }
        }
    }

    private func exportCSV() {
        let projectName = sanitizeFileName(project.name)
        guard let url = CSVExportService.exportToFile(project, fileName: "\(projectName)_pins.csv") else {
            exportError = "Failed to create CSV file."
            showingExportError = true
            return
        }
        shareItems = [url]
        showingShareSheet = true
    }

    private func shareProject() {
        do {
            let url = try ProjectSharingService.exportToFile(project)
            shareItems = [url]
            showingShareSheet = true
        } catch {
            exportError = "Failed to export project: \(error.localizedDescription)"
            showingExportError = true
        }
    }

    private func handleSyncImport(_ result: Result<[URL], Error>) {
        guard case .success(let urls) = result, let url = urls.first else { return }
        do {
            try ProjectSharingService.mergeProject(from: url, into: project, context: modelContext)
            syncMessage = "Sync complete! New pins and comments have been merged."
        } catch {
            syncMessage = "Failed to sync: \(error.localizedDescription)"
        }
        showingSyncAlert = true
    }

    private func deleteDocuments(at offsets: IndexSet) {
        let sorted = sortedDocuments
        for index in offsets {
            modelContext.delete(sorted[index])
        }
    }

    private func sanitizeFileName(_ name: String) -> String {
        let sanitized = name.replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "\\", with: "_")
            .replacingOccurrences(of: ":", with: "_")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return sanitized.isEmpty ? "Untitled" : String(sanitized.prefix(100))
    }
}

// MARK: - Pin Dashboard

struct PinDashboardView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    let project: Project
    @State private var selectedTab = 0

    private var allPins: [(pin: Pin, document: PlanDocument)] {
        project.documents.flatMap { doc in
            doc.pins.map { (pin: $0, document: doc) }
        }.sorted(by: { $0.pin.createdAt < $1.pin.createdAt })
    }

    private var todoPins: [(pin: Pin, document: PlanDocument)] {
        allPins.filter { $0.pin.status == PinStatus.open.rawValue || $0.pin.status == PinStatus.inProgress.rawValue }
    }

    private var donePins: [(pin: Pin, document: PlanDocument)] {
        allPins.filter { $0.pin.status == PinStatus.resolved.rawValue }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("View", selection: $selectedTab) {
                    Text("To Do (\(todoPins.count))").tag(0)
                    Text("Done (\(donePins.count))").tag(1)
                }
                .pickerStyle(.segmented)
                .padding()

                let displayPins = selectedTab == 0 ? todoPins : donePins

                if displayPins.isEmpty {
                    ContentUnavailableView(
                        selectedTab == 0 ? "All Done!" : "No Completed Pins",
                        systemImage: selectedTab == 0 ? "checkmark.circle" : "circle",
                        description: Text(selectedTab == 0 ? "No open pins remaining" : "Resolve pins to see them here")
                    )
                } else {
                    List {
                        ForEach(Array(displayPins.enumerated()), id: \.element.pin.id) { _, entry in
                            PinDashboardRow(pin: entry.pin, documentName: entry.document.name)
                                .swipeActions(edge: .trailing) {
                                    if entry.pin.status != PinStatus.resolved.rawValue {
                                        Button {
                                            entry.pin.status = PinStatus.resolved.rawValue
                                            entry.pin.modifiedAt = Date()
                                        } label: {
                                            Label("Resolve", systemImage: PinStatus.resolved.icon)
                                        }
                                        .tint(PinStatus.resolved.color)
                                    } else {
                                        Button {
                                            entry.pin.status = PinStatus.open.rawValue
                                            entry.pin.modifiedAt = Date()
                                        } label: {
                                            Label("Reopen", systemImage: "arrow.uturn.backward")
                                        }
                                        .tint(PinStatus.open.color)
                                    }
                                }
                                .swipeActions(edge: .leading) {
                                    if entry.pin.status == PinStatus.open.rawValue {
                                        Button {
                                            entry.pin.status = PinStatus.inProgress.rawValue
                                            entry.pin.modifiedAt = Date()
                                        } label: {
                                            Label(PinStatus.inProgress.label, systemImage: PinStatus.inProgress.icon)
                                        }
                                        .tint(PinStatus.inProgress.color)
                                    }
                                }
                        }
                    }
                }
            }
            .navigationTitle("Pin Overview")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

struct PinDashboardRow: View {
    let pin: Pin
    let documentName: String

    var pinCategory: PinCategory {
        PinCategory(rawValue: pin.category) ?? .general
    }

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: pinCategory.icon)
                .foregroundStyle(pinCategory.color)
                .frame(width: 28)

            VStack(alignment: .leading, spacing: 2) {
                Text(pin.title.isEmpty ? "Untitled" : pin.title)
                    .font(.subheadline.weight(.medium))
                HStack(spacing: 6) {
                    Text(documentName)
                    if !pin.location.isEmpty {
                        Text("- \(pin.location)")
                    }
                }
                .font(.caption)
                .foregroundStyle(.secondary)
                if !pin.author.isEmpty {
                    Text(pin.author)
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                }
            }

            Spacer()

            statusBadge
        }
        .padding(.vertical, 2)
    }

    private var statusBadge: some View {
        let status = PinStatus(rawValue: pin.status) ?? .open
        return Image(systemName: status == .resolved ? "\(status.icon).fill" : (status == .inProgress ? "clock.fill" : status.icon))
            .foregroundStyle(status.color)
    }
}

struct ShareSheet: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
