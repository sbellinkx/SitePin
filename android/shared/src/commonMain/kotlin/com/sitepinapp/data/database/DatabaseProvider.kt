package com.sitepinapp.data.database

import com.sitepinapp.data.repository.SitePinRepository

object DatabaseProvider {
    private var database: SitePinDatabase? = null
    private var repository: SitePinRepository? = null

    fun getDatabase(): SitePinDatabase {
        return database ?: getDatabaseBuilder()
            .addMigrations(MIGRATION_1_2)
            .setQueryCoroutineContext(kotlinx.coroutines.Dispatchers.Default)
            .build()
            .also { database = it }
    }

    fun getRepository(): SitePinRepository {
        return repository ?: SitePinRepository(getDatabase()).also { repository = it }
    }
}
