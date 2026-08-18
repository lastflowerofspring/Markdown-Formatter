package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.parser.MarkdownParser
import com.example.model.MarkdownBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Markdown Formatter", appName)
  }

  @Test
  fun `parse markdown headers and code block correctly`() {
    val raw = """
      # Main Title
      ## Subtitle
      ```kotlin
      val x = 42
      ```
      > [!NOTE]
      > This is a note
    """.trimIndent()

    val doc = MarkdownParser.parse(raw)
    assertEquals(4, doc.blocks.size)
    assertTrue(doc.blocks[0] is MarkdownBlock.HeaderBlock)
    assertTrue(doc.blocks[1] is MarkdownBlock.HeaderBlock)
    assertTrue(doc.blocks[2] is MarkdownBlock.CodeBlock)
    assertTrue(doc.blocks[3] is MarkdownBlock.CalloutBlock)
    assertEquals(2, doc.headings.size)
  }
}

