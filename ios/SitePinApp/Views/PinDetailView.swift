import SwiftUI
import PhotosUI

struct PinDetailView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Bindable var pin: Pin
    @State private var showingCamera = false
    @State private var selectedPhotoItems: [PhotosPickerItem] = []
    @State private var showDeleteConfirm = false
    @State private var newComment = ""
    @State private var photoToDelete: PinPhoto?
    @State private var photoLimitMessage: String?

    private static let maxPhotosPerPin = SitePinConstants.maxPhotosPerPin

    var body: some View {
        NavigationStack {
            Form {
                Section("Pin Details") {
                    TextField("Title", text: $pin.title)
                    TextField("Description", text: $pin.pinDescription, axis: .vertical)
                        .lineLimit(3...6)

                    Picker("Category", selection: $pin.category) {
                        ForEach(PinCategory.allCases) { cat in
                            Label(cat.label, systemImage: cat.icon)
                                .tag(cat.rawValue)
                        }
                    }

                    Picker("Status", selection: $pin.status) {
                        ForEach(PinStatus.allCases) { status in
                            Label(status.label, systemImage: status.icon)
                                .tag(status.rawValue)
                        }
                    }
                }

                Section("Measurements & Location") {
                    HStack {
                        Image(systemName: "mappin")
                            .foregroundStyle(.secondary)
                        TextField("Location / Room", text: $pin.location)
                    }
                    HStack {
                        Image(systemName: "arrow.up.and.down")
                            .foregroundStyle(.secondary)
                        TextField("Height (e.g. 2.5m)", text: $pin.height)
                    }
                    HStack {
                        Image(systemName: "arrow.left.and.right")
                            .foregroundStyle(.secondary)
                        TextField("Width (e.g. 1.2m)", text: $pin.width)
                    }
                }

                Section("Photos (\(pin.photos.count))") {
                    if !pin.photos.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 12) {
                                ForEach(pin.photos) { photo in
                                    PhotoThumbnailView(photo: photo) {
                                        photoToDelete = photo
                                    }
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    }

                    HStack {
                        PhotosPicker(
                            selection: $selectedPhotoItems,
                            maxSelectionCount: 5,
                            matching: .images
                        ) {
                            Label("Photo Library", systemImage: "photo.on.rectangle")
                        }

                        Spacer()

                        Button(action: { showingCamera = true }) {
                            Label("Camera", systemImage: "camera")
                        }
                    }
                }

                // Comments / History
                Section("Comments (\(pin.comments.count))") {
                    ForEach(pin.comments.sorted(by: { $0.createdAt < $1.createdAt })) { comment in
                        VStack(alignment: .leading, spacing: 4) {
                            HStack {
                                Text(comment.author.isEmpty ? "Unknown" : comment.author)
                                    .font(.caption.bold())
                                Spacer()
                                Text(comment.createdAt, style: .relative)
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                            Text(comment.text)
                                .font(.subheadline)
                        }
                        .padding(.vertical, 2)
                    }

                    HStack {
                        TextField("Add comment...", text: $newComment)
                            .textFieldStyle(.roundedBorder)
                        Button(action: addComment) {
                            Image(systemName: "paperplane.fill")
                        }
                        .disabled(newComment.trimmingCharacters(in: .whitespaces).isEmpty)
                    }
                }

                Section("Info") {
                    if !pin.author.isEmpty {
                        LabeledContent("Author") {
                            Text(pin.author)
                        }
                    }
                    LabeledContent("Created") {
                        Text(pin.createdAt, style: .date)
                    }
                    LabeledContent("Position") {
                        Text(String(format: "%.1f%%, %.1f%%", pin.relativeX * 100, pin.relativeY * 100))
                            .foregroundStyle(.secondary)
                    }
                    if pin.pageIndex > 0 {
                        LabeledContent("Page") {
                            Text("\(pin.pageIndex + 1)")
                        }
                    }
                }

                Section {
                    Button("Delete Pin", role: .destructive) {
                        showDeleteConfirm = true
                    }
                }
            }
            .navigationTitle(pin.title.isEmpty ? "New Pin" : pin.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        pin.modifiedAt = Date()
                        dismiss()
                    }
                }
            }
            .onChange(of: selectedPhotoItems) { _, items in
                Task {
                    for item in items {
                        guard pin.photos.count < Self.maxPhotosPerPin else {
                            photoLimitMessage = "Maximum of \(Self.maxPhotosPerPin) photos per pin reached."
                            break
                        }
                        if let data = try? await item.loadTransferable(type: Data.self) {
                            if let uiImage = UIImage(data: data),
                               let compressed = uiImage.jpegData(compressionQuality: SitePinConstants.jpegCompressionQuality) {
                                let photo = PinPhoto(imageData: compressed)
                                photo.pin = pin
                                pin.photos.append(photo)
                            }
                        }
                    }
                    selectedPhotoItems = []
                }
            }
            .fullScreenCover(isPresented: $showingCamera) {
                CameraView { imageData in
                    guard pin.photos.count < Self.maxPhotosPerPin else {
                        photoLimitMessage = "Maximum of \(Self.maxPhotosPerPin) photos per pin reached."
                        return
                    }
                    let photo = PinPhoto(imageData: imageData)
                    photo.pin = pin
                    pin.photos.append(photo)
                }
            }
            .alert("Photo Limit", isPresented: Binding(
                get: { photoLimitMessage != nil },
                set: { if !$0 { photoLimitMessage = nil } }
            )) {
                Button("OK") { photoLimitMessage = nil }
            } message: {
                Text(photoLimitMessage ?? "")
            }
            .alert("Delete Pin?", isPresented: $showDeleteConfirm) {
                Button("Delete", role: .destructive) {
                    modelContext.delete(pin)
                    dismiss()
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("This will permanently delete the pin and all attached photos.")
            }
            .alert("Delete Photo?", isPresented: Binding(
                get: { photoToDelete != nil },
                set: { if !$0 { photoToDelete = nil } }
            )) {
                Button("Delete", role: .destructive) {
                    if let photo = photoToDelete {
                        modelContext.delete(photo)
                        photoToDelete = nil
                    }
                }
                Button("Cancel", role: .cancel) {
                    photoToDelete = nil
                }
            } message: {
                Text("This photo will be permanently removed.")
            }
        }
    }

    private func addComment() {
        let text = newComment.trimmingCharacters(in: .whitespaces)
        guard !text.isEmpty else { return }
        let comment = PinComment(text: text)
        comment.pin = pin
        pin.comments.append(comment)
        newComment = ""
    }
}

struct PhotoThumbnailView: View {
    let photo: PinPhoto
    let onDelete: () -> Void
    @State private var showFullScreen = false

    var body: some View {
        if let uiImage = UIImage(data: photo.imageData) {
            Image(uiImage: uiImage)
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: 80, height: 80)
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .overlay(alignment: .topTrailing) {
                    Button(action: onDelete) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.caption)
                            .foregroundStyle(.white, .red)
                    }
                    .offset(x: 4, y: -4)
                }
                .onTapGesture { showFullScreen = true }
                .fullScreenCover(isPresented: $showFullScreen) {
                    FullScreenPhotoView(image: uiImage)
                }
        }
    }
}

struct FullScreenPhotoView: View {
    let image: UIImage
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black.ignoresSafeArea()
            Image(uiImage: image)
                .resizable()
                .aspectRatio(contentMode: .fit)
            Button(action: { dismiss() }) {
                Image(systemName: "xmark.circle.fill")
                    .font(.title)
                    .foregroundStyle(.white.opacity(0.8))
                    .padding()
            }
        }
    }
}
