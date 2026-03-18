package com.sitepinapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sitepinapp.data.database.DatabaseProvider
import com.sitepinapp.data.model.Project
import com.sitepinapp.data.repository.SitePinRepository
import com.sitepinapp.services.ProjectSharingService
import com.sitepinapp.services.UserProfileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProjectSummary(
    val documentCount: Int = 0,
    val openCount: Int = 0,
    val resolvedCount: Int = 0
)

class ProjectViewModel : ViewModel() {
    private val repository = DatabaseProvider.getRepository()

    val allProjects: StateFlow<List<Project>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _summaries = MutableStateFlow<Map<Long, ProjectSummary>>(emptyMap())
    val summaries: StateFlow<Map<Long, ProjectSummary>> = _summaries.asStateFlow()

    private val _themeMode = MutableStateFlow(UserProfileManager.getTheme())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    init {
        viewModelScope.launch {
            allProjects.collect { projects ->
                val map = mutableMapOf<Long, ProjectSummary>()
                for (p in projects) {
                    val docCount = repository.getDocumentCount(p.id)
                    val open = repository.getPinCountByProjectIdAndStatus(p.id, "open") +
                            repository.getPinCountByProjectIdAndStatus(p.id, "in_progress")
                    val resolved = repository.getPinCountByProjectIdAndStatus(p.id, "resolved")
                    map[p.id] = ProjectSummary(docCount, open, resolved)
                }
                _summaries.value = map
            }
        }
    }

    fun createProject(name: String) {
        viewModelScope.launch { repository.insertProject(Project(name = name)) }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch { repository.deleteProject(project) }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch { repository.updateProject(project) }
    }

    fun importProjectFromJson(json: String, onResult: (Result<Long>) -> Unit) {
        viewModelScope.launch {
            try {
                val id = ProjectSharingService.importProjectFromJson(repository, json)
                onResult(Result.success(id))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun getDisplayName(): String = UserProfileManager.getDisplayName()
    fun setDisplayName(name: String) = UserProfileManager.setDisplayName(name)
    fun hasProfile(): Boolean = UserProfileManager.hasProfile()

    fun getTheme(): String = UserProfileManager.getTheme()
    fun setTheme(theme: String) {
        UserProfileManager.setTheme(theme)
        _themeMode.value = theme
    }

    fun getRepository(): SitePinRepository = repository
}
