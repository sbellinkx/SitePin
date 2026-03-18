package com.sitepinapp.data.database

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual fun getDatabaseBuilder(): RoomDatabase.Builder<SitePinDatabase> {
    val paths = NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
    val documentsDir = paths.first() as NSURL
    val dbPath = "${documentsDir.path}/sitepin.db"
    return Room.databaseBuilder<SitePinDatabase>(name = dbPath)
}
