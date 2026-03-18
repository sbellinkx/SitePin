package com.sitepinapp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject

// Retain delegate to prevent GC during picker presentation
private var activeImagePickerDelegate: ImagePickerDelegate? = null

@Composable
actual fun rememberImagePickerLauncher(
    onResult: (ByteArray?) -> Unit
): ImagePickerLauncher {
    return remember {
        ImagePickerLauncher(
            launchCamera = {
                if (UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
                    presentImagePicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera, onResult)
                } else {
                    onResult(null)
                }
            },
            launchGallery = {
                presentImagePicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary, onResult)
            }
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun presentImagePicker(
    sourceType: UIImagePickerControllerSourceType,
    onResult: (ByteArray?) -> Unit
) {
    val delegate = ImagePickerDelegate { result ->
        activeImagePickerDelegate = null
        onResult(result)
    }
    activeImagePickerDelegate = delegate

    val picker = UIImagePickerController()
    picker.sourceType = sourceType
    picker.delegate = delegate
    picker.allowsEditing = false

    val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
    rootVC?.presentViewController(picker, animated = true, completion = null)
}

private class ImagePickerDelegate(
    private val onResult: (ByteArray?) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    @OptIn(ExperimentalForeignApi::class)
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)

        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        if (image == null) {
            onResult(null)
            return
        }

        val jpegData = UIImageJPEGRepresentation(image, 0.7)
        if (jpegData == null) {
            onResult(null)
            return
        }

        val bytes = ByteArray(jpegData.length.toInt())
        bytes.usePinned { pinned ->
            jpegData.getBytes(pinned.addressOf(0), length = jpegData.length)
        }
        onResult(bytes)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onResult(null)
    }
}
