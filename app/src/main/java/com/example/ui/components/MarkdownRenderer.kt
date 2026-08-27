package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun MarkdownRenderer(
    document: FormattedDocument,
    themeColors: ReaderThemeColors,
    fontSize: FontSizePreference,
    lineSpacing: LineSpacingPreference,
    fontFamily: FontFamilyPreference,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    isInteractiveMode: Boolean = false,
    onToggleTask: ((Int, Boolean) -> Unit)? = null,
    onEditBlock: ((MarkdownBlock) -> Unit)? = null,
    onEditTableCell: ((block: MarkdownBlock.TableBlock, rowIdx: Int, colIdx: Int) -> Unit)? = null
) {
    if (document.blocks.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "No content to display",
                    color = themeColors.onSurfaceVariant,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Paste text, share from another app, or open a file to format.",
                    color = themeColors.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .testTag("markdown_rendered_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Optional hint banner when in Interactive Edit mode
        if (isInteractiveMode) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = themeColors.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = themeColors.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Interactive Formatted Mode: Tap any block or table cell to edit inline.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = themeColors.onPrimaryContainer
                        )
                    }
                }
            }
        }

        itemsIndexed(
            items = document.blocks,
            key = { _, block -> block.blockId },
            contentType = { _, block -> block::class.java.simpleName }
        ) { index, block ->
            val blockModifier = if (isInteractiveMode && block !is MarkdownBlock.DividerBlock && block !is MarkdownBlock.TableBlock) {
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeColors.primary.copy(alpha = 0.035f))
                    .border(0.8.dp, themeColors.primary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .clickable { onEditBlock?.invoke(block) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            } else {
                Modifier.fillMaxWidth()
            }

            Box(modifier = blockModifier) {
                when (block) {
                    is MarkdownBlock.HeaderBlock -> {
                        RenderHeader(
                            header = block,
                            themeColors = themeColors,
                            fontSize = fontSize,
                            fontFamily = fontFamily
                        )
                    }

                    is MarkdownBlock.ParagraphBlock -> {
                        RenderInlineSpans(
                            spanGroup = block.content,
                            themeColors = themeColors,
                            fontSize = fontSize,
                            lineSpacing = lineSpacing,
                            fontFamily = fontFamily,
                            searchQuery = searchQuery,
                            onSpanClick = if (isInteractiveMode) { { onEditBlock?.invoke(block) } } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    is MarkdownBlock.CodeBlock -> {
                        CodeBlockView(
                            code = block.code,
                            language = block.language,
                            themeColors = themeColors,
                            fontSize = fontSize
                        )
                    }

                    is MarkdownBlock.CalloutBlock -> {
                        CalloutView(
                            type = block.type,
                            title = block.title,
                            content = block.content,
                            themeColors = themeColors,
                            fontSize = fontSize,
                            lineSpacing = lineSpacing,
                            fontFamily = fontFamily,
                            searchQuery = searchQuery,
                            onSpanClick = if (isInteractiveMode) { { onEditBlock?.invoke(block) } } else null
                        )
                    }

                    is MarkdownBlock.BulletListBlock -> {
                        RenderBulletList(
                            bulletList = block,
                            themeColors = themeColors,
                            fontSize = fontSize,
                            lineSpacing = lineSpacing,
                            fontFamily = fontFamily,
                            searchQuery = searchQuery,
                            onSpanClick = if (isInteractiveMode) { { onEditBlock?.invoke(block) } } else null
                        )
                    }

                    is MarkdownBlock.NumberedListBlock -> {
                        RenderNumberedList(
                            numberedList = block,
                            themeColors = themeColors,
                            fontSize = fontSize,
                            lineSpacing = lineSpacing,
                            fontFamily = fontFamily,
                            searchQuery = searchQuery,
                            onSpanClick = if (isInteractiveMode) { { onEditBlock?.invoke(block) } } else null
                        )
                    }

                    is MarkdownBlock.TaskListBlock -> {
                        RenderTaskList(
                            taskList = block,
                            themeColors = themeColors,
                            fontSize = fontSize,
                            lineSpacing = lineSpacing,
                            fontFamily = fontFamily,
                            searchQuery = searchQuery,
                            onToggle = { lineIdx, isChecked ->
                                onToggleTask?.invoke(lineIdx, isChecked)
                            },
                            onSpanClick = if (isInteractiveMode) { { onEditBlock?.invoke(block) } } else null
                        )
                    }

                    is MarkdownBlock.TableBlock -> {
                        TableView(
                            headers = block.headers,
                            alignments = block.alignments,
                            rows = block.rows,
                            themeColors = themeColors,
                            fontSize = fontSize,
                            lineSpacing = lineSpacing,
                            fontFamily = fontFamily,
                            searchQuery = searchQuery,
                            isInteractiveMode = isInteractiveMode,
                            onCellClick = { rowIdx, colIdx ->
                                onEditTableCell?.invoke(block, rowIdx, colIdx)
                            }
                        )
                    }

                    is MarkdownBlock.DividerBlock -> {
                        Divider(
                            color = themeColors.divider,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    is MarkdownBlock.MathBlock -> {
                        RenderMathBlock(
                            math = block,
                            themeColors = themeColors,
                            fontSize = fontSize
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderHeader(
    header: MarkdownBlock.HeaderBlock,
    themeColors: ReaderThemeColors,
    fontSize: FontSizePreference,
    fontFamily: FontFamilyPreference
) {
    val (headerSp, weight) = when (header.level) {
        1 -> Pair(fontSize.h1Sp.sp, FontWeight.ExtraBold)
        2 -> Pair(fontSize.h2Sp.sp, FontWeight.Bold)
        3 -> Pair(fontSize.h3Sp.sp, FontWeight.SemiBold)
        4 -> Pair((fontSize.h3Sp * 0.9f).sp, FontWeight.SemiBold)
        5 -> Pair((fontSize.h3Sp * 0.8f).sp, FontWeight.Medium)
        else -> Pair(fontSize.bodySp.sp, FontWeight.Medium)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (header.level <= 2) 12.dp else 6.dp, bottom = 4.dp)
            .testTag("header_h${header.level}_${header.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (header.level == 1) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(themeColors.primary)
                )
            } else if (header.level == 2) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(themeColors.secondary)
                )
            }

            Text(
                text = header.text,
                color = themeColors.onBackground,
                fontSize = headerSp,
                fontWeight = weight,
                fontFamily = fontFamily.composeFontFamily,
                lineHeight = (headerSp.value * 1.3f).sp
            )
        }

        if (header.level <= 2) {
            Divider(
                color = themeColors.outline.copy(alpha = 0.3f),
                thickness = 0.8.dp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun RenderBulletList(
    bulletList: MarkdownBlock.BulletListBlock,
    themeColors: ReaderThemeColors,
    fontSize: FontSizePreference,
    lineSpacing: LineSpacingPreference,
    fontFamily: FontFamilyPreference,
    searchQuery: String,
    onSpanClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        bulletList.items.forEach { item ->
            val indentDp = (item.level * 16).dp
            val bulletGlyph = when (item.level) {
                0 -> "•"
                1 -> "◦"
                else -> "▪"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = indentDp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = bulletGlyph,
                    color = themeColors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize.bodySp.sp,
                    modifier = Modifier.padding(top = 1.dp)
                )

                RenderInlineSpans(
                    spanGroup = item.content,
                    themeColors = themeColors,
                    fontSize = fontSize,
                    lineSpacing = lineSpacing,
                    fontFamily = fontFamily,
                    searchQuery = searchQuery,
                    onSpanClick = onSpanClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RenderNumberedList(
    numberedList: MarkdownBlock.NumberedListBlock,
    themeColors: ReaderThemeColors,
    fontSize: FontSizePreference,
    lineSpacing: LineSpacingPreference,
    fontFamily: FontFamilyPreference,
    searchQuery: String,
    onSpanClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        numberedList.items.forEach { item ->
            val indentDp = (item.level * 16).dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = indentDp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${item.number}.",
                    color = themeColors.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = fontSize.bodySp.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 1.dp)
                )

                RenderInlineSpans(
                    spanGroup = item.content,
                    themeColors = themeColors,
                    fontSize = fontSize,
                    lineSpacing = lineSpacing,
                    fontFamily = fontFamily,
                    searchQuery = searchQuery,
                    onSpanClick = onSpanClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RenderTaskList(
    taskList: MarkdownBlock.TaskListBlock,
    themeColors: ReaderThemeColors,
    fontSize: FontSizePreference,
    lineSpacing: LineSpacingPreference,
    fontFamily: FontFamilyPreference,
    searchQuery: String,
    onToggle: (Int, Boolean) -> Unit,
    onSpanClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        taskList.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onToggle(item.lineIndex, !item.isChecked) }
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { onToggle(item.lineIndex, !item.isChecked) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = if (item.isChecked) "Completed" else "Not completed",
                        tint = if (item.isChecked) themeColors.secondary else themeColors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                RenderInlineSpans(
                    spanGroup = item.content,
                    themeColors = themeColors,
                    fontSize = fontSize,
                    lineSpacing = lineSpacing,
                    fontFamily = fontFamily,
                    searchQuery = searchQuery,
                    onSpanClick = onSpanClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RenderMathBlock(
    math: MarkdownBlock.MathBlock,
    themeColors: ReaderThemeColors,
    fontSize: FontSizePreference
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, themeColors.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        color = themeColors.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Functions,
                contentDescription = "Math expression",
                tint = themeColors.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = math.latex,
                color = themeColors.onSurface,
                fontSize = (fontSize.bodySp * 1.05f).sp,
                fontFamily = FontFamily.Monospace,
                fontStyle = FontStyle.Italic
            )
        }
    }
}
