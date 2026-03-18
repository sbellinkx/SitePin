package com.sitepinapp.platform

import android.content.Context

actual object PlatformContext {
    private lateinit var appContext: Context

    actual fun initialize(context: Any) {
        appContext = (context as Context).applicationContext
        PlatformPreferences.init(appContext)
        PlatformFileHandler.init(appContext)
    }

    fun getContext(): Context = appContext
}
