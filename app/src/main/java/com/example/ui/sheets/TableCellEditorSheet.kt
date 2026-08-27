package com.example.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReaderThemeColors
import com.example.model.TableAlignment
import com.example.model.TableMatrix

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableCellEditorSheet(
    matrix: TableMatrix,
    selectedRow: Int,
    selectedCol: Int,
    themeColors: ReaderThemeColors,
    onSaveMatrix: (TableMatrix) -> Unit,
    onDismiss: () -> Unit
) {
    var activeRow by remember { mutableIntStateOf(selectedRow) }
    var activeCol by remember { mutableIntStateOf(selectedCol) }
    var currentMatrix by remember { mutableStateOf(matrix) }

    var cellValue by remember(activeRow, activeCol, currentMatrix) {
        val text = currentMatrix.getCell(activeRow, activeCol)
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(activeRow, activeCol) {
        try {
            kotlinx.coroutines.delay(150)
            focusRequester.requestFocus()
        } catch (_: Exception) {
            // Ignore focus exception during composition/teardown
        }
    }

    fun updateCurrentCellAndCommit(newText: String) {
        val updated = currentMatrix.updateCell(activeRow, activeCol, newText)
        currentMatrix = updated
        onSaveMatrix(updated)
    }

    fun handleDismiss() {
        keyboardController?.hide()
        updateCurrentCellAndCommit(cellValue.text)
        onDismiss()
    }

    fun moveToNextCell() {
        updateCurrentCellAndCommit(cellValue.text)
        val numCols = currentMatrix.columnCount
        val numRows = currentMatrix.rowCount
        if (activeCol < numCols - 1) {
            activeCol++
        } else if (activeRow < numRows - 1) {
            activeRow++
            activeCol = 0
        }
    }

    fun moveToPrevCell() {
        updateCurrentCellAndCommit(cellValue.text)
        val numCols = currentMatrix.columnCount
        if (activeCol > 0) {
            activeCol--
        } else if (activeRow > -1) {
            activeRow--
            activeCol = numCols - 1
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            handleDismiss()
        },
        sheetState = sheetState,
        containerColor = themeColors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = themeColors.outline) },
        modifier = Modifier.testTag("table_cell_editor_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Info & Coordinate Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = themeColors.primaryContainer,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = if (activeRow == -1) "HEADER · COL ${activeCol + 1}" else "ROW ${activeRow + 1} · COL ${activeCol + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "Pro Sheet Editor",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Previous Cell
                    IconButton(
                        onClick = { moveToPrevCell() },
                        modifier = Modifier.size(32.dp).testTag("table_prev_cell_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Prev Cell", tint = themeColors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }

                    // Next Cell
                    IconButton(
                        onClick = { moveToNextCell() },
                        modifier = Modifier.size(32.dp).testTag("table_next_cell_btn")
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next Cell", tint = themeColors.primary, modifier = Modifier.size(18.dp))
                    }

                    // Done / Close
                    IconButton(
                        onClick = {
                            handleDismiss()
                        },
                        modifier = Modifier.size(32.dp).testTag("table_done_cell_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Done", tint = themeColors.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cell Input Field
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.5.dp, themeColors.primary, RoundedCornerShape(8.dp)),
                color = themeColors.surfaceVariant.copy(alpha = 0.4f)
            ) {
                BasicTextField(
                    value = cellValue,
                    onValueChange = {
                        cellValue = it
                        updateCurrentCellAndCommit(it.text)
                    },
                    textStyle = TextStyle(
                        color = themeColors.onSurface,
                        fontSize = 15.sp,
                        fontWeight = if (activeRow == -1) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = 22.sp
                    ),
                    cursorBrush = SolidColor(themeColors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { moveToNextCell() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .focusRequester(focusRequester)
                        .testTag("table_cell_text_input"),
                    decorationBox = { innerTextField ->
                        if (cellValue.text.isEmpty()) {
                            Text(
                                text = "Enter cell content...",
                                color = themeColors.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Table Structure Actions Toolbar (Add/Delete Rows & Columns)
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column Alignment Toggle
                FilledTonalButton(
                    onClick = {
                        val updated = currentMatrix.toggleAlignment(activeCol)
                        currentMatrix = updated
                        onSaveMatrix(updated)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    val align = currentMatrix.alignments.getOrElse(activeCol) { TableAlignment.LEFT }
                    val icon = when (align) {
                        TableAlignment.LEFT -> Icons.Default.FormatAlignLeft
                        TableAlignment.CENTER -> Icons.Default.FormatAlignCenter
                        TableAlignment.RIGHT -> Icons.Default.FormatAlignRight
                    }
                    Icon(icon, contentDescription = "Align", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Align", fontSize = 11.sp)
                }

                // Add Row Below
                OutlinedButton(
                    onClick = {
                        val updated = currentMatrix.addRow(if (activeRow == -1) 0 else activeRow)
                        currentMatrix = updated
                        onSaveMatrix(updated)
                        activeRow = if (activeRow == -1) 0 else activeRow + 1
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("+ Row", fontSize = 11.sp)
                }

                // Add Column Right
                OutlinedButton(
                    onClick = {
                        val updated = currentMatrix.addColumn(activeCol)
                        currentMatrix = updated
                        onSaveMatrix(updated)
                        activeCol = activeCol + 1
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("+ Col", fontSize = 11.sp)
                }

                // Delete Current Row (if body row)
                if (activeRow >= 0 && currentMatrix.rowCount > 1) {
                    OutlinedButton(
                        onClick = {
                            val targetRow = activeRow
                            val updated = currentMatrix.deleteRow(targetRow)
                            currentMatrix = updated
                            onSaveMatrix(updated)
                            activeRow = (targetRow - 1).coerceAtLeast(-1)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Del Row", fontSize = 11.sp)
                    }
                }

                // Delete Column
                if (currentMatrix.columnCount > 1) {
                    OutlinedButton(
                        onClick = {
                            val targetCol = activeCol
                            val updated = currentMatrix.deleteColumn(targetCol)
                            currentMatrix = updated
                            onSaveMatrix(updated)
                            activeCol = (targetCol - 1).coerceAtLeast(0)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Del Col", fontSize = 11.sp)
                    }
                }

                // Inline formatting helpers inside cell
                val cellHelpers = listOf(
                    "**B**" to Pair("**", "**"),
                    "*I*" to Pair("*", "*"),
                    "`code`" to Pair("`", "`"),
                    "br" to Pair("", "<br>")
                )

                cellHelpers.forEach { (label, wrapper) ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = themeColors.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                val cur = cellValue.text
                                val newText = if (wrapper.first.isEmpty()) {
                                    "$cur${wrapper.second}"
                                } else {
                                    "${wrapper.first}$cur${wrapper.second}"
                                }
                                cellValue = TextFieldValue(newText, TextRange(newText.length))
                                updateCurrentCellAndCommit(newText)
                            }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
