package com.sitepinapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "pins",
    foreignKeys = [ForeignKey(entity = PlanDocument::class, parentColumns = ["id"], childColumns = ["documentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("documentId")]
)
@Serializable
data class Pin(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val relativeX: Double,
    val relativeY: Double,
    val pageIndex: Int = 0,
    val title: String = "",
    val pinDescription: String = "",
    val location: String = "",
    val height: String = "",
    val width: String = "",
    val status: String = "open",
    val category: String = "general",
    val author: String = "",
    val createdAt: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
    val modifiedAt: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
    val syncID: String = kotlin.uuid.Uuid.random().toString()
)
