package com.sitepinapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "plan_documents",
    foreignKeys = [ForeignKey(entity = Project::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")]
)
data class PlanDocument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val fileData: ByteArray,
    val fileType: String,
    val pageCount: Int = 1,
    val createdAt: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
    val syncID: String = kotlin.uuid.Uuid.random().toString()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlanDocument) return false
        return id == other.id && name == other.name && fileData.contentEquals(other.fileData) && syncID == other.syncID
    }
    override fun hashCode(): Int = 31 * id.hashCode() + name.hashCode() + fileData.contentHashCode()
}
