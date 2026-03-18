package com.sitepinapp.platform

expect object PlatformPreferences {
    fun getString(key: String, default: String): String
    fun setString(key: String, value: String)
}
