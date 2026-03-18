package com.sitepinapp.data.dao

import androidx.room.*
import com.sitepinapp.data.model.PinPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface PinPhotoDao {
    @Query("SELECT * FROM pin_photos WHERE pinId = :pinId ORDER BY createdAt ASC")
    fun getPhotosByPinId(pinId: Long): Flow<List<PinPhoto>>

    @Query("SELECT * FROM pin_photos WHERE pinId = :pinId ORDER BY createdAt ASC")
    suspend fun getPhotosByPinIdSuspend(pinId: Long): List<PinPhoto>

    @Query("SELECT COUNT(*) FROM pin_photos WHERE pinId = :pinId")
    suspend fun getPhotoCountByPinId(pinId: Long): Int

    @Insert
    suspend fun insert(photo: PinPhoto): Long

    @Delete
    suspend fun delete(photo: PinPhoto)
}
