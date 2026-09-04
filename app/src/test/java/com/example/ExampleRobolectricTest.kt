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

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.model.FontFamilyPreference
import com.example.model.FontSizePreference
import com.example.model.LineSpacingPreference
import com.example.model.ThemePalettes
import com.example.ui.sheets.SampleData
import com.example.ui.components.MarkdownRenderer
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Markdown Formatter", appName)
  }

  @Test
  fun `render all sample contents in MarkdownRenderer without crash`() {
    val currentDocState = androidx.compose.runtime.mutableStateOf(MarkdownParser.parse(SampleData.samples[0].rawMarkdown))
    composeTestRule.setContent {
      MyApplicationTheme {
        MarkdownRenderer(
          document = currentDocState.value,
          themeColors = ThemePalettes.EditorialLight,
          fontSize = FontSizePreference.STANDARD,
          lineSpacing = LineSpacingPreference.BALANCED,
          fontFamily = FontFamilyPreference.SANS,
          lazyListState = rememberLazyListState()
        )
      }
    }
    for (sample in SampleData.samples) {
      currentDocState.value = MarkdownParser.parse(sample.rawMarkdown)
      composeTestRule.waitForIdle()
    }
  }

  @Test
  fun `render user pasted Bengali HTML without crash`() {
    val text = """
      ৬ মাস ৪ দিন।</strong></div>
      <div>১৩. হযরত উসমান (রা.)-এর জীবনের শ্রেষ্ঠ অমর কীর্তি কোনটি? — <strong>কুরআন মাজীদের প্রমিত পাণ্ডুলিপি তৈরি ও বিশ্বব্যাপী প্রচার</strong></div>
      <div>১৪. অপর মুসলিমদের জানমাল ও ইজ্জতের নিরাপত্তা দেওয়া — <strong>মুমিনের প্রধান দায়িত্ব</strong></div>
    """.trimIndent()
    val doc = MarkdownParser.parse(text)
    composeTestRule.setContent {
      MyApplicationTheme {
        MarkdownRenderer(
          document = doc,
          themeColors = ThemePalettes.EditorialLight,
          fontSize = FontSizePreference.STANDARD,
          lineSpacing = LineSpacingPreference.BALANCED,
          fontFamily = FontFamilyPreference.SANS,
          lazyListState = rememberLazyListState()
        )
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `test full MainFormatterScreen flow pasting html and clicking format`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.viewmodel.FormatterViewModel(app)
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.MainFormatterScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()

    // 1. Switch to RAW_EDITOR
    viewModel.setViewMode(com.example.viewmodel.ViewMode.RAW_EDITOR)
    composeTestRule.waitForIdle()

    // 2. Paste HTML/CSS sample
    val htmlSample = SampleData.samples.first { it.title.contains("HTML", ignoreCase = true) }
    viewModel.updateRawText(htmlSample.rawMarkdown)
    composeTestRule.waitForIdle()

    // 3. Click Format HTML
    viewModel.formatHtmlAndCss()
    composeTestRule.waitForIdle()

    // 4. Click Formatted view
    viewModel.setViewMode(com.example.viewmodel.ViewMode.FORMATTED)
    composeTestRule.waitForIdle()

    // 5. Test each sample via selection
    for (sample in SampleData.samples) {
      viewModel.setViewMode(com.example.viewmodel.ViewMode.RAW_EDITOR)
      composeTestRule.waitForIdle()
      viewModel.updateRawText(sample.rawMarkdown)
      composeTestRule.waitForIdle()
      viewModel.setViewMode(com.example.viewmodel.ViewMode.FORMATTED)
      composeTestRule.waitForIdle()
    }
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

