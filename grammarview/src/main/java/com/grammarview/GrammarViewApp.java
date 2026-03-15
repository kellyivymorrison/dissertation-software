// Copyright (c) 2026 Kelly Morrison
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License version 3.

package com.grammarview;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * GrammarViewApp is a command-line utility that generates a visual representation 
 * of a YACC/Bison grammar in PDF format.
 * 
 * Key Features:
 * - ANTLR4-based parsing of YACC/Bison files.
 * - Color-coded symbols: Non-terminals (Yellow), Terminals (Blue), Nullable (Gray).
 * - Structural indicators: Recursive rules (stacked boxes), Start rule (< shaped).
 * - Orthogonal line routing for flow-chart style clarity.
 */
@Command(name = "grammarview", mixinStandardHelpOptions = true, version = "grammarview 1.0",
        description = "Produces a PDF view of a YACC grammar.")
public class GrammarViewApp implements Callable<Integer> {

    // --- Exit Codes ---
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_GENERAL_ERROR = 1;
    private static final int EXIT_USAGE_ERROR = 2;
    private static final int EXIT_FILE_NOT_FOUND = 3;
    private static final int EXIT_PARSE_ERROR = 4;
    private static final int EXIT_PDF_ERROR = 5;

    // --- Layout Constants ---
    
    private static final float MARGIN_X = 50f;
    private static final float MARGIN_Y = 50f;
    private static final float INITIAL_Y_OFFSET = 100f;
    private static final float BOTTOM_OVERFLOW_LIMIT = 50f;
    
    private static final float RHS_START_X_OFFSET = 40f;
    private static final float RHS_ITEM_LINK_LENGTH = 15f;
    private static final float SPACING_ALTERNATIVES = 10f;
    private static final float SPACING_RULES = 20f;
    
    private static final float LEGEND_LINE_SPACING = 20f;

    @Parameters(index = "0", description = "The YACC file to process.")
    private File yaccFile;

    @Option(names = {"-legend"}, description = "Add a legend page to the PDF.")
    private boolean legend;

    @Option(names = {"-p", "--portrait"}, description = "Use portrait orientation (default: landscape).")
    private boolean portrait;

    @Option(names = {"--page-size"}, defaultValue = "LETTER", 
            description = "Page size (LETTER, LEGAL, A0, A1, A2, A3, A4, A5, A6).")
    private String pageSizeName;

    @Option(names = {"-s", "--font-size"}, defaultValue = "12", 
            description = "Font size in points (default: 12, range: 6-32).")
    private float fontSize;

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose logging.")
    private boolean verbose;

    /**
     * Internal representation of a Grammar Rule.
     */
    static class Rule {
        String name;
        /** List of alternatives, where each alternative is a list of symbol names. */
        List<List<String>> alternatives = new ArrayList<>();

        Rule(String name) {
            this.name = name;
        }
    }

    /**
     * Exception thrown when YACC parsing fails.
     */
    private static class GrammarParseException extends Exception {
        GrammarParseException(String message) {
            super(message);
        }
    }

    /**
     * Main execution logic for the CLI command.
     */
    @Override
    public Integer call() {
        try {
            if (verbose) {
                System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
                System.out.println("Verbose logging enabled.");
            }

            if (!yaccFile.exists()) {
                System.err.println("Error: File not found: " + yaccFile.getPath());
                return EXIT_FILE_NOT_FOUND;
            }

            // Validate font size range
            if (fontSize < 6 || fontSize > 32) {
                System.err.println("Error: Font size must be between 6 and 32 points.");
                return EXIT_USAGE_ERROR;
            }

            System.out.println("Processing YACC file: " + yaccFile.getName());
            
            // 1. Parse the YACC file into internal Rule objects using ANTLR
            List<Rule> rules;
            try {
                rules = parseYacc(yaccFile);
            } catch (GrammarParseException e) {
                System.err.println("Error: " + e.getMessage());
                return EXIT_PARSE_ERROR;
            } catch (Exception e) {
                System.err.println("Error: Failed to parse YACC file: " + e.getMessage());
                return EXIT_PARSE_ERROR;
            }
            
            // 2. Generate the PDF visualization using PDFBox
            try {
                generatePdf(rules);
            } catch (IOException e) {
                System.err.println("Error: Failed to create PDF file: " + e.getMessage());
                return EXIT_PDF_ERROR;
            }

            return EXIT_SUCCESS;
        } catch (Exception e) {
            System.err.println("Error: An unexpected error occurred: " + e.getMessage());
            if (verbose) e.printStackTrace(System.err);
            return EXIT_GENERAL_ERROR;
        }
    }

    /**
     * Uses the generated ANTLR4 parser to extract rules from the YACC source.
     * 
     * @param file The input YACC/Bison file.
     * @return A list of extracted Rule objects.
     */
    List<Rule> parseYacc(File file) throws Exception {
        CharStream input;
        try {
            input = CharStreams.fromPath(file.toPath());
        } catch (IOException e) {
            throw new Exception("Failed to read YACC file: " + e.getMessage());
        }

        BisonLexer lexer = new BisonLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        BisonParser parser = new BisonParser(tokens);
        
        ParseTree tree = parser.input_();

        if (parser.getNumberOfSyntaxErrors() > 0) {
            throw new GrammarParseException("Found " + parser.getNumberOfSyntaxErrors() + " syntax errors in the YACC grammar.");
        }

        List<Rule> rules = new ArrayList<>();
        ParseTreeWalker walker = new ParseTreeWalker();
        
        // Custom listener to capture rule definitions and their RHS alternatives
        walker.walk(new BisonParserBaseListener() {
            @Override
            public void enterRules(BisonParser.RulesContext ctx) {
                // Ignore malformed rules
                if (ctx.id() == null || ctx.rhses_1() == null) return;
                
                Rule rule = new Rule(ctx.id().getText());
                for (BisonParser.RhsContext rhsCtx : ctx.rhses_1().rhs()) {
                    List<String> items = new ArrayList<>();
                    // Extract symbols (we ignore actions, predicates, etc. for visualization)
                    for (int i = 0; i < rhsCtx.getChildCount(); i++) {
                        ParseTree child = rhsCtx.getChild(i);
                        if (child instanceof BisonParser.SymbolContext) {
                            items.add(child.getText());
                        }
                    }
                    rule.alternatives.add(items);
                }
                rules.add(rule);
            }
        }, tree);

        return rules;
    }

    /**
     * Renders the extracted grammar rules into a multi-page PDF document.
     * 
     * @param rules The list of grammar rules to visualize.
     */
    private void generatePdf(List<Rule> rules) throws IOException {
        String outputFileName = yaccFile.getName() + ".pdf";
        System.out.println("Generating PDF: " + outputFileName + " with font size " + fontSize + ", size " + pageSizeName);

        // Analyze grammar metadata for rendering hints
        Set<String> nonTerminals = new HashSet<>();
        Set<String> recursiveRules = new HashSet<>();
        Set<String> nullableRules = new HashSet<>();
        
        for (Rule rule : rules) {
            nonTerminals.add(rule.name);
            for (List<String> alt : rule.alternatives) {
                if (alt.contains(rule.name)) {
                    recursiveRules.add(rule.name);
                }
            }
        }

        // Iterative nullability calculation (Fixed-point iteration)
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Rule rule : rules) {
                if (nullableRules.contains(rule.name)) continue;

                for (List<String> alt : rule.alternatives) {
                    if (alt.isEmpty()) {
                        if (nullableRules.add(rule.name)) {
                            changed = true;
                        }
                        break;
                    } else {
                        // Check if all items in this alternative are nullable non-terminals
                        boolean allNullable = true;
                        for (String item : alt) {
                            if (!nullableRules.contains(item)) {
                                allNullable = false;
                                break;
                            }
                        }
                        if (allNullable) {
                            if (nullableRules.add(rule.name)) {
                                changed = true;
                            }
                            break;
                        }
                    }
                }
            }
        }
        
        try (PDDocument document = new PDDocument()) {
            // Setup page orientation and size based on CLI options
            PDRectangle baseSize = getPageSize(pageSizeName);
            PDRectangle pageSize = portrait ? baseSize : 
                new PDRectangle(baseSize.getHeight(), baseSize.getWidth());
            PDPage page = new PDPage(pageSize);
            document.addPage(page);

            PDType1Font boldFont = PDType1Font.HELVETICA_BOLD;

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                PdfSymbolRenderer renderer = new PdfSymbolRenderer(contentStream, boldFont);
                
                // Title
                contentStream.beginText();
                contentStream.setFont(boldFont, PdfSymbolRenderer.FONT_SIZE_TITLE);
                contentStream.newLineAtOffset(MARGIN_X, pageSize.getHeight() - MARGIN_Y);
                contentStream.showText("Grammar View: " + yaccFile.getName());
                contentStream.endText();

                float yOffset = pageSize.getHeight() - INITIAL_Y_OFFSET;

                // Vertical centering adjustment based on font cap height
                float capHeight = boldFont.getFontDescriptor().getCapHeight() / PdfSymbolRenderer.FONT_UNIT_CONVERSION * fontSize;

                for (int ruleIdx = 0; ruleIdx < rules.size(); ruleIdx++) {
                    Rule rule = rules.get(ruleIdx);
                    boolean isStartRule = (ruleIdx == 0);
                    boolean isRecursive = recursiveRules.contains(rule.name);
                    boolean isNullable = nullableRules.contains(rule.name);

                    // Calculate bounding box for LHS
                    float ruleNameWidth = boldFont.getStringWidth(rule.name) / PdfSymbolRenderer.FONT_UNIT_CONVERSION * fontSize;
                    float boxWidth = ruleNameWidth + (PdfSymbolRenderer.BOX_PADDING_X * 2);
                    float boxHeight = fontSize + (PdfSymbolRenderer.BOX_PADDING_Y * 2);
                    float xPos = MARGIN_X;
                    float boxY = yOffset - PdfSymbolRenderer.BOX_PADDING_Y;

                    // 1. Draw Shadow for LHS
                    contentStream.setNonStrokingColor(PdfSymbolRenderer.COLOR_SHADOW);
                    if (isStartRule) {
                        renderer.drawLHSStartShape(xPos + PdfSymbolRenderer.SHADOW_OFFSET, boxY - PdfSymbolRenderer.SHADOW_OFFSET, boxWidth, boxHeight, true);
                    } else {
                        contentStream.addRect(xPos + PdfSymbolRenderer.SHADOW_OFFSET, boxY - PdfSymbolRenderer.SHADOW_OFFSET, boxWidth, boxHeight);
                        contentStream.fill();
                    }

                    // 2. Draw LHS Main Shape
                    // Priority: Nullable (Gray) > Rule Definition (Yellow)
                    Color lhsColor = isNullable ? PdfSymbolRenderer.COLOR_LIGHT_GRAY : PdfSymbolRenderer.COLOR_VIVID_YELLOW;
                    renderer.drawNonTerminalShape(xPos, boxY, boxWidth, boxHeight, lhsColor, isRecursive, isStartRule);

                    // 3. Render LHS Text (Centered)
                    renderer.drawCenteredText(rule.name, xPos, boxY, boxWidth, boxHeight, fontSize, capHeight, isStartRule);

                    float lineStartX = xPos + boxWidth;
                    float lineStartY = boxY + (boxHeight / 2);

                    // 4. Render RHS Alternatives
                    for (int i = 0; i < rule.alternatives.size(); i++) {
                        List<String> items = rule.alternatives.get(i);
                        float currentX = xPos + boxWidth + RHS_START_X_OFFSET;
                        float currentY = yOffset;
                        float targetY = (currentY - PdfSymbolRenderer.BOX_PADDING_Y) + (boxHeight / 2);

                        // Draw Orthogonal Connecting Line
                        contentStream.setStrokingColor(PdfSymbolRenderer.COLOR_STROKE);
                        float midX = lineStartX + (currentX - lineStartX) / 2;
                        contentStream.moveTo(lineStartX, lineStartY);
                        contentStream.lineTo(midX, lineStartY);
                        contentStream.lineTo(midX, targetY);
                        contentStream.lineTo(currentX, targetY);
                        contentStream.stroke();

                        // Render individual symbols in the RHS sequence
                        if (!items.isEmpty()) {
                            for (int j = 0; j < items.size(); j++) {
                                String itemText = items.get(j);
                                float itemWidth = boldFont.getStringWidth(itemText) / PdfSymbolRenderer.FONT_UNIT_CONVERSION * fontSize;
                                float rectWidth = itemWidth + (PdfSymbolRenderer.BOX_PADDING_X * 2);
                                float rectHeight = fontSize + (PdfSymbolRenderer.BOX_PADDING_Y * 2);
                                float itemBoxY = currentY - PdfSymbolRenderer.BOX_PADDING_Y;

                                boolean isItemRecursive = recursiveRules.contains(itemText);
                                boolean isItemNullable = nullableRules.contains(itemText);
                                boolean isTerminal = !nonTerminals.contains(itemText) 
                                                  || itemText.startsWith("'") 
                                                  || itemText.startsWith("\"");

                                // 4a. Draw item symbol
                                if (isTerminal) {
                                    renderer.drawRoundedRectangle(currentX, itemBoxY, rectWidth, rectHeight, PdfSymbolRenderer.ROUNDED_RECT_RADIUS, PdfSymbolRenderer.COLOR_LIGHT_BLUE);
                                } else {
                                    Color itemColor = isItemNullable ? PdfSymbolRenderer.COLOR_LIGHT_GRAY : PdfSymbolRenderer.COLOR_VIVID_YELLOW;
                                    renderer.drawNonTerminalShape(currentX, itemBoxY, rectWidth, rectHeight, itemColor, isItemRecursive, false);
                                }

                                // 4b. Render item text (Centered)
                                renderer.drawCenteredText(itemText, currentX, itemBoxY, rectWidth, rectHeight, fontSize, capHeight, false);

                                currentX += rectWidth;

                                // 4c. Draw horizontal link to next item
                                if (j < items.size() - 1) {
                                    float nextLineEndX = currentX + RHS_ITEM_LINK_LENGTH;
                                    contentStream.moveTo(currentX, targetY);
                                    contentStream.lineTo(nextLineEndX, targetY);
                                    contentStream.stroke();
                                    currentX = nextLineEndX;
                                }
                            }
                        }
                        
                        // Vertical spacing between alternatives
                        if (i < rule.alternatives.size() - 1) {
                            yOffset -= (boxHeight + SPACING_ALTERNATIVES);
                        }
                    }
                    
                    // Vertical spacing between different rules
                    yOffset -= (boxHeight + SPACING_RULES + (isRecursive ? 10 : 0));
                    if (yOffset < BOTTOM_OVERFLOW_LIMIT) break; // Overflow protection
                }
            }

            // Optional Legend Page Generation
            if (legend) {
                PDPage legendPage = new PDPage(pageSize);
                document.addPage(legendPage);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, legendPage)) {
                    contentStream.beginText();
                    contentStream.setFont(boldFont, PdfSymbolRenderer.FONT_SIZE_TITLE);
                    contentStream.newLineAtOffset(MARGIN_X, pageSize.getHeight() - MARGIN_Y);
                    contentStream.showText("Legend (All text is BOLD)");
                    contentStream.endText();

                    float legendY = pageSize.getHeight() - INITIAL_Y_OFFSET;
                    
                    contentStream.beginText();
                    contentStream.setFont(boldFont, fontSize);
                    contentStream.newLineAtOffset(MARGIN_X, legendY);
                    contentStream.showText("Yellow Rectangular: Rule Definition or Non-terminal Symbol");
                    contentStream.endText();
                    legendY -= LEGEND_LINE_SPACING;

                    contentStream.beginText();
                    contentStream.newLineAtOffset(MARGIN_X, legendY);
                    contentStream.showText("Light Gray Rectangular: Nullable Symbol (Has at least one empty RHS)");
                    contentStream.endText();
                    legendY -= LEGEND_LINE_SPACING;
                    
                    contentStream.beginText();
                    contentStream.newLineAtOffset(MARGIN_X, legendY);
                    contentStream.showText("Light Blue Rounded Rect: Terminal Item (Token/Literal)");
                    contentStream.endText();
                    legendY -= LEGEND_LINE_SPACING;

                    contentStream.beginText();
                    contentStream.newLineAtOffset(MARGIN_X, legendY);
                    contentStream.showText("Stacked Symbols: Recursive Symbol (Appears on its own RHS)");
                    contentStream.endText();
                    legendY -= LEGEND_LINE_SPACING;

                    contentStream.beginText();
                    contentStream.newLineAtOffset(MARGIN_X, legendY);
                    contentStream.showText("< Shape: Start/Top-level Rule");
                    contentStream.endText();
                    legendY -= LEGEND_LINE_SPACING;

                    contentStream.beginText();
                    contentStream.newLineAtOffset(MARGIN_X, legendY);
                    contentStream.showText("Empty Line: Epsilon (Represents an empty rule)");
                    contentStream.endText();
                }
            }

            document.save(outputFileName);
        }
    }

    /**
     * Maps the page size name to the corresponding PDRectangle.
     */
    private PDRectangle getPageSize(String name) {
        switch (name.toUpperCase()) {
            case "A0": return PDRectangle.A0;
            case "A1": return PDRectangle.A1;
            case "A2": return PDRectangle.A2;
            case "A3": return PDRectangle.A3;
            case "A4": return PDRectangle.A4;
            case "A5": return PDRectangle.A5;
            case "A6": return PDRectangle.A6;
            case "LEGAL": return PDRectangle.LEGAL;
            case "LETTER":
            default: return PDRectangle.LETTER;
        }
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new GrammarViewApp()).execute(args);
        System.exit(exitCode);
    }
}
