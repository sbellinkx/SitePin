package com.sitepinapp.platform

// Platform context holder for accessing platform-specific APIs
expect object PlatformContext {
    fun initialize(context: Any)
}
