# Project History

## 2026-03-16

- **Refactoring:**
    - Centralized all PDF layout and rendering constants into a new `PdfLayoutConstants` class.
    - Moved private layout constants from `PdfGenerator` and public layout constants from `PdfSymbolRenderer` to `PdfLayoutConstants`.
    - Added comprehensive Javadoc documentation to all layout constants to improve maintainability and clarity.
    - Updated `PdfGenerator` and `PdfSymbolRenderer` to reference the centralized constants, improving separation of concerns and reducing code duplication.
- **PDF Generation Improvements:**
    - Implemented S-shaped line wrapping for long grammar rules to prevent them from extending beyond the page margin.
    - Added a custom `drawWrappingCurve` method to `PdfSymbolRenderer` for rendering smooth, curved transitions between wrapped lines.
    - Refined S-shaped line wrapping to ensure the middle horizontal segment returns fully to the indented left margin, correctly connecting the end of one line to the beginning of the next.
    - Updated `drawWrappingCurve` to handle multiple vertical drop points for a cleaner visual transition.
    - Added a horizontal tail to the wrapping curve and perfectly aligned it with the first symbol of each wrapped line to ensure a seamless connection.
- **Refactoring:**
    - Refactored `GrammarModel.Rule` to use a `List<RuleAlternative>` instead of a `List<List<String>>`.
    - Introduced `RuleAlternative` as a first-class object to encapsulate the symbols of a grammar rule's alternative.
    - Updated `GrammarModel`, `GrammarViewApp`, `PdfGenerator`, and `GrammarViewAppTest` to use the new `RuleAlternative` API.
    - Moved PDF generation logic from `GrammarViewApp` into a dedicated `PdfGenerator` class to improve separation of concerns.
    - Updated `PdfGenerator` to be immutable and changed its constructor to accept the YACC file name as a `String` instead of a `File` object.
    - Relocated layout constants and pagination helper methods (`calculateRuleHeight`, `getPageSize`) to `PdfGenerator`.
    - Introduced `GrammarModel` class to encapsulate grammar rules and their metadata (non-terminals, nullability, recursion).
    - Updated `GrammarViewApp.parseYacc()` to return a `GrammarModel` instead of a raw `List<Rule>`.
    - Centralized grammar analysis logic (`isNullable`, `isRecursive`, `findRule`) within the `GrammarModel` class.
    - Refactored `GrammarViewAppTest` to use the new `GrammarModel` API, removing redundant local helper methods.
    - Updated `GrammarViewAppTest` to use Java text blocks for inline grammars in unit tests for improved readability.
- **Multi-page Support:**
    - Implemented multi-page PDF generation to handle large grammars.
    - Added a pagination strategy based on pre-calculating the height of each grammar rule to prevent symbols from being cut off at page boundaries.
    - Improved stability by ensuring page breaks only occur between rules, avoiding nested PDF stream operations.
- **Testing:**
    - Created `examples/test3.y` with a 26-symbol rule to verify the wrapping mechanism.
    - Created `examples/long_grammar.y` to verify multi-page PDF generation and pagination.
    - Added a new unit test `testParseTest3Y` to `GrammarViewAppTest.java` to ensure correct parsing of long rules.
- **Documentation:**
    - Updated `PROMPTS.md` with the latest user request for rule wrapping.

## 2026-03-15

- **Grammar Analysis Improvements:**
    - Enhanced nullability detection: A rule is now correctly identified as nullable if it has an empty RHS alternative OR if all items in any of its RHS alternatives are themselves nullable.
    - Implemented a fixed-point iteration algorithm to handle recursive nullability dependencies between rules.
- **Testing:**
    - Added a `testRecursiveNullability` unit test to verify complex nullability propagation.
    - Integrated JUnit 5 into the project.
...
    - Added comprehensive unit tests in `GrammarViewAppTest.java` for both `test.y` and `test2.y`.
    - Implemented `GIVEN... WHEN... THEN...` documentation pattern for all test cases.
    - Verified grammar properties: rule counts, start symbols, nullability, and recursion.
    - Refactored `GrammarViewApp` to improve testability by making internal classes and methods package-private.
- **Features:**
    - Added `--page-size` command-line option supporting common sizes (LETTER, LEGAL, A0-A6).
    - Added `-p` / `--portrait` command-line option to support portrait orientation in generated PDFs.
    - Added `-s` / `--font-size` command-line option to allow users to customize the font size (range: 6-32, default: 12).
- **Documentation:**
    - Documented all Linux return codes in `README.md`.
    - Updated `PROMPTS.md` with the latest user requests.
- **Error Handling:**
    - Implemented a robust error handling strategy with specific Linux exit codes for different failure scenarios (File Not Found: 3, Parse Error: 4, PDF Error: 5, General Error: 1).
    - Added syntax error detection during YACC parsing to ensure invalid grammars are reported and result in a non-zero exit code.
    - Updated the application to print clear, relevant error messages to `stderr`.
- **Refactoring:**
    - Standardized exit codes by introducing `EXIT_USAGE_ERROR = 2` constant.
    - Created `PdfSymbolRenderer` class to encapsulate all PDF drawing logic, separating it from the main application flow.
...    - Replaced hard-coded "magic numbers" in `GrammarViewApp.java` with well-named constants in `PdfSymbolRenderer`.
    - Improved code modularity and readability by delegating symbol and text rendering to the new renderer class.
- **Documentation:**
    - Updated `PROMPTS.md` with the latest user requests.

- **Maintenance:**
    - Removed `dependency-reduced-pom.xml` (temporary Maven Shade artifact).
- **Docker Support:**
    - Fixed Docker JAR accessibility: Moved the application JAR to `/opt` in the Docker image to prevent it from being hidden when a local volume is mounted to the working directory (`/app`).
    - Created a multi-stage `Dockerfile` using Maven 3.9 and Alpine-based OpenJDK 21.
- **Compliance:**
    - Added attributions for ANTLR4 grammars (BisonLexer.g4, BisonParser.g4) to `README.md`.
    - Renamed `LICENSE.txt` to `LICENSE`.
    - Updated `README.md` with GPL-3.0 license and explanatory text regarding its benefits for the research community.
    - Added a standard license header to all Java source files (`GrammarViewApp.java`, `BisonLexerBase.java`).
- **Documentation:**
    - Restored and expanded comprehensive JavaDoc and inline comments across `GrammarViewApp.java`.
- **PDF Generation Improvements:**
    - Standardized LHS Background: All LHS rule names now have a yellow background by default.
    - Priority for Nullable Coloring: Nullable symbols use a light gray background, taking precedence over other coloring.
    - Bold Text Everywhere: All text in the PDF is now rendered in bold.
    - Refined Centering: Implemented more precise centering logic using font cap height and string width.
    - Universal Recursive Symbol Drawing: Recursive non-terminals are drawn with the stacked rectangle style wherever they appear.
    - Custom Shape for Starting Rule: The LHS box for the first rule features a "less than" (<) shape.
    - Modified rendering of empty rules (epsilon) to draw a simple line.
- **Bug Fixes:**
    - Restored black borders for non-starting LHS boxes.
    - Added null checks in the YACC parser.
    - Fixed syntax errors in example files.

## 2026-03-11

- **Documentation & Logging:**
    - Created `PROMPTS.md` to log all user requests throughout the project.
    - Added a mandate to `GEMINI.md` to ensure `PROMPTS.md` is updated with every future request.
    - Created `README.md` with project overview and usage instructions.
    - Created `.gitignore` to manage build artifacts, IDE files, and PDF outputs.
    - Created `HISTORY.md` to document project changes.
    - Updated `HISTORY.md` with historical data from the "Day 1" session.
- **PDF Generation Improvements:**
    - Centered text horizontally and vertically within LHS boxes and RHS rounded rectangles.
    - Updated RHS background to a more vivid yellow.
    - Replaced RHS ellipses with rounded rectangles.
    - Added ellipses around each right-hand side (RHS) alternative and connected them to the left-hand side (LHS) with lines.
    - Added rectangular boxes with drop shadows around the LHS of each grammar rule.
    - Modified output format to put each alternative (RHS) of a grammar rule on a separate line.
    - Changed default page size from A4 to US Letter.
    - Switched PDF generation to landscape mode for better grammar readability.
    - Updated text placement logic to utilize landscape dimensions.
- **Project Reorganization:**
    - Created `examples/` directory and moved `test.y` into it for better project structure.
- **Added Project Mandates:** Created `GEMINI.md` to define core directives and architectural patterns.
- **Enhanced Parsing with ANTLR4:**
    - Replaced basic regex parsing with a robust ANTLR4-based parser for YACC/Bison files.
    - Added `BisonLexer.g4` and `BisonParser.g4` grammars.
    - Integrated `antlr4-maven-plugin` and `antlr4-runtime` into the build process.
    - Refactored `GrammarViewApp.java` to utilize the new parser.

## 2026-03-10 (Day 1)

- **Initial Project Scaffolding:**
  - Established a Java 20 Maven project structure.
  - Integrated `picocli` for CLI argument parsing and `slf4j-simple` for logging.
  - Configured `maven-shade-plugin` for executable fat JAR generation.
- **CLI Implementation:**
  - Developed `GrammarViewApp` with support for YACC file input and optional flags: `-legend`, `-v`/`--verbose`.
- **Initial Grammar Processing:**
  - Implemented basic rule extraction using regular expressions for initial YACC parsing.
- **PDF Generation Engine:**
  - Integrated Apache PDFBox for visual grammar representation.
  - Developed logic to render rules into multi-page PDF documents.
  - Implemented a dynamic "Legend" page generator.
- **Dependency Refactor:**
  - Downgraded PDFBox to 2.0.29 to resolve macOS-specific font scanning issues.
  - Refactored code to use the PDFBox 2.x API.
- **Verification:**
  - Created `test.y` and verified the full build and execution cycle.
