package com.example.biowatch.presentation.screen.subject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biowatch.data.storage.BaselinePreferences
import com.example.biowatch.data.storage.SavedBaseline
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SubjectSetupViewModel @Inject constructor(
    private val baselinePreferences: BaselinePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectSetupUiState())
    val uiState: StateFlow<SubjectSetupUiState> = _uiState.asStateFlow()

    private val destinationChannel = Channel<SubjectSetupDestination>(Channel.BUFFERED)
    val destinations = destinationChannel.receiveAsFlow()

    private var savedBaseline = SavedBaseline()
    private var hasUserEditedId = false

    init {
        viewModelScope.launch {
            baselinePreferences.savedBaseline.collect { saved ->
                savedBaseline = saved
                val input = if (hasUserEditedId) _uiState.value.subjectId else saved.subjectId
                _uiState.value = _uiState.value.copy(
                    subjectId = input,
                    isLoading = false,
                    hasMatchingBaseline = saved.baselineId != null && saved.subjectId == input,
                    baselineCreatedAt = saved.baselineCreatedAt
                        .takeIf { saved.baselineId != null && saved.subjectId == input }
                )
            }
        }
    }

    fun updateSubjectId(value: String) {
        hasUserEditedId = true
        val filtered = value
            .filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            .take(MAX_SUBJECT_ID_LENGTH)
        _uiState.value = _uiState.value.copy(
            subjectId = filtered,
            hasMatchingBaseline = savedBaseline.baselineId != null &&
                savedBaseline.subjectId == filtered,
            baselineCreatedAt = savedBaseline.baselineCreatedAt
                .takeIf { savedBaseline.baselineId != null && savedBaseline.subjectId == filtered },
            errorMessage = null
        )
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
        _uiState.value = _uiState.value.copy(showBaselineChoice = false)
        destinationChannel.trySend(SubjectSetupDestination.USE_EXISTING_BASELINE)
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

    private companion object {
        const val MAX_SUBJECT_ID_LENGTH = 32
    }
}
