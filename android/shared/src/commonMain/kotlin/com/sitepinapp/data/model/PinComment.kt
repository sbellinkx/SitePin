package com.sitepinapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "pin_comments",
    foreignKeys = [ForeignKey(entity = Pin::class, parentColumns = ["id"], childColumns = ["pinId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("pinId")]
)
@Serializable
data class PinComment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pinId: Long,
    val text: String,
    val author: String,
    val createdAt: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
    val syncID: String = kotlin.uuid.Uuid.random().toString()
)
