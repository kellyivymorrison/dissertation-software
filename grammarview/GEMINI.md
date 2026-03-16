# GrammarView Mandates

This document outlines the foundational mandates and project-specific instructions for Gemini CLI when working on the GrammarView project.

## Core Directives

- **PDF Generation:** Ensure that all PDF generation logic remains modular and easily testable.
- **YACC Parsing:** Prioritize robust parsing of YACC files, specifically handling rule definitions and alternatives correctly.
- **Styling:** Maintain a consistent visual style for the generated PDF, using bold fonts for rule names and standard fonts for definitions.
- **Multi-page Support:** Always ensure that grammars exceeding a single page are handled gracefully, with rules continuing on subsequent pages.
- **Documentation & History:**
  - Keep `HISTORY.md` updated with technical changes.
  - Append every new user prompt to `PROMPTS.md` to maintain a log of requirements.

## Architectural Patterns

- Use `pdfbox` for all PDF manipulations.
- Use `picocli` for command-line argument parsing and help generation.
- Keep the main application logic in `GrammarViewApp.java`, but consider refactoring into separate classes (e.g., `YaccParser`, `PdfGenerator`) as complexity increases.

## Conventions

- Follow standard Java naming conventions (CamelCase for classes, camelCase for methods and variables).
- Use `slf4j` for all logging.
- Ensure all new features are accompanied by corresponding tests.
