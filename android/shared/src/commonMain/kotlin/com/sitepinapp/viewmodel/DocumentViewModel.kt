package com.sitepinapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sitepinapp.data.database.DatabaseProvider
import com.sitepinapp.data.model.PlanDocument
import com.sitepinapp.data.model.Project
import com.sitepinapp.data.repository.SitePinRepository
import com.sitepinapp.platform.PlatformFileHandler
import com.sitepinapp.services.CSVExportService
import com.sitepinapp.services.ProjectSharingService
import com.sitepinapp.services.SnapshotService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DocumentViewModel(private val projectId: Long) : ViewModel() {
    private val repository = DatabaseProvider.getRepository()

    val documents: StateFlow<List<PlanDocument>> = repository.getDocumentsByProjectId(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus: StateFlow<ExportStatus> = _exportStatus.asStateFlow()

    // Pin counts per document for display
    private val _documentPinCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val documentPinCounts: StateFlow<Map<Long, Int>> = _documentPinCounts.asStateFlow()

    // Dashboard summary counts
    private val _openCount = MutableStateFlow(0)
    val openCount: StateFlow<Int> = _openCount.asStateFlow()
    private val _inProgressCount = MutableStateFlow(0)
    val inProgressCount: StateFlow<Int> = _inProgressCount.asStateFlow()
    private val _resolvedCount = MutableStateFlow(0)
    val resolvedCount: StateFlow<Int> = _resolvedCount.asStateFlow()

    init {
        viewModelScope.launch {
            _project.value = repository.getProjectById(projectId)
        }
        viewModelScope.launch {
            documents.collect { docs ->
                val counts = mutableMapOf<Long, Int>()
                for (doc in docs) {
                    counts[doc.id] = repository.getPinCountByDocumentId(doc.id)
                }
                _documentPinCounts.value = counts
            }
        }
        viewModelScope.launch {
            repository.getAllPinsByProjectId(projectId).collect { pins ->
                _openCount.value = pins.count { it.status == "open" }
                _inProgressCount.value = pins.count { it.status == "in_progress" }
                _resolvedCount.value = pins.count { it.status == "resolved" }
            }
        }
    }

    fun insertDocument(document: PlanDocument) {
        viewModelScope.launch { repository.insertDocument(document) }
    }

    fun deleteDocument(document: PlanDocument) {
        viewModelScope.launch { repository.deleteDocument(document) }
    }

    fun exportCSV(onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            _exportStatus.value = ExportStatus.Exporting
            try {
                val snapshot = SnapshotService.createSnapshot(repository, projectId)
                val csv = CSVExportService.buildCSV(snapshot)
                val fileName = "${snapshot.name.replace(" ", "_")}_pins.csv"
                val path = PlatformFileHandler.saveToCacheString(fileName, csv)
                _exportStatus.value = ExportStatus.Idle
                onComplete(path)
            } catch (e: Exception) {
                _exportStatus.value = ExportStatus.Error(e.message ?: "Export failed")
                onComplete(null)
            }
        }
    }

    fun exportSitePin(onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            _exportStatus.value = ExportStatus.Exporting
            try {
                val json = ProjectSharingService.exportProjectToJson(repository, projectId)
                val projectName = _project.value?.name ?: "project"
                val fileName = "${projectName.replace(" ", "_")}.sitepin"
                val path = PlatformFileHandler.saveToCacheString(fileName, json)
                _exportStatus.value = ExportStatus.Idle
                onComplete(path)
            } catch (e: Exception) {
                _exportStatus.value = ExportStatus.Error(e.message ?: "Export failed")
                onComplete(null)
            }
        }
    }

    fun shareFile(path: String, mimeType: String) {
        viewModelScope.launch {
            PlatformFileHandler.shareFile(path, mimeType)
        }
    }

    fun resetExportState() { _exportStatus.value = ExportStatus.Idle }

    fun getRepository(): SitePinRepository = repository
}

sealed class ExportStatus {
    data object Idle : ExportStatus()
    data object Exporting : ExportStatus()
    data class Error(val message: String) : ExportStatus()
}
