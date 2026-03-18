package com.sitepinapp.data.dao

import androidx.room.*
import com.sitepinapp.data.model.PinComment
import kotlinx.coroutines.flow.Flow

@Dao
interface PinCommentDao {
    @Query("SELECT * FROM pin_comments WHERE pinId = :pinId ORDER BY createdAt ASC")
    fun getCommentsByPinId(pinId: Long): Flow<List<PinComment>>

    @Query("SELECT * FROM pin_comments WHERE pinId = :pinId ORDER BY createdAt ASC")
    suspend fun getCommentsByPinIdSuspend(pinId: Long): List<PinComment>

    @Insert
    suspend fun insert(comment: PinComment): Long

    @Delete
    suspend fun delete(comment: PinComment)
}
