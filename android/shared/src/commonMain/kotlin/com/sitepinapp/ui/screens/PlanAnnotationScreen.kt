package com.sitepinapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sitepinapp.data.model.PinCategory
import com.sitepinapp.platform.PlatformPdfRenderer
import com.sitepinapp.platform.toImageBitmap
import com.sitepinapp.ui.components.CategoryFilterPicker
import com.sitepinapp.ui.components.PinMarkerView
import com.sitepinapp.ui.components.StatusFilterPicker
import com.sitepinapp.ui.theme.SitePinBlue
import com.sitepinapp.viewmodel.PinViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanAnnotationScreen(
    documentId: Long,
    onBack: () -> Unit,
    viewModel: PinViewModel = viewModel { PinViewModel(documentId) }
) {
    val document by viewModel.document.collectAsState()
    val filteredPins by viewModel.filteredPins.collectAsState()
    val allPins by viewModel.allPinsForDocument.collectAsState()
    val selectedPinId by viewModel.selectedPinId.collectAsState()
    val selectedPinDetails by viewModel.selectedPinDetails.collectAsState()
    val filterCategory by viewModel.filterCategory.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()

    var isPlacementMode by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var showPinDetail by remember { mutableStateOf(false) }
    val pinDetailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Render the document page
    val pageImage: ImageBitmap? = remember(document, currentPage) {
        val doc = document ?: return@remember null
        if (doc.fileType.equals("pdf", ignoreCase = true)) {
            val renderer = PlatformPdfRenderer(doc.fileData)
            val bitmap = renderer.renderPage(currentPage)
            renderer.close()
            bitmap
        } else {
            doc.fileData.toImageBitmap()
        }
    }

    // Zoom/pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offset = Offset(
            x = offset.x + panChange.x,
            y = offset.y + panChange.y
        )
    }

    // Pin numbering map
    val pinNumberMap = remember(allPins) {
        allPins.sortedBy { it.createdAt }.mapIndexed { index, pin -> pin.id to index + 1 }.toMap()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = document?.name ?: "Plan",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if ((document?.pageCount ?: 1) > 1) {
                            Text(
                                text = "Page ${currentPage + 1} of ${document?.pageCount ?: 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isPlacementMode) {
                        IconButton(onClick = { isPlacementMode = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel placement")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                // Page navigation
                if ((document?.pageCount ?: 1) > 1) {
                    Row {
                        SmallFloatingActionButton(
                            onClick = {
                                if (currentPage > 0) viewModel.setCurrentPage(currentPage - 1)
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous page")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        SmallFloatingActionButton(
                            onClick = {
                                val maxPage = (document?.pageCount ?: 1) - 1
                                if (currentPage < maxPage) viewModel.setCurrentPage(currentPage + 1)
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next page")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Add pin FAB
                FloatingActionButton(
                    onClick = { isPlacementMode = !isPlacementMode },
                    containerColor = if (isPlacementMode) MaterialTheme.colorScheme.error else SitePinBlue
                ) {
                    Icon(
                        imageVector = if (isPlacementMode) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (isPlacementMode) "Cancel" else "Add Pin"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter bar
            if (showFilters) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    CategoryFilterPicker(
                        selectedCategory = filterCategory,
                        onCategorySelected = { viewModel.setFilterCategory(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusFilterPicker(
                        selectedStatus = filterStatus,
                        onStatusSelected = { viewModel.setFilterStatus(it) }
                    )
                }
            }

            // Filter toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showFilters = !showFilters }) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Toggle filters",
                        tint = if (showFilters || filterCategory != null || filterStatus != null)
                            SitePinBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${filteredPins.size} pin${if (filteredPins.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isPlacementMode) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Tap on the plan to place a pin",
                        style = MaterialTheme.typography.bodySmall,
                        color = SitePinBlue
                    )
                }
            }

            // Plan view with pins
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2A2A2A))
            ) {
                if (pageImage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .transformable(state = transformableState)
                            .pointerInput(isPlacementMode) {
                                if (isPlacementMode) {
                                    detectTapGestures { tapOffset ->
                                        if (imageSize.width > 0 && imageSize.height > 0) {
                                            // Convert screen coordinates to image coordinates accounting for zoom/pan
                                            val adjustedX = (tapOffset.x - offset.x) / scale
                                            val adjustedY = (tapOffset.y - offset.y) / scale
                                            // Calculate relative position from adjusted coordinates
                                            val relX = (adjustedX / imageSize.width).toDouble().coerceIn(0.0, 1.0)
                                            val relY = (adjustedY / imageSize.height).toDouble().coerceIn(0.0, 1.0)
                                            viewModel.addPin(relX, relY)
                                            isPlacementMode = false
                                            showPinDetail = true
                                        }
                                    }
                                }
                            }
                    ) {
                        Image(
                            bitmap = pageImage,
                            contentDescription = "Plan document",
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { imageSize = it },
                            contentScale = ContentScale.Fit
                        )

                        // Pin markers overlay
                        filteredPins.forEach { pin ->
                            val pinNumber = pinNumberMap[pin.id] ?: 0
                            val category = PinCategory.fromString(pin.category)
                            val isSelected = pin.id == selectedPinId

                            if (imageSize.width > 0 && imageSize.height > 0) {
                                val pixelX = (pin.relativeX * imageSize.width).roundToInt()
                                val pixelY = (pin.relativeY * imageSize.height).roundToInt()

                                PinMarkerView(
                                    number = pinNumber,
                                    category = category,
                                    isSelected = isSelected,
                                    modifier = Modifier.offset {
                                        IntOffset(pixelX - 14, pixelY - 14)
                                    },
                                    onClick = {
                                        viewModel.selectPin(pin.id)
                                        showPinDetail = true
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loading document...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    // Pin detail bottom sheet
    if (showPinDetail && selectedPinDetails != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showPinDetail = false
                viewModel.selectPin(null)
            },
            sheetState = pinDetailSheetState
        ) {
            PinDetailContent(
                pinWithDetails = selectedPinDetails!!,
                onUpdatePin = { updatedPin -> viewModel.updatePin(updatedPin) },
                onUpdateStatus = { status ->
                    selectedPinDetails?.pin?.id?.let { viewModel.updatePinStatus(it, status) }
                },
                onDeletePin = { pin ->
                    viewModel.deletePin(pin)
                    showPinDetail = false
                },
                onAddPhoto = { imageData ->
                    selectedPinDetails?.pin?.id?.let { viewModel.addPhoto(it, imageData) }
                },
                onDeletePhoto = { photo -> viewModel.deletePhoto(photo) },
                onAddComment = { text ->
                    selectedPinDetails?.pin?.id?.let { viewModel.addComment(it, text) }
                },
                onDeleteComment = { comment -> viewModel.deleteComment(comment) },
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}
