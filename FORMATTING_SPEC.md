# Markdown Formatter & Reader Enhancement Specification

This document details the approved functional and UX improvements for the Markdown Formatter & Reader application.

---

## 1. Interaction & Visual Rendering

### 1.1 Task Item Row-Wide Click Target
- **Current Behavior**: Only tapping the checkbox icon toggles the task state (`- [ ]` <-> `- [x]`).
- **Approved Improvement**:
  - Expand the interactive clickable area to the entire task list item row (both checkbox and text body).
  - Preserve clickable URL link detection within the text so clicking a link opens the URL, while clicking anywhere else on the row toggles the checkbox.
  - Smooth visual feedback and state synchronization directly to the underlying document.

### 1.3 Code Block Enhancements
- **Current Behavior**: Syntax highlighted code block with copy action. Long lines require horizontal scrolling.
- **Approved Improvement**:
  - **Word Wrap Toggle**: Header button on code blocks to toggle between single-line horizontal scrolling and multi-line word wrapping.
  - **Line Numbers Toggle**: Option to display subtle, aligned line numbers on the left gutter for structured code inspection.
  - Preserve one-tap copy and syntax theme styling.

---

## 2. Formatter Engine & Smart Clean-Up Tools

### 2.1 AI & Web Paste Auto-Fixer (Sanitize & Standardize)
- **Current Behavior**: Pasting text from LLMs/webpages may introduce broken HTML entities or irregular indentation.
- **Approved Improvement**:
  - **Entity Decoding**: Replaces `&gt;`, `&lt;`, `&amp;`, `&quot;`, `&#39;`, `&nbsp;` with standard markdown equivalents.
  - **Delimiter Auto-Repair**: Detects unclosed code fences (```), blockquotes, and math blocks, automatically balancing them.
  - **List Indentation Normalizer**: Standardizes mixed 2-space / 4-space / tab nested list indentations to consistent standard markdown.
  - One-click trigger accessible from Format Tools.

### 2.2 Table Column Auto-Padding & Alignment Formatter
- **Current Behavior**: Tables in raw markdown can be jagged with uneven pipe widths.
- **Approved Improvement**:
  - Parses all markdown tables in the source document.
  - Computes the maximum width per column.
  - Pads cells with whitespace and standardizes header hyphens/colons (`:---:`, `---:`, `:---`) so the raw markdown is neat, clean, and perfectly aligned in ASCII.

---

## 3. Editor & UX Enhancements

### 3.1 Contextual / Inline Selection Formatting Toolbar
- **Current Behavior**: Formatting tools are located in the bottom action bar.
- **Approved Improvement**:
  - When text is selected in the editor, present a contextual action bar or quick floating formatting strip for instant Bold (`**`), Italic (`*`), Inline Code (`` ` ``), Strikethrough (`~~`), and Link insertion (`[text](url)`).
  - Works seamlessly with keyboard focus and Android text selection handles.

### 3.2 Bi-Directional Scroll & Cursor Sync
- **Current Behavior**: Switching between Reader and Editor modes resets or maintains independent scroll states.
- **Approved Improvement**:
  - When switching from Reader to Editor: Scroll the editor to the corresponding section or line viewed in the reader.
  - When switching from Editor to Reader: Scroll the reader smoothly to the section corresponding to the editor's cursor position / top visible line.

### 3.3 Find & Replace
- **Current Behavior**: No in-app search/replace tool.
- **Approved Improvement**:
  - Dedicated expandable search bar in Editor mode with:
    - Search query input and Replace query input.
    - Match count badge (`X of Y matches`).
    - Next / Previous match navigation buttons with highlight focus.
    - Single Replace and "Replace All" actions with undo support.
    - Case sensitivity toggle.
