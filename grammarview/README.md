# GrammarView

GrammarView is a command-line tool that produces a clean, visual PDF representation of YACC/Bison grammars. It uses ANTLR4 for robust parsing and Apache PDFBox for high-quality PDF generation.

## Features

- **Robust Parsing:** Utilizes an ANTLR4-based parser to accurately extract rules from YACC/Bison files.
- **Landscape Layout:** Generates PDFs in landscape mode for better readability of complex grammar rules.
- **Legend Page:** Optional legend page to explain grammar symbols (`->`, `|`).
- **Verbose Logging:** Detailed output for debugging and transparency.

## Installation

### Prerequisites

- Java 20 or higher
- Maven

### Build

Clone the repository and build the executable JAR using Maven:

```bash
mvn clean package
```

The shaded JAR will be created in the `target/` directory: `target/grammarview-1.0-SNAPSHOT.jar`.

## Usage

Run the tool using `java -jar`:

```bash
java -jar target/grammarview-1.0-SNAPSHOT.jar [OPTIONS] <YACC_FILE>
```

### Options

- `-legend`: Add a legend page to the end of the PDF.
- `-v`, `--verbose`: Enable verbose logging.
- `-h`, `--help`: Show the help message.

### Example

```bash
java -jar target/grammarview-1.0-SNAPSHOT.jar -legend examples/test.y
```

This will produce `test.y.pdf` in the current directory.

## Docker Usage

You can build and run GrammarView using Docker to avoid local environment setup.

### Build Image

```bash
docker build -t grammarview .
```

### Run with Docker

To process a file, mount your local directory to the container's `/app` directory. The JAR is stored in `/opt`, so it won't be hidden by your mount:

```bash
docker run --rm -v "$(pwd):/app" grammarview [OPTIONS] <YACC_FILE>
```

**Example:**

```bash
docker run --rm -v "$(pwd):/app" grammarview -legend examples/test.y
```

## Project Structure

- `src/main/antlr4`: ANTLR4 grammar files for YACC/Bison.
- `src/main/java`: Java source code for the application and parser base classes.
- `examples/`: Sample YACC files for testing.
- `GEMINI.md`: Project mandates and architectural guidelines.
- `HISTORY.md`: Detailed history of project changes.

## Attributions

This project uses ANTLR4 grammars for YACC/Bison parsing, which were derived from the [antlr/grammars-v4](https://github.com/antlr/grammars-v4) repository:

- `BisonLexer.g4`: Authored by Ken Domino, licensed under the MIT License.
- `BisonParser.g4`: Copyright (C) Free Software Foundation, Inc., licensed under the GNU General Public License version 3.

## License

GPL-3.0

This software is released under GPL-3.0 to ensure that improvements
remain open and available to the research community.
