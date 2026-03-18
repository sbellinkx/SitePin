package com.sitepinapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sitepinapp.data.model.PlanDocument
import com.sitepinapp.platform.FilePickerResult
import com.sitepinapp.platform.rememberFilePickerLauncher
import com.sitepinapp.ui.components.EmptyStateView
import com.sitepinapp.ui.theme.SitePinBlue
import com.sitepinapp.ui.theme.StatusInProgress
import com.sitepinapp.ui.theme.StatusOpen
import com.sitepinapp.ui.theme.StatusResolved
import com.sitepinapp.viewmodel.DocumentViewModel
import com.sitepinapp.viewmodel.ExportStatus
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentListScreen(
    projectId: Long,
    onDocumentClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: DocumentViewModel = viewModel { DocumentViewModel(projectId) }
) {
    val documents by viewModel.documents.collectAsState()
    val project by viewModel.project.collectAsState()
    val exportStatus by viewModel.exportStatus.collectAsState()
    val documentPinCounts by viewModel.documentPinCounts.collectAsState()
    val openCount by viewModel.openCount.collectAsState()
    val inProgressCount by viewModel.inProgressCount.collectAsState()
    val resolvedCount by viewModel.resolvedCount.collectAsState()
    val totalPins = openCount + inProgressCount + resolvedCount
    var showMenu by remember { mutableStateOf(false) }
    var documentToDelete by remember { mutableStateOf<PlanDocument?>(null) }
    var showDashboard by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val dashboardSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // File picker for adding documents (PDF, images)
    val documentPickerLauncher = rememberFilePickerLauncher(
        mimeTypes = listOf("application/pdf", "image/jpeg", "image/png", "image/webp"),
        onResult = { result ->
            if (result != null) {
                val fileType = when {
                    result.mimeType.contains("pdf") -> "pdf"
                    result.mimeType.contains("png") -> "png"
                    else -> "jpg"
                }
                val pageCount = if (fileType == "pdf") {
                    try {
                        val renderer = com.sitepinapp.platform.PlatformPdfRenderer(result.data)
                        val count = renderer.pageCount
                        renderer.close()
                        count
                    } catch (_: Exception) { 1 }
                } else 1
                val doc = PlanDocument(
                    projectId = projectId,
                    name = result.fileName.substringBeforeLast("."),
                    fileData = result.data,
                    fileType = fileType,
                    pageCount = pageCount
                )
                viewModel.insertDocument(doc)
            }
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = project?.name ?: "Documents",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export CSV") },
                            leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.exportCSV { path ->
                                    if (path != null) {
                                        viewModel.shareFile(path, "text/csv")
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share .sitepin") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.exportSitePin { path ->
                                    if (path != null) {
                                        viewModel.shareFile(path, "application/octet-stream")
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Dashboard") },
                            leadingIcon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showDashboard = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { documentPickerLauncher() },
                containerColor = SitePinBlue
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Document")
            }
        }
    ) { paddingValues ->
        if (documents.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Description,
                title = "No Documents",
                subtitle = "Add a plan or image to start placing pins.",
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                // Dashboard summary card
                if (totalPins > 0) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDashboard = true },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = openCount.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = StatusOpen
                                    )
                                    Text("Open", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = inProgressCount.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = StatusInProgress
                                    )
                                    Text("In Progress", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = resolvedCount.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = StatusResolved
                                    )
                                    Text("Done", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                items(documents, key = { it.id }) { document ->
                    DocumentCard(
                        document = document,
                        pinCount = documentPinCounts[document.id] ?: 0,
                        onClick = { onDocumentClick(document.id) },
                        onDelete = { documentToDelete = document }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Exporting indicator
        if (exportStatus is ExportStatus.Exporting) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Exporting...") },
                text = { Text("Please wait while the export is prepared.") },
                confirmButton = {}
            )
        }

        if (exportStatus is ExportStatus.Error) {
            AlertDialog(
                onDismissRequest = { viewModel.resetExportState() },
                title = { Text("Export Failed") },
                text = { Text((exportStatus as ExportStatus.Error).message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetExportState() }) {
                        Text("OK")
                    }
                }
            )
        }

        importError?.let { error ->
            AlertDialog(
                onDismissRequest = { importError = null },
                title = { Text("Import Failed") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { importError = null }) {
                        Text("OK")
                    }
                }
            )
        }
    }

    // Delete confirmation
    documentToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { documentToDelete = null },
            title = { Text("Delete Document") },
            text = { Text("Are you sure you want to delete \"${doc.name}\"? All pins on this document will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDocument(doc)
                    documentToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dashboard bottom sheet
    if (showDashboard) {
        ModalBottomSheet(
            onDismissRequest = { showDashboard = false },
            sheetState = dashboardSheetState
        ) {
            PinDashboardContent(
                projectId = projectId,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun DocumentCard(
    document: PlanDocument,
    pinCount: Int = 0,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = SitePinBlue,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = document.fileType.uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (document.pageCount > 1) {
                        Text(
                            text = "${document.pageCount} pages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (pinCount > 0) {
                        Text(
                            text = "$pinCount pin(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
