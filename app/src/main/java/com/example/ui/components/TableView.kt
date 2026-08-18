package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.*

@Composable
fun TableView(
    headers: List<InlineSpanGroup>,
    alignments: List<TableAlignment>,
    rows: List<List<InlineSpanGroup>>,
    themeColors: ReaderThemeColors,
    fontSize: FontSizePreference,
    lineSpacing: LineSpacingPreference,
    fontFamily: FontFamilyPreference,
    modifier: Modifier = Modifier,
    searchQuery: String = ""
) {
    val numCols = remember(headers, rows) {
        maxOf(headers.size, rows.maxOfOrNull { it.size } ?: 0)
    }
    if (numCols == 0) return

    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, themeColors.tableBorder, RoundedCornerShape(10.dp))
            .testTag("table_view_card")
    ) {
        val availableWidth = maxWidth

        // Calculate natural column widths based on content length
        val baseWidths = remember(headers, rows, fontSize) {
            (0 until numCols).map { colIdx ->
                val headerText = headers.getOrNull(colIdx)?.rawText ?: ""
                val rowTexts = rows.map { it.getOrNull(colIdx)?.rawText ?: "" }
                val allTexts = listOf(headerText) + rowTexts
                val maxLen = allTexts.maxOfOrNull { it.length } ?: 8

                when {
                    maxLen <= 6 -> 85.dp
                    maxLen <= 12 -> 110.dp
                    maxLen <= 22 -> 155.dp
                    maxLen <= 38 -> 210.dp
                    maxLen <= 65 -> 280.dp
                    else -> 350.dp
                }
            }
        }

        val sumBaseWidth = baseWidths.fold(0.dp) { acc, dp -> acc + dp }

        // If screen is wider than table, distribute extra width proportionally
        val effectiveWidths = remember(baseWidths, availableWidth, sumBaseWidth) {
            if (availableWidth > sumBaseWidth && sumBaseWidth > 0.dp) {
                val extra = availableWidth - sumBaseWidth
                baseWidths.map { w ->
                    val ratio = (w.value / sumBaseWidth.value).coerceIn(0.1f, 0.7f)
                    w + (extra * ratio)
                }
            } else {
                baseWidths
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeColors.surface)
                .horizontalScroll(scrollState)
        ) {
            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(themeColors.tableHeaderBg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (colIdx in 0 until numCols) {
                        val headerGroup = headers.getOrNull(colIdx) ?: InlineSpanGroup(emptyList(), "")
                        val align = alignments.getOrNull(colIdx) ?: TableAlignment.LEFT
                        val textAlign = when (align) {
                            TableAlignment.CENTER -> TextAlign.Center
                            TableAlignment.RIGHT -> TextAlign.End
                            TableAlignment.LEFT -> TextAlign.Start
                        }
                        val boxAlignment = when (align) {
                            TableAlignment.CENTER -> Alignment.Center
                            TableAlignment.RIGHT -> Alignment.CenterEnd
                            TableAlignment.LEFT -> Alignment.CenterStart
                        }
                        val colWidth = effectiveWidths.getOrElse(colIdx) { 120.dp }

                        Box(
                            modifier = Modifier
                                .width(colWidth)
                                .fillMaxHeight()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = boxAlignment
                        ) {
                            RenderInlineSpans(
                                spanGroup = headerGroup,
                                themeColors = themeColors,
                                fontSize = fontSize,
                                lineSpacing = lineSpacing,
                                fontFamily = fontFamily,
                                isHeader = true,
                                textAlign = textAlign,
                                searchQuery = searchQuery
                            )
                        }

                        // Column vertical divider
                        if (colIdx < numCols - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .background(themeColors.tableBorder.copy(alpha = 0.5f))
                            )
                        }
                    }
                }

                Divider(color = themeColors.tableBorder, thickness = 1.dp)

                // Body Rows
                rows.forEachIndexed { rowIdx, rowCells ->
                    val isZebra = rowIdx % 2 == 1
                    val rowBg = if (isZebra) themeColors.tableZebraBg else themeColors.surface

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(rowBg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (colIdx in 0 until numCols) {
                            val cellGroup = rowCells.getOrNull(colIdx) ?: InlineSpanGroup(emptyList(), "")
                            val align = alignments.getOrNull(colIdx) ?: TableAlignment.LEFT
                            val textAlign = when (align) {
                                TableAlignment.CENTER -> TextAlign.Center
                                TableAlignment.RIGHT -> TextAlign.End
                                TableAlignment.LEFT -> TextAlign.Start
                            }
                            val boxAlignment = when (align) {
                                TableAlignment.CENTER -> Alignment.Center
                                TableAlignment.RIGHT -> Alignment.CenterEnd
                                TableAlignment.LEFT -> Alignment.CenterStart
                            }
                            val colWidth = effectiveWidths.getOrElse(colIdx) { 120.dp }

                            Box(
                                modifier = Modifier
                                    .width(colWidth)
                                    .fillMaxHeight()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = boxAlignment
                            ) {
                                RenderInlineSpans(
                                    spanGroup = cellGroup,
                                    themeColors = themeColors,
                                    fontSize = fontSize,
                                    lineSpacing = lineSpacing,
                                    fontFamily = fontFamily,
                                    isHeader = false,
                                    textAlign = textAlign,
                                    searchQuery = searchQuery
                                )
                            }

                            // Column vertical divider
                            if (colIdx < numCols - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(1.dp)
                                        .background(themeColors.tableBorder.copy(alpha = 0.35f))
                                )
                            }
                        }
                    }

                    if (rowIdx < rows.size - 1) {
                        Divider(color = themeColors.tableBorder.copy(alpha = 0.35f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
