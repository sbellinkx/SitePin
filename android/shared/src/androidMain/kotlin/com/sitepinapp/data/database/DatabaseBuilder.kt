package com.sitepinapp.data.database

import androidx.room.Room
import androidx.room.RoomDatabase
import com.sitepinapp.platform.PlatformContext

actual fun getDatabaseBuilder(): RoomDatabase.Builder<SitePinDatabase> {
    val context = PlatformContext.getContext()
    val dbFile = context.getDatabasePath("sitepin.db")
    return Room.databaseBuilder<SitePinDatabase>(
        context = context,
        name = dbFile.absolutePath
    )
}
