package com.sitepinapp.data.dao

import androidx.room.*
import com.sitepinapp.data.model.Project
import com.sitepinapp.data.model.ProjectWithDocuments
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): Project?

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectWithDocuments(id: Long): ProjectWithDocuments?

    @Insert
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long)
}
