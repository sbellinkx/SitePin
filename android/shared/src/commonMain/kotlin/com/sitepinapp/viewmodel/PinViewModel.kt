package com.sitepinapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sitepinapp.data.database.DatabaseProvider
import com.sitepinapp.data.model.Pin
import com.sitepinapp.data.model.PinCategory
import com.sitepinapp.data.model.PinComment
import com.sitepinapp.data.model.PinPhoto
import com.sitepinapp.data.model.PinWithDetails
import com.sitepinapp.data.model.PlanDocument
import com.sitepinapp.data.repository.SitePinRepository
import com.sitepinapp.services.UserProfileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import com.sitepinapp.platform.ImportLimits

class PinViewModel(private val documentId: Long) : ViewModel() {
    private val repository = DatabaseProvider.getRepository()

    private val allPins: StateFlow<List<Pin>> = repository.getPinsByDocumentId(documentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _document = MutableStateFlow<PlanDocument?>(null)
    val document: StateFlow<PlanDocument?> = _document.asStateFlow()

    private val _selectedPinId = MutableStateFlow<Long?>(null)
    val selectedPinId: StateFlow<Long?> = _selectedPinId.asStateFlow()

    private val _selectedPinDetails = MutableStateFlow<PinWithDetails?>(null)
    val selectedPinDetails: StateFlow<PinWithDetails?> = _selectedPinDetails.asStateFlow()

    private val _filterCategory = MutableStateFlow<PinCategory?>(null)
    val filterCategory: StateFlow<PinCategory?> = _filterCategory.asStateFlow()

    private val _filterStatus = MutableStateFlow<String?>(null)
    val filterStatus: StateFlow<String?> = _filterStatus.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    val filteredPins: StateFlow<List<Pin>> = combine(
        allPins, _filterCategory, _filterStatus, _currentPage
    ) { pins, category, status, page ->
        pins.filter { pin ->
            (category == null || pin.category.equals(category.name, ignoreCase = true)) &&
            (status == null || pin.status == status) &&
            pin.pageIndex == page
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPinsForDocument: StateFlow<List<Pin>> = allPins

    init {
        viewModelScope.launch {
            _document.value = repository.getDocumentById(documentId)
        }
    }

    fun setFilterCategory(category: PinCategory?) {
        _filterCategory.value = category
    }

    fun setFilterStatus(status: String?) {
        _filterStatus.value = status
    }

    fun setCurrentPage(page: Int) {
        _currentPage.value = page
    }

    fun selectPin(pinId: Long?) {
        _selectedPinId.value = pinId
        if (pinId != null) {
            viewModelScope.launch {
                _selectedPinDetails.value = repository.getPinWithDetails(pinId)
            }
        } else {
            _selectedPinDetails.value = null
        }
    }

    fun refreshSelectedPin() {
        val pinId = _selectedPinId.value ?: return
        viewModelScope.launch {
            _selectedPinDetails.value = repository.getPinWithDetails(pinId)
        }
    }

    fun addPin(relativeX: Double, relativeY: Double) {
        viewModelScope.launch {
            val pinCount = allPins.value.size + 1
            val pin = Pin(
                documentId = documentId,
                relativeX = relativeX,
                relativeY = relativeY,
                pageIndex = _currentPage.value,
                title = "Pin $pinCount",
                author = UserProfileManager.getDisplayName(),
                status = "open",
                category = "general"
            )
            val id = repository.insertPin(pin)
            selectPin(id)
        }
    }

    fun updatePin(pin: Pin) {
        viewModelScope.launch {
            repository.updatePin(pin.copy(modifiedAt = Clock.System.now().toEpochMilliseconds()))
            refreshSelectedPin()
        }
    }

    fun updatePinStatus(pinId: Long, status: String) {
        viewModelScope.launch {
            repository.updatePinStatus(pinId, status)
            refreshSelectedPin()
        }
    }

    fun deletePin(pin: Pin) {
        viewModelScope.launch {
            repository.deletePin(pin)
            _selectedPinId.value = null
            _selectedPinDetails.value = null
        }
    }

    private val _photoLimitError = MutableStateFlow<String?>(null)
    val photoLimitError: StateFlow<String?> = _photoLimitError.asStateFlow()

    fun clearPhotoLimitError() { _photoLimitError.value = null }

    fun addPhoto(pinId: Long, imageData: ByteArray, caption: String = "") {
        viewModelScope.launch {
            val currentCount = repository.getPhotoCountByPinId(pinId)
            if (currentCount >= ImportLimits.MAX_PHOTOS_PER_PIN) {
                _photoLimitError.value = "Maximum of ${ImportLimits.MAX_PHOTOS_PER_PIN} photos per pin reached."
                return@launch
            }
            repository.insertPhoto(PinPhoto(pinId = pinId, imageData = imageData, caption = caption))
            refreshSelectedPin()
        }
    }

    fun deletePhoto(photo: PinPhoto) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
            refreshSelectedPin()
        }
    }

    fun addComment(pinId: Long, text: String) {
        viewModelScope.launch {
            val comment = PinComment(
                pinId = pinId,
                text = text,
                author = UserProfileManager.getDisplayName()
            )
            repository.insertComment(comment)
            refreshSelectedPin()
        }
    }

    fun deleteComment(comment: PinComment) {
        viewModelScope.launch {
            repository.deleteComment(comment)
            refreshSelectedPin()
        }
    }
}
