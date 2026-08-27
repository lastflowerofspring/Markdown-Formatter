package com.example.util

/**
 * Utility functions for smart markdown text cleaning, repair, and normalization.
 */
object MarkdownSanitizer {

    /**
     * Decode HTML entities commonly found when pasting from AI chatbots, web browsers, or rich text.
     */
    fun decodeHtmlEntities(input: String): String {
        return input
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace("&copy;", "©")
            .replace("&reg;", "®")
            .replace("&trade;", "™")
            .replace("&hellip;", "…")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
    }

    /**
     * Fixes unclosed markdown blocks like unclosed code fences, blockquotes, and LaTeX equations.
     */
    fun repairUnclosedDelimiters(input: String): String {
        var result = input
        val lines = result.lines()

        // 1. Code fences check
        var inCodeFence = false
        for (line in lines) {
            val trimmed = line.trimStart()
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                inCodeFence = !inCodeFence
            }
        }
        if (inCodeFence) {
            result = if (result.endsWith("\n")) "${result}```" else "$result\n```"
        }

        // 2. Math blocks check ($$)
        val mathFenceMatches = Regex("^\\s*\\$\\$", RegexOption.MULTILINE).findAll(result).count()
        if (mathFenceMatches % 2 != 0) {
            result = if (result.endsWith("\n")) "${result}$$\n" else "$result\n$$\n"
        }

        return result
    }

    /**
     * Standardizes list indentation (e.g. converting 2-space nesting to clean 2-space or tab consistency)
     * and normalizes bullet symbols to standard `-`.
     */
    fun standardizeListIndentation(input: String): String {
        val lines = input.lines()
        val processed = lines.map { line ->
            val trimmedStart = line.trimStart()
            val indentSpaces = line.length - trimmedStart.length

            if (trimmedStart.startsWith("* ") || trimmedStart.startsWith("+ ")) {
                val indent = " ".repeat(indentSpaces)
                "$indent- ${trimmedStart.substring(2)}"
            } else {
                line
            }
        }
        return processed.joinToString("\n")
    }

    /**
     * Complete sanitize & standardize workflow.
     */
    fun sanitizeAndStandardize(input: String): String {
        val decoded = decodeHtmlEntities(input)
        val withCleanLists = standardizeListIndentation(decoded)
        return repairUnclosedDelimiters(withCleanLists)
    }
}
