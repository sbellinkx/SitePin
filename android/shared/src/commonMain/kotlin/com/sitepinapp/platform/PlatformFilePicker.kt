package com.sitepinapp.platform

import androidx.compose.runtime.Composable

/**
 * Result of picking a file.
 */
data class FilePickerResult(
    val data: ByteArray,
    val fileName: String,
    val mimeType: String
)

/** Shared import limits */
object ImportLimits {
    /** Maximum file size for plan documents (50 MB) */
    const val MAX_DOCUMENT_SIZE_BYTES = 50 * 1024 * 1024
    /** Maximum file size for .sitepin project imports (100 MB) */
    const val MAX_IMPORT_SIZE_BYTES = 100 * 1024 * 1024
    /** Maximum photos allowed per pin */
    const val MAX_PHOTOS_PER_PIN = 20
}

// Composable that remembers a file picker launcher.
// Returns a lambda that, when called, launches the native file picker.
@Composable
expect fun rememberFilePickerLauncher(
    mimeTypes: List<String>,
    onResult: (FilePickerResult?) -> Unit
): () -> Unit
