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

        // Break multiple back-to-back block tags across lines so minified HTML parses cleanly
        val normalizedText = rawText
            .replace(Regex("(?i)</div>(?=\\s*<div)"), "</div>\n")
            .replace(Regex("(?i)</p>(?=\\s*<p)"), "</p>\n")
            .replace(Regex("(?i)</h1>(?=\\s*<)"), "</h1>\n")
            .replace(Regex("(?i)</h2>(?=\\s*<)"), "</h2>\n")
            .replace(Regex("(?i)</h3>(?=\\s*<)"), "</h3>\n")
            .replace(Regex("(?i)</li>(?=\\s*<li)"), "</li>\n")
            .replace(Regex("(?i)</tr>(?=\\s*<tr)"), "</tr>\n")

        val lines = normalizedText.lines()
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

            // 2. Skip HTML comments <!-- ... -->
            if (trimmed.startsWith("<!--")) {
                while (i < lines.size) {
                    if (lines[i].contains("-->")) {
                        i++
                        break
                    }
                    i++
                }
                continue
            }

            // 3. Skip document wrappers (<!DOCTYPE html>, <html>, </html>, <head>, </head>, <body>, </body>)
            // or orphaned closing tags
            if (trimmed.startsWith("<!DOCTYPE", ignoreCase = true) ||
                trimmed.matches(Regex("^</?(html|head|body)[^>]*>$", RegexOption.IGNORE_CASE)) ||
                trimmed.matches(Regex("^</?(div|p|span|section|article|header|footer|aside|main|font)[^>]*>$", RegexOption.IGNORE_CASE))
            ) {
                i++
                continue
            }

            // 4. HTML <style> block (CSS)
            if (trimmed.startsWith("<style", ignoreCase = true)) {
                val styleBuilder = StringBuilder()
                if (trimmed.contains("</style>", ignoreCase = true)) {
                    val css = trimmed.replace(Regex("^<style[^>]*>", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("</style>[\\s\\S]*$", RegexOption.IGNORE_CASE), "").trim()
                    blocks.add(
                        MarkdownBlock.CodeBlock(
                            language = "css",
                            code = css,
                            blockId = "code_css_${blocks.size}_$startLine",
                            lineStart = startLine,
                            lineEnd = startLine,
                            isHtml = true
                        )
                    )
                    i++
                    continue
                } else {
                    val first = trimmed.replace(Regex("^<style[^>]*>", RegexOption.IGNORE_CASE), "").trim()
                    if (first.isNotEmpty()) styleBuilder.append(first)
                    i++
                    while (i < lines.size) {
                        val sLine = lines[i]
                        if (sLine.contains("</style>", ignoreCase = true)) {
                            val beforeClose = sLine.substringBefore("</style>").trim()
                            if (beforeClose.isNotEmpty()) {
                                if (styleBuilder.isNotEmpty()) styleBuilder.append("\n")
                                styleBuilder.append(beforeClose)
                            }
                            i++
                            break
                        }
                        if (styleBuilder.isNotEmpty()) styleBuilder.append("\n")
                        styleBuilder.append(sLine)
                        i++
                    }
                    blocks.add(
                        MarkdownBlock.CodeBlock(
                            language = "css",
                            code = styleBuilder.toString().trim(),
                            blockId = "code_css_${blocks.size}_$startLine",
                            lineStart = startLine,
                            lineEnd = i - 1,
                            isHtml = true
                        )
                    )
                    continue
                }
            }

            // 5. HTML <pre> block
            if (trimmed.startsWith("<pre", ignoreCase = true)) {
                val preBuilder = StringBuilder()
                while (i < lines.size) {
                    val cur = lines[i]
                    preBuilder.append(cur).append("\n")
                    if (cur.contains("</pre>", ignoreCase = true)) {
                        i++
                        break
                    }
                    i++
                }
                val fullPre = preBuilder.toString()
                val langMatch = Regex("class=[\"'](?:language-)?([a-zA-Z0-9_-]+)[\"']", RegexOption.IGNORE_CASE).find(fullPre)
                val lang = langMatch?.groupValues?.get(1) ?: ""
                val codeContent = fullPre
                    .replace(Regex("^<pre[^>]*>\\s*(?:<code[^>]*>)?", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("(?:</code>)?\\s*</pre>\\s*$", RegexOption.IGNORE_CASE), "")
                    .trim()
                blocks.add(
                    MarkdownBlock.CodeBlock(
                        language = lang,
                        code = unescapeHtml(codeContent),
                        blockId = "code_html_${blocks.size}_$startLine",
                        lineStart = startLine,
                        lineEnd = i - 1,
                        isHtml = true
                    )
                )
                continue
            }

            // 6. HTML Headings: <h1 ...> to <h6 ...>
            val htmlHeaderMatch = Regex("^<h([1-6])(?:\\s+[^>]*)?>([\\s\\S]*?)</h\\1>$", RegexOption.IGNORE_CASE).find(trimmed)
            if (htmlHeaderMatch != null) {
                val level = htmlHeaderMatch.groupValues[1].toInt()
                val rawTitle = htmlHeaderMatch.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
                val id = "h_${headings.size}_${rawTitle.filter { it.isLetterOrDigit() }.take(16)}"
                val block = MarkdownBlock.HeaderBlock(
                    level = level,
                    text = rawTitle,
                    id = id,
                    blockId = "hdr_html_${blocks.size}_$startLine",
                    lineStart = startLine,
                    lineEnd = startLine,
                    isHtml = true
                )
                headings.add(HeadingOutlineItem(id = id, level = level, title = rawTitle, blockIndex = blocks.size))
                blocks.add(block)
                i++
                continue
            }

            // 7. HTML <blockquote>
            if (trimmed.startsWith("<blockquote", ignoreCase = true)) {
                val bqBuilder = StringBuilder()
                while (i < lines.size) {
                    val cur = lines[i]
                    bqBuilder.append(cur).append("\n")
                    if (cur.contains("</blockquote>", ignoreCase = true)) {
                        i++
                        break
                    }
                    i++
                }
                val inner = bqBuilder.toString()
                    .replace(Regex("^<blockquote[^>]*>", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("</blockquote>\\s*$", RegexOption.IGNORE_CASE), "")
                    .trim()
                blocks.add(
                    MarkdownBlock.CalloutBlock(
                        type = CalloutType.QUOTE,
                        title = null,
                        content = parseInlineSpans(inner),
                        blockId = "callout_html_${blocks.size}_$startLine",
                        lineStart = startLine,
                        lineEnd = i - 1,
                        isHtml = true
                    )
                )
                continue
            }

            // 8. HTML <hr>
            if (trimmed.matches(Regex("^<hr(?:\\s*/?>|\\s+[^>]*>)$", RegexOption.IGNORE_CASE))) {
                blocks.add(
                    MarkdownBlock.DividerBlock(
                        blockId = "div_html_${blocks.size}_$startLine",
                        lineStart = startLine,
                        lineEnd = startLine,
                        isHtml = true
                    )
                )
                i++
                continue
            }

            // 9. HTML <table>
            if (trimmed.startsWith("<table", ignoreCase = true)) {
                val tableLines = mutableListOf<String>()
                while (i < lines.size) {
                    val cur = lines[i]
                    tableLines.add(cur)
                    if (cur.contains("</table>", ignoreCase = true)) {
                        i++
                        break
                    }
                    i++
                }
                val fullTable = tableLines.joinToString("\n")
                val parsedTableBlock = parseHtmlTable(fullTable, blocks.size, startLine, i - 1)
                if (parsedTableBlock != null) {
                    blocks.add(parsedTableBlock)
                    continue
                }
            }

            // 10. HTML <ul> and <ol>
            if (trimmed.startsWith("<ul", ignoreCase = true) || trimmed.startsWith("<ol", ignoreCase = true)) {
                val isOrdered = trimmed.startsWith("<ol", ignoreCase = true)
                val closeTag = if (isOrdered) "</ol>" else "</ul>"
                val listStartLine = startLine
                val listLines = mutableListOf<Pair<Int, String>>()
                while (i < lines.size) {
                    val cur = lines[i]
                    listLines.add(Pair(i, cur))
                    if (cur.contains(closeTag, ignoreCase = true)) {
                        i++
                        break
                    }
                    i++
                }
                val listBlock = parseHtmlList(listLines, isOrdered, blocks.size, listStartLine, i - 1)
                if (listBlock != null) {
                    blocks.add(listBlock)
                    continue
                }
            }

            // 11. HTML <p>, <div>, <section>, <article>, <main>, <header>, <footer>, <aside>, <center>
            if (trimmed.matches(Regex("^<(?:p|div|section|article|main|header|footer|aside|center)\\b[^>]*>.*$", RegexOption.IGNORE_CASE))) {
                val tagMatch = Regex("^<([a-zA-Z0-9]+)", RegexOption.IGNORE_CASE).find(trimmed)
                val tagName = tagMatch?.groupValues?.get(1)?.lowercase() ?: "p"
                val closeTag = "</$tagName>"
                val pBuilder = StringBuilder()
                var endLine = startLine

                while (i < lines.size) {
                    val cur = lines[i]
                    pBuilder.append(cur).append(" ")
                    endLine = i
                    if (cur.contains(closeTag, ignoreCase = true)) {
                        i++
                        break
                    }
                    // Guard against unclosed tag: stop if another distinct block starts
                    if (i + 1 < lines.size) {
                        val nextTrim = lines[i + 1].trim()
                        if (nextTrim.startsWith("#") || nextTrim.startsWith("```") || nextTrim.startsWith("<table") || nextTrim.startsWith("<h")) {
                            i++
                            break
                        }
                    }
                    i++
                }
                val fullP = pBuilder.toString().trim()
                val inner = fullP
                    .replace(Regex("^<${tagName}[^>]*>", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("</${tagName}>[\\s\\S]*$", RegexOption.IGNORE_CASE), "")
                    .trim()
                if (inner.isNotEmpty()) {
                    blocks.add(
                        MarkdownBlock.ParagraphBlock(
                            content = parseInlineSpans(inner),
                            blockId = "p_html_${blocks.size}_$startLine",
                            lineStart = startLine,
                            lineEnd = endLine,
                            isHtml = true
                        )
                    )
                }
                continue
            }

            // 12. Fenced Code Block: ```lang or ~~~lang
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

            // 13. Math Block: $$ ... $$
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

            // 14. Horizontal Rule / Divider: ---, ***, ___
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

            // 15. Headings: # H1, ## H2, ### H3, #### H4, ##### H5, ###### H6
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

            // 16. Blockquote & Callouts (> [!NOTE], etc.)
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines.add(lines[i].trim().removePrefix(">").trim())
                    i++
                }

                val firstLine = quoteLines.firstOrNull() ?: ""
                val calloutMatch = Regex("^\\[!([A-Za-z]+)\\](?:\\s+(.*))?$").find(firstLine)

                if (calloutMatch != null) {
                    val rawType = calloutMatch.groupValues[1].uppercase()
                    val title = calloutMatch.groupValues[2].ifBlank { null }
                    val type = when (rawType) {
                        "NOTE" -> CalloutType.NOTE
                        "TIP" -> CalloutType.TIP
                        "IMPORTANT" -> CalloutType.IMPORTANT
                        "WARNING" -> CalloutType.WARNING
                        "CAUTION" -> CalloutType.CAUTION
                        else -> CalloutType.NOTE
                    }
                    val contentText = quoteLines.drop(1).joinToString(" ")
                    blocks.add(
                        MarkdownBlock.CalloutBlock(
                            type = type,
                            title = title,
                            content = parseInlineSpans(contentText),
                            blockId = "callout_${blocks.size}_$startLine",
                            lineStart = startLine,
                            lineEnd = i - 1
                        )
                    )
                } else {
                    val contentText = quoteLines.joinToString(" ")
                    blocks.add(
                        MarkdownBlock.CalloutBlock(
                            type = CalloutType.QUOTE,
                            title = null,
                            content = parseInlineSpans(contentText),
                            blockId = "quote_${blocks.size}_$startLine",
                            lineStart = startLine,
                            lineEnd = i - 1
                        )
                    )
                }
                continue
            }

            // 17. Task List: - [ ] or - [x] or * [ ] or + [ ]
            if (trimmed.matches(Regex("^[-*+]\\s+\\[[ xX]\\]\\s+.*$"))) {
                val taskItems = mutableListOf<TaskItem>()
                while (i < lines.size) {
                    val curTrim = lines[i].trim()
                    val tMatch = Regex("^[-*+]\\s+\\[([ xX])\\]\\s+(.*)$").find(curTrim)
                    if (tMatch == null) break
                    val checked = tMatch.groupValues[1].lowercase() == "x"
                    val content = tMatch.groupValues[2]
                    taskItems.add(
                        TaskItem(
                            id = "task_${blocks.size}_$i",
                            isChecked = checked,
                            content = parseInlineSpans(content),
                            lineIndex = i
                        )
                    )
                    i++
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

            // 18. Bullet List: - item, * item, + item
            if (trimmed.matches(Regex("^[-*+]\\s+.*$"))) {
                val bulletItems = mutableListOf<BulletItem>()
                while (i < lines.size) {
                    val curLine = lines[i]
                    val curTrim = curLine.trim()
                    val bMatch = Regex("^([-*+])\\s+(.*)$").find(curTrim)
                    if (bMatch == null) {
                        if (curTrim.isEmpty()) {
                            i++
                            continue
                        } else break
                    }
                    val indent = curLine.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
                    val level = indent / 2
                    val content = bMatch.groupValues[2]
                    bulletItems.add(
                        BulletItem(
                            level = level,
                            content = parseInlineSpans(content),
                            rawIndex = i
                        )
                    )
                    i++
                }
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

            // 19. Numbered List: 1. item, 2. item
            if (trimmed.matches(Regex("^\\d+[.)]\\s+.*$"))) {
                val numberedItems = mutableListOf<NumberedItem>()
                while (i < lines.size) {
                    val curLine = lines[i]
                    val curTrim = curLine.trim()
                    val nMatch = Regex("^(\\d+)[.)]\\s+(.*)$").find(curTrim)
                    if (nMatch == null) {
                        if (curTrim.isEmpty()) {
                            i++
                            continue
                        } else break
                    }
                    val indent = curLine.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
                    val level = indent / 2
                    val number = nMatch.groupValues[1]
                    val content = nMatch.groupValues[2]
                    numberedItems.add(
                        NumberedItem(
                            level = level,
                            number = number,
                            content = parseInlineSpans(content),
                            rawIndex = i
                        )
                    )
                    i++
                }
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

            // 20. Markdown Table: | Col 1 | Col 2 |
            if (trimmed.startsWith("|") && trimmed.endsWith("|") && i + 1 < lines.size && lines[i + 1].trim().matches(Regex("^\\|[\\s:|-]+\\|$"))) {
                val headerCells = splitTableCells(line)
                val sepCells = splitTableCells(lines[i + 1])
                val alignments = sepCells.map { cell ->
                    val clean = cell.trim()
                    when {
                        clean.startsWith(":") && clean.endsWith(":") -> TableAlignment.CENTER
                        clean.endsWith(":") -> TableAlignment.RIGHT
                        else -> TableAlignment.LEFT
                    }
                }

                val rows = mutableListOf<List<InlineSpanGroup>>()
                i += 2
                while (i < lines.size) {
                    val rowLine = lines[i].trim()
                    if (!rowLine.startsWith("|") || !rowLine.endsWith("|")) break
                    val cells = splitTableCells(rowLine)
                    rows.add(cells.map { parseInlineSpans(it) })
                    i++
                }

                blocks.add(
                    MarkdownBlock.TableBlock(
                        headers = headerCells.map { parseInlineSpans(it) },
                        alignments = alignments,
                        rows = rows,
                        blockId = "tbl_${blocks.size}_$startLine",
                        lineStart = startLine,
                        lineEnd = i - 1
                    )
                )
                continue
            }

            // 21. Normal Paragraph or HTML inline/block
            val paragraphBuilder = StringBuilder()
            val paragraphStartLine = i
            var hasHtml = false

            while (i < lines.size) {
                val currentLine = lines[i]
                val curTrim = currentLine.trim()

                // Only check breaking condition after consuming at least one line
                if (i > paragraphStartLine) {
                    if (curTrim.isEmpty() ||
                        curTrim.startsWith("#") ||
                        curTrim.startsWith("```") ||
                        curTrim.startsWith("~~~") ||
                        curTrim.startsWith(">") ||
                        curTrim.startsWith("$$") ||
                        curTrim.startsWith("<style", ignoreCase = true) ||
                        curTrim.startsWith("<pre", ignoreCase = true) ||
                        curTrim.startsWith("<table", ignoreCase = true) ||
                        curTrim.startsWith("<ul", ignoreCase = true) ||
                        curTrim.startsWith("<ol", ignoreCase = true) ||
                        curTrim.startsWith("<blockquote", ignoreCase = true) ||
                        curTrim.matches(Regex("^<h[1-6]\\b", RegexOption.IGNORE_CASE)) ||
                        curTrim.matches(Regex("^<(?:p|div|section|article|main|header|footer|aside|center)\\b", RegexOption.IGNORE_CASE)) ||
                        curTrim.startsWith("<hr", ignoreCase = true) ||
                        curTrim.matches(Regex("^(\\*{3,}|-{3,}|_{3,})$")) ||
                        curTrim.matches(Regex("^[-*+]\\s+.*$")) ||
                        curTrim.matches(Regex("^\\d+[.)]\\s+.*$")) ||
                        (curTrim.startsWith("|") && curTrim.endsWith("|") && i + 1 < lines.size && lines[i + 1].trim().matches(Regex("^\\|[\\s:|-]+\\|$")))
                    ) {
                        break
                    }
                }

                if (curTrim.contains("<")) hasHtml = true
                if (paragraphBuilder.isNotEmpty()) paragraphBuilder.append(" ")
                paragraphBuilder.append(curTrim)
                i++
            }

            val pText = paragraphBuilder.toString().trim()
            if (pText.isNotEmpty()) {
                blocks.add(
                    MarkdownBlock.ParagraphBlock(
                        content = parseInlineSpans(pText),
                        blockId = "p_${blocks.size}_$paragraphStartLine",
                        lineStart = paragraphStartLine,
                        lineEnd = (i - 1).coerceAtLeast(paragraphStartLine),
                        isHtml = hasHtml
                    )
                )
            }

            // Inviolable guarantee: i must always advance
            if (i == startLine) {
                i++
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

    private fun parseHtmlTable(fullTable: String, blockIndex: Int, startLine: Int, endLine: Int): MarkdownBlock.TableBlock? {
        val rowRegex = Regex("<tr[^>]*>([\\s\\S]*?)</tr>", RegexOption.IGNORE_CASE)
        val trMatches = rowRegex.findAll(fullTable).toList()
        if (trMatches.isEmpty()) return null

        val headerSpans = mutableListOf<InlineSpanGroup>()
        val alignments = mutableListOf<TableAlignment>()
        val dataRows = mutableListOf<List<InlineSpanGroup>>()

        val firstTr = trMatches[0].groupValues[1]
        val thRegex = Regex("<th([^>]*)>([\\s\\S]*?)</th>", RegexOption.IGNORE_CASE)
        val thMatches = thRegex.findAll(firstTr).toList()

        var dataStartRow = 0
        if (thMatches.isNotEmpty()) {
            thMatches.forEach { thMatch ->
                val attrs = thMatch.groupValues[1]
                val content = thMatch.groupValues[2].trim()
                val align = parseAlignment(attrs)
                alignments.add(align)
                headerSpans.add(parseInlineSpans(unescapeHtml(content)))
            }
            dataStartRow = 1
        }

        val tdRegex = Regex("<td([^>]*)>([\\s\\S]*?)</td>", RegexOption.IGNORE_CASE)
        for (rowIndex in dataStartRow until trMatches.size) {
            val trContent = trMatches[rowIndex].groupValues[1]
            val tdMatches = tdRegex.findAll(trContent).toList()
            if (tdMatches.isNotEmpty()) {
                val rowCells = tdMatches.map { tdMatch ->
                    val content = tdMatch.groupValues[2].trim()
                    parseInlineSpans(unescapeHtml(content))
                }
                dataRows.add(rowCells)
            }
        }

        if (headerSpans.isEmpty()) {
            val numCols = dataRows.firstOrNull()?.size ?: 1
            for (c in 1..numCols) {
                headerSpans.add(parseInlineSpans("Col $c"))
                alignments.add(TableAlignment.LEFT)
            }
        }

        return MarkdownBlock.TableBlock(
            headers = headerSpans,
            alignments = alignments,
            rows = dataRows,
            blockId = "tbl_html_${blockIndex}_$startLine",
            lineStart = startLine,
            lineEnd = endLine,
            isHtml = true
        )
    }

    private fun parseAlignment(attrString: String): TableAlignment {
        val alignMatch = Regex("align=[\"'](left|center|right)[\"']", RegexOption.IGNORE_CASE).find(attrString)
        if (alignMatch != null) {
            return when (alignMatch.groupValues[1].lowercase()) {
                "center" -> TableAlignment.CENTER
                "right" -> TableAlignment.RIGHT
                else -> TableAlignment.LEFT
            }
        }
        val styleMatch = Regex("text-align\\s*:\\s*(left|center|right)", RegexOption.IGNORE_CASE).find(attrString)
        if (styleMatch != null) {
            return when (styleMatch.groupValues[1].lowercase()) {
                "center" -> TableAlignment.CENTER
                "right" -> TableAlignment.RIGHT
                else -> TableAlignment.LEFT
            }
        }
        return TableAlignment.LEFT
    }

    private fun parseHtmlList(
        listLines: List<Pair<Int, String>>,
        isOrdered: Boolean,
        blockIndex: Int,
        startLine: Int,
        endLine: Int
    ): MarkdownBlock? {
        val fullListText = listLines.joinToString("\n") { it.second }
        val liRegex = Regex("<li[^>]*>([\\s\\S]*?)</li>", RegexOption.IGNORE_CASE)
        val matches = liRegex.findAll(fullListText).toList()
        if (matches.isEmpty()) return null

        val hasCheckboxes = matches.any { it.groupValues[1].contains("<input", ignoreCase = true) }

        if (hasCheckboxes) {
            val taskItems = mutableListOf<TaskItem>()
            matches.forEachIndexed { idx, match ->
                val liContent = match.groupValues[1]
                val isChecked = liContent.contains("checked", ignoreCase = true)
                val cleanContent = liContent.replace(Regex("<input[^>]*>", RegexOption.IGNORE_CASE), "").trim()

                // Approximate the line index
                val itemLineIndex = listLines.getOrNull(idx + 1)?.first ?: (startLine + idx + 1)

                taskItems.add(
                    TaskItem(
                        id = "task_html_${blockIndex}_$idx",
                        isChecked = isChecked,
                        content = parseInlineSpans(unescapeHtml(cleanContent)),
                        lineIndex = itemLineIndex
                    )
                )
            }
            return MarkdownBlock.TaskListBlock(
                items = taskItems,
                blockId = "tasklist_html_${blockIndex}_$startLine",
                lineStart = startLine,
                lineEnd = endLine,
                isHtml = true
            )
        } else if (isOrdered) {
            val numberedItems = matches.mapIndexed { idx, match ->
                val clean = match.groupValues[1].trim()
                NumberedItem(
                    level = 0,
                    number = "${idx + 1}",
                    content = parseInlineSpans(unescapeHtml(clean)),
                    rawIndex = startLine + idx
                )
            }
            return MarkdownBlock.NumberedListBlock(
                items = numberedItems,
                blockId = "numbered_html_${blockIndex}_$startLine",
                lineStart = startLine,
                lineEnd = endLine,
                isHtml = true
            )
        } else {
            val bulletItems = matches.mapIndexed { idx, match ->
                val clean = match.groupValues[1].trim()
                BulletItem(
                    level = 0,
                    content = parseInlineSpans(unescapeHtml(clean)),
                    rawIndex = startLine + idx
                )
            }
            return MarkdownBlock.BulletListBlock(
                items = bulletItems,
                blockId = "bullet_html_${blockIndex}_$startLine",
                lineStart = startLine,
                lineEnd = endLine,
                isHtml = true
            )
        }
    }

    private fun splitTableCells(line: String): List<String> {
        val trimmed = line.trim().removePrefix("|").removeSuffix("|")
        return trimmed.split("|").map { it.trim() }
    }

    fun parseInlineSpans(text: String): InlineSpanGroup {
        if (text.isEmpty()) return InlineSpanGroup(emptyList(), "")
        val spans = mutableListOf<InlineSpan>()

        // Combines Markdown and HTML inline tokens without catastrophic backtracking
        val pattern = Regex(
            "(`[^`\n]+`" +
            "|\\[[^\\]\n]+\\]\\([^)\n]+\\)" +
            "|\\$\\$?[^$\n]+\\$\\$?" +
            "|\\*\\*\\*[^*\n]+\\*\\*\\*" +
            "|___[^_\n]+___" +
            "|\\*\\*[^*\n]+\\*\\*" +
            "|__[^_\n]+__" +
            "|~~[^~\n]+~~" +
            "|\\*[^*\n]+\\*" +
            "|_[^_\n]+_" +
            "|<span\\b[^>]*>.*?</span>" +
            "|<a\\b[^>]*>.*?</a>" +
            "|<(?:strong|b)\\b[^>]*>.*?</(?:strong|b)>" +
            "|<(?:em|i)\\b[^>]*>.*?</(?:em|i)>" +
            "|<(?:u|ins)\\b[^>]*>.*?</(?:u|ins)>" +
            "|<(?:s|del|strike)\\b[^>]*>.*?</(?:s|del|strike)>" +
            "|<code\\b[^>]*>.*?</code>" +
            "|<mark\\b[^>]*>.*?</mark>" +
            "|<br\\s*/?>" +
            "|<[^>]+>)",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        var currentIndex = 0
        pattern.findAll(text).forEach { match ->
            if (match.range.first > currentIndex) {
                val plain = text.substring(currentIndex, match.range.first)
                val cleanPlain = unescapeHtml(plain)
                if (cleanPlain.isNotEmpty()) {
                    spans.add(InlineSpan.Text(content = cleanPlain))
                }
            }

            val token = match.value
            when {
                // HTML <span style="...">
                token.startsWith("<span", ignoreCase = true) -> {
                    val styleVal = Regex("style=[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE).find(token)?.groupValues?.get(1) ?: ""
                    val inner = token
                        .replace(Regex("^<span[^>]*>", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("</span>$", RegexOption.IGNORE_CASE), "")
                    val color = extractStyleProperty(styleVal, "color")
                    val bg = extractStyleProperty(styleVal, "background-color") ?: extractStyleProperty(styleVal, "background")
                    val isBold = styleVal.contains("font-weight\\s*:\\s*bold".toRegex(RegexOption.IGNORE_CASE))
                    val isItalic = styleVal.contains("font-style\\s*:\\s*italic".toRegex(RegexOption.IGNORE_CASE))
                    val isUnderline = styleVal.contains("text-decoration\\s*:\\s*underline".toRegex(RegexOption.IGNORE_CASE))
                    val isStrike = styleVal.contains("text-decoration\\s*:\\s*line-through".toRegex(RegexOption.IGNORE_CASE))
                    val cleanInner = unescapeHtml(inner.replace(Regex("<[^>]+>"), ""))
                    if (cleanInner.isNotEmpty()) {
                        spans.add(
                            InlineSpan.Text(
                                content = cleanInner,
                                isBold = isBold,
                                isItalic = isItalic,
                                isUnderline = isUnderline,
                                isStrike = isStrike,
                                colorHex = color,
                                bgHex = bg
                            )
                        )
                    }
                }

                // HTML <a href="...">
                token.startsWith("<a", ignoreCase = true) -> {
                    val href = Regex("href=[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE).find(token)?.groupValues?.get(1) ?: ""
                    val inner = token
                        .replace(Regex("^<a[^>]*>", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("</a>$", RegexOption.IGNORE_CASE), "")
                    val cleanInner = unescapeHtml(inner.replace(Regex("<[^>]+>"), ""))
                    spans.add(InlineSpan.Link(text = cleanInner, url = href))
                }

                // HTML <b> or <strong>
                token.startsWith("<b", ignoreCase = true) || token.startsWith("<strong", ignoreCase = true) -> {
                    val inner = token
                        .replace(Regex("^<(?:strong|b)[^>]*>", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("</(?:strong|b)>$", RegexOption.IGNORE_CASE), "")
                    val cleanInner = unescapeHtml(inner.replace(Regex("<[^>]+>"), ""))
                    if (cleanInner.isNotEmpty()) {
                        spans.add(InlineSpan.Text(content = cleanInner, isBold = true))
                    }
                }

                // HTML <i> or <em>
                token.startsWith("<i", ignoreCase = true) || token.startsWith("<em", ignoreCase = true) -> {
                    val inner = token
                        .replace(Regex("^<(?:em|i)[^>]*>", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("</(?:em|i)>$", RegexOption.IGNORE_CASE), "")
                    val cleanInner = unescapeHtml(inner.replace(Regex("<[^>]+>"), ""))
                    if (cleanInner.isNotEmpty()) {
                        spans.add(InlineSpan.Text(content = cleanInner, isItalic = true))
                    }
                }

                // HTML <u> or <ins>
                token.startsWith("<u", ignoreCase = true) || token.startsWith("<ins", ignoreCase = true) -> {
                    val inner = token
                        .replace(Regex("^<(?:u|ins)[^>]*>", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("</(?:u|ins)>$", RegexOption.IGNORE_CASE), "")
                    val cleanInner = unescapeHtml(inner.replace(Regex("<[^>]+>"), ""))
                    if (cleanInner.isNotEmpty()) {
                        spans.add(InlineSpan.Text(content = cleanInner, isUnderline = true))
                    }
                }

                // HTML <s> or <del> or <strike>
                token.startsWith("<s", ignoreCase = true) || token.startsWith("<del", ignoreCase = true) || token.startsWith("<strike", ignoreCase = true) -> {
                    val inner = token
                        .replace(Regex("^<(?:s|del|strike)[^>]*>", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("</(?:s|del|strike)>$", RegexOption.IGNORE_CASE), "")
                    val cleanInner = unescapeHtml(inner.replace(Regex("<[^>]+>"), ""))
                    if (cleanInner.isNotEmpty()) {
                        spans.add(InlineSpan.Text(content = cleanInner, isStrike = true))
                    }
                }

                // HTML <code>
                token.startsWith("<code", ignoreCase = true) -> {
                    val inner = token
                        .replace(Regex("^<code[^>]*>", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("</code>$", RegexOption.IGNORE_CASE), "")
                    spans.add(InlineSpan.InlineCode(code = unescapeHtml(inner.replace(Regex("<[^>]+>"), ""))))
                }

                // HTML <mark>
                token.startsWith("<mark", ignoreCase = true) -> {
                    val inner = token
                        .replace(Regex("^<mark[^>]*>", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("</mark>$", RegexOption.IGNORE_CASE), "")
                    val cleanInner = unescapeHtml(inner.replace(Regex("<[^>]+>"), ""))
                    if (cleanInner.isNotEmpty()) {
                        spans.add(InlineSpan.Text(content = cleanInner, bgHex = "#fef08a"))
                    }
                }

                // HTML <br>
                token.startsWith("<br", ignoreCase = true) -> {
                    spans.add(InlineSpan.Text(content = "\n"))
                }

                // Markdown Inline Code: `code`
                token.startsWith("`") && token.endsWith("`") -> {
                    spans.add(InlineSpan.InlineCode(code = token.removeSurrounding("`")))
                }

                // Markdown Link: [text](url)
                token.startsWith("[") && token.contains("](") && token.endsWith(")") -> {
                    val linkText = token.substringAfter("[").substringBefore("](")
                    val url = token.substringAfter("](").substringBeforeLast(")")
                    spans.add(InlineSpan.Link(text = linkText, url = url))
                }

                // Markdown Math: $math$
                token.startsWith("$") && token.endsWith("$") -> {
                    val latex = token.removeSurrounding("$").trim()
                    spans.add(InlineSpan.InlineMath(latex = latex))
                }

                // Markdown Bold + Italic: ***text*** or ___text___
                (token.startsWith("***") && token.endsWith("***")) || (token.startsWith("___") && token.endsWith("___")) -> {
                    val inner = if (token.startsWith("***")) token.removeSurrounding("***") else token.removeSurrounding("___")
                    spans.add(InlineSpan.Text(content = inner, isBold = true, isItalic = true))
                }

                // Markdown Bold: **text** or __text__
                (token.startsWith("**") && token.endsWith("**")) || (token.startsWith("__") && token.endsWith("__")) -> {
                    val inner = if (token.startsWith("**")) token.removeSurrounding("**") else token.removeSurrounding("__")
                    spans.add(InlineSpan.Text(content = inner, isBold = true))
                }

                // Markdown Strike: ~~text~~
                token.startsWith("~~") && token.endsWith("~~") -> {
                    spans.add(InlineSpan.Text(content = token.removeSurrounding("~~"), isStrike = true))
                }

                // Markdown Italic: *text* or _text_
                (token.startsWith("*") && token.endsWith("*")) || (token.startsWith("_") && token.endsWith("_")) -> {
                    val inner = if (token.startsWith("*")) token.removeSurrounding("*") else token.removeSurrounding("_")
                    spans.add(InlineSpan.Text(content = inner, isItalic = true))
                }

                // Any other HTML tag: cleanly strip it (e.g. stray <div>, </div>, <font>, etc.)
                token.startsWith("<") && token.endsWith(">") -> {
                    // Stripped from visual output
                }

                else -> {
                    val clean = unescapeHtml(token)
                    if (clean.isNotEmpty()) {
                        spans.add(InlineSpan.Text(content = clean))
                    }
                }
            }
            currentIndex = match.range.last + 1
        }

        if (currentIndex < text.length) {
            val remaining = text.substring(currentIndex)
            val cleanRemaining = unescapeHtml(remaining)
            if (cleanRemaining.isNotEmpty()) {
                spans.add(InlineSpan.Text(content = cleanRemaining))
            }
        }

        return InlineSpanGroup(spans = spans, rawText = text)
    }

    private fun extractStyleProperty(styleStr: String, propName: String): String? {
        val regex = Regex("(?:^|;)\\s*${Regex.escape(propName)}\\s*:\\s*([^;]+)", RegexOption.IGNORE_CASE)
        return regex.find(styleStr)?.groupValues?.get(1)?.trim()
    }

    private fun unescapeHtml(text: String): String {
        return text.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&nbsp;", " ")
    }
}
