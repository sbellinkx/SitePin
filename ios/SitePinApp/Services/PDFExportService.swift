import UIKit
import PDFKit

// MARK: - Snapshot types (thread-safe copies of SwiftData models)

struct ProjectSnapshot {
    let name: String
    let documents: [DocumentSnapshot]
}

struct DocumentSnapshot {
    let name: String
    let fileType: String
    let fileData: Data
    let pageCount: Int
    let pins: [PinSnapshot]
}

struct PinSnapshot {
    let number: Int
    let title: String
    let pinDescription: String
    let location: String
    let height: String
    let width: String
    let status: String
    let category: String
    let author: String
    let relativeX: Double
    let relativeY: Double
    let pageIndex: Int
    let createdAt: Date
    let photoData: [Data]
}

struct PDFExportService {

    // MARK: - Snapshot from SwiftData (must be called on main thread)

    static func snapshotProject(_ project: Project) -> ProjectSnapshot {
        var counter = 1
        let docs = project.documents.sorted(by: { $0.createdAt < $1.createdAt }).map { doc in
            let pins = doc.pins.sorted(by: { $0.createdAt < $1.createdAt }).map { pin -> PinSnapshot in
                let snap = PinSnapshot(
                    number: counter,
                    title: pin.title,
                    pinDescription: pin.pinDescription,
                    location: pin.location,
                    height: pin.height,
                    width: pin.width,
                    status: pin.status,
                    category: pin.category,
                    author: pin.author,
                    relativeX: pin.relativeX,
                    relativeY: pin.relativeY,
                    pageIndex: pin.pageIndex,
                    createdAt: pin.createdAt,
                    photoData: pin.photos.map(\.imageData)
                )
                counter += 1
                return snap
            }
            return DocumentSnapshot(
                name: doc.name,
                fileType: doc.fileType,
                fileData: doc.fileData,
                pageCount: doc.pageCount,
                pins: pins
            )
        }
        return ProjectSnapshot(name: project.name, documents: docs)
    }

    // MARK: - Legacy entry point (runs on main thread, fine for test)

    static func exportProject(_ project: Project) -> Data {
        let snapshot = snapshotProject(project)
        return exportFromSnapshot(snapshot)
    }

    // MARK: - Thread-safe export from snapshot

    static func exportFromSnapshot(_ snapshot: ProjectSnapshot) -> Data {
        let pageWidth = SitePinConstants.pdfPageWidth
        let pageHeight = SitePinConstants.pdfPageHeight
        let margin = SitePinConstants.pdfMargin
        let contentWidth = pageWidth - 2 * margin

        let allPins = snapshot.documents.flatMap(\.pins)

        let renderer = UIGraphicsPDFRenderer(bounds: CGRect(x: 0, y: 0, width: pageWidth, height: pageHeight))

        let data = renderer.pdfData { context in
            // --- Title page ---
            context.beginPage()
            drawTitlePage(projectName: snapshot.name, docCount: snapshot.documents.count, totalPins: allPins.count,
                          pageWidth: pageWidth, pageHeight: pageHeight, margin: margin)

            // --- Per-document overview pages ---
            for doc in snapshot.documents {
                context.beginPage()
                drawDocumentOverview(document: doc,
                                     pageWidth: pageWidth, pageHeight: pageHeight, margin: margin, contentWidth: contentWidth)
            }

            // --- Per-pin detail pages ---
            for doc in snapshot.documents {
                for pin in doc.pins {
                    drawPinDetailPages(context: context, pin: pin, document: doc,
                                       pageWidth: pageWidth, pageHeight: pageHeight, margin: margin, contentWidth: contentWidth)
                }
            }
        }

        return data
    }

    // MARK: - Title Page

    private static func drawTitlePage(projectName: String, docCount: Int, totalPins: Int,
                                       pageWidth: CGFloat, pageHeight: CGFloat, margin: CGFloat) {
        let titleAttrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 28, weight: .bold),
            .foregroundColor: UIColor.black,
        ]
        let subtitleAttrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 16, weight: .regular),
            .foregroundColor: UIColor.darkGray,
        ]
        let infoAttrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 13, weight: .regular),
            .foregroundColor: UIColor.gray,
        ]

        let title = projectName as NSString
        let titleSize = title.boundingRect(with: CGSize(width: pageWidth - 2 * margin, height: 200),
                                            options: .usesLineFragmentOrigin, attributes: titleAttrs, context: nil)
        let titleY = pageHeight * 0.35
        title.draw(in: CGRect(x: margin, y: titleY, width: pageWidth - 2 * margin, height: titleSize.height),
                   withAttributes: titleAttrs)

        let subtitle = "Site Inspection Report" as NSString
        subtitle.draw(in: CGRect(x: margin, y: titleY + titleSize.height + 12, width: pageWidth - 2 * margin, height: 30),
                      withAttributes: subtitleAttrs)

        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .long
        let info = "\(dateFormatter.string(from: Date()))  |  \(docCount) document(s)  |  \(totalPins) pin(s)" as NSString
        info.draw(in: CGRect(x: margin, y: titleY + titleSize.height + 50, width: pageWidth - 2 * margin, height: 25),
                  withAttributes: infoAttrs)

        guard let ctx = UIGraphicsGetCurrentContext() else { return }
        ctx.setStrokeColor(UIColor.systemBlue.cgColor)
        ctx.setLineWidth(2)
        ctx.move(to: CGPoint(x: margin, y: titleY + titleSize.height + 85))
        ctx.addLine(to: CGPoint(x: pageWidth - margin, y: titleY + titleSize.height + 85))
        ctx.strokePath()
    }

    // MARK: - Document Overview

    private static func drawDocumentOverview(document: DocumentSnapshot,
                                              pageWidth: CGFloat, pageHeight: CGFloat, margin: CGFloat, contentWidth: CGFloat) {
        let headerAttrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 18, weight: .bold),
            .foregroundColor: UIColor.black,
        ]
        let name = document.name as NSString
        name.draw(at: CGPoint(x: margin, y: margin), withAttributes: headerAttrs)

        let captionAttrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 11, weight: .regular),
            .foregroundColor: UIColor.gray,
        ]
        let caption = "\(document.fileType.uppercased()) - \(document.pins.count) pin(s)" as NSString
        caption.draw(at: CGPoint(x: margin, y: margin + 26), withAttributes: captionAttrs)

        let imageTop: CGFloat = margin + 50
        let maxImageHeight = pageHeight - imageTop - margin
        if let planImage = renderPlanImage(fileData: document.fileData, fileType: document.fileType,
                                            maxSize: CGSize(width: contentWidth, height: maxImageHeight)) {
            let imageRect = fitRect(imageSize: planImage.size, into: CGSize(width: contentWidth, height: maxImageHeight), origin: CGPoint(x: margin, y: imageTop))
            planImage.draw(in: imageRect)

            guard let ctx = UIGraphicsGetCurrentContext() else { return }
            for pin in document.pins {
                let pinX = imageRect.origin.x + CGFloat(pin.relativeX) * imageRect.width
                let pinY = imageRect.origin.y + CGFloat(pin.relativeY) * imageRect.height
                drawPinMarker(context: ctx, center: CGPoint(x: pinX, y: pinY), number: pin.number, color: pinColor(for: pin), radius: 10)
            }
        }
    }

    // MARK: - Pin Detail Pages

    private static func drawPinDetailPages(context: UIGraphicsPDFRendererContext, pin: PinSnapshot, document: DocumentSnapshot,
                                            pageWidth: CGFloat, pageHeight: CGFloat, margin: CGFloat, contentWidth: CGFloat) {
        context.beginPage()
        var cursorY: CGFloat = margin

        // Header
        let headerAttrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 20, weight: .bold),
            .foregroundColor: UIColor.black,
        ]
        let header = "Pin #\(pin.number) - \(pin.title.isEmpty ? "Untitled" : pin.title)" as NSString
        let headerRect = header.boundingRect(with: CGSize(width: contentWidth, height: 100),
                                              options: .usesLineFragmentOrigin, attributes: headerAttrs, context: nil)
        header.draw(in: CGRect(x: margin, y: cursorY, width: contentWidth, height: headerRect.height), withAttributes: headerAttrs)
        cursorY += headerRect.height + 8

        // Status badge
        let parsedStatus = PinStatus(rawValue: pin.status) ?? .open
        let statusText = parsedStatus.label.uppercased()
        let statusColor = parsedStatus.uiColor
        let badgeAttrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 10, weight: .bold),
            .foregroundColor: UIColor.white,
        ]
        let badgeSize = (statusText as NSString).size(withAttributes: badgeAttrs)
        let badgeRect = CGRect(x: margin, y: cursorY, width: badgeSize.width + 12, height: badgeSize.height + 6)
        guard let ctx = UIGraphicsGetCurrentContext() else { return }
        ctx.setFillColor(statusColor.cgColor)
        let badgePath = UIBezierPath(roundedRect: badgeRect, cornerRadius: 4)
        ctx.addPath(badgePath.cgPath)
        ctx.fillPath()
        (statusText as NSString).draw(at: CGPoint(x: badgeRect.origin.x + 6, y: badgeRect.origin.y + 3), withAttributes: badgeAttrs)
        cursorY += badgeRect.height + 16

        // Zoomed plan view with this pin highlighted
        let zoomHeight: CGFloat = 250
        if let planImage = renderPlanImage(fileData: document.fileData, fileType: document.fileType,
                                            maxSize: CGSize(width: contentWidth * 2, height: zoomHeight * 2), pageIndex: pin.pageIndex) {
            let croppedImage = cropAroundPin(image: planImage, relativeX: pin.relativeX, relativeY: pin.relativeY, cropFraction: 0.25)
            let imageRect = fitRect(imageSize: croppedImage.size, into: CGSize(width: contentWidth, height: zoomHeight),
                                    origin: CGPoint(x: margin, y: cursorY))

            ctx.setStrokeColor(UIColor.lightGray.cgColor)
            ctx.setLineWidth(0.5)
            ctx.stroke(imageRect.insetBy(dx: -1, dy: -1))

            croppedImage.draw(in: imageRect)

            // Draw the highlighted pin in the center of the crop
            let pinCenterX = imageRect.midX
            let pinCenterY = imageRect.midY
            drawPinMarker(context: ctx, center: CGPoint(x: pinCenterX, y: pinCenterY), number: pin.number, color: .systemBlue, radius: 14)

            // Draw other pins that fall in the crop area
            for other in document.pins where other.number != pin.number {
                let relDx = other.relativeX - pin.relativeX
                let relDy = other.relativeY - pin.relativeY
                if abs(relDx) < 0.25 && abs(relDy) < 0.25 {
                    let ox = imageRect.midX + CGFloat(relDx / 0.25) * (imageRect.width / 2)
                    let oy = imageRect.midY + CGFloat(relDy / 0.25) * (imageRect.height / 2)
                    if imageRect.contains(CGPoint(x: ox, y: oy)) {
                        drawPinMarker(context: ctx, center: CGPoint(x: ox, y: oy), number: other.number, color: pinColor(for: other).withAlphaComponent(0.5), radius: 8)
                    }
                }
            }

            cursorY += imageRect.height + 16
        }

        // Pin info table
        cursorY = drawInfoTable(pin: pin, startY: cursorY, margin: margin, contentWidth: contentWidth)

        // Photos
        if !pin.photoData.isEmpty {
            cursorY += 12
            let photoLabel = "Photos (\(pin.photoData.count))" as NSString
            let labelAttrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 13, weight: .semibold),
                .foregroundColor: UIColor.black,
            ]
            photoLabel.draw(at: CGPoint(x: margin, y: cursorY), withAttributes: labelAttrs)
            cursorY += 22

            let photosPerRow = 3
            let spacing: CGFloat = 8
            let photoSize = (contentWidth - CGFloat(photosPerRow - 1) * spacing) / CGFloat(photosPerRow)

            for (index, data) in pin.photoData.enumerated() {
                let col = index % photosPerRow
                let x = margin + CGFloat(col) * (photoSize + spacing)

                // Check if we need a new page
                if cursorY + photoSize > pageHeight - margin {
                    context.beginPage()
                    cursorY = margin
                }

                if let uiImage = UIImage(data: data) {
                    let photoRect = CGRect(x: x, y: cursorY, width: photoSize, height: photoSize)
                    drawPhotoInRect(image: uiImage, rect: photoRect)
                }

                // Move to next row after filling a row
                if col == photosPerRow - 1 {
                    cursorY += photoSize + spacing
                }
            }
            // Handle incomplete last row
            if pin.photoData.count % photosPerRow != 0 {
                cursorY += photoSize + spacing
            }
        }
    }

    // MARK: - Drawing Helpers

    private static func drawInfoTable(pin: PinSnapshot, startY: CGFloat, margin: CGFloat, contentWidth: CGFloat) -> CGFloat {
        var y = startY
        let labelAttrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 11, weight: .semibold),
            .foregroundColor: UIColor.darkGray,
        ]
        let valueAttrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 11, weight: .regular),
            .foregroundColor: UIColor.black,
        ]

        let categoryLabel = PinCategory(rawValue: pin.category)?.label ?? pin.category
        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .medium
        let rows: [(String, String)] = [
            ("Author", pin.author.isEmpty ? "-" : pin.author),
            ("Category", categoryLabel),
            ("Description", pin.pinDescription.isEmpty ? "-" : pin.pinDescription),
            ("Location", pin.location.isEmpty ? "-" : pin.location),
            ("Height", pin.height.isEmpty ? "-" : pin.height),
            ("Width", pin.width.isEmpty ? "-" : pin.width),
            ("Created", dateFormatter.string(from: pin.createdAt)),
        ]

        let labelWidth: CGFloat = 80
        let valueWidth = contentWidth - labelWidth - 8

        guard let ctx = UIGraphicsGetCurrentContext() else { return y }
        for (i, row) in rows.enumerated() {
            let bgColor: UIColor = i % 2 == 0 ? UIColor(white: 0.96, alpha: 1) : .white
            let rowHeight: CGFloat = max(20, (row.1 as NSString).boundingRect(
                with: CGSize(width: valueWidth, height: 200),
                options: .usesLineFragmentOrigin, attributes: valueAttrs, context: nil).height + 8)

            ctx.setFillColor(bgColor.cgColor)
            ctx.fill(CGRect(x: margin, y: y, width: contentWidth, height: rowHeight))

            (row.0 as NSString).draw(in: CGRect(x: margin + 6, y: y + 4, width: labelWidth, height: rowHeight), withAttributes: labelAttrs)
            (row.1 as NSString).draw(in: CGRect(x: margin + labelWidth + 8, y: y + 4, width: valueWidth, height: rowHeight), withAttributes: valueAttrs)
            y += rowHeight
        }

        // Table border
        ctx.setStrokeColor(UIColor.lightGray.cgColor)
        ctx.setLineWidth(0.5)
        ctx.stroke(CGRect(x: margin, y: startY, width: contentWidth, height: y - startY))

        return y
    }

    private static func drawPinMarker(context: CGContext, center: CGPoint, number: Int, color: UIColor, radius: CGFloat) {
        context.setFillColor(color.cgColor)
        context.fillEllipse(in: CGRect(x: center.x - radius, y: center.y - radius, width: radius * 2, height: radius * 2))

        context.setStrokeColor(UIColor.white.cgColor)
        context.setLineWidth(2)
        context.strokeEllipse(in: CGRect(x: center.x - radius, y: center.y - radius, width: radius * 2, height: radius * 2))

        let text = "\(number)" as NSString
        let attrs: [NSAttributedString.Key: Any] = [
            .foregroundColor: UIColor.white,
            .font: UIFont.boldSystemFont(ofSize: radius * 1.1),
        ]
        let textSize = text.size(withAttributes: attrs)
        text.draw(at: CGPoint(x: center.x - textSize.width / 2, y: center.y - textSize.height / 2), withAttributes: attrs)
    }

    private static func drawPhotoInRect(image: UIImage, rect: CGRect) {
        guard let ctx = UIGraphicsGetCurrentContext() else { return }
        ctx.saveGState()
        let clipPath = UIBezierPath(roundedRect: rect, cornerRadius: 4)
        ctx.addPath(clipPath.cgPath)
        ctx.clip()

        let imgAspect = image.size.width / max(image.size.height, 1)
        let rectAspect = rect.width / max(rect.height, 1)
        var drawRect = rect
        if imgAspect > rectAspect {
            let newWidth = rect.height * imgAspect
            drawRect = CGRect(x: rect.midX - newWidth / 2, y: rect.origin.y, width: newWidth, height: rect.height)
        } else {
            let newHeight = rect.width / max(imgAspect, 0.001)
            drawRect = CGRect(x: rect.origin.x, y: rect.midY - newHeight / 2, width: rect.width, height: newHeight)
        }
        image.draw(in: drawRect)

        ctx.restoreGState()

        ctx.setStrokeColor(UIColor.lightGray.cgColor)
        ctx.setLineWidth(0.5)
        ctx.addPath(clipPath.cgPath)
        ctx.strokePath()
    }

    // MARK: - Image Rendering

    private static func renderPlanImage(fileData: Data, fileType: String, maxSize: CGSize, pageIndex: Int = 0) -> UIImage? {
        if fileType == "pdf" {
            guard let pdfDoc = PDFDocument(data: fileData),
                  let page = pdfDoc.page(at: pageIndex) else { return nil }
            let pageBounds = page.bounds(for: .mediaBox)
            let scale = min(maxSize.width / pageBounds.width, maxSize.height / pageBounds.height, 4.0)
            let scaledSize = CGSize(width: pageBounds.width * scale, height: pageBounds.height * scale)
            let renderer = UIGraphicsImageRenderer(size: scaledSize)
            return renderer.image { ctx in
                ctx.cgContext.setFillColor(UIColor.white.cgColor)
                ctx.cgContext.fill(CGRect(origin: .zero, size: scaledSize))
                ctx.cgContext.translateBy(x: 0, y: scaledSize.height)
                ctx.cgContext.scaleBy(x: scale, y: -scale)
                page.draw(with: .mediaBox, to: ctx.cgContext)
            }
        } else {
            return UIImage(data: fileData)
        }
    }

    private static func cropAroundPin(image: UIImage, relativeX: Double, relativeY: Double, cropFraction: Double) -> UIImage {
        let imgW = image.size.width * image.scale
        let imgH = image.size.height * image.scale
        let cropW = imgW * CGFloat(cropFraction * 2)
        let cropH = imgH * CGFloat(cropFraction * 2)

        var cropX = CGFloat(relativeX) * imgW - cropW / 2
        var cropY = CGFloat(relativeY) * imgH - cropH / 2

        cropX = max(0, min(cropX, imgW - cropW))
        cropY = max(0, min(cropY, imgH - cropH))

        let cropRect = CGRect(x: cropX, y: cropY, width: min(cropW, imgW), height: min(cropH, imgH))

        guard let cgImage = image.cgImage?.cropping(to: cropRect) else { return image }
        return UIImage(cgImage: cgImage, scale: image.scale, orientation: image.imageOrientation)
    }

    private static func fitRect(imageSize: CGSize, into containerSize: CGSize, origin: CGPoint) -> CGRect {
        let imgAspect = imageSize.width / max(imageSize.height, 1)
        let containerAspect = containerSize.width / max(containerSize.height, 1)
        var rect: CGRect
        if imgAspect > containerAspect {
            let w = containerSize.width
            let h = w / max(imgAspect, 0.001)
            rect = CGRect(x: origin.x, y: origin.y + (containerSize.height - h) / 2, width: w, height: h)
        } else {
            let h = containerSize.height
            let w = h * imgAspect
            rect = CGRect(x: origin.x + (containerSize.width - w) / 2, y: origin.y, width: w, height: h)
        }
        return rect
    }

    private static func pinColor(for pin: PinSnapshot) -> UIColor {
        PinCategory(rawValue: pin.category)?.uiColor ?? .systemGray
    }
}
