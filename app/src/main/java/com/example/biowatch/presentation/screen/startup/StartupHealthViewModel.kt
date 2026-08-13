package com.example.biowatch.presentation.screen.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biowatch.data.network.AnalysisApiClient
import com.example.biowatch.domain.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class StartupHealthViewModel @Inject constructor(
    private val analysisApiClient: AnalysisApiClient,
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StartupHealthUiState())
    val uiState: StateFlow<StartupHealthUiState> = _uiState.asStateFlow()

    init {
        healthRepository.disconnect()
        checkServer()
    }

    fun checkServer() {
        _uiState.value = StartupHealthUiState(status = StartupHealthStatus.CHECKING)
        viewModelScope.launch {
            runCatching { analysisApiClient.health() }
                .onSuccess { health ->
                    _uiState.value = if (health.status == "ok" && health.modelLoaded) {
                        StartupHealthUiState(
                            status = StartupHealthStatus.READY,
                            modelVersion = health.modelVersion
                        )
                    } else {
                        StartupHealthUiState(
                            status = StartupHealthStatus.ERROR,
                            errorMessage = "분석 서버의 모델이 아직 준비되지 않았습니다."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = StartupHealthUiState(
                        status = StartupHealthStatus.ERROR,
                        errorMessage = error.message
                            ?: "FastAPI 서버에 연결할 수 없습니다."
                    )
                }
        }
    }
}
