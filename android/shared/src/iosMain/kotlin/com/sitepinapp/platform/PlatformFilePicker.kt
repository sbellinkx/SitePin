package com.sitepinapp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.*
import platform.darwin.NSObject

// Retain delegate to prevent GC during picker presentation
private var activeFilePickerDelegate: FilePickerDelegate? = null

@Composable
actual fun rememberFilePickerLauncher(
    mimeTypes: List<String>,
    onResult: (FilePickerResult?) -> Unit
): () -> Unit {
    return remember(mimeTypes) {
        { presentFilePicker(mimeTypes, onResult) }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun presentFilePicker(
    mimeTypes: List<String>,
    onResult: (FilePickerResult?) -> Unit
) {
    val utTypes = mimeTypes.mapNotNull { mimeType ->
        UTType.typeWithMIMEType(mimeType)
    }.ifEmpty {
        // Fall back to common types using UTI identifiers
        listOfNotNull(
            UTType.typeWithIdentifier("public.data"),
            UTType.typeWithIdentifier("public.image"),
            UTType.typeWithIdentifier("com.adobe.pdf")
        )
    }

    val delegate = FilePickerDelegate { result ->
        activeFilePickerDelegate = null
        onResult(result)
    }
    activeFilePickerDelegate = delegate

    val picker = UIDocumentPickerViewController(forOpeningContentTypes = utTypes)
    picker.delegate = delegate
    picker.allowsMultipleSelection = false

    val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
    rootVC?.presentViewController(picker, animated = true, completion = null)
}

private class FilePickerDelegate(
    private val onResult: (FilePickerResult?) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {

    @OptIn(ExperimentalForeignApi::class)
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        if (url == null) {
            onResult(null)
            return
        }

        val accessing = url.startAccessingSecurityScopedResource()
        try {
            val data = NSData.dataWithContentsOfURL(url)
            if (data == null) {
                onResult(null)
                return
            }

            // A4: Validate file size
            if (data.length.toInt() > ImportLimits.MAX_DOCUMENT_SIZE_BYTES) {
                onResult(null)
                return
            }

            val bytes = ByteArray(data.length.toInt())
            bytes.usePinned { pinned ->
                data.getBytes(pinned.addressOf(0), length = data.length)
            }

            val fileName = url.lastPathComponent ?: "unknown"
            val mimeType = UTType.typeWithFilenameExtension(url.pathExtension ?: "")
                ?.preferredMIMEType ?: "application/octet-stream"

            onResult(FilePickerResult(data = bytes, fileName = fileName, mimeType = mimeType))
        } finally {
            if (accessing) url.stopAccessingSecurityScopedResource()
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(null)
    }
}
