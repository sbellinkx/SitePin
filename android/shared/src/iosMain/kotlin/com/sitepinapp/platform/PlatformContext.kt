package com.sitepinapp.platform

actual object PlatformContext {
    actual fun initialize(context: Any) {
        // No initialization needed on iOS - NSUserDefaults and file system are always available
    }
}
