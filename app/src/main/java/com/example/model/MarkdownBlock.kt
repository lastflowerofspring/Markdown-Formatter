package com.example.model

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
        val isUnderline: Boolean = false,
        val colorHex: String? = null,
        val bgHex: String? = null
    ) : InlineSpan()

    data class InlineCode(val code: String) : InlineSpan()
    data class Link(val text: String, val url: String) : InlineSpan()
    data class InlineMath(val latex: String) : InlineSpan()
}

data class BulletItem(
    val level: Int,
    val content: InlineSpanGroup,
    val rawIndex: Int = 0
)

data class NumberedItem(
    val level: Int,
    val number: String,
    val content: InlineSpanGroup,
    val rawIndex: Int = 0
)

data class TaskItem(
    val id: String,
    val isChecked: Boolean,
    val content: InlineSpanGroup,
    val lineIndex: Int
)

sealed class MarkdownBlock {
    abstract val blockId: String
    abstract val lineStart: Int
    abstract val lineEnd: Int
    abstract val isHtml: Boolean

    data class HeaderBlock(
        val level: Int,
        val text: String,
        val id: String,
        override val blockId: String = id,
        override val lineStart: Int = 0,
        override val lineEnd: Int = 0,
        override val isHtml: Boolean = false
    ) : MarkdownBlock()

    data class ParagraphBlock(
        val content: InlineSpanGroup,
        override val blockId: String,
        override val lineStart: Int = 0,
        override val lineEnd: Int = 0,
        override val isHtml: Boolean = false
    ) : MarkdownBlock()

    data class CodeBlock(
        val language: String,
        val code: String,
        override val blockId: String,
        override val lineStart: Int = 0,
        override val lineEnd: Int = 0,
        override val isHtml: Boolean = false
    ) : MarkdownBlock()

    data class CalloutBlock(
        val type: CalloutType,
        val title: String?,
        val content: InlineSpanGroup,
        override val blockId: String,
        override val lineStart: Int = 0,
        override val lineEnd: Int = 0,
        override val isHtml: Boolean = false
    ) : MarkdownBlock()

    data class BulletListBlock(
        val items: List<BulletItem>,
        override val blockId: String,
        override val lineStart: Int = 0,
        override val lineEnd: Int = 0,
        override val isHtml: Boolean = false
    ) : MarkdownBlock()

    data class NumberedListBlock(
        val items: List<NumberedItem>,
        override val blockId: String,
        override val lineStart: Int = 0,
        override val lineEnd: Int = 0,
        override val isHtml: Boolean = false
    ) : MarkdownBlock()

    data class TaskListBlock(
        val items: List<TaskItem>,
        override val blockId: String,
        override val lineStart: Int = 0,
        override val lineEnd: Int = 0,
        override val isHtml: Boolean = false
    ) : MarkdownBlock()

    data class TableBlock(
        val headers: List<InlineSpanGroup>,
        val alignments: List<TableAlignment>,
        val rows: List<List<InlineSpanGroup>>,
        override val blockId: String,
        override val lineStart: Int = 0,
        override val lineEnd: Int = 0,
        override val isHtml: Boolean = false
    ) : MarkdownBlock()

    data class DividerBlock(
        override val blockId: String,
        override val lineStart: Int = 0,
        override val lineEnd: Int = 0,
        override val isHtml: Boolean = false
    ) : MarkdownBlock()

    data class MathBlock(
        val latex: String,
        override val blockId: String,
        override val lineStart: Int = 0,
        override val lineEnd: Int = 0,
        override val isHtml: Boolean = false
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
