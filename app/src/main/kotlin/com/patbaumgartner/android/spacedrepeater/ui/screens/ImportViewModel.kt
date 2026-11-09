package com.patbaumgartner.android.spacedrepeater.ui.screens

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patbaumgartner.android.spacedrepeater.data.importing.DeckImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportUiState(
    val preview: ImportPreview? = null,
    val importing: Boolean = false,
    val error: String? = null,
    val lastResult: ImportOutcome? = null,
    /** SAF Uri of the picked archive; kept here so it survives configuration changes. */
    val archiveUri: Uri? = null,
)

data class ImportPreview(
    val name: String,
    /** Actual number of card files found in the archive. */
    val cardCount: Int,
    /** Manifest-declared cardCount, kept for the ADR-0006 integrity check. */
    val declaredCardCount: Int? = null,
    val description: String = "",
    val tags: List<String> = emptyList(),
) {
    /** True when the manifest's declared cardCount differs from the actual card files. */
    val countMismatch: Boolean get() = declaredCardCount != null && declaredCardCount != cardCount
}

data class ImportOutcome(
    val deckName: String,
    val imported: Int,
    val skipped: Int,
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importer: DeckImporter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun onArchiveSelected(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ImportUiState(importing = true, error = null, archiveUri = uri)
            try {
                val preview = contentResolver.openInputStream(uri)?.use { importer.readPreview(it) }
                    ?: throw Exception("Could not open archive")
                _uiState.value = ImportUiState(
                    preview = ImportPreview(
                        name = preview.manifest.name,
                        // Show the ACTUAL number of card files; the declared manifest
                        // count is kept for the ADR-0006 integrity warning.
                        cardCount = preview.actualCardCount,
                        declaredCardCount = preview.manifest.declaredCardCount,
                        description = preview.manifest.description,
                        tags = preview.manifest.tags,
                    ),
                    archiveUri = uri,
                )
            } catch (e: Exception) {
                _uiState.value = ImportUiState(error = e.message ?: "Import failed", archiveUri = uri)
            }
        }
    }

    fun confirmImport(contentResolver: ContentResolver) {
        val uri = _uiState.value.archiveUri ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(importing = true, error = null)
            try {
                val result = contentResolver.openInputStream(uri)?.use { importer.importArchive(it) }
                    ?: throw Exception("Could not open archive")
                val preview = _uiState.value.preview
                _uiState.value = ImportUiState(
                    lastResult = ImportOutcome(
                        deckName = preview?.name ?: "",
                        imported = result.importedCards,
                        skipped = result.skippedCards,
                    ),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(importing = false, error = e.message ?: "Import failed")
            }
        }
    }

    fun reset() {
        _uiState.value = ImportUiState()
    }
}
