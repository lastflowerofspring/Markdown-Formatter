# Interactive Formatted Markdown Editor (WYSIWYG Mode) - Architecture & Roadmap

## 🎯 Objective
Enable direct, in-place visual editing inside the Formatted view of the Markdown Formatter app while keeping the underlying raw Markdown synchronized bi-directionally in real-time.

---

## 🏗️ Core Architectural Design

### 1. Block-Based AST with Source Ranges
Every parsed Markdown element is assigned a unique `blockId` and retains its source index bounds `[startOffset, endOffset]`:
- **Heading Block**: Level (1-6), content, raw range.
- **Paragraph / List Item Block**: Formatted text spans, raw range.
- **Table Matrix Block**: 2D Grid structure (`headers: List<String>`, `rows: List<List<String>>`, alignments), raw range.
- **Code Block**: Language, lines of code, raw range.
- **Quote / Callout Block**: Variant, inner content, raw range.

### 2. Bidirectional Synchronization Pipeline
- When a user interacts in **Formatted View (Edit Mode)**:
  1. The user taps a text block or table cell.
  2. A focused editor / bottom sheet modal opens.
  3. Edits trigger an immediate slice replacement in the raw Markdown state.
  4. The formatted AST updates seamlessly without losing scroll position or view state.
- When switching between **Formatted**, **Raw Input**, and **Split** tabs, all states remain identical and synchronized.

---

## 🎨 Feature Specifications

### 1. Global In-Place Edit Mode
- **Edit Action Icon**: Positioned beside the **Copy** action in the bottom action bar.
- **Visual Feedback**:
  - Distinct indicator when In-Place Edit mode is active.
  - Interactive outline and subtle highlight on editable blocks.
  - Quick action toolbar with formatting shortcuts.

### 2. "Pro Sheet" Interactive Table Editor
- **Cell Selection**:
  - Tap any table cell to open the Table Cell Quick Editor docked above the keyboard.
  - Displays cell coordinates (e.g., `Row 2, Column 1`).
  - **Navigation**: `Previous Cell (⇥)` and `Next Cell (⇤)` buttons to move through the table rapidly.
- **Table Structure Operations**:
  - Add Row Above / Below.
  - Add Column Left / Right.
  - Delete Active Row / Column.
  - Column Text Alignment (Left, Center, Right).
- **Auto-Formatting**:
  - Generates aligned Markdown table pipes (`| header | header |`) dynamically.
  - Handles multi-line cell breaks gracefully.

### 3. Text & Heading Visual Editor
- **Inline Modal / Sheet Editor**:
  - Clean text field with active font styling (e.g. Heading 1 size for H1, monospace for code).
  - Formatting ribbon: **Bold**, *Italic*, `Code`, Link, Strikethrough, Bullet (`-`), Task (`[x]`).

### 4. Code Block & Callout Visual Editor
- Monospace editor with language tagging and copy/replace helpers.

---

## 🚀 Execution Steps

1. **Step 1: AST Parser Enhancement & Table Matrix Parser**
   - Extend `MarkdownParser.kt` to generate editable block models with source mapping and table matrix serialization.
2. **Step 2: Table Matrix Data Structure & Helper Functions**
   - Create `TableMatrix` model with row/col manipulation methods (insert, delete, cell edit, markdown string generator).
3. **Step 3: In-Place Editing UI & Bottom Sheet Controllers**
   - Implement `TableCellEditorSheet.kt` and `BlockEditorSheet.kt` with keyboard avoidance (`WindowInsets.ime`).
4. **Step 4: Formatted View Integration**
   - Add the Edit Mode toggle icon to bottom actions.
   - Attach click listeners to headings, paragraphs, lists, and table cells in `FormattedView.kt`.
5. **Step 5: Verification & Compilation**
   - Test full bi-directional editing with complex Markdown and Bengali/multilingual tables.
   - Verify build with `compile_applet`.
