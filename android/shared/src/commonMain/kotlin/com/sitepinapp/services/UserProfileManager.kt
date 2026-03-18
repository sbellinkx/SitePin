package com.sitepinapp.services

import com.sitepinapp.platform.PlatformPreferences

object UserProfileManager {
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_THEME = "app_theme"

    fun getDisplayName(): String = PlatformPreferences.getString(KEY_DISPLAY_NAME, "")
    fun setDisplayName(name: String) = PlatformPreferences.setString(KEY_DISPLAY_NAME, name)
    fun hasProfile(): Boolean = getDisplayName().isNotBlank()

    fun getTheme(): String = PlatformPreferences.getString(KEY_THEME, "system")
    fun setTheme(theme: String) = PlatformPreferences.setString(KEY_THEME, theme)
}
