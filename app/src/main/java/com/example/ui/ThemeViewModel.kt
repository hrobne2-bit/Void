package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ThemeEntity
import com.example.data.ThemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class ThemeViewModel(private val repository: ThemeRepository) : ViewModel() {

    val allThemes: StateFlow<List<ThemeEntity>> = repository.allThemes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeTheme: StateFlow<ThemeEntity?> = repository.activeTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _editingTheme = MutableStateFlow<ThemeEntity?>(null)
    val editingTheme: StateFlow<ThemeEntity?> = _editingTheme.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndSeedDefaults()
        }
    }

    fun selectTheme(themeId: Int) {
        viewModelScope.launch {
            repository.setActiveTheme(themeId)
        }
    }

    fun startNewTheme() {
        _editingTheme.value = ThemeEntity(name = "New Theme")
    }

    fun startEditing(theme: ThemeEntity) {
        _editingTheme.value = theme
    }

    fun cancelEditing() {
        _editingTheme.value = null
    }

    fun updateEditingTheme(updater: (ThemeEntity) -> ThemeEntity) {
        _editingTheme.value = _editingTheme.value?.let { updater(it) }
    }

    fun saveEditingTheme() {
        val theme = _editingTheme.value ?: return
        viewModelScope.launch {
            if (theme.id == 0) {
                repository.insertTheme(theme)
            } else {
                repository.updateTheme(theme)
            }
            _editingTheme.value = null
        }
    }

    fun deleteTheme(theme: ThemeEntity) {
        viewModelScope.launch {
            repository.deleteTheme(theme)
        }
    }

    fun exportTheme(context: Context, theme: ThemeEntity, uri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(theme.toJsonString().toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importTheme(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val reader = BufferedReader(InputStreamReader(input))
                    val stringBuilder = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                    val jsonStr = stringBuilder.toString()
                    val imported = ThemeEntity.fromJsonString(jsonStr)
                    if (imported != null) {
                        repository.insertTheme(imported)
                        onSuccess()
                    } else {
                        onError("Invalid theme file format")
                    }
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to import theme")
            }
        }
    }
}

class ThemeViewModelFactory(private val repository: ThemeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ThemeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
