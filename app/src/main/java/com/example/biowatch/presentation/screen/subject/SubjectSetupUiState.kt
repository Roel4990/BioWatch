package com.example.biowatch.presentation.screen.subject

data class SubjectSetupUiState(
    val subjectId: String = "",
    val isLoading: Boolean = true,
    val hasMatchingBaseline: Boolean = false,
    val baselineCreatedAt: String? = null,
    val showBaselineChoice: Boolean = false,
    val errorMessage: String? = null
)

enum class SubjectSetupDestination {
    USE_EXISTING_BASELINE,
    CREATE_BASELINE
}
