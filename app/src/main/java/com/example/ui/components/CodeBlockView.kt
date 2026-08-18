package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FontSizePreference
import com.example.model.ReaderThemeColors
import com.example.syntax.SyntaxHighlighter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CodeBlockView(
    code: String,
    language: String,
    themeColors: ReaderThemeColors,
    fontSize: FontSizePreference,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }
    var isWrapped by remember { mutableStateOf(false) }
    var showLineNumbers by remember { mutableStateOf(true) }

    val displayLang = if (language.isBlank()) "CODE" else language.uppercase()
    val highlightedCode = remember(code, language, themeColors.syntax) {
        SyntaxHighlighter.highlight(code, language, themeColors.syntax)
    }

    val lines = remember(code) { code.lines() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, themeColors.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .testTag("code_block_card"),
        color = themeColors.syntax.background
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeColors.surface.copy(alpha = 0.8f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Code",
                        tint = themeColors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = displayLang,
                        color = themeColors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "• ${lines.size} lines",
                        color = themeColors.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Line wrap toggle
                    IconButton(
                        onClick = { isWrapped = !isWrapped },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("toggle_wrap_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.WrapText,
                            contentDescription = if (isWrapped) "Horizontal scroll" else "Wrap lines",
                            tint = if (isWrapped) themeColors.primary else themeColors.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Copy button
                    FilledTonalButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Code snippet", code)
                            clipboard.setPrimaryClip(clip)
                            isCopied = true
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                            coroutineScope.launch {
                                delay(2000)
                                isCopied = false
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("copy_code_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isCopied) themeColors.secondary.copy(alpha = 0.2f) else themeColors.surfaceVariant,
                            contentColor = if (isCopied) themeColors.secondary else themeColors.onSurface
                        )
                    ) {
                        AnimatedContent(
                            targetState = isCopied,
                            label = "copy_anim"
                        ) { copied ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = if (copied) "Copied" else "Copy",
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (copied) "Copied" else "Copy",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = themeColors.outline.copy(alpha = 0.2f), thickness = 0.8.dp)

            // Code Content Area
            val codeScrollState = rememberScrollState()
            val contentModifier = if (isWrapped) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.horizontalScroll(codeScrollState)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                // Line Numbers Gutter
                if (showLineNumbers && lines.size > 1) {
                    Column(
                        modifier = Modifier
                            .padding(start = 10.dp, end = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        lines.indices.forEach { idx ->
                            Text(
                                text = "${idx + 1}",
                                color = themeColors.syntax.lineNumber,
                                fontSize = fontSize.codeSp.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = (fontSize.codeSp * 1.45f).sp
                            )
                        }
                    }

                    // Vertical gutter divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height((lines.size * fontSize.codeSp * 1.45f).dp)
                            .background(themeColors.outline.copy(alpha = 0.15f))
                    )
                }

                // Actual Code
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = highlightedCode,
                        modifier = contentModifier.testTag("code_text_view"),
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize.codeSp.sp,
                        lineHeight = (fontSize.codeSp * 1.45f).sp
                    )
                }
            }
        }
    }
}
