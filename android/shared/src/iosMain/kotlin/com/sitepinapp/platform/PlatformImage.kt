package com.sitepinapp.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image as SkiaImage

actual fun ByteArray.toImageBitmap(): ImageBitmap? {
    return try {
        SkiaImage.makeFromEncoded(this).toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}

actual fun ImageBitmap.toByteArray(quality: Int): ByteArray {
    return try {
        // Get the underlying Skia bitmap from the Compose ImageBitmap
        val skiaBitmap = this.asSkiaBitmap()
        val skiaImage = SkiaImage.makeFromBitmap(skiaBitmap)
        val data = skiaImage.encodeToData(EncodedImageFormat.JPEG, quality)
        data?.bytes ?: byteArrayOf()
    } catch (e: Exception) {
        byteArrayOf()
    }
}
