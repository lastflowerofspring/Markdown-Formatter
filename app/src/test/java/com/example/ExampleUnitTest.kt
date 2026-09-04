package com.example

import com.example.model.MarkdownBlock
import com.example.parser.MarkdownParser
import com.example.util.HtmlCssFormatter
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testHtmlHeadersAndParagraphsParsing() {
    val raw = """
      <h1>Main Title</h1>
      <p>This is a paragraph with <span style="color: #ff0000;">red text</span>.</p>
    """.trimIndent()
    val doc = MarkdownParser.parse(raw)
    assertTrue("Should have parsed blocks", doc.blocks.isNotEmpty())
    val h1 = doc.blocks.filterIsInstance<MarkdownBlock.HeaderBlock>().firstOrNull()
    assertNotNull("Should contain HeaderBlock", h1)
    assertEquals("Main Title", h1?.text)
    assertEquals(1, h1?.level)
    assertTrue("Header should be marked as isHtml", h1?.isHtml == true)
  }

  @Test
  fun testHtmlCssBlockParsing() {
    val raw = """
      <style>
      body { color: blue; }
      </style>
    """.trimIndent()
    val doc = MarkdownParser.parse(raw)
    val code = doc.blocks.filterIsInstance<MarkdownBlock.CodeBlock>().firstOrNull()
    assertNotNull("Should parse style as CodeBlock", code)
    assertEquals("css", code?.language)
    assertTrue(code?.isHtml == true)
  }

  @Test
  fun testHtmlTableParsing() {
    val raw = """
      <table>
        <tr><th>Header A</th><th>Header B</th></tr>
        <tr><td>Data 1</td><td>Data 2</td></tr>
      </table>
    """.trimIndent()
    val doc = MarkdownParser.parse(raw)
    val table = doc.blocks.filterIsInstance<MarkdownBlock.TableBlock>().firstOrNull()
    assertNotNull("Should parse html table", table)
    assertTrue(table?.isHtml == true)
    assertEquals(2, table?.headers?.size)
    assertEquals("Header A", table?.headers?.get(0))
  }

  @Test
  fun testHtmlCssFormatter() {
    val messy = "<style>body{color:red;margin:0;}</style>"
    val formatted = HtmlCssFormatter.formatDocument(messy)
    assertTrue(formatted.contains("\n"))
    assertTrue(formatted.contains("color: red;"))
  }
}
