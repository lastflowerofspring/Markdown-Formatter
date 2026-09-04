package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun RenderInlineSpans(
    spanGroup: InlineSpanGroup,
    themeColors: ReaderThemeColors,
    fontSize: FontSizePreference,
    lineSpacing: LineSpacingPreference,
    fontFamily: FontFamilyPreference,
    modifier: Modifier = Modifier,
    isHeader: Boolean = false,
    textAlign: TextAlign = TextAlign.Start,
    overrideColor: Color? = null,
    searchQuery: String = "",
    onSpanClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val baseTextColor = overrideColor ?: if (isHeader) themeColors.onSurface else themeColors.onBackground
    val targetFontFamily = fontFamily.composeFontFamily
    val bodyFontSize = (if (isHeader) fontSize.bodySp * 0.95f else fontSize.bodySp).sp
    val regularBodyFontSize = fontSize.bodySp.sp
    val inlineCodeFontSize = (fontSize.bodySp * 0.9f).sp

    val annotatedString = remember(spanGroup, baseTextColor, targetFontFamily, bodyFontSize, regularBodyFontSize, inlineCodeFontSize, searchQuery, themeColors) {
        buildAnnotatedString {
            spanGroup.spans.forEach { span ->
                when (span) {
                    is InlineSpan.Text -> {
                        val start = length
                        append(span.content)
                        val end = length
                        if (start < end) {
                            val parsedColor = parseHexOrNamedColor(span.colorHex) ?: baseTextColor
                            val parsedBg = parseHexOrNamedColor(span.bgHex)
                            var style = SpanStyle(
                                color = parsedColor,
                                fontSize = bodyFontSize,
                                fontFamily = targetFontFamily,
                                fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
                                background = parsedBg ?: Color.Unspecified
                            )
                            if (span.isBold) style = style.copy(fontWeight = FontWeight.Bold)
                            if (span.isItalic) style = style.copy(fontStyle = FontStyle.Italic)
                            if (span.isStrike) style = style.copy(textDecoration = TextDecoration.LineThrough)
                            if (span.isUnderline) style = style.copy(textDecoration = TextDecoration.Underline)
                            addStyle(style, start, end)
                        }
                    }

                    is InlineSpan.InlineCode -> {
                        val start = length
                        append(" ${span.code} ")
                        val end = length
                        if (start < end) {
                            addStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = inlineCodeFontSize,
                                    color = themeColors.inlineCodeText,
                                    background = themeColors.inlineCodeBg
                                ),
                                start,
                                end
                            )
                        }
                    }

                    is InlineSpan.Link -> {
                        val start = length
                        append(span.text)
                        val end = length
                        if (start < end) {
                            addStyle(
                                SpanStyle(
                                    color = themeColors.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = TextDecoration.Underline,
                                    fontSize = regularBodyFontSize,
                                    fontFamily = targetFontFamily
                                ),
                                start,
                                end
                            )
                            addStringAnnotation(
                                tag = "URL",
                                annotation = span.url,
                                start = start,
                                end = end
                            )
                        }
                    }

                    is InlineSpan.InlineMath -> {
                        val start = length
                        append(" ${span.latex} ")
                        val end = length
                        if (start < end) {
                            addStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontStyle = FontStyle.Italic,
                                    color = themeColors.secondary,
                                    fontSize = regularBodyFontSize,
                                    background = themeColors.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                start,
                                end
                            )
                        }
                    }
                }
            }

            // Apply search query highlighting safely
            if (searchQuery.isNotBlank()) {
                val fullText = this.toAnnotatedString().text
                val textLen = fullText.length
                try {
                    val matches = Regex(Regex.escape(searchQuery), RegexOption.IGNORE_CASE).findAll(fullText)
                    matches.forEach { match ->
                        val start = match.range.first.coerceIn(0, textLen)
                        val end = (match.range.last + 1).coerceIn(0, textLen)
                        if (start < end) {
                            addStyle(
                                SpanStyle(
                                    background = Color(0xFFFBBF24),
                                    color = Color(0xFF1E293B),
                                    fontWeight = FontWeight.Bold
                                ),
                                start,
                                end
                            )
                        }
                    }
                } catch (_: Exception) {
                    // Ignore regex error
                }
            }
        }
    }

    val lineHeightSp = (fontSize.bodySp * lineSpacing.multiplier).sp
    val hasUrls = remember(spanGroup) {
        spanGroup.spans.any { it is InlineSpan.Link }
    }

    if (onSpanClick != null) {
        // In Interactive Edit mode or explicit span click delegation
        ClickableText(
            text = annotatedString,
            modifier = modifier,
            style = TextStyle(
                lineHeight = lineHeightSp,
                textAlign = textAlign
            ),
            onClick = { offset ->
                val urlAnnotation = try {
                    annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()
                } catch (_: Exception) { null }

                if (urlAnnotation != null) {
                    val url = urlAnnotation.item
                    try {
                        val uri = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        onSpanClick()
                    }
                } else {
                    onSpanClick()
                }
            }
        )
    } else if (hasUrls) {
        // Standard view mode with links
        ClickableText(
            text = annotatedString,
            modifier = modifier,
            style = TextStyle(
                lineHeight = lineHeightSp,
                textAlign = textAlign
            ),
            onClick = { offset ->
                val urlAnnotation = try {
                    annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()
                } catch (_: Exception) { null }

                urlAnnotation?.let { annotation ->
                    val url = annotation.item
                    try {
                        val uri = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open link: $url", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    } else {
        // Standard view mode without links
        Text(
            text = annotatedString,
            modifier = modifier,
            style = TextStyle(
                lineHeight = lineHeightSp,
                textAlign = textAlign
            )
        )
    }
}

private fun parseHexOrNamedColor(colorStr: String?): Color? {
    if (colorStr.isNullOrBlank()) return null
    var clean = colorStr.trim().lowercase().removeSuffix(";").removeSurrounding("\"", "\"").removeSurrounding("'", "'").trim()
    
    // Support rgb(r, g, b) and rgba(r, g, b, a)
    if (clean.startsWith("rgb")) {
        try {
            val parts = clean.substringAfter("(").substringBefore(")").split(",").map { it.trim() }
            if (parts.size >= 3) {
                val r = parts[0].toInt().coerceIn(0, 255)
                val g = parts[1].toInt().coerceIn(0, 255)
                val b = parts[2].toInt().coerceIn(0, 255)
                val a = if (parts.size >= 4) {
                    val alphaFloat = parts[3].toFloatOrNull() ?: 1f
                    if (alphaFloat <= 1.0f) (alphaFloat * 255).toInt().coerceIn(0, 255) else parts[3].toInt().coerceIn(0, 255)
                } else 255
                return Color(r, g, b, a)
            }
        } catch (_: Exception) {}
    }

    return try {
        when (clean) {
            "red" -> Color(0xFFE53935)
            "pink" -> Color(0xFFD81B60)
            "purple" -> Color(0xFF8E24AA)
            "deep-purple" -> Color(0xFF5E35B1)
            "indigo" -> Color(0xFF3949AB)
            "blue" -> Color(0xFF1E88E5)
            "light-blue" -> Color(0xFF039BE5)
            "cyan" -> Color(0xFF00ACC1)
            "teal" -> Color(0xFF00897B)
            "green" -> Color(0xFF43A047)
            "light-green" -> Color(0xFF7CB342)
            "lime" -> Color(0xFFC0CA33)
            "yellow" -> Color(0xFFFDD835)
            "amber" -> Color(0xFFFFB300)
            "orange" -> Color(0xFFFB8C00)
            "deep-orange" -> Color(0xFFF4511E)
            "brown" -> Color(0xFF6D4C41)
            "gray", "grey" -> Color(0xFF757575)
            "black" -> Color(0xFF000000)
            "white" -> Color(0xFFFFFFFF)
            else -> {
                val hex = if (clean.startsWith("#")) clean.removePrefix("#") else clean
                when (hex.length) {
                    3 -> {
                        val r = hex[0].toString().repeat(2).toInt(16)
                        val g = hex[1].toString().repeat(2).toInt(16)
                        val b = hex[2].toString().repeat(2).toInt(16)
                        Color(r, g, b)
                    }
                    6 -> Color(android.graphics.Color.parseColor("#$hex"))
                    8 -> Color(android.graphics.Color.parseColor("#$hex"))
                    else -> null
                }
            }
        }
    } catch (_: Exception) {
        null
    }
}
