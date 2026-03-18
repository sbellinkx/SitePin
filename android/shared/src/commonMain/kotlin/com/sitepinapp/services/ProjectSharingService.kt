package com.sitepinapp.services

import com.sitepinapp.data.model.*
import com.sitepinapp.data.repository.SitePinRepository
import com.sitepinapp.platform.ImportLimits
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object ProjectSharingService {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private const val MAX_STRING_LENGTH = 1000
    private const val MAX_DESCRIPTION_LENGTH = 5000
    private const val MAX_FORMAT_VERSION = 2
    private val validStatuses = setOf("open", "in_progress", "resolved")
    private val validCategories = PinCategory.entries.map { it.name.lowercase() }.toSet()

    private fun sanitize(value: String, maxLength: Int = MAX_STRING_LENGTH): String = value.take(maxLength)
    private fun sanitizeStatus(value: String): String = if (value in validStatuses) value else "open"
    private fun sanitizeCategory(value: String): String = if (value in validCategories) value else "general"
    private fun sanitizeSyncID(value: String): String = value.trim().ifEmpty { Uuid.random().toString() }.take(100)

    // ── EXPORT ──

    suspend fun exportProjectToJson(repository: SitePinRepository, projectId: Long): String {
        val fullData = repository.getFullProjectData(projectId)
            ?: throw IllegalStateException("Project not found")

        val export = ProjectExport(
            name = fullData.project.name,
            syncID = fullData.project.syncID,
            createdAt = DateUtils.toISO8601(fullData.project.createdAt),
            exportedBy = UserProfileManager.getDisplayName(),
            exportedAt = DateUtils.toISO8601(Clock.System.now().toEpochMilliseconds()),
            documents = fullData.documents.map { docData ->
                DocumentExport(
                    name = docData.document.name,
                    syncID = docData.document.syncID,
                    fileType = docData.document.fileType,
                    pageCount = docData.document.pageCount,
                    createdAt = DateUtils.toISO8601(docData.document.createdAt),
                    fileData = DateUtils.encodeBase64(docData.document.fileData),
                    pins = docData.pins.map { pinData ->
                        PinExport(
                            syncID = pinData.pin.syncID,
                            relativeX = pinData.pin.relativeX,
                            relativeY = pinData.pin.relativeY,
                            pageIndex = pinData.pin.pageIndex,
                            title = pinData.pin.title,
                            pinDescription = pinData.pin.pinDescription,
                            location = pinData.pin.location,
                            height = pinData.pin.height,
                            width = pinData.pin.width,
                            status = pinData.pin.status,
                            category = pinData.pin.category,
                            author = pinData.pin.author,
                            createdAt = DateUtils.toISO8601(pinData.pin.createdAt),
                            modifiedAt = DateUtils.toISO8601(pinData.pin.modifiedAt),
                            photos = pinData.photos.map { PhotoExport(DateUtils.encodeBase64(it.imageData), it.caption, DateUtils.toISO8601(it.createdAt), it.syncID) },
                            comments = pinData.comments.map { CommentExport(it.text, it.author, DateUtils.toISO8601(it.createdAt), it.syncID) }
                        )
                    }
                )
            }
        )
        return json.encodeToString(ProjectExport.serializer(), export)
    }

    // ── IMPORT ──

    suspend fun importProjectFromJson(repository: SitePinRepository, jsonString: String): Long {
        // A4: Validate import size
        require(jsonString.length <= ImportLimits.MAX_IMPORT_SIZE_BYTES) {
            "Import file is too large (${jsonString.length / (1024 * 1024)} MB). Maximum is ${ImportLimits.MAX_IMPORT_SIZE_BYTES / (1024 * 1024)} MB."
        }

        val export = json.decodeFromString(ProjectExport.serializer(), jsonString)

        // Validate format version
        require(export.formatVersion <= MAX_FORMAT_VERSION) {
            "This file was created with a newer version of SitePin (format v${export.formatVersion}). Please update the app."
        }

        // A3: Validate photo counts per pin
        for (docExport in export.documents) {
            for (pinExport in docExport.pins) {
                require(pinExport.photos.size <= ImportLimits.MAX_PHOTOS_PER_PIN) {
                    "Pin \"${pinExport.title.ifEmpty { "Untitled" }}\" has ${pinExport.photos.size} photos, exceeding the maximum of ${ImportLimits.MAX_PHOTOS_PER_PIN}."
                }
            }
        }

        val project = Project(name = sanitize(export.name), syncID = sanitizeSyncID(export.syncID), createdAt = DateUtils.fromISO8601(export.createdAt))
        val projectId = repository.insertProject(project)

        for (docExport in export.documents) {
            val document = PlanDocument(
                projectId = projectId, name = sanitize(docExport.name), syncID = sanitizeSyncID(docExport.syncID),
                fileType = docExport.fileType, pageCount = docExport.pageCount,
                createdAt = DateUtils.fromISO8601(docExport.createdAt),
                fileData = DateUtils.decodeBase64(docExport.fileData)
            )
            val documentId = repository.insertDocument(document)

            for (pinExport in docExport.pins) {
                val pin = Pin(
                    documentId = documentId, syncID = sanitizeSyncID(pinExport.syncID),
                    relativeX = pinExport.relativeX.coerceIn(0.0, 1.0),
                    relativeY = pinExport.relativeY.coerceIn(0.0, 1.0),
                    pageIndex = pinExport.pageIndex, title = sanitize(pinExport.title),
                    pinDescription = sanitize(pinExport.pinDescription, MAX_DESCRIPTION_LENGTH),
                    location = sanitize(pinExport.location),
                    height = sanitize(pinExport.height), width = sanitize(pinExport.width),
                    status = sanitizeStatus(pinExport.status), category = sanitizeCategory(pinExport.category),
                    author = sanitize(pinExport.author),
                    createdAt = DateUtils.fromISO8601(pinExport.createdAt),
                    modifiedAt = DateUtils.fromISO8601(pinExport.modifiedAt)
                )
                val pinId = repository.insertPin(pin)

                for (photoExport in pinExport.photos) {
                    val photoSyncID = sanitizeSyncID(photoExport.syncID)
                    repository.insertPhoto(PinPhoto(pinId = pinId, imageData = DateUtils.decodeBase64(photoExport.imageData), caption = sanitize(photoExport.caption), createdAt = DateUtils.fromISO8601(photoExport.createdAt), syncID = photoSyncID))
                }
                for (commentExport in pinExport.comments) {
                    val commentSyncID = sanitizeSyncID(commentExport.syncID)
                    repository.insertComment(PinComment(pinId = pinId, text = sanitize(commentExport.text, MAX_DESCRIPTION_LENGTH), author = sanitize(commentExport.author), createdAt = DateUtils.fromISO8601(commentExport.createdAt), syncID = commentSyncID))
                }
            }
        }
        return projectId
    }

    // ── MERGE ──

    suspend fun mergeProjectFromJson(repository: SitePinRepository, jsonString: String, existingProjectId: Long) {
        // Validate import size and format
        require(jsonString.length <= ImportLimits.MAX_IMPORT_SIZE_BYTES) {
            "Import file is too large (${jsonString.length / (1024 * 1024)} MB). Maximum is ${ImportLimits.MAX_IMPORT_SIZE_BYTES / (1024 * 1024)} MB."
        }
        val export = json.decodeFromString(ProjectExport.serializer(), jsonString)
        require(export.formatVersion <= MAX_FORMAT_VERSION) {
            "This file was created with a newer version of SitePin (format v${export.formatVersion}). Please update the app."
        }
        // Validate photo counts per pin
        for (docExport in export.documents) {
            for (pinExport in docExport.pins) {
                require(pinExport.photos.size <= ImportLimits.MAX_PHOTOS_PER_PIN) {
                    "Pin \"${pinExport.title.ifEmpty { "Untitled" }}\" has ${pinExport.photos.size} photos, exceeding the maximum of ${ImportLimits.MAX_PHOTOS_PER_PIN}."
                }
            }
        }

        for (docExport in export.documents) {
            val existingDoc = repository.getDocumentBySyncID(existingProjectId, docExport.syncID)
            val documentId: Long = existingDoc?.id ?: repository.insertDocument(
                PlanDocument(
                    projectId = existingProjectId, name = sanitize(docExport.name), syncID = sanitizeSyncID(docExport.syncID),
                    fileType = docExport.fileType, pageCount = docExport.pageCount,
                    createdAt = DateUtils.fromISO8601(docExport.createdAt),
                    fileData = DateUtils.decodeBase64(docExport.fileData)
                )
            )

            for (pinExport in docExport.pins) {
                val existingPin = repository.getPinBySyncID(documentId, pinExport.syncID)
                val pinId: Long

                if (existingPin != null) {
                    pinId = existingPin.id
                    val incomingModified = DateUtils.fromISO8601(pinExport.modifiedAt)
                    if (incomingModified > existingPin.modifiedAt) {
                        repository.updatePin(existingPin.copy(
                            title = sanitize(pinExport.title),
                            pinDescription = sanitize(pinExport.pinDescription, MAX_DESCRIPTION_LENGTH),
                            location = sanitize(pinExport.location),
                            height = sanitize(pinExport.height), width = sanitize(pinExport.width),
                            status = sanitizeStatus(pinExport.status), category = sanitizeCategory(pinExport.category),
                            modifiedAt = incomingModified
                        ))
                    }
                } else {
                    pinId = repository.insertPin(Pin(
                        documentId = documentId, syncID = sanitizeSyncID(pinExport.syncID),
                        relativeX = pinExport.relativeX.coerceIn(0.0, 1.0),
                        relativeY = pinExport.relativeY.coerceIn(0.0, 1.0),
                        pageIndex = pinExport.pageIndex, title = sanitize(pinExport.title),
                        pinDescription = sanitize(pinExport.pinDescription, MAX_DESCRIPTION_LENGTH),
                        location = sanitize(pinExport.location),
                        height = sanitize(pinExport.height), width = sanitize(pinExport.width),
                        status = sanitizeStatus(pinExport.status), category = sanitizeCategory(pinExport.category),
                        author = sanitize(pinExport.author),
                        createdAt = DateUtils.fromISO8601(pinExport.createdAt),
                        modifiedAt = DateUtils.fromISO8601(pinExport.modifiedAt)
                    ))
                }

                // Merge comments (use syncID if available, fall back to text+author+timestamp)
                val existingComments = repository.getCommentsByPinIdSuspend(pinId)
                val commentList: List<CommentExport> = pinExport.comments.toList()
                for (c: CommentExport in commentList) {
                    val cTime = DateUtils.fromISO8601(c.createdAt)
                    val isDup = if (c.syncID.isNotBlank()) {
                        existingComments.any { it.syncID == c.syncID }
                    } else {
                        existingComments.any { it.text == c.text && it.author == c.author && abs(it.createdAt - cTime) < 1000 }
                    }
                    if (!isDup) {
                        val syncID = sanitizeSyncID(c.syncID)
                        repository.insertComment(PinComment(pinId = pinId, text = sanitize(c.text, MAX_DESCRIPTION_LENGTH), author = sanitize(c.author), createdAt = cTime, syncID = syncID))
                    }
                }

                // Merge photos (use syncID if available, fall back to caption+timestamp)
                val existingPhotos = repository.getPhotosByPinIdSuspend(pinId)
                val photoList: List<PhotoExport> = pinExport.photos.toList()
                for (p: PhotoExport in photoList) {
                    val pTime = DateUtils.fromISO8601(p.createdAt)
                    val isDup = if (p.syncID.isNotBlank()) {
                        existingPhotos.any { it.syncID == p.syncID }
                    } else {
                        existingPhotos.any { it.caption == p.caption && abs(it.createdAt - pTime) < 1000 }
                    }
                    if (!isDup) {
                        val syncID = sanitizeSyncID(p.syncID)
                        repository.insertPhoto(PinPhoto(pinId = pinId, imageData = DateUtils.decodeBase64(p.imageData), caption = sanitize(p.caption), createdAt = pTime, syncID = syncID))
                    }
                }
            }
        }
    }
}
