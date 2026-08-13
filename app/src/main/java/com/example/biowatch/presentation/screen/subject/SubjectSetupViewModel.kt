package com.example.biowatch.presentation.screen.subject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biowatch.data.storage.BaselinePreferences
import com.example.biowatch.data.storage.SavedBaseline
import com.example.biowatch.domain.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SubjectSetupViewModel @Inject constructor(
    private val baselinePreferences: BaselinePreferences,
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectSetupUiState())
    val uiState: StateFlow<SubjectSetupUiState> = _uiState.asStateFlow()

    private val destinationChannel = Channel<SubjectSetupDestination>(Channel.BUFFERED)
    val destinations = destinationChannel.receiveAsFlow()

    private var savedBaseline = SavedBaseline()
    private var baselineLookupJob: Job? = null

    init {
        viewModelScope.launch {
            val activeBaseline = baselinePreferences.savedBaseline.first()
            observeBaseline(activeBaseline.subjectId)
        }
    }

    fun updateSubjectId(value: String) {
        val filtered = value
            .filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            .take(MAX_SUBJECT_ID_LENGTH)
        _uiState.value = _uiState.value.copy(
            subjectId = filtered,
            isLoading = filtered.isNotBlank(),
            hasMatchingBaseline = false,
            baselineCreatedAt = null,
            errorMessage = null
        )
        observeBaseline(filtered)
    }

    fun onScreenShown() {
        healthRepository.disconnect()
    }

    fun continueSetup() {
        val id = _uiState.value.subjectId.trim()
        if (id.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "사용자 ID를 입력해 주세요.")
            return
        }

        if (savedBaseline.baselineId != null && savedBaseline.subjectId == id) {
            _uiState.value = _uiState.value.copy(showBaselineChoice = true)
        } else {
            prepareNewSubject(id)
        }
    }

    fun useExistingBaseline() {
        val id = _uiState.value.subjectId.trim()
        _uiState.value = _uiState.value.copy(showBaselineChoice = false)
        viewModelScope.launch {
            baselinePreferences.saveSubjectId(id)
            destinationChannel.send(SubjectSetupDestination.USE_EXISTING_BASELINE)
        }
    }

    fun createNewBaseline() {
        val id = _uiState.value.subjectId.trim()
        _uiState.value = _uiState.value.copy(showBaselineChoice = false)
        viewModelScope.launch {
            baselinePreferences.saveSubjectId(id)
            destinationChannel.send(SubjectSetupDestination.CREATE_BASELINE)
        }
    }

    fun dismissBaselineChoice() {
        _uiState.value = _uiState.value.copy(showBaselineChoice = false)
    }

    private fun prepareNewSubject(id: String) {
        viewModelScope.launch {
            baselinePreferences.saveSubjectId(id)
            destinationChannel.send(SubjectSetupDestination.CREATE_BASELINE)
        }
    }

    private fun observeBaseline(subjectId: String) {
        baselineLookupJob?.cancel()
        if (subjectId.isBlank()) {
            savedBaseline = SavedBaseline(subjectId = subjectId)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                hasMatchingBaseline = false,
                baselineCreatedAt = null
            )
            return
        }
        baselineLookupJob = viewModelScope.launch {
            baselinePreferences.baselineForSubject(subjectId).collect { saved ->
                savedBaseline = saved
                _uiState.value = _uiState.value.copy(
                    subjectId = saved.subjectId,
                    isLoading = false,
                    hasMatchingBaseline = saved.baselineId != null,
                    baselineCreatedAt = saved.baselineCreatedAt
                        .takeIf { saved.baselineId != null }
                )
            }
        }
    }

    private companion object {
        const val MAX_SUBJECT_ID_LENGTH = 32
    }
}
