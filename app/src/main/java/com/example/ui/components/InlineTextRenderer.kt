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
import androidx.compose.runtime.Composable
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
    searchQuery: String = ""
) {
    val context = LocalContext.current

    val annotatedString = buildAnnotatedString {
        spanGroup.spans.forEach { span ->
            when (span) {
                is InlineSpan.Text -> {
                    val start = length
                    append(span.content)
                    val end = length
                    val baseTextColor = overrideColor ?: if (isHeader) themeColors.onSurface else themeColors.onBackground
                    var style = SpanStyle(
                        color = baseTextColor,
                        fontSize = (if (isHeader) fontSize.bodySp * 0.95f else fontSize.bodySp).sp,
                        fontFamily = fontFamily.composeFontFamily,
                        fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (span.isBold) style = style.copy(fontWeight = FontWeight.Bold)
                    if (span.isItalic) style = style.copy(fontStyle = FontStyle.Italic)
                    if (span.isStrike) style = style.copy(textDecoration = TextDecoration.LineThrough)
                    if (span.isUnderline) style = style.copy(textDecoration = TextDecoration.Underline)
                    addStyle(style, start, end)
                }

                is InlineSpan.InlineCode -> {
                    val start = length
                    append(" ${span.code} ")
                    val end = length
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = (fontSize.bodySp * 0.9f).sp,
                            color = themeColors.inlineCodeText,
                            background = themeColors.inlineCodeBg
                        ),
                        start,
                        end
                    )
                }

                is InlineSpan.Link -> {
                    val start = length
                    append(span.text)
                    val end = length
                    addStyle(
                        SpanStyle(
                            color = themeColors.primary,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline,
                            fontSize = fontSize.bodySp.sp,
                            fontFamily = fontFamily.composeFontFamily
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

                is InlineSpan.InlineMath -> {
                    val start = length
                    append(" ${span.latex} ")
                    val end = length
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontStyle = FontStyle.Italic,
                            color = themeColors.secondary,
                            fontSize = fontSize.bodySp.sp,
                            background = themeColors.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        start,
                        end
                    )
                }
            }
        }

        // Apply search query highlighting
        if (searchQuery.isNotBlank()) {
            val text = this.toAnnotatedString().text
            val matches = Regex(Regex.escape(searchQuery), RegexOption.IGNORE_CASE).findAll(text)
            matches.forEach { match ->
                addStyle(
                    SpanStyle(
                        background = Color(0xFFFBBF24),
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold
                    ),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }
    }

    val lineHeightSp = (fontSize.bodySp * lineSpacing.multiplier).sp

    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = TextStyle(
            lineHeight = lineHeightSp,
            textAlign = textAlign
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()?.let { annotation ->
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
}
