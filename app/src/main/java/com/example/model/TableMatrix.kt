package com.example.model

data class TableMatrix(
    val headers: List<String>,
    val alignments: List<TableAlignment>,
    val rows: List<List<String>>
) {
    val columnCount: Int
        get() = maxOf(headers.size, rows.maxOfOrNull { it.size } ?: 0, 1)

    val rowCount: Int
        get() = rows.size

    fun getCell(rowIdx: Int, colIdx: Int): String {
        return if (rowIdx == -1) {
            headers.getOrElse(colIdx) { "" }
        } else {
            rows.getOrNull(rowIdx)?.getOrElse(colIdx) { "" } ?: ""
        }
    }

    fun updateCell(rowIdx: Int, colIdx: Int, newText: String): TableMatrix {
        val numCols = columnCount
        if (rowIdx == -1) {
            val newHeaders = (0 until numCols).map { c ->
                if (c == colIdx) newText else headers.getOrElse(c) { "" }
            }
            return copy(headers = newHeaders)
        } else {
            val newRows = rows.mapIndexed { r, row ->
                if (r == rowIdx) {
                    (0 until numCols).map { c ->
                        if (c == colIdx) newText else row.getOrElse(c) { "" }
                    }
                } else {
                    row
                }
            }
            return copy(rows = newRows)
        }
    }

    fun addRow(afterRowIdx: Int = rows.size - 1): TableMatrix {
        val numCols = columnCount
        val emptyRow = List(numCols) { "" }
        val newRows = rows.toMutableList()
        val insertAt = (afterRowIdx + 1).coerceIn(0, newRows.size)
        newRows.add(insertAt, emptyRow)
        return copy(rows = newRows)
    }

    fun deleteRow(rowIdx: Int): TableMatrix {
        if (rows.size <= 1) return this
        val newRows = rows.toMutableList()
        if (rowIdx in newRows.indices) {
            newRows.removeAt(rowIdx)
        }
        return copy(rows = newRows)
    }

    fun addColumn(afterColIdx: Int = columnCount - 1): TableMatrix {
        val insertAt = (afterColIdx + 1).coerceIn(0, columnCount)
        val newHeaders = headers.toMutableList().apply { add(insertAt, "Col ${size + 1}") }
        val newAlignments = alignments.toMutableList().apply { add(insertAt, TableAlignment.LEFT) }
        val newRows = rows.map { row ->
            row.toMutableList().apply { add(insertAt, "") }
        }
        return copy(headers = newHeaders, alignments = newAlignments, rows = newRows)
    }

    fun deleteColumn(colIdx: Int): TableMatrix {
        if (columnCount <= 1) return this
        val newHeaders = headers.toMutableList().apply { if (colIdx in indices) removeAt(colIdx) }
        val newAlignments = alignments.toMutableList().apply { if (colIdx in indices) removeAt(colIdx) }
        val newRows = rows.map { row ->
            row.toMutableList().apply { if (colIdx in indices) removeAt(colIdx) }
        }
        return copy(headers = newHeaders, alignments = newAlignments, rows = newRows)
    }

    fun toggleAlignment(colIdx: Int): TableMatrix {
        val newAlignments = (0 until columnCount).map { c ->
            val cur = alignments.getOrElse(c) { TableAlignment.LEFT }
            if (c == colIdx) {
                when (cur) {
                    TableAlignment.LEFT -> TableAlignment.CENTER
                    TableAlignment.CENTER -> TableAlignment.RIGHT
                    TableAlignment.RIGHT -> TableAlignment.LEFT
                }
            } else {
                cur
            }
        }
        return copy(alignments = newAlignments)
    }

    fun toMarkdown(): String {
        val numCols = columnCount
        val headerStrings = (0 until numCols).map { c -> headers.getOrElse(c) { "Col ${c + 1}" }.replace("\n", "<br>") }
        val alignStrings = (0 until numCols).map { c ->
            when (alignments.getOrElse(c) { TableAlignment.LEFT }) {
                TableAlignment.LEFT -> ":---"
                TableAlignment.CENTER -> ":---:"
                TableAlignment.RIGHT -> "---:"
            }
        }

        val sb = StringBuilder()
        sb.append("| ").append(headerStrings.joinToString(" | ")).append(" |\n")
        sb.append("| ").append(alignStrings.joinToString(" | ")).append(" |\n")

        rows.forEach { row ->
            val rowStrings = (0 until numCols).map { c -> row.getOrElse(c) { "" }.replace("\n", "<br>") }
            sb.append("| ").append(rowStrings.joinToString(" | ")).append(" |\n")
        }

        return sb.toString().trimEnd()
    }

    fun toHtml(): String {
        val numCols = columnCount
        val sb = StringBuilder()
        sb.append("<table>\n")
        if (headers.isNotEmpty()) {
            sb.append("  <thead>\n    <tr>\n")
            (0 until numCols).forEach { c ->
                val h = headers.getOrElse(c) { "Col ${c + 1}" }
                val alignAttr = when (alignments.getOrElse(c) { TableAlignment.LEFT }) {
                    TableAlignment.CENTER -> " align=\"center\""
                    TableAlignment.RIGHT -> " align=\"right\""
                    TableAlignment.LEFT -> ""
                }
                sb.append("      <th$alignAttr>$h</th>\n")
            }
            sb.append("    </tr>\n  </thead>\n")
        }
        if (rows.isNotEmpty()) {
            sb.append("  <tbody>\n")
            rows.forEach { row ->
                sb.append("    <tr>\n")
                (0 until numCols).forEach { c ->
                    val cell = row.getOrElse(c) { "" }
                    val alignAttr = when (alignments.getOrElse(c) { TableAlignment.LEFT }) {
                        TableAlignment.CENTER -> " align=\"center\""
                        TableAlignment.RIGHT -> " align=\"right\""
                        TableAlignment.LEFT -> ""
                    }
                    sb.append("      <td$alignAttr>$cell</td>\n")
                }
                sb.append("    </tr>\n")
            }
            sb.append("  </tbody>\n")
        }
        sb.append("</table>")
        return sb.toString()
    }
}
