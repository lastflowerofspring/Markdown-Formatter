package com.example

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.FontFamilyPreference
import com.example.model.FontSizePreference
import com.example.model.LineSpacingPreference
import com.example.model.ThemePalettes
import com.example.parser.MarkdownParser
import com.example.ui.components.MarkdownRenderer
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleMarkdown = """
      # Hello Pixel 7 Pro
      Formatted **beautifully** with code highlighting and tables.
      
      ```kotlin
      val app = "Markdown Formatter"
      println(app)
      ```
    """.trimIndent()
    val doc = MarkdownParser.parse(sampleMarkdown)

    composeTestRule.setContent {
      MyApplicationTheme {
        MarkdownRenderer(
          document = doc,
          themeColors = ThemePalettes.SlateDark,
          fontSize = FontSizePreference.STANDARD,
          lineSpacing = LineSpacingPreference.BALANCED,
          fontFamily = FontFamilyPreference.SANS,
          lazyListState = rememberLazyListState()
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

