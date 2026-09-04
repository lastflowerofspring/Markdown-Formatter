package com.example.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SnippetEntity
import com.example.model.*
import com.example.parser.MarkdownParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

enum class ViewMode {
    FORMATTED,
    RAW_EDITOR,
    SPLIT
}

class FormatterViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val snippetDao = db.snippetDao()

    private val _rawText = MutableStateFlow(
        """
# Markdown Formatter

Format raw text from AI models (Gemini, Claude, GPT), developer notes, and text files with Markdown styling, syntax highlighting, and reading themes.

> [!TIP]
> Tap any paragraph, header, code snippet or table cell to edit inline directly while keeping rich formatting.

## Code Syntax Highlighting

```kotlin
fun formatDocument(rawInput: String): FormattedDocument {
    val parser = MarkdownParser()
    return parser.parse(rawInput)
}
```

## Features Overview

| Feature | Status | Description |
| :--- | :---: | :--- |
| Interactive WYSIWYG | Supported | Tap elements to edit inline with real-time sync |
| Pro Sheet Tables | Supported | Tap any cell to edit, navigate cells & add rows/cols |
| Syntax Highlighting | Supported | 15+ programming languages with line numbers |
| Themes & Custom Fonts | Supported | Minimal Dark, Slate Night, Dracula, Warm Sepia |

- [x] Switch between themes in Appearance settings
- [x] Open external text files from Files app
- [ ] Save notes and prompts to local history
        """.trimIndent()
    )
    val rawText: StateFlow<String> = _rawText.asStateFlow()

    private val _loadedFileName = MutableStateFlow<String?>(null)
    val loadedFileName: StateFlow<String?> = _loadedFileName.asStateFlow()

    private val _formattedDoc = MutableStateFlow(MarkdownParser.parse(_rawText.value))
    val formattedDoc: StateFlow<FormattedDocument> = _formattedDoc.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.FORMATTED)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _isInteractiveEditMode = MutableStateFlow(false)
    val isInteractiveEditMode: StateFlow<Boolean> = _isInteractiveEditMode.asStateFlow()

    private val _isReadingMode = MutableStateFlow(false)
    val isReadingMode: StateFlow<Boolean> = _isReadingMode.asStateFlow()

    private val _themePreset = MutableStateFlow(ThemePreset.AI_STUDIO_DARK)
    val themePreset: StateFlow<ThemePreset> = _themePreset.asStateFlow()

    private val _fontSize = MutableStateFlow(FontSizePreference.STANDARD)
    val fontSize: StateFlow<FontSizePreference> = _fontSize.asStateFlow()

    private val _lineSpacing = MutableStateFlow(LineSpacingPreference.BALANCED)
    val lineSpacing: StateFlow<LineSpacingPreference> = _lineSpacing.asStateFlow()

    private val _fontFamily = MutableStateFlow(FontFamilyPreference.SANS)
    val fontFamily: StateFlow<FontFamilyPreference> = _fontFamily.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val snippets: StateFlow<List<SnippetEntity>> = snippetDao.getAllSnippets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var parseJob: kotlinx.coroutines.Job? = null

    fun updateRawText(newText: String) {
        _rawText.value = newText
        parseJob?.cancel()
        parseJob = viewModelScope.launch(Dispatchers.Default) {
            val parsed = MarkdownParser.parse(newText)
            _formattedDoc.value = parsed
        }
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun toggleInteractiveEditMode() {
        _isInteractiveEditMode.value = !_isInteractiveEditMode.value
    }

    fun setInteractiveEditMode(enabled: Boolean) {
        _isInteractiveEditMode.value = enabled
    }

    fun toggleReadingMode() {
        _isReadingMode.value = !_isReadingMode.value
    }

    fun setReadingMode(enabled: Boolean) {
        _isReadingMode.value = enabled
    }

    fun setThemePreset(preset: ThemePreset) {
        _themePreset.value = preset
    }

    fun setFontSize(size: FontSizePreference) {
        _fontSize.value = size
    }

    fun setLineSpacing(spacing: LineSpacingPreference) {
        _lineSpacing.value = spacing
    }

    fun setFontFamily(family: FontFamilyPreference) {
        _fontFamily.value = family
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearText() {
        updateRawText("")
    }

    fun pasteText(pasted: String) {
        if (pasted.isNotBlank()) {
            updateRawText(pasted)
            _viewMode.value = ViewMode.FORMATTED
        }
    }

    fun sanitizeText() {
        val sanitized = com.example.util.MarkdownSanitizer.sanitizeAndStandardize(_rawText.value)
        updateRawText(sanitized)
    }

    fun formatAllTables() {
        val formatted = com.example.util.MarkdownTableFormatter.formatAllTables(_rawText.value)
        updateRawText(formatted)
    }

    fun formatHtmlAndCss() {
        val formatted = com.example.util.HtmlCssFormatter.formatDocument(_rawText.value)
        updateRawText(formatted)
    }

    fun replaceAll(findText: String, replaceWith: String, ignoreCase: Boolean = true) {
        if (findText.isEmpty()) return
        val current = _rawText.value
        val regex = Regex(Regex.escape(findText), if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet())
        val updated = current.replace(regex, replaceWith)
        updateRawText(updated)
    }

    fun toggleTaskItem(lineIndex: Int, isChecked: Boolean) {
        val lines = _rawText.value.lines().toMutableList()
        if (lineIndex in lines.indices) {
            val line = lines[lineIndex]
            val updatedLine = when {
                line.contains(Regex("<input[^>]*type=[\"']checkbox[\"']", RegexOption.IGNORE_CASE)) -> {
                    if (isChecked) {
                        if (!line.contains("checked", ignoreCase = true)) {
                            line.replaceFirst(Regex("(<input[^>]*type=[\"']checkbox[\"'])", RegexOption.IGNORE_CASE), "$1 checked")
                        } else line
                    } else {
                        line.replaceFirst(Regex("\\s+checked(=[\"'][^\"']*[\"'])?", RegexOption.IGNORE_CASE), "")
                    }
                }
                isChecked -> line.replaceFirst(Regex("\\[([ ])\\]"), "[x]")
                else -> line.replaceFirst(Regex("\\[([xX])\\]"), "[ ]")
            }
            lines[lineIndex] = updatedLine
            updateRawText(lines.joinToString("\n"))
        }
    }

    fun updateBlockContent(lineStart: Int, lineEnd: Int, newBlockMarkdown: String) {
        val lines = _rawText.value.lines().toMutableList()
        if (lineStart in lines.indices && lineEnd in lines.indices && lineStart <= lineEnd) {
            val newLines = newBlockMarkdown.lines()
            val before = lines.subList(0, lineStart)
            val after = lines.subList(lineEnd + 1, lines.size)
            val combined = ArrayList<String>(before.size + newLines.size + after.size)
            combined.addAll(before)
            combined.addAll(newLines)
            combined.addAll(after)
            updateRawText(combined.joinToString("\n"))
        } else {
            // Fallback: If bounds mismatch, re-parse and append or update
            updateRawText(newBlockMarkdown)
        }
    }

    fun saveCurrentSnippet(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val text = _rawText.value
            val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            val preview = text.lines().filter { it.isNotBlank() }.take(3).joinToString(" ").take(140)
            val snippet = SnippetEntity(
                title = title.ifBlank { "Untitled Note" },
                content = text,
                previewText = preview,
                wordCount = words,
                createdAt = System.currentTimeMillis()
            )
            snippetDao.insertSnippet(snippet)
        }
    }

    fun toggleFavorite(snippet: SnippetEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            snippetDao.updateSnippet(snippet.copy(isFavorite = !snippet.isFavorite))
        }
    }

    fun deleteSnippet(snippet: SnippetEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            snippetDao.deleteSnippet(snippet)
        }
    }

    fun clearAllSnippets() {
        viewModelScope.launch(Dispatchers.IO) {
            snippetDao.clearAll()
        }
    }

    fun handleIncomingIntent(intent: Intent?, contentResolver: ContentResolver) {
        if (intent == null) return

        val action = intent.action
        val type = intent.type

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (Intent.ACTION_SEND == action) {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            updateRawText(sharedText)
                            _viewMode.value = ViewMode.FORMATTED
                        }
                        return@launch
                    }

                    // Check for stream / uri in SEND action
                    val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    if (streamUri != null) {
                        readContentFromUri(streamUri, contentResolver)
                        return@launch
                    }
                } else if (Intent.ACTION_VIEW == action) {
                    val dataUri = intent.data
                    if (dataUri != null) {
                        readContentFromUri(dataUri, contentResolver)
                        return@launch
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadFromUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            readContentFromUri(uri, contentResolver)
        }
    }

    private suspend fun readContentFromUri(uri: Uri, contentResolver: ContentResolver) {
        try {
            var retrievedName: String? = null
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        retrievedName = cursor.getString(nameIndex)
                    }
                }
            } catch (_: Exception) { }

            if (retrievedName == null) {
                retrievedName = uri.lastPathSegment?.substringAfterLast('/')
            }

            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val stringBuilder = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        if (stringBuilder.isNotEmpty()) stringBuilder.append("\n")
                        stringBuilder.append(line)
                    }
                    val content = stringBuilder.toString()
                    val finalFileName = retrievedName
                    withContext(Dispatchers.Main) {
                        _loadedFileName.value = finalFileName
                        if (content.isNotBlank()) {
                            updateRawText(content)
                            _viewMode.value = ViewMode.FORMATTED
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
