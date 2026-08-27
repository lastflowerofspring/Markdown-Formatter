package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReaderThemeColors

@Composable
fun FindAndReplaceBar(
    themeColors: ReaderThemeColors,
    findQuery: String,
    replaceQuery: String,
    matchCount: Int,
    currentMatchIndex: Int,
    ignoreCase: Boolean,
    onFindQueryChange: (String) -> Unit,
    onReplaceQueryChange: (String) -> Unit,
    onToggleIgnoreCase: () -> Unit,
    onNextMatch: () -> Unit,
    onPrevMatch: () -> Unit,
    onReplaceSingle: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showReplaceRow by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("find_and_replace_bar"),
        color = themeColors.surface,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Find Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = themeColors.primary,
                    modifier = Modifier.size(18.dp)
                )

                // Search Input Field
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, themeColors.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    color = themeColors.background
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = findQuery,
                            onValueChange = onFindQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = themeColors.onSurface,
                                fontSize = 13.sp
                            ),
                            cursorBrush = SolidColor(themeColors.primary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("find_query_input"),
                            decorationBox = { innerTextField ->
                                if (findQuery.isEmpty()) {
                                    Text(
                                        "Find text...",
                                        color = themeColors.onSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        if (findQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onFindQueryChange("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = themeColors.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Match count badge
                if (findQuery.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (matchCount > 0) themeColors.primary.copy(alpha = 0.15f) else themeColors.surfaceVariant
                    ) {
                        Text(
                            text = if (matchCount > 0) "${currentMatchIndex + 1}/$matchCount" else "0/0",
                            color = if (matchCount > 0) themeColors.primary else themeColors.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Prev Match
                IconButton(
                    onClick = onPrevMatch,
                    enabled = matchCount > 0,
                    modifier = Modifier.size(32.dp).testTag("prev_match_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Match",
                        tint = if (matchCount > 0) themeColors.onSurface else themeColors.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Next Match
                IconButton(
                    onClick = onNextMatch,
                    enabled = matchCount > 0,
                    modifier = Modifier.size(32.dp).testTag("next_match_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Match",
                        tint = if (matchCount > 0) themeColors.onSurface else themeColors.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Toggle Case Sensitivity
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (!ignoreCase) themeColors.primary.copy(alpha = 0.2f) else themeColors.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onToggleIgnoreCase() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Aa",
                        color = if (!ignoreCase) themeColors.primary else themeColors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Close Button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp).testTag("close_find_replace_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Find & Replace",
                        tint = themeColors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Replace Row
            AnimatedVisibility(
                visible = showReplaceRow,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FindReplace,
                        contentDescription = null,
                        tint = themeColors.secondary,
                        modifier = Modifier.size(18.dp)
                    )

                    // Replace Input Field
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, themeColors.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        color = themeColors.background
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = replaceQuery,
                                onValueChange = onReplaceQueryChange,
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = themeColors.onSurface,
                                    fontSize = 13.sp
                                ),
                                cursorBrush = SolidColor(themeColors.secondary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("replace_query_input"),
                                decorationBox = { innerTextField ->
                                    if (replaceQuery.isEmpty()) {
                                        Text(
                                            "Replace with...",
                                            color = themeColors.onSurfaceVariant.copy(alpha = 0.5f),
                                            fontSize = 13.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            if (replaceQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onReplaceQueryChange("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear replace",
                                        tint = themeColors.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Single Replace
                    OutlinedButton(
                        onClick = onReplaceSingle,
                        enabled = matchCount > 0 && findQuery.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp).testTag("replace_single_btn")
                    ) {
                        Text("Replace", fontSize = 11.sp)
                    }

                    // Replace All
                    Button(
                        onClick = onReplaceAll,
                        enabled = matchCount > 0 && findQuery.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp).testTag("replace_all_btn")
                    ) {
                        Text("All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
