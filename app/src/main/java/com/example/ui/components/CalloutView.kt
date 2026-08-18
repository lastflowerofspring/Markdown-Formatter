package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    searchQuery: String = ""
) {
    val (accentColor, icon, defaultLabel) = when (type) {
        CalloutType.NOTE -> Triple(Color(0xFF3B82F6), Icons.Default.Info, "Note")
        CalloutType.TIP -> Triple(Color(0xFF10B981), Icons.Default.Lightbulb, "Tip")
        CalloutType.WARNING -> Triple(Color(0xFFF59E0B), Icons.Default.Warning, "Warning")
        CalloutType.CAUTION -> Triple(Color(0xFFEF4444), Icons.Default.Dangerous, "Caution")
        CalloutType.IMPORTANT -> Triple(Color(0xFF8B5CF6), Icons.Default.PriorityHigh, "Important")
        CalloutType.INFO -> Triple(Color(0xFF06B6D4), Icons.Default.HelpOutline, "Info")
        CalloutType.QUOTE -> Triple(themeColors.primary, Icons.Default.FormatQuote, "Quote")
    }

    val displayTitle = title ?: defaultLabel

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .testTag("callout_view_${type.name.lowercase()}"),
        color = accentColor.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .intrinsicHeight()
        ) {
            // Left Accent Bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Header (Icon + Title)
                if (type != CalloutType.QUOTE || title != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = displayTitle,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = displayTitle,
                            color = accentColor,
                            fontSize = (fontSize.bodySp * 0.9f).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily.composeFontFamily
                        )
                    }
                }

                // Body content
                RenderInlineSpans(
                    spanGroup = content,
                    themeColors = themeColors,
                    fontSize = fontSize,
                    lineSpacing = lineSpacing,
                    fontFamily = fontFamily,
                    searchQuery = searchQuery
                )
            }
        }
    }
}

private fun Modifier.intrinsicHeight() = this.height(IntrinsicSize.Min)
