import SwiftUI
import PDFKit

struct PlanAnnotationView: View {
    @Environment(\.modelContext) private var modelContext
    let document: PlanDocument
    @State private var selectedPin: Pin?
    @State private var isPlacingPin = false
    @State private var planImage: UIImage?
    @State private var currentPage: Int = 0
    @State private var showingFilter = false
    @State private var filterCategory: PinCategory?
    @State private var filterStatus: String?

    var filteredPins: [Pin] {
        document.pins.filter { pin in
            pin.pageIndex == currentPage &&
            (filterCategory.map { pin.category == $0.rawValue } ?? true) &&
            (filterStatus.map { pin.status == $0 } ?? true)
        }
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            if let image = planImage {
                ImagePlanView(
                    image: image,
                    pins: filteredPins,
                    isPlacingPin: $isPlacingPin,
                    selectedPin: $selectedPin,
                    onPinPlaced: { relX, relY in
                        addPin(relativeX: relX, relativeY: relY)
                    }
                )
            } else {
                ProgressView("Loading plan...")
            }

            VStack(spacing: 8) {
                // Filter bar
                if showingFilter {
                    filterBar
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }

                HStack {
                    // Page navigation (multi-page PDF)
                    if document.pageCount > 1 {
                        HStack(spacing: 4) {
                            Button(action: { changePage(-1) }) {
                                Image(systemName: "chevron.left")
                                    .font(.caption)
                            }
                            .disabled(currentPage == 0)

                            Text("\(currentPage + 1)/\(document.pageCount)")
                                .font(.caption.monospacedDigit())
                                .padding(.horizontal, 6)

                            Button(action: { changePage(1) }) {
                                Image(systemName: "chevron.right")
                                    .font(.caption)
                            }
                            .disabled(currentPage >= document.pageCount - 1)
                        }
                        .padding(8)
                        .background(.ultraThinMaterial)
                        .clipShape(Capsule())
                    }

                    Spacer()

                    // Filter toggle
                    Button(action: { withAnimation { showingFilter.toggle() } }) {
                        Image(systemName: (filterCategory != nil || filterStatus != nil) ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle")
                            .font(.title3)
                            .foregroundStyle(.white)
                            .padding(10)
                            .background(.ultraThinMaterial)
                            .clipShape(Circle())
                    }

                    // Place pin toggle
                    Button(action: { isPlacingPin.toggle() }) {
                        Image(systemName: isPlacingPin ? "mappin.slash" : "mappin.and.ellipse")
                            .font(.title2)
                            .foregroundStyle(.white)
                            .padding()
                            .background(isPlacingPin ? Color.red : Color.blue)
                            .clipShape(Circle())
                            .shadow(radius: 4)
                    }
                }
                .padding(.horizontal)
                .padding(.bottom, 4)
            }
        }
        .navigationTitle(document.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Text("\(document.pins.count) pins")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .sheet(item: $selectedPin) { pin in
            PinDetailView(pin: pin)
        }
        .onAppear {
            planImage = Self.loadImage(from: document, pageIndex: currentPage)
        }
    }

    private var filterBar: some View {
        VStack(spacing: 8) {
            // Category filter
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    filterChip(label: "All", isActive: filterCategory == nil) {
                        filterCategory = nil
                    }
                    ForEach(PinCategory.allCases) { cat in
                        filterChip(label: cat.label, icon: cat.icon, color: cat.color,
                                   isActive: filterCategory == cat) {
                            filterCategory = filterCategory == cat ? nil : cat
                        }
                    }
                }
                .padding(.horizontal)
            }

            // Status filter
            HStack(spacing: 6) {
                filterChip(label: "All", isActive: filterStatus == nil) {
                    filterStatus = nil
                }
                ForEach(PinStatus.allCases) { status in
                    filterChip(label: status.label, color: status.color,
                               isActive: filterStatus == status.rawValue) {
                        filterStatus = filterStatus == status.rawValue ? nil : status.rawValue
                    }
                }
            }
        }
        .padding(.vertical, 8)
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal)
    }

    private func filterChip(label: String, icon: String? = nil, color: Color = .primary,
                             isActive: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 3) {
                if let icon {
                    Image(systemName: icon)
                        .font(.system(size: 9))
                }
                Text(label)
                    .font(.caption2)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 5)
            .background(isActive ? color.opacity(0.3) : Color.clear)
            .foregroundStyle(isActive ? color : .secondary)
            .clipShape(Capsule())
            .overlay(Capsule().stroke(isActive ? color : .secondary.opacity(0.3), lineWidth: 1))
        }
    }

    private func changePage(_ delta: Int) {
        let newPage = currentPage + delta
        guard newPage >= 0, newPage < document.pageCount else { return }
        currentPage = newPage
        planImage = Self.loadImage(from: document, pageIndex: currentPage)
    }

    private func addPin(relativeX: Double, relativeY: Double) {
        let pin = Pin(relativeX: relativeX, relativeY: relativeY, pageIndex: currentPage)
        pin.document = document
        document.pins.append(pin)
        selectedPin = pin
        isPlacingPin = false
    }

    static func loadImage(from document: PlanDocument, pageIndex: Int = 0) -> UIImage? {
        if document.fileType == "pdf" {
            guard let pdfDoc = PDFDocument(data: document.fileData),
                  let page = pdfDoc.page(at: pageIndex) else { return nil }
            let pageBounds = page.bounds(for: .mediaBox)
            let maxDimension = SitePinConstants.maxImageDimension
            let naturalScale: CGFloat = 3.0
            let maxScale = maxDimension / max(pageBounds.width, pageBounds.height)
            let scale = min(naturalScale, maxScale)
            let size = CGSize(width: pageBounds.width * scale, height: pageBounds.height * scale)
            let renderer = UIGraphicsImageRenderer(size: size)
            return renderer.image { ctx in
                ctx.cgContext.setFillColor(UIColor.white.cgColor)
                ctx.cgContext.fill(CGRect(origin: .zero, size: size))
                ctx.cgContext.translateBy(x: 0, y: size.height)
                ctx.cgContext.scaleBy(x: scale, y: -scale)
                page.draw(with: .mediaBox, to: ctx.cgContext)
            }
        } else {
            return UIImage(data: document.fileData)
        }
    }
}

// MARK: - Unified Image Plan View

struct ImagePlanView: View {
    let image: UIImage
    let pins: [Pin]
    @Binding var isPlacingPin: Bool
    @Binding var selectedPin: Pin?
    let onPinPlaced: (Double, Double) -> Void

    @State private var scale: CGFloat = 1.0
    @State private var lastScale: CGFloat = 1.0
    @State private var offset: CGSize = .zero
    @State private var lastOffset: CGSize = .zero

    var body: some View {
        GeometryReader { geo in
            let imageAspect = image.size.width / max(image.size.height, 1)
            let viewAspect = geo.size.width / max(geo.size.height, 1)
            let fittedSize: CGSize = {
                if imageAspect > viewAspect {
                    let w = geo.size.width
                    return CGSize(width: w, height: w / max(imageAspect, 0.001))
                } else {
                    let h = geo.size.height
                    return CGSize(width: h * imageAspect, height: h)
                }
            }()

            ZStack {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: .fit)

                let sortedPins = pins.sorted(by: { $0.createdAt < $1.createdAt })
                ForEach(Array(sortedPins.enumerated()), id: \.element.id) { index, pin in
                    PinMarkerView(pin: pin, isSelected: selectedPin?.id == pin.id, number: index + 1)
                        .position(
                            x: pin.relativeX * fittedSize.width,
                            y: pin.relativeY * fittedSize.height
                        )
                        .onTapGesture {
                            selectedPin = pin
                        }
                }
            }
            .frame(width: fittedSize.width, height: fittedSize.height)
            .scaleEffect(scale)
            .offset(offset)
            .frame(width: geo.size.width, height: geo.size.height)
            .contentShape(Rectangle())
            .onTapGesture { location in
                guard isPlacingPin else { return }
                let centerX = geo.size.width / 2
                let centerY = geo.size.height / 2
                let imageOriginX = centerX - (fittedSize.width * scale) / 2 + offset.width
                let imageOriginY = centerY - (fittedSize.height * scale) / 2 + offset.height

                let relX = (location.x - imageOriginX) / (fittedSize.width * scale)
                let relY = (location.y - imageOriginY) / (fittedSize.height * scale)

                guard relX >= 0, relX <= 1, relY >= 0, relY <= 1 else { return }
                onPinPlaced(relX, relY)
            }
            .simultaneousGesture(
                MagnifyGesture()
                    .onChanged { value in
                        scale = lastScale * value.magnification
                    }
                    .onEnded { value in
                        lastScale = max(1.0, scale)
                        scale = lastScale
                    }
            )
            .simultaneousGesture(
                DragGesture(minimumDistance: 10)
                    .onChanged { value in
                        offset = CGSize(
                            width: lastOffset.width + value.translation.width,
                            height: lastOffset.height + value.translation.height
                        )
                    }
                    .onEnded { _ in
                        lastOffset = offset
                    }
            )
        }
        .clipped()
        .onChange(of: image) { _, _ in
            // Reset zoom/pan when page changes
            scale = 1.0
            lastScale = 1.0
            offset = .zero
            lastOffset = .zero
        }
    }
}

// MARK: - Pin Marker

struct PinMarkerView: View {
    let pin: Pin
    let isSelected: Bool
    var number: Int = 0

    var pinColor: Color {
        PinCategory(rawValue: pin.category)?.color ?? .red
    }

    var body: some View {
        ZStack {
            Circle()
                .fill(pinColor)
                .frame(width: 24, height: 24)
            Circle()
                .stroke(.white, lineWidth: 2)
                .frame(width: 24, height: 24)
            Text("\(number)")
                .font(.system(size: 10, weight: .bold))
                .foregroundStyle(.white)
        }
        .scaleEffect(isSelected ? 1.3 : 1.0)
        .animation(.spring(duration: 0.2), value: isSelected)
        .shadow(color: pinColor.opacity(0.5), radius: 4)
    }
}
