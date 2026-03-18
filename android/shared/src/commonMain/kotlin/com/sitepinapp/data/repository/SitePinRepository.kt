package com.sitepinapp.data.repository

import com.sitepinapp.data.database.SitePinDatabase
import com.sitepinapp.data.model.*
import kotlinx.coroutines.flow.Flow

class SitePinRepository(private val db: SitePinDatabase) {

    private val projectDao = db.projectDao()
    private val documentDao = db.planDocumentDao()
    private val pinDao = db.pinDao()
    private val photoDao = db.pinPhotoDao()
    private val commentDao = db.pinCommentDao()

    // Projects
    fun getAllProjects(): Flow<List<Project>> = projectDao.getAllProjects()
    suspend fun getProjectById(id: Long): Project? = projectDao.getProjectById(id)
    suspend fun getProjectWithDocuments(id: Long): ProjectWithDocuments? = projectDao.getProjectWithDocuments(id)
    suspend fun insertProject(project: Project): Long = projectDao.insert(project)
    suspend fun updateProject(project: Project) = projectDao.update(project)
    suspend fun deleteProject(project: Project) = projectDao.delete(project)

    // Documents
    fun getDocumentsByProjectId(projectId: Long): Flow<List<PlanDocument>> = documentDao.getDocumentsByProjectId(projectId)
    suspend fun getDocumentById(id: Long): PlanDocument? = documentDao.getDocumentById(id)
    suspend fun getDocumentWithPins(id: Long): DocumentWithPins? = documentDao.getDocumentWithPins(id)
    suspend fun getDocumentsWithPinsByProjectId(projectId: Long): List<DocumentWithPins> = documentDao.getDocumentsWithPinsByProjectId(projectId)
    suspend fun getDocumentBySyncID(projectId: Long, syncID: String): PlanDocument? = documentDao.getDocumentBySyncID(projectId, syncID)
    suspend fun insertDocument(document: PlanDocument): Long = documentDao.insert(document)
    suspend fun updateDocument(document: PlanDocument) = documentDao.update(document)
    suspend fun deleteDocument(document: PlanDocument) = documentDao.delete(document)
    suspend fun getDocumentCount(projectId: Long): Int = documentDao.getDocumentCount(projectId)

    // Pins
    fun getPinsByDocumentId(documentId: Long): Flow<List<Pin>> = pinDao.getPinsByDocumentId(documentId)
    fun getAllPinsByProjectId(projectId: Long): Flow<List<Pin>> = pinDao.getAllPinsByProjectId(projectId)
    suspend fun getPinById(id: Long): Pin? = pinDao.getPinById(id)
    suspend fun getPinWithDetails(id: Long): PinWithDetails? = pinDao.getPinWithDetails(id)
    suspend fun getPinBySyncID(documentId: Long, syncID: String): Pin? = pinDao.getPinBySyncID(documentId, syncID)
    suspend fun getPinCountByProjectId(projectId: Long): Int = pinDao.getPinCountByProjectId(projectId)
    suspend fun getPinCountByProjectIdAndStatus(projectId: Long, status: String): Int = pinDao.getPinCountByProjectIdAndStatus(projectId, status)
    suspend fun getPinCountByDocumentId(documentId: Long): Int = pinDao.getPinCountByDocumentId(documentId)
    suspend fun insertPin(pin: Pin): Long = pinDao.insert(pin)
    suspend fun updatePin(pin: Pin) = pinDao.update(pin)
    suspend fun deletePin(pin: Pin) = pinDao.delete(pin)
    suspend fun updatePinStatus(id: Long, status: String) = pinDao.updateStatus(id, status, kotlinx.datetime.Clock.System.now().toEpochMilliseconds())

    // Photos
    fun getPhotosByPinId(pinId: Long): Flow<List<PinPhoto>> = photoDao.getPhotosByPinId(pinId)
    suspend fun getPhotosByPinIdSuspend(pinId: Long): List<PinPhoto> = photoDao.getPhotosByPinIdSuspend(pinId)
    suspend fun getPhotoCountByPinId(pinId: Long): Int = photoDao.getPhotoCountByPinId(pinId)
    suspend fun insertPhoto(photo: PinPhoto): Long = photoDao.insert(photo)
    suspend fun deletePhoto(photo: PinPhoto) = photoDao.delete(photo)

    // Comments
    fun getCommentsByPinId(pinId: Long): Flow<List<PinComment>> = commentDao.getCommentsByPinId(pinId)
    suspend fun getCommentsByPinIdSuspend(pinId: Long): List<PinComment> = commentDao.getCommentsByPinIdSuspend(pinId)
    suspend fun insertComment(comment: PinComment): Long = commentDao.insert(comment)
    suspend fun deleteComment(comment: PinComment) = commentDao.delete(comment)

    // Full project data for export
    suspend fun getFullProjectData(projectId: Long): FullProjectData? {
        val project = getProjectById(projectId) ?: return null
        val documentsWithPins = getDocumentsWithPinsByProjectId(projectId)
        val documentDetails = documentsWithPins.map { docWithPins ->
            val pinDetails = docWithPins.pins.map { pin ->
                val details = getPinWithDetails(pin.id)
                PinFullData(pin = pin, photos = details?.photos ?: emptyList(), comments = details?.comments ?: emptyList())
            }
            DocumentFullData(document = docWithPins.document, pins = pinDetails)
        }
        return FullProjectData(project = project, documents = documentDetails)
    }
}

data class FullProjectData(val project: Project, val documents: List<DocumentFullData>)
data class DocumentFullData(val document: PlanDocument, val pins: List<PinFullData>)
data class PinFullData(val pin: Pin, val photos: List<PinPhoto>, val comments: List<PinComment>)
