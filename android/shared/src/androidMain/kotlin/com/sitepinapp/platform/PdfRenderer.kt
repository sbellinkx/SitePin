package com.sitepinapp.platform

import android.graphics.Bitmap
import android.graphics.Color
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

actual class PlatformPdfRenderer actual constructor(data: ByteArray) {
    private val tempFile: File = File.createTempFile("pdf_render", ".pdf")
    private val fd: ParcelFileDescriptor
    private val renderer: android.graphics.pdf.PdfRenderer

    init {
        tempFile.writeBytes(data)
        fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = android.graphics.pdf.PdfRenderer(fd)
    }

    actual val pageCount: Int get() = renderer.pageCount

    actual fun renderPage(pageIndex: Int, maxDimension: Int): ImageBitmap? {
        if (pageIndex >= pageCount) return null
        val page = renderer.openPage(pageIndex)
        val scale = minOf(maxDimension.toFloat() / page.width, maxDimension.toFloat() / page.height)
        val width = (page.width * scale).toInt()
        val height = (page.height * scale).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap.asImageBitmap()
    }

    actual fun close() {
        renderer.close()
        fd.close()
        tempFile.delete()
    }
}
