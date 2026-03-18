package com.sitepinapp.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.readBytes
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFRelease
import platform.CoreGraphics.*
import platform.Foundation.NSData
import platform.Foundation.create
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.ImageInfo

@OptIn(ExperimentalForeignApi::class)
actual class PlatformPdfRenderer actual constructor(data: ByteArray) {
    private val cgPdfDocument: CGPDFDocumentRef?

    init {
        val nsData = data.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = data.size.toULong())
        }
        @Suppress("UNCHECKED_CAST")
        val dataProvider = CGDataProviderCreateWithCFData(nsData as platform.CoreFoundation.CFDataRef)
        cgPdfDocument = CGPDFDocumentCreateWithProvider(dataProvider)
        CGDataProviderRelease(dataProvider)
    }

    actual val pageCount: Int
        get() = cgPdfDocument?.let { CGPDFDocumentGetNumberOfPages(it).toInt() } ?: 0

    actual fun renderPage(pageIndex: Int, maxDimension: Int): ImageBitmap? {
        val doc = cgPdfDocument ?: return null
        val page = CGPDFDocumentGetPage(doc, (pageIndex + 1).toULong()) ?: return null

        val mediaBox = CGPDFPageGetBoxRect(page, kCGPDFMediaBox)
        val pageWidth = CGRectGetWidth(mediaBox)
        val pageHeight = CGRectGetHeight(mediaBox)
        val scale = minOf(maxDimension.toDouble() / pageWidth, maxDimension.toDouble() / pageHeight)

        val width = (pageWidth * scale).toInt()
        val height = (pageHeight * scale).toInt()

        val colorSpace = CGColorSpaceCreateDeviceRGB()
        val context = CGBitmapContextCreate(
            null, width.toULong(), height.toULong(), 8u, (width * 4).toULong(),
            colorSpace, CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
        )
        CGColorSpaceRelease(colorSpace)

        if (context == null) return null

        // White background
        CGContextSetRGBFillColor(context, 1.0, 1.0, 1.0, 1.0)
        CGContextFillRect(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))

        // Scale and draw PDF page
        CGContextScaleCTM(context, scale, scale)
        CGContextDrawPDFPage(context, page)

        val cgImage = CGBitmapContextCreateImage(context) ?: run {
            CGContextRelease(context)
            return null
        }

        // Convert CGImage to Skia ImageBitmap
        val imageWidth = CGImageGetWidth(cgImage).toInt()
        val imageHeight = CGImageGetHeight(cgImage).toInt()
        val dataProvider2 = CGImageGetDataProvider(cgImage)
        val cfData = CGDataProviderCopyData(dataProvider2)

        if (cfData == null) {
            CGImageRelease(cgImage)
            CGContextRelease(context)
            return null
        }

        // Copy pixel data from CGImage into Skia Image
        val dataLength = CFDataGetLength(cfData).toInt()
        val dataPtr = CFDataGetBytePtr(cfData)
        val pixelBytes = dataPtr!!.readBytes(dataLength)

        val imageInfo = ImageInfo(imageWidth, imageHeight, ColorType.RGBA_8888, ColorAlphaType.PREMUL)
        val rowBytes = imageWidth * 4 // 4 bytes per pixel (RGBA)

        // Create Skia Image directly from raw pixel data
        val skiaImage = SkiaImage.makeRaster(imageInfo, pixelBytes, rowBytes)
        val result = skiaImage.toComposeImageBitmap()

        CFRelease(cfData)
        CGImageRelease(cgImage)
        CGContextRelease(context)

        return result
    }

    actual fun close() {
        cgPdfDocument?.let { CGPDFDocumentRelease(it) }
    }
}
