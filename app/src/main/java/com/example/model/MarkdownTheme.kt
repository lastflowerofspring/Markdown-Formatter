package com.example.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

enum class ThemePreset(
    val title: String,
    val description: String,
    val isDark: Boolean
) {
    AI_STUDIO_DARK("AI Studio", "Google AI Studio dark workspace palette with high contrast and clean spacing", true),
    MINIMAL_DARK("Minimal Dark", "Refined warm charcoal dark slate for focused reading", true),
    SLATE_DARK("Slate Night", "Deep slate blue-grey canvas", true),
    DRACULA("Dracula", "Iconic dark purple developer palette", true),
    GITHUB_DARK("GitHub Dark", "Subdued dark charcoal with pastel accents", true),
    NORD("Nordic Frost", "Cool arctic slate aesthetic", true),
    MONOKAI("Monokai Pro", "High-contrast palette for technical docs", true),
    OLED_BLACK("Pitch Black OLED", "Pure #000000 black for maximum contrast", true),
    EDITORIAL_LIGHT("Paper White", "Crisp high-contrast editorial look", false),
    SEPIA("Warm Sepia", "Gentle warm book-paper eye-friendly tone", false)
}

data class CodeSyntaxColors(
    val background: Color,
    val text: Color,
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val type: Color,
    val function: Color,
    val operator: Color,
    val punctuation: Color,
    val annotation: Color,
    val tag: Color,
    val attribute: Color,
    val lineNumber: Color
)

data class ReaderThemeColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val outline: Color,
    val divider: Color,
    val inlineCodeBg: Color,
    val inlineCodeText: Color,
    val blockquoteBg: Color,
    val blockquoteBorder: Color,
    val tableHeaderBg: Color,
    val tableZebraBg: Color,
    val tableBorder: Color,
    val syntax: CodeSyntaxColors
)

enum class FontSizePreference(val label: String, val bodySp: Float, val h1Sp: Float, val h2Sp: Float, val h3Sp: Float, val codeSp: Float) {
    COMPACT("Compact", 14f, 22f, 19f, 16f, 12.5f),
    STANDARD("Standard", 16f, 25f, 21f, 18f, 14f),
    COMFORTABLE("Comfortable", 18f, 28f, 24f, 20f, 15.5f),
    LARGE("Large", 21f, 32f, 27f, 23f, 17.5f)
}

enum class LineSpacingPreference(val label: String, val multiplier: Float) {
    TIGHT("Tight", 1.35f),
    BALANCED("Balanced", 1.6f),
    RELAXED("Relaxed", 1.85f)
}

enum class FontFamilyPreference(val label: String, val composeFontFamily: FontFamily) {
    SANS("Sans Serif", FontFamily.SansSerif),
    SERIF("Serif (Editorial)", FontFamily.Serif),
    MONOSPACE("Monospace (Code)", FontFamily.Monospace),
    CURSIVE("Literata Style", FontFamily.Cursive)
}

object ThemePalettes {
    // Google AI Studio Dark Canvas Palette (Deep #131314 / #1E1F20 backdrop with clean contrast and refined accents)
    val AiStudioDark = ReaderThemeColors(
        background = Color(0xFF131314),
        surface = Color(0xFF1E1F20),
        surfaceVariant = Color(0xFF282A2C),
        onBackground = Color(0xFFE3E3E3),
        onSurface = Color(0xFFE3E3E3),
        onSurfaceVariant = Color(0xFFC4C7C5),
        primary = Color(0xFFA8C7FA),
        primaryContainer = Color(0xFF004A77),
        onPrimaryContainer = Color(0xFFC2E7FF),
        secondary = Color(0xFF6DD58C),
        outline = Color(0xFF444746),
        divider = Color(0xFF2B2C2E),
        inlineCodeBg = Color(0xFF282A2C),
        inlineCodeText = Color(0xFFE3E3E3),
        blockquoteBg = Color(0xFF1E1F20),
        blockquoteBorder = Color(0xFFA8C7FA),
        tableHeaderBg = Color(0xFF232527),
        tableZebraBg = Color(0xFF1A1B1C),
        tableBorder = Color(0xFF3C4043),
        syntax = CodeSyntaxColors(
            background = Color(0xFF1E1F20),
            text = Color(0xFFE3E3E3),
            keyword = Color(0xFF7DA0FA),
            string = Color(0xFF6DD58C),
            number = Color(0xFFF2B8B5),
            comment = Color(0xFF8E918F),
            type = Color(0xFFFFB77C),
            function = Color(0xFFA8C7FA),
            operator = Color(0xFFC4C7C5),
            punctuation = Color(0xFF8E918F),
            annotation = Color(0xFFFFB77C),
            tag = Color(0xFFF2B8B5),
            attribute = Color(0xFFA8C7FA),
            lineNumber = Color(0xFF747775)
        )
    )

    // Primary Minimal Dark Theme (Warm Neutral Charcoal, non-pure-black, non-blueish)
    val MinimalDark = ReaderThemeColors(
        background = Color(0xFF141416),
        surface = Color(0xFF1E1E22),
        surfaceVariant = Color(0xFF28282D),
        onBackground = Color(0xFFEDEDF0),
        onSurface = Color(0xFFEDEDF0),
        onSurfaceVariant = Color(0xFFA1A1AA),
        primary = Color(0xFFE4E4E7),
        primaryContainer = Color(0xFF2D2D35),
        onPrimaryContainer = Color(0xFFFFFFFF),
        secondary = Color(0xFF71717A),
        outline = Color(0xFF383840),
        divider = Color(0xFF28282E),
        inlineCodeBg = Color(0xFF24242A),
        inlineCodeText = Color(0xFFF4F4F5),
        blockquoteBg = Color(0xFF1C1C20),
        blockquoteBorder = Color(0xFF71717A),
        tableHeaderBg = Color(0xFF25252B),
        tableZebraBg = Color(0xFF19191D),
        tableBorder = Color(0xFF33333C),
        syntax = CodeSyntaxColors(
            background = Color(0xFF18181B),
            text = Color(0xFFEDEDF0),
            keyword = Color(0xFFD4BFFF),
            string = Color(0xFF86EFAC),
            number = Color(0xFFFDE047),
            comment = Color(0xFF71717A),
            type = Color(0xFFFDBA74),
            function = Color(0xFF93C5FD),
            operator = Color(0xFFA1A1AA),
            punctuation = Color(0xFF71717A),
            annotation = Color(0xFFFB923C),
            tag = Color(0xFFFCA5A5),
            attribute = Color(0xFF93C5FD),
            lineNumber = Color(0xFF52525B)
        )
    )

    val EditorialLight = ReaderThemeColors(
        background = Color(0xFFFAF9F6),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF1EFEA),
        onBackground = Color(0xFF191C1E),
        onSurface = Color(0xFF191C1E),
        onSurfaceVariant = Color(0xFF53555A),
        primary = Color(0xFF18181B),
        primaryContainer = Color(0xFFE4E4E7),
        onPrimaryContainer = Color(0xFF18181B),
        secondary = Color(0xFF71717A),
        outline = Color(0xFFE2E0D8),
        divider = Color(0xFFE5E7EB),
        inlineCodeBg = Color(0xFFEBE8DF),
        inlineCodeText = Color(0xFFB91C1C),
        blockquoteBg = Color(0xFFF4F3EE),
        blockquoteBorder = Color(0xFF27272A),
        tableHeaderBg = Color(0xFFEDEAE1),
        tableZebraBg = Color(0xFFF7F5EE),
        tableBorder = Color(0xFFDCD8CD),
        syntax = CodeSyntaxColors(
            background = Color(0xFFF3F4F6),
            text = Color(0xFF1F2937),
            keyword = Color(0xFF7C3AED),
            string = Color(0xFF059669),
            number = Color(0xFFD97706),
            comment = Color(0xFF6B7280),
            type = Color(0xFF2563EB),
            function = Color(0xFFDB2777),
            operator = Color(0xFF4B5563),
            punctuation = Color(0xFF9CA3AF),
            annotation = Color(0xFFEA580C),
            tag = Color(0xFFDC2626),
            attribute = Color(0xFF7C3AED),
            lineNumber = Color(0xFF9CA3AF)
        )
    )

    val WarmSepia = ReaderThemeColors(
        background = Color(0xFFFBF0D9),
        surface = Color(0xFFF4E5C4),
        surfaceVariant = Color(0xFFEADBBA),
        onBackground = Color(0xFF433422),
        onSurface = Color(0xFF433422),
        onSurfaceVariant = Color(0xFF6A573F),
        primary = Color(0xFF8F4E18),
        primaryContainer = Color(0xFFFFDCC1),
        onPrimaryContainer = Color(0xFF552A00),
        secondary = Color(0xFF705D00),
        outline = Color(0xFFDCC8A3),
        divider = Color(0xFFDFCDA8),
        inlineCodeBg = Color(0xFFE6D3AF),
        inlineCodeText = Color(0xFF8B2500),
        blockquoteBg = Color(0xFFEFE0BC),
        blockquoteBorder = Color(0xFF8F4E18),
        tableHeaderBg = Color(0xFFE5D2A8),
        tableZebraBg = Color(0xFFF8ECCC),
        tableBorder = Color(0xFFD2BE93),
        syntax = CodeSyntaxColors(
            background = Color(0xFFEDE0C0),
            text = Color(0xFF382C1E),
            keyword = Color(0xFF8B1E3F),
            string = Color(0xFF2E6F40),
            number = Color(0xFFB05000),
            comment = Color(0xFF887864),
            type = Color(0xFF1E5B8B),
            function = Color(0xFF9E3D00),
            operator = Color(0xFF634D35),
            punctuation = Color(0xFF96826C),
            annotation = Color(0xFF8B4500),
            tag = Color(0xFF8B1E3F),
            attribute = Color(0xFF1E5B8B),
            lineNumber = Color(0xFF9D8A74)
        )
    )

    val SlateDark = ReaderThemeColors(
        background = Color(0xFF0F172A),
        surface = Color(0xFF1E293B),
        surfaceVariant = Color(0xFF334155),
        onBackground = Color(0xFFF8FAFC),
        onSurface = Color(0xFFF8FAFC),
        onSurfaceVariant = Color(0xFF94A3B8),
        primary = Color(0xFF38BDF8),
        primaryContainer = Color(0xFF0369A1),
        onPrimaryContainer = Color(0xFFE0F2FE),
        secondary = Color(0xFF34D399),
        outline = Color(0xFF475569),
        divider = Color(0xFF334155),
        inlineCodeBg = Color(0xFF1E293B),
        inlineCodeText = Color(0xFF38BDF8),
        blockquoteBg = Color(0xFF1E293B),
        blockquoteBorder = Color(0xFF38BDF8),
        tableHeaderBg = Color(0xFF243247),
        tableZebraBg = Color(0xFF182234),
        tableBorder = Color(0xFF334155),
        syntax = CodeSyntaxColors(
            background = Color(0xFF090D16),
            text = Color(0xFFE2E8F0),
            keyword = Color(0xFFC084FC),
            string = Color(0xFF4ADE80),
            number = Color(0xFFFBBF24),
            comment = Color(0xFF64748B),
            type = Color(0xFF38BDF8),
            function = Color(0xFFF472B6),
            operator = Color(0xFF94A3B8),
            punctuation = Color(0xFF64748B),
            annotation = Color(0xFFFB923C),
            tag = Color(0xFFF87171),
            attribute = Color(0xFF38BDF8),
            lineNumber = Color(0xFF475569)
        )
    )

    val DraculaTheme = ReaderThemeColors(
        background = Color(0xFF282A36),
        surface = Color(0xFF343746),
        surfaceVariant = Color(0xFF44475A),
        onBackground = Color(0xFFF8F8F2),
        onSurface = Color(0xFFF8F8F2),
        onSurfaceVariant = Color(0xFFBFBFBF),
        primary = Color(0xFFBD93F9),
        primaryContainer = Color(0xFF6272A4),
        onPrimaryContainer = Color(0xFFF8F8F2),
        secondary = Color(0xFF50FA7B),
        outline = Color(0xFF6272A4),
        divider = Color(0xFF44475A),
        inlineCodeBg = Color(0xFF44475A),
        inlineCodeText = Color(0xFFFF79C6),
        blockquoteBg = Color(0xFF343746),
        blockquoteBorder = Color(0xFFBD93F9),
        tableHeaderBg = Color(0xFF44475A),
        tableZebraBg = Color(0xFF2E303E),
        tableBorder = Color(0xFF6272A4),
        syntax = CodeSyntaxColors(
            background = Color(0xFF1E1F29),
            text = Color(0xFFF8F8F2),
            keyword = Color(0xFFFF79C6),
            string = Color(0xFFF1FA8C),
            number = Color(0xFFBD93F9),
            comment = Color(0xFF6272A4),
            type = Color(0xFF8BE9FD),
            function = Color(0xFF50FA7B),
            operator = Color(0xFFFF79C6),
            punctuation = Color(0xFFF8F8F2),
            annotation = Color(0xFFFFB86C),
            tag = Color(0xFFFF79C6),
            attribute = Color(0xFF50FA7B),
            lineNumber = Color(0xFF6272A4)
        )
    )

    val GitHubDarkTheme = ReaderThemeColors(
        background = Color(0xFF0D1117),
        surface = Color(0xFF161B22),
        surfaceVariant = Color(0xFF21262D),
        onBackground = Color(0xFFC9D1D9),
        onSurface = Color(0xFFC9D1D9),
        onSurfaceVariant = Color(0xFF8B949E),
        primary = Color(0xFF58A6FF),
        primaryContainer = Color(0xFF1F6FEB),
        onPrimaryContainer = Color(0xFFF0F6FC),
        secondary = Color(0xFF3FB950),
        outline = Color(0xFF30363D),
        divider = Color(0xFF21262D),
        inlineCodeBg = Color(0xFF1F242C),
        inlineCodeText = Color(0xFF79C0FF),
        blockquoteBg = Color(0xFF161B22),
        blockquoteBorder = Color(0xFF388BFD),
        tableHeaderBg = Color(0xFF1B212A),
        tableZebraBg = Color(0xFF11161E),
        tableBorder = Color(0xFF30363D),
        syntax = CodeSyntaxColors(
            background = Color(0xFF05070A),
            text = Color(0xFFC9D1D9),
            keyword = Color(0xFFFF7B72),
            string = Color(0xFFA5D6FF),
            number = Color(0xFF79C0FF),
            comment = Color(0xFF8B949E),
            type = Color(0xFFFFA657),
            function = Color(0xFFD2A8FF),
            operator = Color(0xFFFF7B72),
            punctuation = Color(0xFF8B949E),
            annotation = Color(0xFFFFA657),
            tag = Color(0xFF7EE787),
            attribute = Color(0xFF79C0FF),
            lineNumber = Color(0xFF484F58)
        )
    )

    val NordTheme = ReaderThemeColors(
        background = Color(0xFF2E3440),
        surface = Color(0xFF3B4252),
        surfaceVariant = Color(0xFF434C5E),
        onBackground = Color(0xFFECEFF4),
        onSurface = Color(0xFFECEFF4),
        onSurfaceVariant = Color(0xFFD8DEE9),
        primary = Color(0xFF88C0D0),
        primaryContainer = Color(0xFF5E81AC),
        onPrimaryContainer = Color(0xFFECEFF4),
        secondary = Color(0xFFA3BE8C),
        outline = Color(0xFF4C566A),
        divider = Color(0xFF434C5E),
        inlineCodeBg = Color(0xFF3B4252),
        inlineCodeText = Color(0xFF88C0D0),
        blockquoteBg = Color(0xFF3B4252),
        blockquoteBorder = Color(0xFF81A1C1),
        tableHeaderBg = Color(0xFF434C5E),
        tableZebraBg = Color(0xFF353C4A),
        tableBorder = Color(0xFF4C566A),
        syntax = CodeSyntaxColors(
            background = Color(0xFF242933),
            text = Color(0xFFD8DEE9),
            keyword = Color(0xFF81A1C1),
            string = Color(0xFFA3BE8C),
            number = Color(0xFFB48EAD),
            comment = Color(0xFF616E88),
            type = Color(0xFF8FBCBB),
            function = Color(0xFF88C0D0),
            operator = Color(0xFF81A1C1),
            punctuation = Color(0xFFE5E9F0),
            annotation = Color(0xFFD08770),
            tag = Color(0xFFBF616A),
            attribute = Color(0xFF8FBCBB),
            lineNumber = Color(0xFF4C566A)
        )
    )

    val MonokaiTheme = ReaderThemeColors(
        background = Color(0xFF272822),
        surface = Color(0xFF383830),
        surfaceVariant = Color(0xFF49483E),
        onBackground = Color(0xFFF8F8F2),
        onSurface = Color(0xFFF8F8F2),
        onSurfaceVariant = Color(0xFFCFCEBA),
        primary = Color(0xFFA6E22E),
        primaryContainer = Color(0xFF75715E),
        onPrimaryContainer = Color(0xFFF8F8F2),
        secondary = Color(0xFF66D9EF),
        outline = Color(0xFF75715E),
        divider = Color(0xFF49483E),
        inlineCodeBg = Color(0xFF3E3D32),
        inlineCodeText = Color(0xFFF92672),
        blockquoteBg = Color(0xFF343329),
        blockquoteBorder = Color(0xFFA6E22E),
        tableHeaderBg = Color(0xFF424136),
        tableZebraBg = Color(0xFF2D2E27),
        tableBorder = Color(0xFF5E5D50),
        syntax = CodeSyntaxColors(
            background = Color(0xFF1E1F1C),
            text = Color(0xFFF8F8F2),
            keyword = Color(0xFFF92672),
            string = Color(0xFFE6DB74),
            number = Color(0xFFAE81FF),
            comment = Color(0xFF75715E),
            type = Color(0xFF66D9EF),
            function = Color(0xFFA6E22E),
            operator = Color(0xFFF92672),
            punctuation = Color(0xFFF8F8F2),
            annotation = Color(0xFFFD971F),
            tag = Color(0xFFF92672),
            attribute = Color(0xFFA6E22E),
            lineNumber = Color(0xFF75715E)
        )
    )

    val OledBlackTheme = ReaderThemeColors(
        background = Color(0xFF000000),
        surface = Color(0xFF121212),
        surfaceVariant = Color(0xFF1E1E1E),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFFAAAAAA),
        primary = Color(0xFF60A5FA),
        primaryContainer = Color(0xFF1E3A8A),
        onPrimaryContainer = Color(0xFFDBEAFE),
        secondary = Color(0xFF34D399),
        outline = Color(0xFF2A2A2A),
        divider = Color(0xFF222222),
        inlineCodeBg = Color(0xFF1A1A1A),
        inlineCodeText = Color(0xFF60A5FA),
        blockquoteBg = Color(0xFF0D0D0D),
        blockquoteBorder = Color(0xFF60A5FA),
        tableHeaderBg = Color(0xFF1A1A1A),
        tableZebraBg = Color(0xFF0A0A0A),
        tableBorder = Color(0xFF2E2E2E),
        syntax = CodeSyntaxColors(
            background = Color(0xFF000000),
            text = Color(0xFFEDEDED),
            keyword = Color(0xFFC084FC),
            string = Color(0xFF86EFAC),
            number = Color(0xFFFDE047),
            comment = Color(0xFF6B7280),
            type = Color(0xFF67E8F9),
            function = Color(0xFFF472B6),
            operator = Color(0xFF9CA3AF),
            punctuation = Color(0xFF6B7280),
            annotation = Color(0xFFFDBA74),
            tag = Color(0xFFFCA5A5),
            attribute = Color(0xFF67E8F9),
            lineNumber = Color(0xFF4B5563)
        )
    )

    fun getColorsForPreset(preset: ThemePreset, isSystemDark: Boolean): ReaderThemeColors {
        return when (preset) {
            ThemePreset.AI_STUDIO_DARK -> AiStudioDark
            ThemePreset.MINIMAL_DARK -> MinimalDark
            ThemePreset.EDITORIAL_LIGHT -> EditorialLight
            ThemePreset.SEPIA -> WarmSepia
            ThemePreset.SLATE_DARK -> SlateDark
            ThemePreset.DRACULA -> DraculaTheme
            ThemePreset.GITHUB_DARK -> GitHubDarkTheme
            ThemePreset.NORD -> NordTheme
            ThemePreset.MONOKAI -> MonokaiTheme
            ThemePreset.OLED_BLACK -> OledBlackTheme
        }
    }
}

