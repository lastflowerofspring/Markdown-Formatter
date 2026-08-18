package com.example.model

import androidx.compose.ui.text.AnnotatedString

enum class CalloutType(val defaultTitle: String) {
    NOTE("Note"),
    TIP("Tip"),
    WARNING("Warning"),
    CAUTION("Caution"),
    IMPORTANT("Important"),
    INFO("Info"),
    QUOTE("Quote")
}

enum class TableAlignment {
    LEFT, CENTER, RIGHT
}

data class InlineSpanGroup(
    val spans: List<InlineSpan>,
    val rawText: String
)

sealed class InlineSpan {
    data class Text(
        val content: String,
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val isStrike: Boolean = false,
        val isUnderline: Boolean = false
    ) : InlineSpan()

    data class InlineCode(val code: String) : InlineSpan()
    data class Link(val text: String, val url: String) : InlineSpan()
    data class InlineMath(val latex: String) : InlineSpan()
}

data class BulletItem(
    val level: Int,
    val content: InlineSpanGroup
)

data class NumberedItem(
    val level: Int,
    val number: String,
    val content: InlineSpanGroup
)

data class TaskItem(
    val id: String,
    val isChecked: Boolean,
    val content: InlineSpanGroup,
    val lineIndex: Int
)

sealed class MarkdownBlock {
    data class HeaderBlock(
        val level: Int,
        val text: String,
        val id: String
    ) : MarkdownBlock()

    data class ParagraphBlock(
        val content: InlineSpanGroup
    ) : MarkdownBlock()

    data class CodeBlock(
        val language: String,
        val code: String
    ) : MarkdownBlock()

    data class CalloutBlock(
        val type: CalloutType,
        val title: String?,
        val content: InlineSpanGroup
    ) : MarkdownBlock()

    data class BulletListBlock(
        val items: List<BulletItem>
    ) : MarkdownBlock()

    data class NumberedListBlock(
        val items: List<NumberedItem>
    ) : MarkdownBlock()

    data class TaskListBlock(
        val items: List<TaskItem>
    ) : MarkdownBlock()

    data class TableBlock(
        val headers: List<InlineSpanGroup>,
        val alignments: List<TableAlignment>,
        val rows: List<List<InlineSpanGroup>>
    ) : MarkdownBlock()

    data object DividerBlock : MarkdownBlock()

    data class MathBlock(
        val latex: String
    ) : MarkdownBlock()
}

data class HeadingOutlineItem(
    val id: String,
    val level: Int,
    val title: String,
    val blockIndex: Int
)

data class FormattedDocument(
    val rawText: String,
    val blocks: List<MarkdownBlock>,
    val headings: List<HeadingOutlineItem>,
    val wordCount: Int,
    val charCount: Int,
    val readingTimeMinutes: Int
)
