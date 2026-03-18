package com.sitepinapp.platform

import android.content.Context
import android.content.SharedPreferences

actual object PlatformPreferences {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("sitepin_prefs", Context.MODE_PRIVATE)
    }

    actual fun getString(key: String, default: String): String = prefs.getString(key, default) ?: default
    actual fun setString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
}
