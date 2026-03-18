package com.sitepinapp.platform

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberFilePickerLauncher(
    mimeTypes: List<String>,
    onResult: (FilePickerResult?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            onResult(null)
            return@rememberLauncherForActivityResult
        }
        try {
            val contentResolver = context.contentResolver
            val data = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (data == null) {
                onResult(null)
                return@rememberLauncherForActivityResult
            }

            // A4: Validate file size
            if (data.size > ImportLimits.MAX_DOCUMENT_SIZE_BYTES) {
                onResult(null)
                return@rememberLauncherForActivityResult
            }

            // Get file name
            var fileName = "unknown"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex) ?: "unknown"
                    }
                }
            }

            // Get mime type
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

            onResult(FilePickerResult(data = data, fileName = fileName, mimeType = mimeType))
        } catch (e: Exception) {
            onResult(null)
        }
    }

    return { launcher.launch(mimeTypes.toTypedArray()) }
}
