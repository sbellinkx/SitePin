package com.sitepinapp.platform

import androidx.compose.ui.graphics.ImageBitmap

expect fun ByteArray.toImageBitmap(): ImageBitmap?

expect fun ImageBitmap.toByteArray(quality: Int = 70): ByteArray
