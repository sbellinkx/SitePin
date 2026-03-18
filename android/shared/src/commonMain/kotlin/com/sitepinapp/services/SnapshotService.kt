package com.sitepinapp.services

import com.sitepinapp.data.repository.SitePinRepository

object SnapshotService {
    suspend fun createSnapshot(repository: SitePinRepository, projectId: Long): ProjectSnapshot {
        val fullData = repository.getFullProjectData(projectId)
            ?: throw IllegalStateException("Project not found")

        var pinNumber = 1
        return ProjectSnapshot(
            name = fullData.project.name,
            documents = fullData.documents.map { docData ->
                DocumentSnapshot(
                    name = docData.document.name,
                    fileData = docData.document.fileData,
                    fileType = docData.document.fileType,
                    pageCount = docData.document.pageCount,
                    pins = docData.pins.map { pinData ->
                        PinSnapshot(
                            number = pinNumber++,
                            title = pinData.pin.title,
                            description = pinData.pin.pinDescription,
                            category = pinData.pin.category,
                            status = pinData.pin.status,
                            author = pinData.pin.author,
                            location = pinData.pin.location,
                            height = pinData.pin.height,
                            width = pinData.pin.width,
                            pageIndex = pinData.pin.pageIndex,
                            documentName = docData.document.name,
                            relativeX = pinData.pin.relativeX,
                            relativeY = pinData.pin.relativeY,
                            photoCount = pinData.photos.size,
                            commentCount = pinData.comments.size,
                            createdAt = pinData.pin.createdAt,
                            photos = pinData.photos.map { PhotoSnapshot(it.imageData, it.caption) },
                            comments = pinData.comments.map { CommentSnapshot(it.text, it.author, it.createdAt) }
                        )
                    }
                )
            }
        )
    }
}
