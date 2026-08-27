package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.*

@Composable
fun CalloutView(
    type: CalloutType,
    title: String?,
    content: InlineSpanGroup,
    themeColors: ReaderThemeColors,
    fontSize: FontSizePreference,
    lineSpacing: LineSpacingPreference,
    fontFamily: FontFamilyPreference,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    onSpanClick: (() -> Unit)? = null
) {
    val isDark = themeColors.background.luminance() < 0.5f

    if (type == CalloutType.QUOTE) {
        // Standard blockquote styling
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(themeColors.blockquoteBg)
                .testTag("callout_view_quote"),
            color = themeColors.blockquoteBg
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(IntrinsicSize.Min)
                        .background(themeColors.blockquoteBorder)
                )

                RenderInlineSpans(
                    spanGroup = content,
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
        return
    }

    // GFM Alerts: Exact 1:1 replica of Google Material Symbols & GFM Alert styling
    val (iconColor, bgColor, borderColor, icon) = when (type) {
        CalloutType.NOTE -> {
            if (isDark) {
                Quadruple(
                    Color(0xFF7DA0FA),
                    Color(0xFF1B2433),
                    Color(0xFF28364A),
                    Icons.Outlined.Info
                )
            } else {
                Quadruple(
                    Color(0xFF1A73E8),
                    Color(0xFFE8F0FE),
                    Color(0xFFD2E3FC),
                    Icons.Outlined.Info
                )
            }
        }
        CalloutType.TIP -> {
            if (isDark) {
                Quadruple(
                    Color(0xFF56D364),
                    Color(0xFF16281D),
                    Color(0xFF223D2A),
                    Icons.Outlined.Lightbulb
                )
            } else {
                Quadruple(
                    Color(0xFF188038),
                    Color(0xFFE6F4EA),
                    Color(0xFFCEEAD6),
                    Icons.Outlined.Lightbulb
                )
            }
        }
        CalloutType.IMPORTANT -> {
            if (isDark) {
                Quadruple(
                    Color(0xFFD2A8FF),
                    Color(0xFF281F33),
                    Color(0xFF3F3050),
                    Icons.Outlined.Report
                )
            } else {
                Quadruple(
                    Color(0xFF9333EA),
                    Color(0xFFF3E8FD),
                    Color(0xFFE9D5FF),
                    Icons.Outlined.Report
                )
            }
        }
        CalloutType.WARNING -> {
            if (isDark) {
                Quadruple(
                    Color(0xFFE3B341),
                    Color(0xFF2E2413),
                    Color(0xFF45361C),
                    Icons.Outlined.Warning
                )
            } else {
                Quadruple(
                    Color(0xFFB06000),
                    Color(0xFFFEF7E0),
                    Color(0xFFFEEFC3),
                    Icons.Outlined.Warning
                )
            }
        }
        CalloutType.CAUTION -> {
            if (isDark) {
                Quadruple(
                    Color(0xFFF85149),
                    Color(0xFF341C21),
                    Color(0xFF4D2830),
                    Icons.Outlined.ErrorOutline
                )
            } else {
                Quadruple(
                    Color(0xFFD93025),
                    Color(0xFFFCE8E6),
                    Color(0xFFFAD2CF),
                    Icons.Outlined.ErrorOutline
                )
            }
        }
        CalloutType.INFO -> {
            if (isDark) {
                Quadruple(
                    Color(0xFF7DA0FA),
                    Color(0xFF1B2433),
                    Color(0xFF28364A),
                    Icons.Outlined.Info
                )
            } else {
                Quadruple(
                    Color(0xFF0284C7),
                    Color(0xFFE0F2FE),
                    Color(0xFFBAE6FD),
                    Icons.Outlined.Info
                )
            }
        }
        CalloutType.QUOTE -> {
            Quadruple(
                themeColors.primary,
                themeColors.blockquoteBg,
                themeColors.blockquoteBorder,
                Icons.Default.FormatQuote
            )
        }
    }

    // Build the final inline span group: include bold title prefix seamlessly
    val finalContent = remember(content, title, type) {
        val startsWithBold = content.spans.firstOrNull()?.let { it is InlineSpan.Text && it.isBold } == true
        if (!title.isNullOrBlank()) {
            if (content.rawText.startsWith(title, ignoreCase = true)) {
                content
            } else {
                val displayTitleText = if (title.endsWith(":") || title.endsWith(".")) "$title " else "$title: "
                val newSpans = listOf(InlineSpan.Text(content = displayTitleText, isBold = true)) + content.spans
                InlineSpanGroup(spans = newSpans, rawText = displayTitleText + content.rawText)
            }
        } else if (!startsWithBold) {
            val defaultTitleText = "${type.defaultTitle}: "
            val newSpans = listOf(InlineSpan.Text(content = defaultTitleText, isBold = true)) + content.spans
            InlineSpanGroup(spans = newSpans, rawText = defaultTitleText + content.rawText)
        } else {
            content
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .testTag("callout_view_${type.name.lowercase()}"),
        color = bgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = type.name,
                tint = iconColor,
                modifier = Modifier
                    .padding(top = 1.5.dp)
                    .size(18.dp)
            )

            RenderInlineSpans(
                spanGroup = finalContent,
                themeColors = themeColors,
                fontSize = fontSize,
                lineSpacing = lineSpacing,
                fontFamily = fontFamily,
                searchQuery = searchQuery,
                overrideColor = if (isDark) Color(0xFFE2E2E5) else null,
                onSpanClick = onSpanClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

