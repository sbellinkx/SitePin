package com.sitepinapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.ConstructedBy
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import com.sitepinapp.data.dao.*
import com.sitepinapp.data.model.*

@Database(
    entities = [Project::class, PlanDocument::class, Pin::class, PinPhoto::class, PinComment::class],
    version = 2,
    exportSchema = true
)
@ConstructedBy(SitePinDatabaseConstructor::class)
abstract class SitePinDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun planDocumentDao(): PlanDocumentDao
    abstract fun pinDao(): PinDao
    abstract fun pinPhotoDao(): PinPhotoDao
    abstract fun pinCommentDao(): PinCommentDao
}

// Migration from version 1 to 2: Add syncID to pin_photos and pin_comments
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE pin_photos ADD COLUMN syncID TEXT NOT NULL DEFAULT ''").use { it.step() }
        connection.prepare("ALTER TABLE pin_comments ADD COLUMN syncID TEXT NOT NULL DEFAULT ''").use { it.step() }
    }
}

// Room KMP requires this expect/actual constructor
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SitePinDatabaseConstructor : RoomDatabaseConstructor<SitePinDatabase> {
    override fun initialize(): SitePinDatabase
}
