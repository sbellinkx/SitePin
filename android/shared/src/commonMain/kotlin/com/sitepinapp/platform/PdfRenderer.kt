package com.sitepinapp.platform

import androidx.compose.ui.graphics.ImageBitmap

expect class PlatformPdfRenderer(data: ByteArray) {
    val pageCount: Int
    fun renderPage(pageIndex: Int, maxDimension: Int = 2048): ImageBitmap?
    fun close()
}
