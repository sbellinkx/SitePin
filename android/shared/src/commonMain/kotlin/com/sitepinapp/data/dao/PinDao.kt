package com.sitepinapp.data.dao

import androidx.room.*
import com.sitepinapp.data.model.Pin
import com.sitepinapp.data.model.PinWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface PinDao {
    @Query("SELECT * FROM pins WHERE documentId = :documentId ORDER BY createdAt ASC")
    fun getPinsByDocumentId(documentId: Long): Flow<List<Pin>>

    @Query("SELECT * FROM pins WHERE id = :id")
    suspend fun getPinById(id: Long): Pin?

    @Transaction
    @Query("SELECT * FROM pins WHERE id = :id")
    suspend fun getPinWithDetails(id: Long): PinWithDetails?

    @Query("SELECT * FROM pins WHERE documentId = :documentId AND pageIndex = :pageIndex ORDER BY createdAt ASC")
    fun getPinsByDocumentIdAndPage(documentId: Long, pageIndex: Int): Flow<List<Pin>>

    @Query("SELECT pins.* FROM pins INNER JOIN plan_documents ON pins.documentId = plan_documents.id WHERE plan_documents.projectId = :projectId ORDER BY pins.createdAt ASC")
    fun getAllPinsByProjectId(projectId: Long): Flow<List<Pin>>

    @Query("SELECT pins.* FROM pins INNER JOIN plan_documents ON pins.documentId = plan_documents.id WHERE plan_documents.projectId = :projectId ORDER BY pins.createdAt ASC")
    suspend fun getAllPinsByProjectIdSuspend(projectId: Long): List<Pin>

    @Query("SELECT * FROM pins WHERE documentId = :documentId AND syncID = :syncID LIMIT 1")
    suspend fun getPinBySyncID(documentId: Long, syncID: String): Pin?

    @Query("SELECT COUNT(*) FROM pins INNER JOIN plan_documents ON pins.documentId = plan_documents.id WHERE plan_documents.projectId = :projectId")
    suspend fun getPinCountByProjectId(projectId: Long): Int

    @Query("SELECT COUNT(*) FROM pins INNER JOIN plan_documents ON pins.documentId = plan_documents.id WHERE plan_documents.projectId = :projectId AND pins.status = :status")
    suspend fun getPinCountByProjectIdAndStatus(projectId: Long, status: String): Int

    @Query("SELECT COUNT(*) FROM pins WHERE documentId = :documentId")
    suspend fun getPinCountByDocumentId(documentId: Long): Int

    @Insert
    suspend fun insert(pin: Pin): Long

    @Update
    suspend fun update(pin: Pin)

    @Delete
    suspend fun delete(pin: Pin)

    @Query("UPDATE pins SET status = :status, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, modifiedAt: Long)
}
