package com.example.util

/**
 * Formats Markdown ASCII tables with even column widths and aligned dividers.
 */
object MarkdownTableFormatter {

    fun formatAllTables(input: String): String {
        val lines = input.lines()
        val result = mutableListOf<String>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            if (isTableCandidate(line) && i + 1 < lines.size && isTableDivider(lines[i + 1])) {
                // Collect table block
                val tableLines = mutableListOf<String>()
                while (i < lines.size && isTableCandidate(lines[i])) {
                    tableLines.add(lines[i])
                    i++
                }
                result.add(formatSingleTable(tableLines))
            } else {
                result.add(line)
                i++
            }
        }

        return result.joinToString("\n")
    }

    private fun isTableCandidate(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("|") || trimmed.contains("|")
    }

    private fun isTableDivider(line: String): Boolean {
        val trimmed = line.trim()
        if (!trimmed.contains("|") && !trimmed.contains("-")) return false
        val cells = parseCells(line)
        return cells.isNotEmpty() && cells.all { cell ->
            val clean = cell.trim()
            clean.isNotEmpty() && clean.all { it == '-' || it == ':' }
        }
    }

    private fun parseCells(line: String): List<String> {
        var content = line.trim()
        if (content.startsWith("|")) content = content.substring(1)
        if (content.endsWith("|")) content = content.substring(0, content.length - 1)
        return content.split("|").map { it.trim() }
    }

    private fun formatSingleTable(tableLines: List<String>): String {
        if (tableLines.size < 2) return tableLines.joinToString("\n")

        val parsedRows = tableLines.map { parseCells(it) }
        val headerRow = parsedRows[0]
        val dividerRow = parsedRows[1]
        val dataRows = if (parsedRows.size > 2) parsedRows.subList(2, parsedRows.size) else emptyList()

        val numCols = parsedRows.maxOfOrNull { it.size } ?: return tableLines.joinToString("\n")
        if (numCols == 0) return tableLines.joinToString("\n")

        // Compute max width per column
        val colWidths = IntArray(numCols) { 3 } // minimum 3 for '---'
        for (row in parsedRows) {
            for (col in 0 until numCols) {
                if (col < row.size) {
                    colWidths[col] = maxOf(colWidths[col], row[col].length)
                }
            }
        }

        // Alignments from divider row
        val alignments = (0 until numCols).map { col ->
            if (col < dividerRow.size) {
                val d = dividerRow[col].trim()
                val left = d.startsWith(":")
                val right = d.endsWith(":")
                when {
                    left && right -> AlignmentType.CENTER
                    right -> AlignmentType.RIGHT
                    else -> AlignmentType.LEFT
                }
            } else {
                AlignmentType.LEFT
            }
        }

        val formattedLines = mutableListOf<String>()

        // Format header
        val headerFormatted = (0 until numCols).map { col ->
            val cell = if (col < headerRow.size) headerRow[col] else ""
            padCell(cell, colWidths[col], alignments[col])
        }.joinToString(" | ", prefix = "| ", postfix = " |")
        formattedLines.add(headerFormatted)

        // Format divider
        val dividerFormatted = (0 until numCols).map { col ->
            val width = colWidths[col]
            when (alignments[col]) {
                AlignmentType.CENTER -> ":" + "-".repeat(maxOf(1, width - 2)) + ":"
                AlignmentType.RIGHT -> "-".repeat(maxOf(2, width - 1)) + ":"
                AlignmentType.LEFT -> ":---".padEnd(width, '-')
            }
        }.joinToString(" | ", prefix = "| ", postfix = " |")
        formattedLines.add(dividerFormatted)

        // Format data rows
        for (row in dataRows) {
            val rowFormatted = (0 until numCols).map { col ->
                val cell = if (col < row.size) row[col] else ""
                padCell(cell, colWidths[col], alignments[col])
            }.joinToString(" | ", prefix = "| ", postfix = " |")
            formattedLines.add(rowFormatted)
        }

        return formattedLines.joinToString("\n")
    }

    private fun padCell(cell: String, width: Int, alignment: AlignmentType): String {
        val padTotal = maxOf(0, width - cell.length)
        return when (alignment) {
            AlignmentType.LEFT -> cell + " ".repeat(padTotal)
            AlignmentType.RIGHT -> " ".repeat(padTotal) + cell
            AlignmentType.CENTER -> {
                val left = padTotal / 2
                val right = padTotal - left
                " ".repeat(left) + cell + " ".repeat(right)
            }
        }
    }

    private enum class AlignmentType {
        LEFT, CENTER, RIGHT
    }
}
