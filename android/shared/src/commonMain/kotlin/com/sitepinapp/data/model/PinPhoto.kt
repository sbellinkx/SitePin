package com.sitepinapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "pin_photos",
    foreignKeys = [ForeignKey(entity = Pin::class, parentColumns = ["id"], childColumns = ["pinId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("pinId")]
)
data class PinPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pinId: Long,
    val imageData: ByteArray,
    val caption: String = "",
    val createdAt: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
    val syncID: String = kotlin.uuid.Uuid.random().toString()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PinPhoto) return false
        return id == other.id && pinId == other.pinId && imageData.contentEquals(other.imageData)
    }
    override fun hashCode(): Int = 31 * id.hashCode() + imageData.contentHashCode()
}
