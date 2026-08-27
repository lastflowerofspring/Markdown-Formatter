package com.example.parser

import com.example.model.*
import java.util.UUID

object MarkdownParser {

    fun parse(rawText: String): FormattedDocument {
        if (rawText.isBlank()) {
            return FormattedDocument(
                rawText = rawText,
                blocks = emptyList(),
                headings = emptyList(),
                wordCount = 0,
                charCount = 0,
                readingTimeMinutes = 0
            )
        }

        val lines = rawText.lines()
        val blocks = mutableListOf<MarkdownBlock>()
        val headings = mutableListOf<HeadingOutlineItem>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // 1. Skip completely empty lines
            if (trimmed.isEmpty()) {
                i++
                continue
            }

            val startLine = i

            // 2. Fenced Code Block: ```lang
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                val fence = if (trimmed.startsWith("```")) "```" else "~~~"
                val lang = trimmed.removePrefix(fence).trim()
                val codeBuilder = StringBuilder()
                i++
                while (i < lines.size) {
                    val codeLine = lines[i]
                    if (codeLine.trim().startsWith(fence)) {
                        i++
                        break
                    }
                    if (codeBuilder.isNotEmpty()) codeBuilder.append("\n")
                    codeBuilder.append(codeLine)
                    i++
                }
                blocks.add(
                    MarkdownBlock.CodeBlock(
                        language = lang,
                        code = codeBuilder.toString(),
                        blockId = "code_${blocks.size}_$startLine",
                        lineStart = startLine,
                        lineEnd = i - 1
                    )
                )
                continue
            }

            // 3. Math Block: $$ ... $$
            if (trimmed.startsWith("$$")) {
                if (trimmed.length > 2 && trimmed.endsWith("$$") && trimmed != "$$") {
                    val latex = trimmed.removePrefix("$$").removeSuffix("$$").trim()
                    blocks.add(
                        MarkdownBlock.MathBlock(
                            latex = latex,
                            blockId = "math_${blocks.size}_$startLine",
                            lineStart = startLine,
                            lineEnd = startLine
                        )
                    )
                    i++
                    continue
                } else {
                    val mathBuilder = StringBuilder()
                    val firstLineMath = trimmed.removePrefix("$$").trim()
                    if (firstLineMath.isNotEmpty()) mathBuilder.append(firstLineMath)
                    i++
                    while (i < lines.size) {
                        val mathLine = lines[i]
                        if (mathLine.trim().endsWith("$$")) {
                            val lastPart = mathLine.trim().removeSuffix("$$").trim()
                            if (lastPart.isNotEmpty()) {
                                if (mathBuilder.isNotEmpty()) mathBuilder.append("\n")
                                mathBuilder.append(lastPart)
                            }
                            i++
                            break
                        }
                        if (mathBuilder.isNotEmpty()) mathBuilder.append("\n")
                        mathBuilder.append(mathLine)
                        i++
                    }
                    blocks.add(
                        MarkdownBlock.MathBlock(
                            latex = mathBuilder.toString(),
                            blockId = "math_${blocks.size}_$startLine",
                            lineStart = startLine,
                            lineEnd = i - 1
                        )
                    )
                    continue
                }
            }

            // 4. Horizontal Rule / Divider: ---, ***, ___
            if (trimmed.matches(Regex("^(\\*{3,}|-{3,}|_{3,})$"))) {
                blocks.add(
                    MarkdownBlock.DividerBlock(
                        blockId = "div_${blocks.size}_$startLine",
                        lineStart = startLine,
                        lineEnd = startLine
                    )
                )
                i++
                continue
            }

            // 5. Headings: # H1, ## H2, ### H3, #### H4, ##### H5, ###### H6
            val headerMatch = Regex("^(#{1,6})\\s+(.+)$").find(trimmed)
            if (headerMatch != null) {
                val level = headerMatch.groupValues[1].length
                val title = headerMatch.groupValues[2].trim()
                val id = "h_${headings.size}_${title.filter { it.isLetterOrDigit() }.take(16)}"
                val block = MarkdownBlock.HeaderBlock(
                    level = level,
                    text = title,
                    id = id,
                    blockId = "hdr_${blocks.size}_$startLine",
                    lineStart = startLine,
                    lineEnd = startLine
                )
                headings.add(HeadingOutlineItem(id = id, level = level, title = title, blockIndex = blocks.size))
                blocks.add(block)
                i++
                continue
            }

            // 6. Blockquote & Callouts (> [!NOTE], etc.)
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines.add(lines[i].trim().removePrefix(">").trim())
                    i++
                }

                val firstQuoteLine = quoteLines.firstOrNull() ?: ""
                val calloutMatch = Regex("^\\[!(NOTE|TIP|WARNING|CAUTION|IMPORTANT|INFO|QUOTE)\\]\\s*(.*)$", RegexOption.IGNORE_CASE).find(firstQuoteLine)

                if (calloutMatch != null) {
                    val typeStr = calloutMatch.groupValues[1].uppercase()
                    val explicitTitle = calloutMatch.groupValues[2].trim().ifEmpty { null }
                    val calloutType = when (typeStr) {
                        "NOTE" -> CalloutType.NOTE
                        "TIP" -> CalloutType.TIP
                        "WARNING" -> CalloutType.WARNING
                        "CAUTION" -> CalloutType.CAUTION
                        "IMPORTANT" -> CalloutType.IMPORTANT
                        "INFO" -> CalloutType.INFO
                        else -> CalloutType.QUOTE
                    }
                    val bodyLines = quoteLines.drop(1)
                    val fullQuoteText = bodyLines.joinToString("\n")
                    blocks.add(
                        MarkdownBlock.CalloutBlock(
                            type = calloutType,
                            title = explicitTitle,
                            content = parseInlineSpans(fullQuoteText.ifEmpty { explicitTitle ?: calloutType.defaultTitle }),
                            blockId = "callout_${blocks.size}_$startLine",
                            lineStart = startLine,
                            lineEnd = i - 1
                        )
                    )
                } else {
                    val fullQuoteText = quoteLines.joinToString("\n")
                    blocks.add(
                        MarkdownBlock.CalloutBlock(
                            type = CalloutType.QUOTE,
                            title = null,
                            content = parseInlineSpans(fullQuoteText),
                            blockId = "callout_${blocks.size}_$startLine",
                            lineStart = startLine,
                            lineEnd = i - 1
                        )
                    )
                }
                continue
            }

            // 7. Task Lists: - [ ] or - [x]
            if (trimmed.matches(Regex("^[-*+]\\s+\\[([ xX])\\]\\s+.*$"))) {
                val taskItems = mutableListOf<TaskItem>()
                while (i < lines.size) {
                    val currentLine = lines[i]
                    val taskMatch = Regex("^\\s*[-*+]\\s+\\[([ xX])\\]\\s+(.*)$").find(currentLine)
                    if (taskMatch != null) {
                        val isChecked = taskMatch.groupValues[1].equals("x", ignoreCase = true)
                        val text = taskMatch.groupValues[2]
                        taskItems.add(
                            TaskItem(
                                id = UUID.randomUUID().toString(),
                                isChecked = isChecked,
                                content = parseInlineSpans(text),
                                lineIndex = i
                            )
                        )
                        i++
                    } else {
                        break
                    }
                }
                blocks.add(
                    MarkdownBlock.TaskListBlock(
                        items = taskItems,
                        blockId = "tasklist_${blocks.size}_$startLine",
                        lineStart = startLine,
                        lineEnd = i - 1
                    )
                )
                continue
            }

            // 8. Markdown Tables: | Header | Header |
            if (trimmed.startsWith("|") && trimmed.endsWith("|") && i + 1 < lines.size && lines[i + 1].trim().matches(Regex("^\\|[\\s:|-]+\\|$"))) {
                val headerCells = splitTableCells(trimmed)
                val separatorCells = splitTableCells(lines[i + 1].trim())
                val alignments = separatorCells.map { cell ->
                    val t = cell.trim()
                    when {
                        t.startsWith(":") && t.endsWith(":") -> TableAlignment.CENTER
                        t.endsWith(":") -> TableAlignment.RIGHT
                        else -> TableAlignment.LEFT
                    }
                }

                val rowBlocks = mutableListOf<List<InlineSpanGroup>>()
                i += 2 // skip header and separator
                while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    val rowCells = splitTableCells(lines[i].trim())
                    val parsedRow = rowCells.map { parseInlineSpans(it) }
                    rowBlocks.add(parsedRow)
                    i++
                }

                blocks.add(
                    MarkdownBlock.TableBlock(
                        headers = headerCells.map { parseInlineSpans(it) },
                        alignments = alignments,
                        rows = rowBlocks,
                        blockId = "table_${blocks.size}_$startLine",
                        lineStart = startLine,
                        lineEnd = i - 1
                    )
                )
                continue
            }

            // 9. Bullet Lists: - item, * item, + item
            if (trimmed.matches(Regex("^[-*+]\\s+.*$"))) {
                val bulletItems = mutableListOf<BulletItem>()
                while (i < lines.size) {
                    val rawListLine = lines[i]
                    val bulletMatch = Regex("^(\\s*)([-*+])\\s+(.*)$").find(rawListLine)
                    if (bulletMatch != null) {
                        val contentText = bulletMatch.groupValues[3]
                        if (contentText.startsWith("[ ]") || contentText.startsWith("[x]") || contentText.startsWith("[X]")) {
                            break
                        }
                        val indentSpaces = bulletMatch.groupValues[1].length
                        val level = (indentSpaces / 2).coerceIn(0, 3)
                        bulletItems.add(
                            BulletItem(
                                level = level,
                                content = parseInlineSpans(contentText),
                                rawIndex = i
                            )
                        )
                        i++
                    } else {
                        break
                    }
                }
                if (bulletItems.isNotEmpty()) {
                    blocks.add(
                        MarkdownBlock.BulletListBlock(
                            items = bulletItems,
                            blockId = "bullet_${blocks.size}_$startLine",
                            lineStart = startLine,
                            lineEnd = i - 1
                        )
                    )
                    continue
                }
            }

            // 10. Numbered Lists: 1. item, 2. item
            if (trimmed.matches(Regex("^\\d+[.)]\\s+.*$"))) {
                val numberedItems = mutableListOf<NumberedItem>()
                while (i < lines.size) {
                    val rawListLine = lines[i]
                    val numMatch = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)$").find(rawListLine)
                    if (numMatch != null) {
                        val indentSpaces = numMatch.groupValues[1].length
                        val numStr = numMatch.groupValues[2]
                        val level = (indentSpaces / 2).coerceIn(0, 3)
                        val text = numMatch.groupValues[3]
                        numberedItems.add(
                            NumberedItem(
                                level = level,
                                number = numStr,
                                content = parseInlineSpans(text),
                                rawIndex = i
                            )
                        )
                        i++
                    } else {
                        break
                    }
                }
                if (numberedItems.isNotEmpty()) {
                    blocks.add(
                        MarkdownBlock.NumberedListBlock(
                            items = numberedItems,
                            blockId = "numbered_${blocks.size}_$startLine",
                            lineStart = startLine,
                            lineEnd = i - 1
                        )
                    )
                    continue
                }
            }

            // 11. Normal Paragraph
            val paragraphBuilder = StringBuilder()
            val paragraphStartLine = i
            while (i < lines.size) {
                val currentLine = lines[i]
                val curTrim = currentLine.trim()
                if (curTrim.isEmpty() ||
                    curTrim.startsWith("#") ||
                    curTrim.startsWith("```") ||
                    curTrim.startsWith("~~~") ||
                    curTrim.startsWith(">") ||
                    curTrim.startsWith("$$") ||
                    curTrim.matches(Regex("^(\\*{3,}|-{3,}|_{3,})$")) ||
                    curTrim.matches(Regex("^[-*+]\\s+.*$")) ||
                    curTrim.matches(Regex("^\\d+[.)]\\s+.*$")) ||
                    (curTrim.startsWith("|") && curTrim.endsWith("|") && i + 1 < lines.size && lines[i + 1].trim().matches(Regex("^\\|[\\s:|-]+\\|$")))
                ) {
                    break
                }
                if (paragraphBuilder.isNotEmpty()) paragraphBuilder.append(" ")
                paragraphBuilder.append(curTrim)
                i++
            }

            val pText = paragraphBuilder.toString()
            if (pText.isNotEmpty()) {
                blocks.add(
                    MarkdownBlock.ParagraphBlock(
                        content = parseInlineSpans(pText),
                        blockId = "p_${blocks.size}_$paragraphStartLine",
                        lineStart = paragraphStartLine,
                        lineEnd = i - 1
                    )
                )
            }
        }

        val words = rawText.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size
        val charCount = rawText.length
        val readingTime = (wordCount / 200).coerceAtLeast(1)

        return FormattedDocument(
            rawText = rawText,
            blocks = blocks,
            headings = headings,
            wordCount = wordCount,
            charCount = charCount,
            readingTimeMinutes = readingTime
        )
    }

    private fun splitTableCells(line: String): List<String> {
        val trimmed = line.trim().removePrefix("|").removeSuffix("|")
        return trimmed.split("|").map { it.trim() }
    }

    fun parseInlineSpans(text: String): InlineSpanGroup {
        val spans = mutableListOf<InlineSpan>()
        val pattern = Regex("(`[^`]+`|\\[[^\\]]+\\]\\([^)]+\\)|\\$\\$?[^$]+\\$\\$?|\\*\\*\\*[^*]+\\*\\*\\*|\\*\\*[^*]+\\*\\*|\\*[^*]+\\*|~~[^~]+~~|__[^_]+__|_[^_]+_)")

        var currentIndex = 0
        pattern.findAll(text).forEach { match ->
            if (match.range.first > currentIndex) {
                val plain = text.substring(currentIndex, match.range.first)
                spans.add(InlineSpan.Text(content = plain))
            }

            val token = match.value
            when {
                token.startsWith("`") && token.endsWith("`") -> {
                    spans.add(InlineSpan.InlineCode(code = token.removeSurrounding("`")))
                }
                token.startsWith("[") && token.contains("](") && token.endsWith(")") -> {
                    val linkText = token.substringAfter("[").substringBefore("](")
                    val url = token.substringAfter("](").substringBeforeLast(")")
                    spans.add(InlineSpan.Link(text = linkText, url = url))
                }
                token.startsWith("$") && token.endsWith("$") -> {
                    val latex = token.removeSurrounding("$").trim()
                    spans.add(InlineSpan.InlineMath(latex = latex))
                }
                (token.startsWith("***") && token.endsWith("***")) || (token.startsWith("___") && token.endsWith("___")) -> {
                    val inner = if (token.startsWith("***")) token.removeSurrounding("***") else token.removeSurrounding("___")
                    spans.add(InlineSpan.Text(content = inner, isBold = true, isItalic = true))
                }
                (token.startsWith("**") && token.endsWith("**")) || (token.startsWith("__") && token.endsWith("__")) -> {
                    val inner = if (token.startsWith("**")) token.removeSurrounding("**") else token.removeSurrounding("__")
                    spans.add(InlineSpan.Text(content = inner, isBold = true))
                }
                token.startsWith("~~") && token.endsWith("~~") -> {
                    spans.add(InlineSpan.Text(content = token.removeSurrounding("~~"), isStrike = true))
                }
                (token.startsWith("*") && token.endsWith("*")) || (token.startsWith("_") && token.endsWith("_")) -> {
                    val inner = if (token.startsWith("*")) token.removeSurrounding("*") else token.removeSurrounding("_")
                    spans.add(InlineSpan.Text(content = inner, isItalic = true))
                }
                else -> {
                    spans.add(InlineSpan.Text(content = token))
                }
            }
            currentIndex = match.range.last + 1
        }

        if (currentIndex < text.length) {
            val remaining = text.substring(currentIndex)
            spans.add(InlineSpan.Text(content = remaining))
        }

        return InlineSpanGroup(spans = spans, rawText = text)
    }
}
