package com.sitepinapp.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
actual fun rememberImagePickerLauncher(
    onResult: (ByteArray?) -> Unit
): ImagePickerLauncher {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            try {
                val data = context.contentResolver.openInputStream(photoUri!!)?.use { it.readBytes() }
                if (data != null) {
                    // Compress to reasonable size
                    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                    val output = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, output)
                    onResult(output.toByteArray())
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(null)
            }
        } else {
            onResult(null)
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) {
            onResult(null)
            return@rememberLauncherForActivityResult
        }
        try {
            val data = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (data != null) {
                // Compress to reasonable size
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                if (bitmap != null) {
                    val output = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, output)
                    onResult(output.toByteArray())
                } else {
                    onResult(data)
                }
            } else {
                onResult(null)
            }
        } catch (e: Exception) {
            onResult(null)
        }
    }

    return ImagePickerLauncher(
        launchCamera = {
            // Create temp file for camera output
            val photoFile = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
            photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            cameraLauncher.launch(photoUri!!)
        },
        launchGallery = {
            galleryLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }
    )
}
