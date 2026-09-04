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
    assertEquals("Header A", table?.headers?.get(0)?.rawText)
  }

  @Test
  fun testHtmlCssFormatter() {
    val messy = "<style>body{color:red;margin:0;}</style>"
    val formatted = HtmlCssFormatter.formatDocument(messy)
    assertTrue(formatted.contains("\n"))
    assertTrue(formatted.contains("color: red;"))
  }

  @Test
  fun testBengaliHtmlParsing() {
    val text = """
      ৬ মাস ৪ দিন।</strong></div>
      <div>১৩. হযরত উসমান (রা.)-এর জীবনের শ্রেষ্ঠ অমর কীর্তি কোনটি? — <strong>কুরআন মাজীদের প্রমিত পাণ্ডুলিপি তৈরি ও বিশ্বব্যাপী প্রচার</strong></div>
      <div>১৪. অপর মুসলিমদের জানমাল ও ইজ্জতের নিরাপত্তা দেওয়া — <strong>মুমিনের প্রধান দায়িত্ব</strong></div>
    """.trimIndent()
    val doc = MarkdownParser.parse(text)
    assertTrue("Should parse into blocks", doc.blocks.isNotEmpty())
    assertEquals(3, doc.blocks.size)
    val secondBlock = doc.blocks[1] as MarkdownBlock.ParagraphBlock
    val boldSpan = secondBlock.content.spans.filterIsInstance<com.example.model.InlineSpan.Text>().find { it.isBold }
    assertNotNull("Should contain bold span", boldSpan)
    assertTrue(boldSpan!!.content.contains("কুরআন মাজীদের প্রমিত পাণ্ডুলিপি তৈরি"))
  }

  @Test
  fun testLargeHtmlInputParsing() {
    val sb = StringBuilder()
    for (i in 1..2000) {
      sb.append("<div>Item $i — <strong>Answer $i</strong> with <span style=\"color: #00ff00;\">green info</span></div>\n")
    }
    val startTime = System.currentTimeMillis()
    val doc = MarkdownParser.parse(sb.toString())
    val elapsed = System.currentTimeMillis() - startTime
    println("Parsed 2000 HTML items in $elapsed ms")
    assertTrue("Parsing 2000 items should take less than 2000ms", elapsed < 2000)
    assertEquals(2000, doc.blocks.size)
  }

  @Test
  fun testOrphanedTagsAndUnclosedBlocks() {
    val text = """
      </div>
      </p>
      <span>test</span>
      <div>Unclosed div without ending
      # Heading after unclosed div
      More text
    """.trimIndent()
    val doc = MarkdownParser.parse(text)
    assertTrue("Should handle orphaned and unclosed tags without infinite loop", doc.blocks.isNotEmpty())
  }
}
