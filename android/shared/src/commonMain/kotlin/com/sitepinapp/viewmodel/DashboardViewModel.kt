package com.sitepinapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sitepinapp.data.database.DatabaseProvider
import com.sitepinapp.data.model.Pin
import com.sitepinapp.data.repository.SitePinRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(private val projectId: Long) : ViewModel() {
    private val repository = DatabaseProvider.getRepository()

    private val allPins: StateFlow<List<Pin>> = repository.getAllPinsByProjectId(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todoPins: StateFlow<List<Pin>> = allPins
        .map { pins -> pins.filter { it.status != "resolved" }.sortedByDescending { it.modifiedAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val donePins: StateFlow<List<Pin>> = allPins
        .map { pins -> pins.filter { it.status == "resolved" }.sortedByDescending { it.modifiedAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = allPins
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val openCount: StateFlow<Int> = allPins
        .map { pins -> pins.count { it.status == "open" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val inProgressCount: StateFlow<Int> = allPins
        .map { pins -> pins.count { it.status == "in_progress" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val resolvedCount: StateFlow<Int> = allPins
        .map { pins -> pins.count { it.status == "resolved" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun updatePinStatus(pinId: Long, status: String) {
        viewModelScope.launch {
            repository.updatePinStatus(pinId, status)
        }
    }
}
