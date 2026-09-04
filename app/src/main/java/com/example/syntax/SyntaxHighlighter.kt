package com.example.syntax

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.model.CodeSyntaxColors

object SyntaxHighlighter {

    private val KEYWORDS = setOf(
        // Kotlin / Java / Swift
        "fun", "val", "var", "class", "interface", "object", "enum", "sealed", "data", "override",
        "open", "abstract", "final", "public", "private", "protected", "internal", "import", "package",
        "return", "if", "else", "when", "for", "while", "do", "break", "continue", "throw", "try",
        "catch", "finally", "is", "as", "in", "by", "suspend", "inline", "crossinline", "noinline",
        "companion", "typealias", "constructor", "init", "super", "this", "null", "true", "false",
        "void", "int", "long", "float", "double", "boolean", "char", "byte", "short", "static", "new",
        "extends", "implements", "instanceof", "synchronized", "volatile", "transient", "native",
        // Python
        "def", "lambda", "elif", "except", "pass", "raise", "yield", "with", "assert",
        "global", "nonlocal", "del", "None", "True", "False", "async", "await", "from",
        // JS / TS
        "const", "let", "function", "export", "default", "typeof", "instanceof", "switch", "case",
        "delete", "undefined", "NaN", "yield", "debugger", "any", "unknown", "never", "declare",
        "namespace", "keyof", "readonly", "implements", "satisfies",
        // Rust / C / C++ / Go
        "fn", "mut", "pub", "struct", "trait", "impl", "use", "mod", "match", "where", "ref", "self", "Self",
        "func", "go", "chan", "defer", "map", "select", "fallthrough", "range",
        "auto", "constexpr", "nullptr", "template", "typename", "using", "virtual", "extern",
        // SQL
        "select", "from", "where", "insert", "into", "update", "delete", "join", "inner", "left",
        "right", "outer", "group", "order", "having", "limit", "offset", "create", "table", "alter",
        "drop", "primary", "foreign", "key", "distinct", "union", "all", "exists", "between", "like"
    )

    private val COMMON_TYPES = setOf(
        "String", "Int", "Long", "Float", "Double", "Boolean", "Char", "Byte", "Short", "Any", "Unit",
        "Nothing", "List", "Map", "Set", "Array", "Sequence", "Flow", "StateFlow", "SharedFlow",
        "ViewModel", "CoroutineScope", "Modifier", "Composable", "Context", "Intent", "Bundle",
        "Promise", "Observable", "Record", "Result", "Option", "Some", "None", "Ok", "Err",
        "str", "int", "float", "bool", "list", "dict", "set", "tuple", "bytes", "object",
        "number", "string", "boolean", "symbol", "bigint", "void", "any", "unknown"
    )

    fun highlight(code: String, language: String, colors: CodeSyntaxColors): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")
        val lang = language.trim().lowercase()
        return try {
            when {
                lang in listOf("json") -> highlightJson(code, colors)
                lang in listOf("html", "xml", "svg") -> highlightXml(code, colors)
                lang in listOf("css", "scss", "sass", "less") -> highlightCss(code, colors)
                lang in listOf("sql") -> highlightSql(code, colors)
                lang in listOf("bash", "sh", "zsh", "shell") -> highlightShell(code, colors)
                else -> highlightGeneric(code, colors)
            }
        } catch (_: Throwable) {
            AnnotatedString(code)
        }
    }

    private fun AnnotatedString.Builder.safeAddStyle(style: SpanStyle, start: Int, end: Int, textLength: Int) {
        val safeStart = start.coerceIn(0, textLength)
        val safeEnd = end.coerceIn(0, textLength)
        if (safeStart < safeEnd) {
            try {
                addStyle(style, safeStart, safeEnd)
            } catch (_: Exception) {
                // Ignore any span boundary exceptions
            }
        }
    }

    private fun highlightGeneric(code: String, colors: CodeSyntaxColors): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            val text = code
            val len = text.length

            // 1. Strings (single, double quotes, backticks, triple quotes)
            val stringRegex = Regex("(\"\"\".*?\"\"\"|'''.*?'''|\".*?(?<!\\\\)\"|'.*?(?<!\\\\)'|`.*?`)", RegexOption.DOT_MATCHES_ALL)
            stringRegex.findAll(text).forEach { match ->
                safeAddStyle(SpanStyle(color = colors.string), match.range.first, match.range.last + 1, len)
            }

            // 2. Comments (// line comments, /* block comments */, # python/shell comments)
            val commentRegex = Regex("(//.*?$|/\\*.*?\\*/|#.*?$)", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
            commentRegex.findAll(text).forEach { match ->
                safeAddStyle(SpanStyle(color = colors.comment, fontWeight = FontWeight.Normal), match.range.first, match.range.last + 1, len)
            }

            // 3. Annotations / Decorators (@Composable, @override, etc.)
            val annotationRegex = Regex("(@[a-zA-Z_][a-zA-Z0-9_]*)")
            annotationRegex.findAll(text).forEach { match ->
                safeAddStyle(SpanStyle(color = colors.annotation, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1, len)
            }

            // 4. Numbers (Hex, Binary, Decimals, Floats)
            val numberRegex = Regex("\\b(0x[0-9a-fA-F_]+|0b[01_]+|[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?[fFdDlL]?)\\b")
            numberRegex.findAll(text).forEach { match ->
                safeAddStyle(SpanStyle(color = colors.number), match.range.first, match.range.last + 1, len)
            }

            // 5. Word tokens (Keywords, Types, Functions)
            val wordRegex = Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b")
            wordRegex.findAll(text).forEach { match ->
                val word = match.value
                val start = match.range.first
                val end = match.range.last + 1

                if (KEYWORDS.contains(word) || KEYWORDS.contains(word.lowercase())) {
                    safeAddStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.SemiBold), start, end, len)
                } else if (COMMON_TYPES.contains(word) || (word.first().isUpperCase() && word.drop(1).any { it.isLowerCase() })) {
                    safeAddStyle(SpanStyle(color = colors.type, fontWeight = FontWeight.Normal), start, end, len)
                } else {
                    // Check if followed by opening parenthesis (function invocation/definition)
                    val nextCharIndex = end
                    var i = nextCharIndex
                    while (i < text.length && text[i].isWhitespace()) i++
                    if (i < text.length && text[i] == '(') {
                        safeAddStyle(SpanStyle(color = colors.function, fontWeight = FontWeight.Medium), start, end, len)
                    }
                }
            }

            // 6. Operators & Punctuation
            val operatorRegex = Regex("([+\\-*/%=<>!&|^~?:;]+)")
            operatorRegex.findAll(text).forEach { match ->
                safeAddStyle(SpanStyle(color = colors.operator), match.range.first, match.range.last + 1, len)
            }
        }
    }

    private fun highlightJson(code: String, colors: CodeSyntaxColors): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            val text = code
            val len = text.length

            // Keys: "key":
            val keyRegex = Regex("\"([^\"]+)\"\\s*:")
            keyRegex.findAll(text).forEach { match ->
                val keyGroup = match.groups[1]
                if (keyGroup != null) {
                    safeAddStyle(SpanStyle(color = colors.attribute, fontWeight = FontWeight.SemiBold), match.range.first, keyGroup.range.last + 2, len)
                }
            }

            // String values: : "value" or in arrays
            val stringValRegex = Regex(":\\s*(\"[^\"]*\")")
            stringValRegex.findAll(text).forEach { match ->
                val valGroup = match.groups[1]
                if (valGroup != null) {
                    safeAddStyle(SpanStyle(color = colors.string), valGroup.range.first, valGroup.range.last + 1, len)
                }
            }

            // Numbers, booleans, null
            val literalRegex = Regex(":\\s*(-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?|true|false|null)\\b")
            literalRegex.findAll(text).forEach { match ->
                val valGroup = match.groups[1]
                if (valGroup != null) {
                    val color = if (valGroup.value in listOf("true", "false", "null")) colors.keyword else colors.number
                    safeAddStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold), valGroup.range.first, valGroup.range.last + 1, len)
                }
            }

            // Punctuation { } [ ] , :
            val punctRegex = Regex("([{}\\[\\],:])")
            punctRegex.findAll(text).forEach { match ->
                safeAddStyle(SpanStyle(color = colors.punctuation), match.range.first, match.range.last + 1, len)
            }
        }
    }

    private fun highlightXml(code: String, colors: CodeSyntaxColors): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            val text = code
            val len = text.length

            // XML comments
            val commentRegex = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
            commentRegex.findAll(text).forEach { match ->
                safeAddStyle(SpanStyle(color = colors.comment), match.range.first, match.range.last + 1, len)
            }

            // Tags <tag ...> or </tag>
            val tagRegex = Regex("</?([a-zA-Z0-9_:-]+)")
            tagRegex.findAll(text).forEach { match ->
                safeAddStyle(SpanStyle(color = colors.tag, fontWeight = FontWeight.SemiBold), match.range.first, match.range.last + 1, len)
            }

            // Attributes attr="val"
            val attrRegex = Regex("\\b([a-zA-Z0-9_:-]+)\\s*=")
            attrRegex.findAll(text).forEach { match ->
                val name = match.groups[1]
                if (name != null) {
                    safeAddStyle(SpanStyle(color = colors.attribute), name.range.first, name.range.last + 1, len)
                }
            }

            // Attribute values
            val valRegex = Regex("=\\s*(\"[^\"]*\"|'[^']*')")
            valRegex.findAll(text).forEach { match ->
                val v = match.groups[1]
                if (v != null) {
                    safeAddStyle(SpanStyle(color = colors.string), v.range.first, v.range.last + 1, len)
                }
            }
        }
    }

    private fun highlightSql(code: String, colors: CodeSyntaxColors): AnnotatedString {
        return highlightGeneric(code, colors)
    }

    private fun highlightShell(code: String, colors: CodeSyntaxColors): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            val text = code
            val len = text.length

            // Comments # ...
            val commentRegex = Regex("#.*?$", RegexOption.MULTILINE)
            commentRegex.findAll(text).forEach { match ->
                safeAddStyle(SpanStyle(color = colors.comment), match.range.first, match.range.last + 1, len)
            }

            // Strings
            val strRegex = Regex("(\"[^\"]*\"|'[^']*')")
            strRegex.findAll(text).forEach { match ->
                safeAddStyle(SpanStyle(color = colors.string), match.range.first, match.range.last + 1, len)
            }

            // Variables $VAR or ${VAR}
            val varRegex = Regex("(\\$[a-zA-Z0-9_]+|\\$\\{[^}]+\\})")
            varRegex.findAll(text).forEach { match ->
                safeAddStyle(SpanStyle(color = colors.type, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1, len)
            }

            // Common shell commands
            val cmdRegex = Regex("\\b(echo|cd|ls|grep|cat|chmod|chown|curl|wget|git|npm|gradle|docker|sudo|kill|ps|mkdir|rm|cp|mv|find|tar|ssh|scp|export|source|alias)\\b")
            cmdRegex.findAll(text).forEach { match ->
                safeAddStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1, len)
            }

            // Flags -a, --flag
            val flagRegex = Regex("(-{1,2}[a-zA-Z0-9_-]+)")
            flagRegex.findAll(text).forEach { match ->
                safeAddStyle(SpanStyle(color = colors.attribute), match.range.first, match.range.last + 1, len)
            }
        }
    }

    private fun highlightCss(code: String, colors: CodeSyntaxColors): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            val text = code
            val len = text.length

            // Comments: /* ... */
            val commentRegex = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
            commentRegex.findAll(text).forEach { match ->
                safeAddStyle(SpanStyle(color = colors.comment), match.range.first, match.range.last + 1, len)
            }

            // Selectors (lines before {)
            val selectorRegex = Regex("(?:^|})\\s*([^{]+)\\{", RegexOption.MULTILINE)
            selectorRegex.findAll(text).forEach { match ->
                val selectorGroup = match.groups[1]
                if (selectorGroup != null) {
                    val selText = selectorGroup.value.trim()
                    if (!selText.startsWith("/*") && !selText.startsWith("@")) {
                        safeAddStyle(SpanStyle(color = colors.tag, fontWeight = FontWeight.SemiBold), selectorGroup.range.first, selectorGroup.range.last + 1, len)
                    } else if (selText.startsWith("@")) {
                        safeAddStyle(SpanStyle(color = colors.annotation, fontWeight = FontWeight.Bold), selectorGroup.range.first, selectorGroup.range.last + 1, len)
                    }
                }
            }

            // Property names: word before colon
            val propRegex = Regex("([{;\\n]\\s*)([a-zA-Z-]+)\\s*:")
            propRegex.findAll(text).forEach { match ->
                val propGroup = match.groups[2]
                if (propGroup != null) {
                    safeAddStyle(SpanStyle(color = colors.attribute, fontWeight = FontWeight.Medium), propGroup.range.first, propGroup.range.last + 1, len)
                }
            }

            // Values with hex colors #fff, #123456
            val hexRegex = Regex("#([0-9a-fA-F]{3,8})\\b")
            hexRegex.findAll(text).forEach { match ->
                safeAddStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1, len)
            }

            // Numbers with units: 12px, 1.5rem, 100%, 0.8em, 24pt
            val unitRegex = Regex("(?<=:|,|\\s)(-?\\d+(?:\\.\\d+)?(?:px|em|rem|%|vh|vw|pt|s|ms|deg|fr)?)\\b")
            unitRegex.findAll(text).forEach { match ->
                safeAddStyle(SpanStyle(color = colors.number), match.range.first, match.range.last + 1, len)
            }

            // Strings inside quotes
            val stringRegex = Regex("(\"[^\"]*\"|'[^']*')")
            stringRegex.findAll(text).forEach { match ->
                safeAddStyle(SpanStyle(color = colors.string), match.range.first, match.range.last + 1, len)
            }
        }
    }
}
