package com.sitepinapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Entity(tableName = "projects")
@Serializable
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
    val syncID: String = kotlin.uuid.Uuid.random().toString()
)
