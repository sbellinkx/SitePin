package com.sitepinapp.services

data class ProjectSnapshot(
    val name: String,
    val documents: List<DocumentSnapshot>
) {
    val totalPinCount: Int get() = documents.sumOf { it.pins.size }
}

data class DocumentSnapshot(
    val name: String,
    val fileData: ByteArray,
    val fileType: String,
    val pageCount: Int,
    val pins: List<PinSnapshot>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DocumentSnapshot) return false
        return name == other.name && fileData.contentEquals(other.fileData)
    }
    override fun hashCode(): Int = 31 * name.hashCode() + fileData.contentHashCode()
}

data class PinSnapshot(
    val number: Int,
    val title: String,
    val description: String,
    val category: String,
    val status: String,
    val author: String,
    val location: String,
    val height: String,
    val width: String,
    val pageIndex: Int,
    val documentName: String,
    val relativeX: Double,
    val relativeY: Double,
    val photoCount: Int,
    val commentCount: Int,
    val createdAt: Long,
    val photos: List<PhotoSnapshot>,
    val comments: List<CommentSnapshot>
)

data class PhotoSnapshot(val imageData: ByteArray, val caption: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhotoSnapshot) return false
        return imageData.contentEquals(other.imageData) && caption == other.caption
    }
    override fun hashCode(): Int = 31 * imageData.contentHashCode() + caption.hashCode()
}

data class CommentSnapshot(val text: String, val author: String, val createdAt: Long)
