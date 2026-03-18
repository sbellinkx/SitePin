package com.sitepinapp.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class ProjectWithDocuments(
    @Embedded val project: Project,
    @Relation(parentColumn = "id", entityColumn = "projectId")
    val documents: List<PlanDocument>
)

data class DocumentWithPins(
    @Embedded val document: PlanDocument,
    @Relation(parentColumn = "id", entityColumn = "documentId")
    val pins: List<Pin>
)

data class PinWithDetails(
    @Embedded val pin: Pin,
    @Relation(parentColumn = "id", entityColumn = "pinId")
    val photos: List<PinPhoto>,
    @Relation(parentColumn = "id", entityColumn = "pinId")
    val comments: List<PinComment>
)
