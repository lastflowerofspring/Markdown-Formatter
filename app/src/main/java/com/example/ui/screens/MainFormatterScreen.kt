package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.*
import com.example.ui.components.FindAndReplaceBar
import com.example.ui.components.MarkdownRenderer
import com.example.ui.sheets.*
import com.example.viewmodel.FormatterViewModel
import com.example.viewmodel.ViewMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFormatterScreen(
    viewModel: FormatterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isSystemDark = isSystemInDarkTheme()

    val rawText by viewModel.rawText.collectAsStateWithLifecycle()
    val loadedFileName by viewModel.loadedFileName.collectAsStateWithLifecycle()
    val formattedDoc by viewModel.formattedDoc.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val isInteractiveEditMode by viewModel.isInteractiveEditMode.collectAsStateWithLifecycle()
    val isReadingMode by viewModel.isReadingMode.collectAsStateWithLifecycle()
    val themePreset by viewModel.themePreset.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val lineSpacing by viewModel.lineSpacing.collectAsStateWithLifecycle()
    val fontFamily by viewModel.fontFamily.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val snippets by viewModel.snippets.collectAsStateWithLifecycle()

    val themeColors = remember(themePreset, isSystemDark) {
        ThemePalettes.getColorsForPreset(themePreset, isSystemDark)
    }

    val lazyListState = rememberLazyListState()

    // Editor Text Field State with Selection
    var editorTextFieldValue by remember { mutableStateOf(TextFieldValue(rawText)) }

    // Sync from ViewModel rawText changes (e.g. file loaded, samples picked, task item toggled)
    LaunchedEffect(rawText) {
        if (rawText != editorTextFieldValue.text) {
            val prevSel = editorTextFieldValue.selection
            val newSel = TextRange(
                prevSel.start.coerceIn(0, rawText.length),
                prevSel.end.coerceIn(0, rawText.length)
            )
            editorTextFieldValue = TextFieldValue(text = rawText, selection = newSel)
        }
    }

    // Sheet states
    var showThemeSheet by remember { mutableStateOf(false) }
    var showTocSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showSamplesSheet by remember { mutableStateOf(false) }
    var showSearchOverlay by remember { mutableStateOf(false) }

    // Interactive Edit Sheet States
    var editingBlock by remember { mutableStateOf<MarkdownBlock?>(null) }
    var editingTableMatrixState by remember { mutableStateOf<Pair<MarkdownBlock.TableBlock, TableMatrix>?>(null) }
    var editingTableCellCoords by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // File Picker Launcher for any common text file
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadFromUri(uri, context.contentResolver)
            Toast.makeText(context, "Loaded file successfully", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .testTag("main_formatter_scaffold"),
        containerColor = themeColors.background,
        topBar = {
            if (!isReadingMode) {
                MainTopBar(
                    themeColors = themeColors,
                    viewMode = viewMode,
                    headingCount = formattedDoc.headings.size,
                    fileName = loadedFileName,
                    searchActive = showSearchOverlay,
                    onViewModeChange = { newMode ->
                        if (viewMode == ViewMode.FORMATTED && newMode == ViewMode.RAW_EDITOR) {
                            // Find line index corresponding to current top visible item in reader
                            val visibleItemIndex = lazyListState.firstVisibleItemIndex
                            val block = formattedDoc.blocks.getOrNull(visibleItemIndex)
                            if (block != null) {
                                val lines = rawText.lines()
                                val targetLine = block.lineStart.coerceIn(0, (lines.size - 1).coerceAtLeast(0))
                                val charOffset = lines.take(targetLine).sumOf { it.length + 1 }
                                editorTextFieldValue = editorTextFieldValue.copy(
                                    selection = TextRange(charOffset.coerceIn(0, rawText.length))
                                )
                            }
                        } else if (viewMode == ViewMode.RAW_EDITOR && newMode == ViewMode.FORMATTED) {
                            // Find formatted block corresponding to current editor cursor offset
                            val cursorOffset = editorTextFieldValue.selection.start
                            val lines = rawText.lines()
                            var accumulated = 0
                            var cursorLine = 0
                            for ((idx, line) in lines.withIndex()) {
                                val lineEnd = accumulated + line.length + 1
                                if (cursorOffset <= lineEnd) {
                                    cursorLine = idx
                                    break
                                }
                                accumulated = lineEnd
                            }
                            val blockIndex = formattedDoc.blocks.indexOfFirst { cursorLine in it.lineStart..it.lineEnd }
                            if (blockIndex >= 0) {
                                coroutineScope.launch {
                                    lazyListState.scrollToItem(blockIndex)
                                }
                            }
                        }
                        viewModel.setViewMode(newMode)
                    },
                    onToggleSearch = {
                        showSearchOverlay = !showSearchOverlay
                        if (!showSearchOverlay) viewModel.setSearchQuery("")
                    },
                    onOpenToc = { showTocSheet = true },
                    onOpenTheme = { showThemeSheet = true },
                    onOpenHistory = { showHistorySheet = true },
                    onOpenSamples = { showSamplesSheet = true },
                    onToggleReadingMode = { viewModel.toggleReadingMode() },
                    onOpenFile = { filePickerLauncher.launch("*/*") }
                )
            } else {
                ReadingModeTopBar(
                    themeColors = themeColors,
                    readingTimeMin = formattedDoc.readingTimeMinutes,
                    wordCount = formattedDoc.wordCount,
                    onOpenToc = { showTocSheet = true },
                    onOpenTheme = { showThemeSheet = true },
                    onExitReadingMode = { viewModel.setReadingMode(false) }
                )
            }
        },
        bottomBar = {
            if (!isReadingMode && viewMode != ViewMode.RAW_EDITOR) {
                ReaderBottomMetricsBar(
                    themeColors = themeColors,
                    doc = formattedDoc,
                    isInteractiveMode = isInteractiveEditMode,
                    onToggleInteractiveMode = {
                        viewModel.toggleInteractiveEditMode()
                        if (!isInteractiveEditMode) {
                            Toast.makeText(context, "Interactive Edit Enabled: Tap any formatted block or table cell to edit", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCopyPlain = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Plain Text", rawText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied plain text", Toast.LENGTH_SHORT).show()
                    },
                    onShare = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, rawText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share formatted markdown")
                        context.startActivity(shareIntent)
                    },
                    onSaveSnippet = { showHistorySheet = true }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(themeColors.background)
        ) {
            when (viewMode) {
                ViewMode.FORMATTED -> {
                    MarkdownRenderer(
                        document = formattedDoc,
                        themeColors = themeColors,
                        fontSize = fontSize,
                        lineSpacing = lineSpacing,
                        fontFamily = fontFamily,
                        lazyListState = lazyListState,
                        searchQuery = searchQuery,
                        isInteractiveMode = isInteractiveEditMode,
                        onToggleTask = { lineIdx, isChecked ->
                            viewModel.toggleTaskItem(lineIdx, isChecked)
                        },
                        onEditBlock = { block ->
                            editingBlock = block
                        },
                        onEditTableCell = { tableBlock, rowIdx, colIdx ->
                            val rawHeaders = tableBlock.headers.map { it.rawText }
                            val rawRows = tableBlock.rows.map { row -> row.map { it.rawText } }
                            val matrix = TableMatrix(
                                headers = rawHeaders,
                                alignments = tableBlock.alignments,
                                rows = rawRows
                            )
                            editingTableMatrixState = Pair(tableBlock, matrix)
                            editingTableCellCoords = Pair(rowIdx, colIdx)
                        }
                    )
                }

                ViewMode.RAW_EDITOR -> {
                    RawEditorView(
                        textFieldValue = editorTextFieldValue,
                        themeColors = themeColors,
                        fontSize = fontSize,
                        onTextFieldValueChange = {
                            editorTextFieldValue = it
                            viewModel.updateRawText(it.text)
                        },
                        onPaste = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val item = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                            if (!item.isNullOrBlank()) {
                                viewModel.pasteText(item)
                                Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onClear = { viewModel.clearText() },
                        onOpenSamples = { showSamplesSheet = true },
                        onOpenFile = { filePickerLauncher.launch("*/*") },
                        onFormatNow = { viewModel.setViewMode(ViewMode.FORMATTED) },
                        onSanitize = { viewModel.sanitizeText() },
                        onFormatTables = { viewModel.formatAllTables() },
                        onReplaceAll = { find, rep, ic -> viewModel.replaceAll(find, rep, ic) }
                    )
                }

                ViewMode.SPLIT -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Half: Quick Raw Editor
                        Box(
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxWidth()
                                .background(themeColors.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            RawEditorView(
                                textFieldValue = editorTextFieldValue,
                                themeColors = themeColors,
                                fontSize = fontSize,
                                isCompact = true,
                                onTextFieldValueChange = {
                                    editorTextFieldValue = it
                                    viewModel.updateRawText(it.text)
                                },
                                onPaste = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val item = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                    if (!item.isNullOrBlank()) viewModel.pasteText(item)
                                },
                                onClear = { viewModel.clearText() },
                                onOpenSamples = { showSamplesSheet = true },
                                onOpenFile = { filePickerLauncher.launch("*/*") },
                                onFormatNow = { viewModel.setViewMode(ViewMode.FORMATTED) },
                                onSanitize = { viewModel.sanitizeText() },
                                onFormatTables = { viewModel.formatAllTables() },
                                onReplaceAll = { find, rep, ic -> viewModel.replaceAll(find, rep, ic) }
                            )
                        }

                        Divider(color = themeColors.outline, thickness = 2.dp)

                        // Bottom Half: Live Formatted View
                        Box(
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxWidth()
                        ) {
                            MarkdownRenderer(
                                document = formattedDoc,
                                themeColors = themeColors,
                                fontSize = fontSize,
                                lineSpacing = lineSpacing,
                                fontFamily = fontFamily,
                                lazyListState = lazyListState,
                                searchQuery = searchQuery,
                                isInteractiveMode = isInteractiveEditMode,
                                onToggleTask = { lineIdx, isChecked ->
                                    viewModel.toggleTaskItem(lineIdx, isChecked)
                                },
                                onEditBlock = { block ->
                                    editingBlock = block
                                },
                                onEditTableCell = { tableBlock, rowIdx, colIdx ->
                                    val rawHeaders = tableBlock.headers.map { it.rawText }
                                    val rawRows = tableBlock.rows.map { row -> row.map { it.rawText } }
                                    val matrix = TableMatrix(
                                        headers = rawHeaders,
                                        alignments = tableBlock.alignments,
                                        rows = rawRows
                                    )
                                    editingTableMatrixState = Pair(tableBlock, matrix)
                                    editingTableCellCoords = Pair(rowIdx, colIdx)
                                }
                            )
                        }
                    }
                }
            }

            // Search Overlay at Top
            AnimatedVisibility(
                visible = showSearchOverlay,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Surface(
                    color = themeColors.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = themeColors.primary)
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = themeColors.onSurface,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(themeColors.primary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reader_search_input"),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search in formatted text...", color = themeColors.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 14.sp)
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = themeColors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Block Editor Sheet
    editingBlock?.let { block ->
        BlockEditorSheet(
            block = block,
            themeColors = themeColors,
            onSaveBlockText = { newMarkdown ->
                viewModel.updateBlockContent(block.lineStart, block.lineEnd, newMarkdown)
            },
            onDismiss = { editingBlock = null }
        )
    }

    // Modal Table Cell / Pro Sheet Editor
    val tablePair = editingTableMatrixState
    val coords = editingTableCellCoords
    if (tablePair != null && coords != null) {
        val (tableBlock, currentMatrix) = tablePair
        TableCellEditorSheet(
            matrix = currentMatrix,
            selectedRow = coords.first,
            selectedCol = coords.second,
            themeColors = themeColors,
            onSaveMatrix = { updatedMatrix ->
                val newMarkdown = updatedMatrix.toMarkdown()
                viewModel.updateBlockContent(tableBlock.lineStart, tableBlock.lineEnd, newMarkdown)
                editingTableMatrixState = Pair(tableBlock, updatedMatrix)
            },
            onDismiss = {
                editingTableMatrixState = null
                editingTableCellCoords = null
            }
        )
    }

    // Modal Bottom Sheets
    if (showThemeSheet) {
        ThemeAndStyleSheet(
            currentTheme = themePreset,
            currentFontSize = fontSize,
            currentLineSpacing = lineSpacing,
            currentFontFamily = fontFamily,
            onSelectTheme = { viewModel.setThemePreset(it) },
            onSelectFontSize = { viewModel.setFontSize(it) },
            onSelectLineSpacing = { viewModel.setLineSpacing(it) },
            onSelectFontFamily = { viewModel.setFontFamily(it) },
            onDismiss = { showThemeSheet = false }
        )
    }

    if (showTocSheet) {
        TableOfContentsSheet(
            headings = formattedDoc.headings,
            onSelectHeading = { heading ->
                showTocSheet = false
                coroutineScope.launch {
                    val targetBlockIdx = heading.blockIndex.coerceIn(0, (formattedDoc.blocks.size - 1).coerceAtLeast(0))
                    lazyListState.animateScrollToItem(targetBlockIdx)
                }
            },
            onDismiss = { showTocSheet = false }
        )
    }

    if (showHistorySheet) {
        HistoryBottomSheet(
            snippets = snippets,
            onSelectSnippet = { snippet ->
                viewModel.updateRawText(snippet.content)
                viewModel.setViewMode(ViewMode.FORMATTED)
            },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onDeleteSnippet = { viewModel.deleteSnippet(it) },
            onClearAll = { viewModel.clearAllSnippets() },
            onSaveCurrent = { title -> viewModel.saveCurrentSnippet(title) },
            currentText = rawText,
            onDismiss = { showHistorySheet = false }
        )
    }

    if (showSamplesSheet) {
        SamplePromptsSheet(
            onSelectSample = { sample ->
                viewModel.updateRawText(sample.rawMarkdown)
                viewModel.setViewMode(ViewMode.FORMATTED)
            },
            onDismiss = { showSamplesSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    themeColors: ReaderThemeColors,
    viewMode: ViewMode,
    headingCount: Int,
    fileName: String?,
    searchActive: Boolean,
    onViewModeChange: (ViewMode) -> Unit,
    onToggleSearch: () -> Unit,
    onOpenToc: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSamples: () -> Unit,
    onToggleReadingMode: () -> Unit,
    onOpenFile: () -> Unit
) {
    Surface(
        color = themeColors.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // App Brand & Subtitle / File Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = themeColors.primaryContainer,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.FormatAlignLeft,
                                contentDescription = null,
                                tint = themeColors.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Markdown Formatter",
                            color = themeColors.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = fileName ?: "Reader & Editor",
                            color = if (fileName != null) themeColors.primary else themeColors.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }

                // Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Open File
                    IconButton(onClick = onOpenFile, modifier = Modifier.size(36.dp).testTag("action_open_file")) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = "Open File",
                            tint = themeColors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Search
                    IconButton(onClick = onToggleSearch, modifier = Modifier.size(36.dp).testTag("action_search")) {
                        Icon(
                            imageVector = if (searchActive) Icons.Outlined.SearchOff else Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = if (searchActive) themeColors.primary else themeColors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Table of contents
                    BadgedBox(
                        badge = {
                            if (headingCount > 0) {
                                Badge(
                                    containerColor = themeColors.primary,
                                    contentColor = themeColors.background,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text("$headingCount", fontSize = 9.sp)
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = onOpenToc, modifier = Modifier.size(36.dp).testTag("action_toc")) {
                            Icon(Icons.Outlined.MenuBook, contentDescription = "Outline", tint = themeColors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Reading Mode
                    IconButton(onClick = onToggleReadingMode, modifier = Modifier.size(36.dp).testTag("action_reading_mode")) {
                        Icon(Icons.Outlined.Fullscreen, contentDescription = "Focus Mode", tint = themeColors.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    }

                    // Appearance & Theme
                    IconButton(onClick = onOpenTheme, modifier = Modifier.size(36.dp).testTag("action_appearance")) {
                        Icon(Icons.Outlined.Palette, contentDescription = "Theme", tint = themeColors.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Mode Selector Bar (Formatted vs Editor vs Split)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        onClick = { onViewModeChange(ViewMode.FORMATTED) },
                        selected = viewMode == ViewMode.FORMATTED,
                        icon = { }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                            Text("Formatted", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        onClick = { onViewModeChange(ViewMode.RAW_EDITOR) },
                        selected = viewMode == ViewMode.RAW_EDITOR,
                        icon = { }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text("Raw Input", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        onClick = { onViewModeChange(ViewMode.SPLIT) },
                        selected = viewMode == ViewMode.SPLIT,
                        icon = { }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.VerticalSplit, contentDescription = null, modifier = Modifier.size(13.dp))
                            Text("Split", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Quick Samples button
                FilledTonalIconButton(
                    onClick = onOpenSamples,
                    modifier = Modifier.size(36.dp).testTag("action_samples"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = themeColors.surfaceVariant,
                        contentColor = themeColors.primary
                    )
                ) {
                    Icon(Icons.Outlined.Lightbulb, contentDescription = "Samples", modifier = Modifier.size(18.dp))
                }

                // History Button
                FilledTonalIconButton(
                    onClick = onOpenHistory,
                    modifier = Modifier.size(36.dp).testTag("action_history"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = themeColors.surfaceVariant,
                        contentColor = themeColors.primary
                    )
                ) {
                    Icon(Icons.Outlined.Bookmarks, contentDescription = "History", modifier = Modifier.size(18.dp))
                }
            }

            Divider(color = themeColors.outline.copy(alpha = 0.3f), thickness = 0.8.dp)
        }
    }
}

@Composable
private fun ReadingModeTopBar(
    themeColors: ReaderThemeColors,
    readingTimeMin: Int,
    wordCount: Int,
    onOpenToc: () -> Unit,
    onOpenTheme: () -> Unit,
    onExitReadingMode: () -> Unit
) {
    Surface(
        color = themeColors.surface.copy(alpha = 0.95f),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onExitReadingMode, modifier = Modifier.size(36.dp).testTag("exit_reading_mode")) {
                    Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Reading Mode", tint = themeColors.primary)
                }
                Text(
                    text = "Reading Mode",
                    color = themeColors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "• ~$readingTimeMin min read ($wordCount words)",
                    color = themeColors.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onOpenToc, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MenuBook, contentDescription = "Outline", tint = themeColors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onOpenTheme, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Palette, contentDescription = "Theme", tint = themeColors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ReaderBottomMetricsBar(
    themeColors: ReaderThemeColors,
    doc: FormattedDocument,
    isInteractiveMode: Boolean,
    onToggleInteractiveMode: () -> Unit,
    onCopyPlain: () -> Unit,
    onShare: () -> Unit,
    onSaveSnippet: () -> Unit
) {
    Surface(
        color = themeColors.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${doc.wordCount} words",
                    fontSize = 12.sp,
                    color = themeColors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "•",
                    fontSize = 12.sp,
                    color = themeColors.outline
                )
                Text(
                    text = "${doc.charCount} chars",
                    fontSize = 12.sp,
                    color = themeColors.onSurfaceVariant
                )
                Text(
                    text = "•",
                    fontSize = 12.sp,
                    color = themeColors.outline
                )
                Text(
                    text = "~${doc.readingTimeMinutes} min",
                    fontSize = 12.sp,
                    color = themeColors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Interactive Edit Mode Toggle Button beside Copy
                IconButton(
                    onClick = onToggleInteractiveMode,
                    modifier = Modifier.size(36.dp).testTag("bottom_edit_mode_btn")
                ) {
                    Icon(
                        imageVector = if (isInteractiveMode) Icons.Default.Edit else Icons.Outlined.Edit,
                        contentDescription = "Interactive Edit Formatted",
                        tint = if (isInteractiveMode) themeColors.primary else themeColors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Copy Plain Text
                IconButton(onClick = onCopyPlain, modifier = Modifier.size(36.dp).testTag("bottom_copy_btn")) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy Text", tint = themeColors.onSurfaceVariant, modifier = Modifier.size(19.dp))
                }

                // Share
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp).testTag("bottom_share_btn")) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share", tint = themeColors.onSurfaceVariant, modifier = Modifier.size(19.dp))
                }

                // Save Snippet
                IconButton(onClick = onSaveSnippet, modifier = Modifier.size(36.dp).testTag("bottom_bookmark_btn")) {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Save", tint = themeColors.primary, modifier = Modifier.size(19.dp))
                }
            }
        }
    }
}

@Composable
private fun RawEditorView(
    textFieldValue: TextFieldValue,
    themeColors: ReaderThemeColors,
    fontSize: FontSizePreference,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    onOpenSamples: () -> Unit,
    onOpenFile: () -> Unit,
    onFormatNow: () -> Unit,
    onSanitize: () -> Unit,
    onFormatTables: () -> Unit,
    onReplaceAll: (String, String, Boolean) -> Unit,
    isCompact: Boolean = false
) {
    val context = LocalContext.current
    var showFindReplace by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var ignoreCase by remember { mutableStateOf(true) }
    var currentMatchIndex by remember { mutableStateOf(0) }

    val rawText = textFieldValue.text

    // Compute matches
    val matchRanges = remember(rawText, findQuery, ignoreCase) {
        if (findQuery.isEmpty()) {
            emptyList()
        } else {
            val regex = Regex(Regex.escape(findQuery), if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet())
            regex.findAll(rawText).map { it.range }.toList()
        }
    }

    LaunchedEffect(matchRanges.size) {
        if (currentMatchIndex >= matchRanges.size) {
            currentMatchIndex = 0
        }
    }

    fun applyFormattingToSelection(prefix: String, suffix: String) {
        val selection = textFieldValue.selection
        val start = selection.min
        val end = selection.max

        val newText: String
        val newSelection: TextRange

        if (start != end) {
            val selectedText = rawText.substring(start, end)
            newText = rawText.substring(0, start) + prefix + selectedText + suffix + rawText.substring(end)
            newSelection = TextRange(start + prefix.length, end + prefix.length)
        } else {
            // No selection: insert at cursor
            newText = rawText.substring(0, start) + prefix + suffix + rawText.substring(start)
            newSelection = TextRange(start + prefix.length)
        }

        onTextFieldValueChange(
            textFieldValue.copy(
                text = newText,
                selection = newSelection
            )
        )
    }

    fun navigateToMatch(index: Int) {
        if (index in matchRanges.indices) {
            currentMatchIndex = index
            val range = matchRanges[index]
            onTextFieldValueChange(
                textFieldValue.copy(
                    selection = TextRange(range.first, range.last + 1)
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isCompact) 8.dp else 14.dp)
    ) {
        // Quick Action Bar for Editor
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                    onClick = onPaste,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp).testTag("editor_paste_btn")
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paste", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onOpenFile,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp).testTag("editor_open_file_btn")
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open File", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenSamples,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp).testTag("editor_samples_btn")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Samples", fontSize = 12.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Find & Replace Toggle
                IconButton(
                    onClick = { showFindReplace = !showFindReplace },
                    modifier = Modifier.size(32.dp).testTag("editor_toggle_find_replace")
                ) {
                    Icon(
                        imageVector = Icons.Default.FindReplace,
                        contentDescription = "Find & Replace",
                        tint = if (showFindReplace) themeColors.primary else themeColors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (rawText.isNotBlank()) {
                    IconButton(onClick = onClear, modifier = Modifier.size(32.dp).testTag("editor_clear_btn")) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }

                    if (!isCompact) {
                        Button(
                            onClick = onFormatNow,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("editor_format_now_btn")
                        ) {
                            Text("Format View", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Find & Replace Expandable Panel
        AnimatedVisibility(
            visible = showFindReplace,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            FindAndReplaceBar(
                themeColors = themeColors,
                findQuery = findQuery,
                replaceQuery = replaceQuery,
                matchCount = matchRanges.size,
                currentMatchIndex = currentMatchIndex,
                ignoreCase = ignoreCase,
                onFindQueryChange = {
                    findQuery = it
                    currentMatchIndex = 0
                },
                onReplaceQueryChange = { replaceQuery = it },
                onToggleIgnoreCase = { ignoreCase = !ignoreCase },
                onNextMatch = {
                    if (matchRanges.isNotEmpty()) {
                        val nextIdx = (currentMatchIndex + 1) % matchRanges.size
                        navigateToMatch(nextIdx)
                    }
                },
                onPrevMatch = {
                    if (matchRanges.isNotEmpty()) {
                        val prevIdx = if (currentMatchIndex - 1 < 0) matchRanges.size - 1 else currentMatchIndex - 1
                        navigateToMatch(prevIdx)
                    }
                },
                onReplaceSingle = {
                    if (matchRanges.isNotEmpty() && currentMatchIndex in matchRanges.indices) {
                        val range = matchRanges[currentMatchIndex]
                        val updated = rawText.substring(0, range.first) + replaceQuery + rawText.substring(range.last + 1)
                        onTextFieldValueChange(
                            TextFieldValue(
                                text = updated,
                                selection = TextRange(range.first + replaceQuery.length)
                            )
                        )
                        Toast.makeText(context, "Replaced 1 match", Toast.LENGTH_SHORT).show()
                    }
                },
                onReplaceAll = {
                    if (findQuery.isNotEmpty()) {
                        onReplaceAll(findQuery, replaceQuery, ignoreCase)
                        Toast.makeText(context, "Replaced all matches", Toast.LENGTH_SHORT).show()
                    }
                },
                onClose = { showFindReplace = false },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Markdown Syntax & Auto-Fix Tool Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Syntax Chips
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val helpers = listOf(
                    "#" to ("# " to ""),
                    "##" to ("## " to ""),
                    "**B**" to ("**" to "**"),
                    "*I*" to ("*" to "*"),
                    "`code`" to ("`" to "`"),
                    "~~S~~" to ("~~" to "~~"),
                    "Link" to ("[" to "](url)"),
                    "```" to ("```kotlin\n" to "\n```"),
                    "Quote" to ("> " to ""),
                    "Table" to ("\n| Header 1 | Header 2 |\n| :--- | :--- |\n| Value 1 | Value 2 |\n" to ""),
                    "Task" to ("- [ ] " to "")
                )

                helpers.take(if (isCompact) 5 else 8).forEach { (label, formatting) ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = themeColors.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                applyFormattingToSelection(formatting.first, formatting.second)
                            }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = themeColors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Smart Auto-Fix Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Table Column Auto-Padding Formatter
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = themeColors.secondary.copy(alpha = 0.15f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            onFormatTables()
                            Toast.makeText(context, "ASCII Table columns aligned", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = themeColors.secondary, modifier = Modifier.size(12.dp))
                        Text(
                            text = "Align Tables",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.secondary
                        )
                    }
                }

                // AI / Web Paste Sanitizer
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = themeColors.primary.copy(alpha = 0.15f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            onSanitize()
                            Toast.makeText(context, "Sanitized entities & repaired delimiters", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = themeColors.primary, modifier = Modifier.size(12.dp))
                        Text(
                            text = "Auto-Fix",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.primary
                        )
                    }
                }
            }
        }

        // Text Area with selection toolbar support
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, themeColors.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            color = themeColors.surface
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = onTextFieldValueChange,
                textStyle = TextStyle(
                    color = themeColors.onSurface,
                    fontSize = fontSize.bodySp.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = (fontSize.bodySp * 1.4f).sp
                ),
                cursorBrush = SolidColor(themeColors.primary),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .testTag("raw_markdown_text_field"),
                decorationBox = { innerTextField ->
                    if (rawText.isEmpty()) {
                        Text(
                            text = "Paste or type raw AI response or Markdown text here...",
                            color = themeColors.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = fontSize.bodySp.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}
