package com.sitepinapp.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sitepinapp.data.model.Pin
import com.sitepinapp.data.model.PinCategory
import com.sitepinapp.data.model.PinComment
import com.sitepinapp.data.model.PinPhoto
import com.sitepinapp.data.model.PinWithDetails
import com.sitepinapp.platform.rememberImagePickerLauncher
import com.sitepinapp.ui.components.CategoryIcon
import com.sitepinapp.ui.components.CategoryPicker
import com.sitepinapp.ui.components.FullScreenPhotoView
import com.sitepinapp.ui.components.PhotoThumbnail
import com.sitepinapp.ui.components.StatusBadge
import com.sitepinapp.ui.components.StatusPicker
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun PinDetailContent(
    pinWithDetails: PinWithDetails,
    onUpdatePin: (Pin) -> Unit,
    onUpdateStatus: (String) -> Unit,
    onDeletePin: (Pin) -> Unit,
    onAddPhoto: (ByteArray) -> Unit,
    onDeletePhoto: (PinPhoto) -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (PinComment) -> Unit,
    modifier: Modifier = Modifier
) {
    val pin = pinWithDetails.pin
    val photos = pinWithDetails.photos
    val comments = pinWithDetails.comments

    var title by remember(pin.id) { mutableStateOf(pin.title) }
    var description by remember(pin.id) { mutableStateOf(pin.pinDescription) }
    var location by remember(pin.id) { mutableStateOf(pin.location) }
    var height by remember(pin.id) { mutableStateOf(pin.height) }
    var width by remember(pin.id) { mutableStateOf(pin.width) }
    var commentText by remember { mutableStateOf("") }
    var fullScreenPhoto by remember { mutableStateOf<ByteArray?>(null) }
    var showPhotoSourceMenu by remember { mutableStateOf(false) }

    // Image picker launcher for camera/gallery
    val imagePickerLauncher = rememberImagePickerLauncher { imageData ->
        if (imageData != null) {
            onAddPhoto(imageData)
        }
    }

    if (fullScreenPhoto != null) {
        FullScreenPhotoView(
            imageData = fullScreenPhoto!!,
            onDismiss = { fullScreenPhoto = null }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // Header with category icon and status
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(
                    category = PinCategory.fromString(pin.category),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = PinCategory.fromString(pin.category).label,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            StatusBadge(status = pin.status)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Description
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Location
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Dimensions
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = height,
                onValueChange = { height = it },
                label = { Text("Height") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = width,
                onValueChange = { width = it },
                label = { Text("Width") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save button
        Button(
            onClick = {
                onUpdatePin(
                    pin.copy(
                        title = title.trim(),
                        pinDescription = description.trim(),
                        location = location.trim(),
                        height = height.trim(),
                        width = width.trim()
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Category picker
        Text("Category", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        CategoryPicker(
            selectedCategory = PinCategory.fromString(pin.category),
            onCategorySelected = { category ->
                onUpdatePin(pin.copy(category = category.name.lowercase()))
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Status picker
        Text("Status", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        StatusPicker(
            selectedStatus = pin.status,
            onStatusSelected = { status -> onUpdateStatus(status) }
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Photos section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Photos (${photos.size})", style = MaterialTheme.typography.titleSmall)
            androidx.compose.foundation.layout.Box {
                IconButton(onClick = { showPhotoSourceMenu = true }) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "Add photo")
                }
                DropdownMenu(
                    expanded = showPhotoSourceMenu,
                    onDismissRequest = { showPhotoSourceMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Take Photo") },
                        onClick = {
                            showPhotoSourceMenu = false
                            imagePickerLauncher.launchCamera()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Choose from Gallery") },
                        onClick = {
                            showPhotoSourceMenu = false
                            imagePickerLauncher.launchGallery()
                        }
                    )
                }
            }
        }

        if (photos.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                photos.forEach { photo ->
                    PhotoThumbnail(
                        imageData = photo.imageData,
                        showDeleteButton = true,
                        onDelete = { onDeletePhoto(photo) },
                        onClick = { fullScreenPhoto = photo.imageData }
                    )
                }
            }
        } else {
            Text(
                text = "No photos yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Comments section
        Text("Comments (${comments.size})", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        // Add comment
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                placeholder = { Text("Add a comment...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (commentText.isNotBlank()) {
                        onAddComment(commentText.trim())
                        commentText = ""
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Comment list
        comments.sortedByDescending { it.createdAt }.forEach { comment ->
            CommentItem(
                comment = comment,
                onDelete = { onDeleteComment(comment) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Author info
        Text(
            text = "Created by ${pin.author.ifBlank { "Unknown" }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Delete button
        OutlinedButton(
            onClick = { onDeletePin(pin) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete Pin")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CommentItem(
    comment: PinComment,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.author.ifBlank { "Anonymous" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatCommentDate(comment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete comment",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatCommentDate(epochMillis: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.dayOfMonth.toString().padStart(2, '0')}/${local.monthNumber.toString().padStart(2, '0')} ${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
}
