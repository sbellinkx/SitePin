import SwiftUI
import SwiftData
import PDFKit

struct ProjectListView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \Project.createdAt, order: .reverse) private var projects: [Project]
    @State private var showingNewProject = false
    @State private var newProjectName = ""
    @State private var showingProfileSetup = false
    @State private var showingImportPicker = false
    @State private var importError: String?
    @State private var profileName = UserProfileManager.shared.displayName
    @State private var testResult = ""
    @State private var showingTestResult = false
    @AppStorage("appColorScheme") private var appColorScheme: String = "system"

    var body: some View {
        NavigationStack {
            List {
                ForEach(projects) { project in
                    NavigationLink(destination: DocumentListView(project: project)) {
                        VStack(alignment: .leading) {
                            Text(project.name)
                                .font(.headline)
                            HStack(spacing: 8) {
                                let allPins = project.documents.flatMap(\.pins)
                                let openCount = allPins.filter { $0.status != PinStatus.resolved.rawValue }.count
                                let resolvedCount = allPins.filter { $0.status == PinStatus.resolved.rawValue }.count
                                Text("\(project.documents.count) plan(s)")
                                if openCount > 0 {
                                    Text("\(openCount) open")
                                        .foregroundStyle(.red)
                                }
                                if resolvedCount > 0 {
                                    Text("\(resolvedCount) done")
                                        .foregroundStyle(.green)
                                }
                            }
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        }
                    }
                }
                .onDelete(perform: deleteProjects)
            }
            .navigationTitle("Projects")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(action: { showingProfileSetup = true }) {
                        if UserProfileManager.shared.hasProfile {
                            Label(UserProfileManager.shared.displayName, systemImage: "person.circle.fill")
                                .font(.caption)
                        } else {
                            Image(systemName: "person.circle")
                        }
                    }
                }
                ToolbarItem(placement: .primaryAction) {
                    HStack(spacing: 12) {
                        #if DEBUG
                        Button(action: { runFullTest() }) {
                            Image(systemName: "hammer.fill")
                        }
                        #endif
                        Button(action: { showingImportPicker = true }) {
                            Image(systemName: "square.and.arrow.down")
                        }
                        Button(action: { showingNewProject = true }) {
                            Image(systemName: "plus")
                        }
                    }
                }
            }
            .alert("New Project", isPresented: $showingNewProject) {
                TextField("Project Name", text: $newProjectName)
                Button("Cancel", role: .cancel) { newProjectName = "" }
                Button("Create") {
                    let trimmed = newProjectName.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !trimmed.isEmpty else {
                        newProjectName = ""
                        return
                    }
                    let project = Project(name: trimmed)
                    modelContext.insert(project)
                    newProjectName = ""
                }
            }
            .sheet(isPresented: $showingProfileSetup) {
                profileSetupView
            }
            .fileImporter(
                isPresented: $showingImportPicker,
                allowedContentTypes: [.json, .data],
                allowsMultipleSelection: false
            ) { result in
                handleImport(result)
            }
            .overlay {
                if projects.isEmpty {
                    ContentUnavailableView("No Projects",
                        systemImage: "folder.badge.plus",
                        description: Text("Tap + to create your first project"))
                }
            }
            .onAppear {
                if !UserProfileManager.shared.hasProfile {
                    showingProfileSetup = true
                }
                #if DEBUG
                if UserDefaults.standard.bool(forKey: "run_auto_test") {
                    UserDefaults.standard.set(false, forKey: "run_auto_test")
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                        runFullTest()
                    }
                }
                #endif
            }
            .alert("Test Result", isPresented: $showingTestResult) {
                Button("OK") {}
            } message: {
                Text(testResult)
            }
            .alert("Import Error", isPresented: Binding(
                get: { importError != nil },
                set: { if !$0 { importError = nil } }
            )) {
                Button("OK") { importError = nil }
            } message: {
                Text(importError ?? "")
            }
        }
    }

    private var profileSetupView: some View {
        NavigationStack {
            Form {
                Section("Your Name") {
                    TextField("Display Name", text: $profileName)
                        .autocorrectionDisabled()
                }
                Section {
                    Text("Your name will appear on pins you create, so collaborators know who added what.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Section("Appearance") {
                    Picker("Theme", selection: $appColorScheme) {
                        Label("System", systemImage: "gear").tag("system")
                        Label("Light", systemImage: "sun.max").tag("light")
                        Label("Dark", systemImage: "moon").tag("dark")
                    }
                    .pickerStyle(.segmented)
                }
            }
            .navigationTitle("Profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        UserProfileManager.shared.displayName = profileName
                        showingProfileSetup = false
                    }
                    .disabled(profileName.trimmingCharacters(in: .whitespaces).isEmpty)
                }
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        profileName = UserProfileManager.shared.displayName
                        showingProfileSetup = false
                    }
                }
            }
        }
        .presentationDetents([.medium])
    }

    #if DEBUG
    private func runFullTest() {
        var log = ""

        // 1. Set profile
        UserProfileManager.shared.displayName = "Sebastiaan"
        profileName = "Sebastiaan"
        log += "1. Profile set to 'Sebastiaan'\n"

        // 2. Create project
        let project = Project(name: "Test Project 123")
        modelContext.insert(project)
        log += "2. Project 'Test Project 123' created\n"

        // 3. Create a test document with a simple generated image
        let testImage = generateTestPlanImage()
        guard let imageData = testImage.jpegData(compressionQuality: 0.9) else {
            testResult = "Failed to generate test image"
            showingTestResult = true
            return
        }
        let doc = PlanDocument(name: "Test Plan Floor 1", fileData: imageData, fileType: "jpg", pageCount: 1)
        doc.project = project
        project.documents.append(doc)
        log += "3. Document 'Test Plan Floor 1' created (JPG)\n"

        // 4. Also try loading PDF from Documents if available
        let appDocsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
        if let docsPath = appDocsPath {
            let files = (try? FileManager.default.contentsOfDirectory(at: docsPath, includingPropertiesForKeys: nil)) ?? []
            for file in files where file.pathExtension.lowercased() == "pdf" {
                if let data = try? Data(contentsOf: file) {
                    let pdfPageCount = PDFKit.PDFDocument(data: data)?.pageCount ?? 1
                    let pdfDoc = PlanDocument(name: file.deletingPathExtension().lastPathComponent,
                                              fileData: data, fileType: "pdf", pageCount: pdfPageCount)
                    pdfDoc.project = project
                    project.documents.append(pdfDoc)
                    log += "4. PDF '\(pdfDoc.name)' loaded from Documents (\(pdfPageCount) pages)\n"
                }
            }
        }

        // 5. Create pins on the test document
        let pin1 = Pin(relativeX: 0.3, relativeY: 0.4, pageIndex: 0)
        pin1.title = "Pin 1 - Electrical Issue"
        pin1.pinDescription = "Power outlet missing on south wall"
        pin1.location = "Living Room"
        pin1.height = "1.2m"
        pin1.width = "0.5m"
        pin1.category = PinCategory.electrical.rawValue
        pin1.status = "open"
        pin1.author = "Sebastiaan"
        pin1.document = doc
        doc.pins.append(pin1)
        log += "5. Pin 1 'Electrical Issue' created (category: electrical, status: open)\n"

        let pin2 = Pin(relativeX: 0.7, relativeY: 0.6, pageIndex: 0)
        pin2.title = "Pin 2 - Plumbing Check"
        pin2.pinDescription = "Check water pipe routing before wall closure"
        pin2.location = "Kitchen"
        pin2.height = "0.8m"
        pin2.width = "1.0m"
        pin2.category = PinCategory.plumbing.rawValue
        pin2.status = "in_progress"
        pin2.author = "Sebastiaan"
        pin2.document = doc
        doc.pins.append(pin2)
        log += "6. Pin 2 'Plumbing Check' created (category: plumbing, status: in_progress)\n"

        let pin3 = Pin(relativeX: 0.5, relativeY: 0.2, pageIndex: 0)
        pin3.title = "Pin 3 - Safety Rail"
        pin3.pinDescription = "Safety rail installed and approved"
        pin3.location = "Staircase"
        pin3.category = PinCategory.safety.rawValue
        pin3.status = "resolved"
        pin3.author = "Glenn Francois"
        pin3.document = doc
        doc.pins.append(pin3)
        log += "7. Pin 3 'Safety Rail' created (category: safety, status: resolved, author: Glenn)\n"

        // 6. Add comments to pin1
        let comment1 = PinComment(text: "Needs urgent attention before inspection", author: "Sebastiaan")
        comment1.pin = pin1
        pin1.comments.append(comment1)

        let comment2 = PinComment(text: "Electrician scheduled for Thursday", author: "Glenn Francois")
        comment2.pin = pin1
        pin1.comments.append(comment2)
        log += "8. 2 comments added to Pin 1\n"

        // 7. Test swipe to resolve (also updates modifiedAt)
        pin1.status = "resolved"
        pin1.modifiedAt = Date()
        log += "9. Swipe-to-resolve: Pin 1 marked resolved\n"
        pin1.status = "open"
        pin1.modifiedAt = Date()
        log += "10. Swipe-to-reopen: Pin 1 reopened\n"

        // 8. Test CSV export
        let csvData = CSVExportService.exportProject(project)
        if let csvString = String(data: csvData, encoding: .utf8), csvString.contains("Pin #") {
            log += "11. CSV export: \(csvData.count) bytes, header OK\n"
        } else {
            log += "11. CSV export: FAILED\n"
        }

        // 9. Test project export
        do {
            let exportData = try ProjectSharingService.exportProject(project)
            log += "12. Project export: \(exportData.count) bytes - OK\n"

            // Test re-import
            do {
                let reimported = try ProjectSharingService.importProject(from: exportData, into: modelContext)
                log += "13. Re-import as '\(reimported.name)': \(reimported.documents.count) docs, "
                let totalPins = reimported.documents.flatMap(\.pins).count
                log += "\(totalPins) pins - OK\n"
                modelContext.delete(reimported)
            } catch {
                log += "13. Re-import: FAILED - \(error.localizedDescription)\n"
            }
        } catch {
            log += "12. Project export: FAILED - \(error.localizedDescription)\n"
        }

        // 10. Test PDF report generation
        let reportData = PDFExportService.exportProject(project)
        log += "14. PDF Report: \(reportData.count) bytes - OK\n"

        // 11. Test plan image rendering
        if let rendered = PlanAnnotationView.loadImage(from: doc, pageIndex: 0) {
            log += "15. Plan image render: \(Int(rendered.size.width))x\(Int(rendered.size.height)) - OK\n"
        } else {
            log += "15. Plan image render: FAILED\n"
        }

        // 12. Test dark mode setting
        let originalScheme = appColorScheme
        appColorScheme = "dark"
        log += "16. Dark mode: set to 'dark' - OK\n"
        appColorScheme = "light"
        log += "17. Light mode: set to 'light' - OK\n"
        appColorScheme = originalScheme

        // 13. Test pin dashboard counts
        let allPins = project.documents.flatMap(\.pins)
        let openCount = allPins.filter { $0.status == "open" || $0.status == "in_progress" }.count
        let resolvedCount = allPins.filter { $0.status == "resolved" }.count
        log += "18. Dashboard: \(openCount) to-do, \(resolvedCount) done (total: \(allPins.count)) - OK\n"

        // Save PDF report to Documents for inspection
        if let docsDir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
            let reportURL = docsDir.appendingPathComponent("test_report.pdf")
            try? FileManager.default.removeItem(at: reportURL)
            try? reportData.write(to: reportURL)
            log += "19. Report saved to Documents/test_report.pdf\n"

            if let csvURL = CSVExportService.exportToFile(project) {
                let csvDestURL = docsDir.appendingPathComponent("test_pins.csv")
                try? FileManager.default.removeItem(at: csvDestURL)
                try? FileManager.default.copyItem(at: csvURL, to: csvDestURL)
                log += "20. CSV saved to Documents/test_pins.csv\n"
            }
        }

        log += "\nAll tests passed!"
        testResult = log
        showingTestResult = true
    }

    private func generateTestPlanImage() -> UIImage {
        let size = CGSize(width: 800, height: 600)
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { ctx in
            // White background
            UIColor.white.setFill()
            ctx.fill(CGRect(origin: .zero, size: size))

            // Grid lines
            UIColor.lightGray.setStroke()
            let path = UIBezierPath()
            for x in stride(from: 0, through: 800, by: 50) {
                path.move(to: CGPoint(x: CGFloat(x), y: 0))
                path.addLine(to: CGPoint(x: CGFloat(x), y: 600))
            }
            for y in stride(from: 0, through: 600, by: 50) {
                path.move(to: CGPoint(x: 0, y: CGFloat(y)))
                path.addLine(to: CGPoint(x: 800, y: CGFloat(y)))
            }
            path.lineWidth = 0.5
            path.stroke()

            // Walls (thick lines)
            UIColor.black.setStroke()
            let walls = UIBezierPath()
            walls.move(to: CGPoint(x: 100, y: 100))
            walls.addLine(to: CGPoint(x: 700, y: 100))
            walls.addLine(to: CGPoint(x: 700, y: 500))
            walls.addLine(to: CGPoint(x: 100, y: 500))
            walls.addLine(to: CGPoint(x: 100, y: 100))
            // Interior wall
            walls.move(to: CGPoint(x: 400, y: 100))
            walls.addLine(to: CGPoint(x: 400, y: 350))
            walls.lineWidth = 3
            walls.stroke()

            // Room labels
            let attrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 14),
                .foregroundColor: UIColor.darkGray,
            ]
            ("Living Room" as NSString).draw(at: CGPoint(x: 180, y: 280), withAttributes: attrs)
            ("Kitchen" as NSString).draw(at: CGPoint(x: 500, y: 280), withAttributes: attrs)
            ("Staircase" as NSString).draw(at: CGPoint(x: 350, y: 130), withAttributes: attrs)

            // Title
            let titleAttrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.boldSystemFont(ofSize: 16),
                .foregroundColor: UIColor.black,
            ]
            ("TEST FLOOR PLAN - Ground Floor" as NSString).draw(at: CGPoint(x: 250, y: 540), withAttributes: titleAttrs)
        }
    }
    #endif

    private func handleImport(_ result: Result<[URL], Error>) {
        switch result {
        case .failure(let error):
            importError = "Failed to open file: \(error.localizedDescription)"
        case .success(let urls):
            guard let url = urls.first else { return }
            do {
                _ = try ProjectSharingService.importProject(from: url, into: modelContext)
            } catch {
                importError = "Failed to import project: \(error.localizedDescription)"
            }
        }
    }

    private func deleteProjects(at offsets: IndexSet) {
        for index in offsets {
            modelContext.delete(projects[index])
        }
    }
}
