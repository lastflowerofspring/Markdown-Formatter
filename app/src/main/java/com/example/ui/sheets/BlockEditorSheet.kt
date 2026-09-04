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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MarkdownBlock
import com.example.model.ReaderThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockEditorSheet(
    block: MarkdownBlock,
    themeColors: ReaderThemeColors,
    onSaveBlockText: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialText = remember {
        when (block) {
            is MarkdownBlock.HeaderBlock -> block.text
            is MarkdownBlock.ParagraphBlock -> block.content.rawText
            is MarkdownBlock.CodeBlock -> block.code
            is MarkdownBlock.CalloutBlock -> block.content.rawText
            is MarkdownBlock.MathBlock -> block.latex
            is MarkdownBlock.BulletListBlock -> block.items.joinToString("\n") { "${"  ".repeat(it.level)}* ${it.content.rawText}" }
            is MarkdownBlock.NumberedListBlock -> block.items.joinToString("\n") { "${"  ".repeat(it.level)}${it.number}. ${it.content.rawText}" }
            is MarkdownBlock.TaskListBlock -> block.items.joinToString("\n") { "* ${if (it.isChecked) "[x]" else "[ ]"} ${it.content.rawText}" }
            else -> ""
        }
    }

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length)))
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        try {
            kotlinx.coroutines.delay(150)
            focusRequester.requestFocus()
        } catch (_: Exception) {
            // Ignore focus exception during composition/teardown
        }
    }

    fun commitChanges(newText: String) {
        val finalMarkdown = when (block) {
            is MarkdownBlock.HeaderBlock -> {
                if (block.isHtml) "<h${block.level}>$newText</h${block.level}>"
                else "${"#".repeat(block.level)} $newText"
            }
            is MarkdownBlock.ParagraphBlock -> {
                if (block.isHtml) "<p>$newText</p>"
                else newText
            }
            is MarkdownBlock.CodeBlock -> {
                if (block.isHtml) {
                    if (block.language.equals("css", ignoreCase = true)) {
                        "<style>\n$newText\n</style>"
                    } else {
                        val langAttr = if (block.language.isNotEmpty()) " class=\"language-${block.language}\"" else ""
                        "<pre><code$langAttr>\n$newText\n</code></pre>"
                    }
                } else {
                    val fence = "```${block.language}"
                    "$fence\n$newText\n```"
                }
            }
            is MarkdownBlock.CalloutBlock -> {
                if (block.isHtml) "<blockquote>\n$newText\n</blockquote>"
                else {
                    val header = if (block.title != null) "> [!${block.type.name}] ${block.title}" else "> [!${block.type.name}]"
                    val body = newText.lines().joinToString("\n") { "> $it" }
                    "$header\n$body"
                }
            }
            is MarkdownBlock.MathBlock -> {
                "$$\n$newText\n$$"
            }
            is MarkdownBlock.BulletListBlock -> {
                if (block.isHtml) {
                    val items = newText.lines().filter { it.isNotBlank() }.map { line ->
                        val clean = line.trimStart('-', '*', ' ', '\t')
                        "  <li>$clean</li>"
                    }
                    "<ul>\n${items.joinToString("\n")}\n</ul>"
                } else newText
            }
            is MarkdownBlock.NumberedListBlock -> {
                if (block.isHtml) {
                    val items = newText.lines().filter { it.isNotBlank() }.map { line ->
                        val clean = line.replaceFirst(Regex("^\\s*\\d+[.)]\\s*"), "")
                        "  <li>$clean</li>"
                    }
                    "<ol>\n${items.joinToString("\n")}\n</ol>"
                } else newText
            }
            is MarkdownBlock.TaskListBlock -> {
                if (block.isHtml) {
                    val items = newText.lines().filter { it.isNotBlank() }.map { line ->
                        val isChecked = line.contains("[x]", ignoreCase = true) || line.contains("checked", ignoreCase = true)
                        val clean = line.replace(Regex("\\[[ xX]\\]"), "").trimStart('-', '*', ' ', '\t')
                        val checkedAttr = if (isChecked) " checked" else ""
                        "  <li><input type=\"checkbox\"$checkedAttr> $clean</li>"
                    }
                    "<ul>\n${items.joinToString("\n")}\n</ul>"
                } else newText
            }
            else -> newText
        }
        onSaveBlockText(finalMarkdown)
    }

    val blockTypeLabel = when (block) {
        is MarkdownBlock.HeaderBlock -> if (block.isHtml) "HTML Heading ${block.level}" else "Heading ${block.level}"
        is MarkdownBlock.ParagraphBlock -> if (block.isHtml) "HTML Paragraph" else "Paragraph"
        is MarkdownBlock.CodeBlock -> {
            if (block.isHtml) {
                if (block.language.equals("css", ignoreCase = true)) "CSS Styles"
                else "HTML Code (${block.language.ifEmpty { "plain" }})"
            } else "Code (${block.language.ifEmpty { "plain" }})"
        }
        is MarkdownBlock.CalloutBlock -> if (block.isHtml) "HTML Blockquote" else "Callout (${block.type.name})"
        is MarkdownBlock.MathBlock -> "Math Formula"
        is MarkdownBlock.BulletListBlock -> if (block.isHtml) "HTML List" else "List Item"
        is MarkdownBlock.NumberedListBlock -> if (block.isHtml) "HTML Numbered List" else "Numbered Item"
        is MarkdownBlock.TaskListBlock -> if (block.isHtml) "HTML Task List" else "Task Item"
        else -> "Block"
    }

    // Debounced background sync while typing without interrupting IME composition
    LaunchedEffect(textFieldValue.text) {
        kotlinx.coroutines.delay(800)
        commitChanges(textFieldValue.text)
    }

    fun handleDismiss() {
        keyboardController?.hide()
        commitChanges(textFieldValue.text)
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = {
            handleDismiss()
        },
        sheetState = sheetState,
        containerColor = themeColors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = themeColors.outline) },
        modifier = Modifier.testTag("block_editor_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Bar
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
                        color = themeColors.primaryContainer
                    ) {
                        Text(
                            text = blockTypeLabel.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "Edit in Formatted View",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.onSurface
                    )
                }

                IconButton(
                    onClick = {
                        handleDismiss()
                    },
                    modifier = Modifier.size(34.dp).testTag("block_editor_done_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Done", tint = themeColors.primary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Editing Area
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 240.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, themeColors.primary, RoundedCornerShape(10.dp)),
                color = themeColors.surfaceVariant.copy(alpha = 0.35f)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = {
                        textFieldValue = it
                    },
                    textStyle = TextStyle(
                        color = themeColors.onSurface,
                        fontSize = 15.sp,
                        fontFamily = if (block is MarkdownBlock.CodeBlock || block is MarkdownBlock.MathBlock) FontFamily.Monospace else FontFamily.Default,
                        lineHeight = 22.sp
                    ),
                    cursorBrush = SolidColor(themeColors.primary),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                        .focusRequester(focusRequester)
                        .testTag("block_editor_text_input"),
                    decorationBox = { innerTextField ->
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = "Enter text...",
                                color = themeColors.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Formatting Toolbar
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val helpers = listOf(
                    "**B**" to Pair("**", "**"),
                    "*I*" to Pair("*", "*"),
                    "`code`" to Pair("`", "`"),
                    "~~S~~" to Pair("~~", "~~"),
                    "[Link]" to Pair("[", "](https://)")
                )

                helpers.forEach { (label, wrapper) ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = themeColors.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val current = textFieldValue.text
                                val start = textFieldValue.selection.min
                                val end = textFieldValue.selection.max
                                val newText = if (start != end) {
                                    val selected = current.substring(start, end)
                                    current.substring(0, start) + wrapper.first + selected + wrapper.second + current.substring(end)
                                } else {
                                    current + wrapper.first + wrapper.second
                                }
                                textFieldValue = TextFieldValue(newText, TextRange(newText.length))
                                commitChanges(newText)
                            }
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
