package com.sitepinapp.data.dao

import androidx.room.*
import com.sitepinapp.data.model.DocumentWithPins
import com.sitepinapp.data.model.PlanDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDocumentDao {
    @Query("SELECT * FROM plan_documents WHERE projectId = :projectId ORDER BY createdAt ASC")
    fun getDocumentsByProjectId(projectId: Long): Flow<List<PlanDocument>>

    @Query("SELECT * FROM plan_documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): PlanDocument?

    @Transaction
    @Query("SELECT * FROM plan_documents WHERE id = :id")
    suspend fun getDocumentWithPins(id: Long): DocumentWithPins?

    @Transaction
    @Query("SELECT * FROM plan_documents WHERE projectId = :projectId ORDER BY createdAt ASC")
    suspend fun getDocumentsWithPinsByProjectId(projectId: Long): List<DocumentWithPins>

    @Query("SELECT * FROM plan_documents WHERE projectId = :projectId AND syncID = :syncID LIMIT 1")
    suspend fun getDocumentBySyncID(projectId: Long, syncID: String): PlanDocument?

    @Insert
    suspend fun insert(document: PlanDocument): Long

    @Update
    suspend fun update(document: PlanDocument)

    @Delete
    suspend fun delete(document: PlanDocument)

    @Query("SELECT COUNT(*) FROM plan_documents WHERE projectId = :projectId")
    suspend fun getDocumentCount(projectId: Long): Int
}
