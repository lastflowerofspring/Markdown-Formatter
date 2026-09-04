package com.example.util

object HtmlCssFormatter {

    private val VOID_TAGS = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input",
        "link", "meta", "param", "source", "track", "wbr"
    )

    private val INLINE_TAGS = setOf(
        "b", "strong", "i", "em", "u", "s", "strike", "del",
        "span", "a", "code", "mark", "small", "sub", "sup", "font"
    )

    fun isHtmlOrCss(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.startsWith("<!DOCTYPE", ignoreCase = true) ||
            trimmed.startsWith("<html", ignoreCase = true) ||
            trimmed.startsWith("<body", ignoreCase = true) ||
            trimmed.startsWith("<div", ignoreCase = true) ||
            trimmed.startsWith("<style", ignoreCase = true) ||
            trimmed.startsWith("<table", ignoreCase = true) ||
            trimmed.startsWith("<p", ignoreCase = true) ||
            trimmed.startsWith("<h1", ignoreCase = true)
        ) {
            return true
        }
        val tagCount = Regex("<[a-zA-Z/][^>]*>").findAll(text).count()
        return tagCount >= 3
    }

    fun formatDocument(text: String): String {
        return if (isHtmlOrCss(text)) {
            formatHtml(text)
        } else {
            // Document might have embedded HTML or CSS blocks
            formatMixedMarkdownHtml(text)
        }
    }

    fun formatHtml(html: String): String {
        if (html.isBlank()) return html

        // 1. First format embedded <style> blocks
        val styleRegex = Regex("(<style[^>]*>)([\\s\\S]*?)(</style>)", RegexOption.IGNORE_CASE)
        val withFormattedStyles = styleRegex.replace(html) { match ->
            val openTag = match.groupValues[1]
            val cssContent = match.groupValues[2]
            val closeTag = match.groupValues[3]
            val formattedCss = formatCss(cssContent, baseIndent = 1)
            "$openTag\n$formattedCss\n$closeTag"
        }

        // 2. Tokenize tags and text
        val tokenRegex = Regex("(<!--[\\s\\S]*?-->|<[^>]+>|[^<]+)")
        val tokens = tokenRegex.findAll(withFormattedStyles).map { it.value }.filter { it.isNotBlank() }.toList()

        val sb = StringBuilder()
        var indentLevel = 0

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i].trim()
            if (token.isEmpty()) {
                i++
                continue
            }

            // Comment
            if (token.startsWith("<!--")) {
                sb.append("  ".repeat(indentLevel)).append(token).append("\n")
                i++
                continue
            }

            // Closing tag </tag>
            if (token.startsWith("</")) {
                val tagName = token.removePrefix("</").removeSuffix(">").trim().lowercase()
                if (tagName !in INLINE_TAGS) {
                    indentLevel = (indentLevel - 1).coerceAtLeast(0)
                }
                sb.append("  ".repeat(indentLevel)).append(token).append("\n")
                i++
                continue
            }

            // Opening or self-closing tag <tag ...> or <tag .../>
            if (token.startsWith("<") && !token.startsWith("<!")) {
                val isSelfClosing = token.endsWith("/>")
                val tagNameMatch = Regex("^<([a-zA-Z0-9_-]+)").find(token)
                val tagName = tagNameMatch?.groupValues?.get(1)?.lowercase() ?: ""
                val isVoid = VOID_TAGS.contains(tagName) || isSelfClosing

                // Check if inline tag with plain content and closing tag on same line: e.g. <p>text</p> or <h1>text</h1>
                if (i + 2 < tokens.size &&
                    !tokens[i + 1].startsWith("<") &&
                    tokens[i + 2].equals("</$tagName>", ignoreCase = true) &&
                    (tagName in listOf("p", "h1", "h2", "h3", "h4", "h5", "h6", "title", "th", "td", "li", "span", "b", "strong", "i", "em", "a"))
                ) {
                    sb.append("  ".repeat(indentLevel))
                        .append(token)
                        .append(tokens[i + 1].trim())
                        .append(tokens[i + 2].trim())
                        .append("\n")
                    i += 3
                    continue
                }

                sb.append("  ".repeat(indentLevel)).append(token).append("\n")
                if (!isVoid && tagName !in INLINE_TAGS) {
                    indentLevel++
                }
                i++
                continue
            }

            // Plain text or doctype
            if (token.startsWith("<!")) {
                sb.append(token).append("\n")
            } else {
                sb.append("  ".repeat(indentLevel)).append(token).append("\n")
            }
            i++
        }

        return sb.toString().trimEnd()
    }

    fun formatCss(css: String, baseIndent: Int = 0): String {
        if (css.isBlank()) return css
        val baseIndentStr = "  ".repeat(baseIndent)
        val propIndentStr = "  ".repeat(baseIndent + 1)

        val clean = css.replace(Regex("/\\*([\\s\\S]*?)\\*/")) { "\n/* ${it.groupValues[1].trim()} */\n" }
        val rules = clean.split("}")
        val sb = StringBuilder()

        for (rawRule in rules) {
            val rule = rawRule.trim()
            if (rule.isEmpty()) continue

            if (rule.contains("{")) {
                val parts = rule.split("{", limit = 2)
                val selector = parts[0].trim()
                val body = parts[1].trim()

                sb.append(baseIndentStr).append(selector).append(" {\n")

                val declarations = body.split(";").map { it.trim() }.filter { it.isNotEmpty() }
                for (decl in declarations) {
                    if (decl.contains(":")) {
                        val propParts = decl.split(":", limit = 2)
                        val prop = propParts[0].trim()
                        val value = propParts[1].trim()
                        sb.append(propIndentStr).append(prop).append(": ").append(value).append(";\n")
                    } else if (decl.isNotEmpty()) {
                        sb.append(propIndentStr).append(decl).append(";\n")
                    }
                }
                sb.append(baseIndentStr).append("}\n\n")
            } else {
                sb.append(baseIndentStr).append(rule).append("\n")
            }
        }

        return sb.toString().trimEnd()
    }

    private fun formatMixedMarkdownHtml(text: String): String {
        // Formats <style>...</style> and <table>...</table> in markdown
        var result = text
        val styleRegex = Regex("(<style[^>]*>)([\\s\\S]*?)(</style>)", RegexOption.IGNORE_CASE)
        result = styleRegex.replace(result) { match ->
            "${match.groupValues[1]}\n${formatCss(match.groupValues[2], baseIndent = 1)}\n${match.groupValues[3]}"
        }
        val tableRegex = Regex("(<table[^>]*>[\\s\\S]*?</table>)", RegexOption.IGNORE_CASE)
        result = tableRegex.replace(result) { match ->
            formatHtml(match.groupValues[1])
        }
        return result
    }
}
