package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.model.FormattedDocument
import com.example.model.MarkdownBlock

object PdfExporter {

    fun printToPdf(
        context: Context,
        document: FormattedDocument,
        rawText: String,
        jobName: String = "Formatted_Document"
    ) {
        try {
            val html = generateStandaloneHtml(document, rawText, jobName)
            val webView = WebView(context)

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    try {
                        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                        if (printManager != null) {
                            val printAdapter = webView.createPrintDocumentAdapter(jobName)
                            val attributes = PrintAttributes.Builder()
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                .setResolution(PrintAttributes.Resolution("pdf_res", "PDF", 300, 300))
                                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                                .build()
                            printManager.print(jobName, printAdapter, attributes)
                        } else {
                            Toast.makeText(context, "Print service not available", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Print error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not prepare document: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateStandaloneHtml(
        document: FormattedDocument,
        rawText: String,
        title: String = "Document"
    ): String {
        // If rawText is already a full HTML document, return or wrap it with print CSS
        val trimmed = rawText.trim()
        if (trimmed.startsWith("<!DOCTYPE", ignoreCase = true) || trimmed.startsWith("<html", ignoreCase = true)) {
            val printCss = """
                <style>
                  @media print {
                    body { margin: 1.5cm; font-size: 11pt; }
                    pre, table, blockquote { page-break-inside: avoid; }
                  }
                </style>
            """.trimIndent()
            return if (rawText.contains("<head>", ignoreCase = true)) {
                rawText.replaceFirst(Regex("<head>", RegexOption.IGNORE_CASE), "<head>\n$printCss\n")
            } else {
                "$printCss\n$rawText"
            }
        }

        val bodyHtml = StringBuilder()
        for (block in document.blocks) {
            when (block) {
                is MarkdownBlock.HeaderBlock -> {
                    bodyHtml.append("<h${block.level} id=\"${block.id}\">${escapeHtml(block.text)}</h${block.level}>\n")
                }
                is MarkdownBlock.ParagraphBlock -> {
                    bodyHtml.append("<p>${renderInlineHtml(block.content)}</p>\n")
                }
                is MarkdownBlock.CodeBlock -> {
                    val lang = if (block.language.isNotEmpty()) " class=\"language-${block.language}\"" else ""
                    bodyHtml.append("<pre><code$lang>${escapeHtml(block.code)}</code></pre>\n")
                }
                is MarkdownBlock.CalloutBlock -> {
                    val titleHtml = if (block.title != null) "<strong>[${block.type.name}] ${escapeHtml(block.title)}</strong><br>" else ""
                    bodyHtml.append("<blockquote class=\"callout callout-${block.type.name.lowercase()}\">$titleHtml${renderInlineHtml(block.content)}</blockquote>\n")
                }
                is MarkdownBlock.BulletListBlock -> {
                    bodyHtml.append("<ul>\n")
                    for (item in block.items) {
                        bodyHtml.append("  <li>${renderInlineHtml(item.content)}</li>\n")
                    }
                    bodyHtml.append("</ul>\n")
                }
                is MarkdownBlock.NumberedListBlock -> {
                    bodyHtml.append("<ol>\n")
                    for (item in block.items) {
                        bodyHtml.append("  <li>${renderInlineHtml(item.content)}</li>\n")
                    }
                    bodyHtml.append("</ol>\n")
                }
                is MarkdownBlock.TaskListBlock -> {
                    bodyHtml.append("<ul class=\"task-list\">\n")
                    for (item in block.items) {
                        val checked = if (item.isChecked) "checked disabled" else "disabled"
                        bodyHtml.append("  <li><input type=\"checkbox\" $checked> ${renderInlineHtml(item.content)}</li>\n")
                    }
                    bodyHtml.append("</ul>\n")
                }
                is MarkdownBlock.TableBlock -> {
                    bodyHtml.append("<table>\n")
                    if (block.headers.isNotEmpty()) {
                        bodyHtml.append("  <thead>\n    <tr>\n")
                        for ((idx, h) in block.headers.withIndex()) {
                            val align = block.alignments.getOrNull(idx)?.name?.lowercase() ?: "left"
                            bodyHtml.append("      <th style=\"text-align: $align;\">${renderInlineHtml(h)}</th>\n")
                        }
                        bodyHtml.append("    </tr>\n  </thead>\n")
                    }
                    if (block.rows.isNotEmpty()) {
                        bodyHtml.append("  <tbody>\n")
                        for (row in block.rows) {
                            bodyHtml.append("    <tr>\n")
                            for ((idx, cell) in row.withIndex()) {
                                val align = block.alignments.getOrNull(idx)?.name?.lowercase() ?: "left"
                                bodyHtml.append("      <td style=\"text-align: $align;\">${renderInlineHtml(cell)}</td>\n")
                            }
                            bodyHtml.append("    </tr>\n")
                        }
                        bodyHtml.append("  </tbody>\n")
                    }
                    bodyHtml.append("</table>\n")
                }
                is MarkdownBlock.DividerBlock -> {
                    bodyHtml.append("<hr>\n")
                }
                is MarkdownBlock.MathBlock -> {
                    bodyHtml.append("<div class=\"math-block\"><code>${escapeHtml(block.latex)}</code></div>\n")
                }
            }
        }

        return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${escapeHtml(title)}</title>
  <style>
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      line-height: 1.6;
      color: #1f2937;
      background-color: #ffffff;
      max-width: 800px;
      margin: 0 auto;
      padding: 24px;
    }
    h1, h2, h3, h4, h5, h6 {
      color: #111827;
      font-weight: 600;
      margin-top: 1.5em;
      margin-bottom: 0.5em;
      line-height: 1.25;
    }
    h1 { font-size: 2em; border-bottom: 1px solid #e5e7eb; padding-bottom: 0.3em; }
    h2 { font-size: 1.5em; border-bottom: 1px solid #f3f4f6; padding-bottom: 0.2em; }
    h3 { font-size: 1.25em; }
    p { margin: 0.8em 0; }
    a { color: #2563eb; text-decoration: underline; }
    code {
      font-family: "JetBrains Mono", Consolas, Menlo, Monaco, monospace;
      font-size: 0.875em;
      background-color: #f3f4f6;
      color: #dc2626;
      padding: 0.2em 0.4em;
      border-radius: 4px;
    }
    pre {
      background-color: #1e293b;
      color: #f8fafc;
      padding: 16px;
      border-radius: 8px;
      overflow-x: auto;
      font-size: 0.875em;
      line-height: 1.5;
    }
    pre code {
      background-color: transparent;
      color: inherit;
      padding: 0;
    }
    blockquote {
      border-left: 4px solid #3b82f6;
      background-color: #eff6ff;
      margin: 1em 0;
      padding: 12px 16px;
      border-radius: 0 8px 8px 0;
      color: #1e3a8a;
    }
    blockquote.callout-warning, blockquote.callout-caution {
      border-left-color: #f59e0b;
      background-color: #fffbeb;
      color: #92400e;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      margin: 1.2em 0;
      font-size: 0.9em;
    }
    th, td {
      border: 1px solid #e5e7eb;
      padding: 8px 12px;
    }
    th {
      background-color: #f9fafb;
      font-weight: 600;
      color: #374151;
    }
    tr:nth-child(even) {
      background-color: #f9fafb;
    }
    ul, ol {
      padding-left: 24px;
      margin: 0.8em 0;
    }
    li { margin: 0.3em 0; }
    ul.task-list {
      list-style-type: none;
      padding-left: 0;
    }
    ul.task-list li {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    hr {
      border: 0;
      height: 1px;
      background-color: #e5e7eb;
      margin: 2em 0;
    }
    .math-block {
      background-color: #f8fafc;
      border: 1px dashed #cbd5e1;
      padding: 12px;
      border-radius: 6px;
      text-align: center;
      margin: 1em 0;
    }
    @media print {
      body { margin: 0; padding: 0; max-width: 100%; }
      pre, table, blockquote { page-break-inside: avoid; }
    }
  </style>
</head>
<body>
  $bodyHtml
</body>
</html>
""".trimIndent()
    }

    private fun renderInlineHtml(spanGroup: com.example.model.InlineSpanGroup): String {
        val sb = StringBuilder()
        for (span in spanGroup.spans) {
            when (span) {
                is com.example.model.InlineSpan.Text -> {
                    var text = escapeHtml(span.content)
                    if (span.isBold) text = "<strong>$text</strong>"
                    if (span.isItalic) text = "<em>$text</em>"
                    if (span.isStrike) text = "<del>$text</del>"
                    if (span.isUnderline) text = "<u>$text</u>"
                    if (span.colorHex != null || span.bgHex != null) {
                        val colorStyle = if (span.colorHex != null) "color: ${span.colorHex}; " else ""
                        val bgStyle = if (span.bgHex != null) "background-color: ${span.bgHex}; " else ""
                        text = "<span style=\"$colorStyle$bgStyle\">$text</span>"
                    }
                    sb.append(text)
                }
                is com.example.model.InlineSpan.InlineCode -> {
                    sb.append("<code>${escapeHtml(span.code)}</code>")
                }
                is com.example.model.InlineSpan.Link -> {
                    sb.append("<a href=\"${escapeHtml(span.url)}\">${escapeHtml(span.text)}</a>")
                }
                is com.example.model.InlineSpan.InlineMath -> {
                    sb.append("<code>${escapeHtml(span.latex)}</code>")
                }
            }
        }
        return sb.toString()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
