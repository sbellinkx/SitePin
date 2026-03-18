package com.sitepinapp.platform

import androidx.compose.runtime.Composable

/**
 * Launcher for picking images from camera or gallery.
 */
data class ImagePickerLauncher(
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit
)

/**
 * Composable that remembers an image picker launcher for camera and gallery.
 *
 * @param onResult Callback invoked with the captured/picked image as ByteArray, or null if cancelled
 */
@Composable
expect fun rememberImagePickerLauncher(
    onResult: (ByteArray?) -> Unit
): ImagePickerLauncher
