package com.sitepinapp.platform

import platform.Foundation.NSUserDefaults

actual object PlatformPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String, default: String): String =
        defaults.stringForKey(key) ?: default

    actual fun setString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }
}
