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

    @Parameters(index = "0", description = "The YACC file to process.")
    private File yaccFile;

    @Option(names = {"-legend"}, description = "Add a legend page to the PDF.")
    private boolean legend;

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose logging.")
    private boolean verbose;

    /**
     * Internal representation of a Grammar Rule.
     */
    private static class Rule {
        String name;
        /** List of alternatives, where each alternative is a list of symbol names. */
        List<List<String>> alternatives = new ArrayList<>();

        Rule(String name) {
            this.name = name;
        }
    }

    /**
     * Main execution logic for the CLI command.
     */
    @Override
    public Integer call() throws Exception {
        if (verbose) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
            System.out.println("Verbose logging enabled.");
        }

        if (!yaccFile.exists()) {
            System.err.println("Error: File not found: " + yaccFile.getPath());
            return 1;
        }

        System.out.println("Processing YACC file: " + yaccFile.getName());
        
        // 1. Parse the YACC file into internal Rule objects using ANTLR
        List<Rule> rules = parseYacc(yaccFile);
        
        // 2. Generate the PDF visualization using PDFBox
        generatePdf(rules);

        return 0;
    }

    /**
     * Uses the generated ANTLR4 parser to extract rules from the YACC source.
     * 
     * @param file The input YACC/Bison file.
     * @return A list of extracted Rule objects.
     */
    private List<Rule> parseYacc(File file) throws IOException {
        CharStream input = CharStreams.fromPath(file.toPath());
        BisonLexer lexer = new BisonLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        BisonParser parser = new BisonParser(tokens);
        ParseTree tree = parser.input_();

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
        System.out.println("Generating PDF: " + outputFileName);

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
                if (alt.isEmpty()) {
                    nullableRules.add(rule.name);
                }
            }
        }
        
        try (PDDocument document = new PDDocument()) {
            // Setup Landscape US Letter page
            PDRectangle landscape = new PDRectangle(PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth());
            PDPage page = new PDPage(landscape);
            document.addPage(page);

            PDType1Font boldFont = PDType1Font.HELVETICA_BOLD;

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Title
                contentStream.beginText();
                contentStream.setFont(boldFont, 18);
                contentStream.newLineAtOffset(50, landscape.getHeight() - 50);
                contentStream.showText("Grammar View: " + yaccFile.getName());
                contentStream.endText();

                float yOffset = landscape.getHeight() - 100;
                float fontSize = 12;
                Color vividYellow = Color.YELLOW;
                Color lightBlue = new Color(173, 216, 230);
                Color lightGray = new Color(211, 211, 211);

                // Vertical centering adjustment based on font cap height
                float capHeight = boldFont.getFontDescriptor().getCapHeight() / 1000f * fontSize;

                for (int ruleIdx = 0; ruleIdx < rules.size(); ruleIdx++) {
                    Rule rule = rules.get(ruleIdx);
                    boolean isStartRule = (ruleIdx == 0);
                    boolean isRecursive = recursiveRules.contains(rule.name);
                    boolean isNullable = nullableRules.contains(rule.name);

                    // Calculate bounding box for LHS
                    float ruleNameWidth = boldFont.getStringWidth(rule.name) / 1000 * fontSize;
                    float boxPaddingX = 10;
                    float boxPaddingY = 6;
                    float boxWidth = ruleNameWidth + (boxPaddingX * 2);
                    float boxHeight = fontSize + (boxPaddingY * 2);
                    float xPos = 50;
                    float boxY = yOffset - boxPaddingY;

                    // 1. Draw Shadow for LHS
                    contentStream.setNonStrokingColor(Color.LIGHT_GRAY);
                    if (isStartRule) {
                        drawLHSStartShape(contentStream, xPos + 2, boxY - 2, boxWidth, boxHeight, true);
                    } else {
                        contentStream.addRect(xPos + 2, boxY - 2, boxWidth, boxHeight);
                        contentStream.fill();
                    }

                    // 2. Draw LHS Main Shape
                    // Priority: Nullable (Gray) > Rule Definition (Yellow)
                    Color lhsColor = isNullable ? lightGray : vividYellow;
                    drawNonTerminalShape(contentStream, xPos, boxY, boxWidth, boxHeight, lhsColor, isRecursive, isStartRule);

                    // 3. Render LHS Text (Centered)
                    contentStream.beginText();
                    contentStream.setFont(boldFont, fontSize);
                    contentStream.setNonStrokingColor(Color.BLACK);
                    float textX = xPos + (boxWidth - ruleNameWidth) / 2 + (isStartRule ? 3 : 0);
                    float textY = boxY + (boxHeight - capHeight) / 2;
                    contentStream.newLineAtOffset(textX, textY);
                    contentStream.showText(rule.name);
                    contentStream.endText();

                    float lineStartX = xPos + boxWidth;
                    float lineStartY = boxY + (boxHeight / 2);

                    // 4. Render RHS Alternatives
                    for (int i = 0; i < rule.alternatives.size(); i++) {
                        List<String> items = rule.alternatives.get(i);
                        float currentX = xPos + boxWidth + 40;
                        float currentY = yOffset;
                        float targetY = (currentY - boxPaddingY) + (boxHeight / 2);

                        // Draw Orthogonal Connecting Line
                        contentStream.setStrokingColor(Color.BLACK);
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
                                float itemWidth = boldFont.getStringWidth(itemText) / 1000 * fontSize;
                                float rectPaddingX = 10;
                                float rectPaddingY = 6;
                                float rectWidth = itemWidth + (rectPaddingX * 2);
                                float rectHeight = fontSize + (rectPaddingY * 2);
                                float itemBoxY = currentY - rectPaddingY;

                                boolean isItemRecursive = recursiveRules.contains(itemText);
                                boolean isItemNullable = nullableRules.contains(itemText);
                                boolean isTerminal = !nonTerminals.contains(itemText) 
                                                  || itemText.startsWith("'") 
                                                  || itemText.startsWith("\"");

                                // 4a. Draw item symbol
                                if (isTerminal) {
                                    drawRoundedRectangle(contentStream, currentX, itemBoxY, rectWidth, rectHeight, 5, lightBlue);
                                } else {
                                    Color itemColor = isItemNullable ? lightGray : vividYellow;
                                    drawNonTerminalShape(contentStream, currentX, itemBoxY, rectWidth, rectHeight, itemColor, isItemRecursive, false);
                                }

                                // 4b. Render item text (Centered)
                                contentStream.beginText();
                                contentStream.setFont(boldFont, fontSize);
                                contentStream.setNonStrokingColor(Color.BLACK);
                                float itemTextX = currentX + (rectWidth - itemWidth) / 2;
                                float itemTextY = itemBoxY + (rectHeight - capHeight) / 2;
                                contentStream.newLineAtOffset(itemTextX, itemTextY);
                                contentStream.showText(itemText);
                                contentStream.endText();

                                currentX += rectWidth;

                                // 4c. Draw horizontal link to next item
                                if (j < items.size() - 1) {
                                    float nextLineEndX = currentX + 15;
                                    contentStream.moveTo(currentX, targetY);
                                    contentStream.lineTo(nextLineEndX, targetY);
                                    contentStream.stroke();
                                    currentX = nextLineEndX;
                                }
                            }
                        }
                        
                        // Vertical spacing between alternatives
                        if (i < rule.alternatives.size() - 1) {
                            yOffset -= (boxHeight + 10);
                        }
                    }
                    
                    // Vertical spacing between different rules
                    yOffset -= (boxHeight + 20 + (isRecursive ? 10 : 0));
                    if (yOffset < 50) break; // Overflow protection
                }
            }

            // Optional Legend Page Generation
            if (legend) {
                PDPage legendPage = new PDPage(landscape);
                document.addPage(legendPage);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, legendPage)) {
                    contentStream.beginText();
                    contentStream.setFont(boldFont, 18);
                    contentStream.newLineAtOffset(50, landscape.getHeight() - 50);
                    contentStream.showText("Legend (All text is BOLD)");
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.setFont(boldFont, 12);
                    contentStream.newLineAtOffset(50, landscape.getHeight() - 100);
                    contentStream.showText("Yellow Rectangular: Rule Definition or Non-terminal Symbol");
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, landscape.getHeight() - 120);
                    contentStream.showText("Light Gray Rectangular: Nullable Symbol (Has at least one empty RHS)");
                    contentStream.endText();
                    
                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, landscape.getHeight() - 140);
                    contentStream.showText("Light Blue Rounded Rect: Terminal Item (Token/Literal)");
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, landscape.getHeight() - 160);
                    contentStream.showText("Stacked Symbols: Recursive Symbol (Appears on its own RHS)");
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, landscape.getHeight() - 180);
                    contentStream.showText("< Shape: Start/Top-level Rule");
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, landscape.getHeight() - 200);
                    contentStream.showText("Empty Line: Epsilon (Represents an empty rule)");
                    contentStream.endText();
                }
            }

            document.save(outputFileName);
        }
    }

    /**
     * Draws a non-terminal shape, handling recursion stacking and start-rule shaping.
     */
    private void drawNonTerminalShape(PDPageContentStream contentStream, float x, float y, float width, float height, Color fillColor, boolean isRecursive, boolean isStart) throws IOException {
        contentStream.setNonStrokingColor(fillColor);
        contentStream.setStrokingColor(Color.BLACK);

        if (isRecursive) {
            // Draw recursive stack (two offset boxes behind)
            for (int stack = 2; stack >= 1; stack--) {
                float sOffset = stack * 4;
                if (isStart) {
                    drawLHSStartShape(contentStream, x + sOffset, y + sOffset, width, height, false);
                } else {
                    contentStream.addRect(x + sOffset, y + sOffset, width, height);
                    contentStream.fillAndStroke();
                }
            }
        }

        if (isStart) {
            drawLHSStartShape(contentStream, x, y, width, height, false);
        } else {
            contentStream.addRect(x, y, width, height);
            contentStream.fillAndStroke();
        }
    }

    /**
     * Helper to draw a rounded rectangle.
     */
    private void drawRoundedRectangle(PDPageContentStream contentStream, float x, float y, float width, float height, float radius, Color fillColor) throws IOException {
        contentStream.setNonStrokingColor(fillColor);
        contentStream.setStrokingColor(Color.BLACK);
        float kappa = 0.552284749831f;
        float kRadius = radius * kappa;
        contentStream.moveTo(x + radius, y);
        contentStream.lineTo(x + width - radius, y);
        contentStream.curveTo(x + width - radius + kRadius, y, x + width, y + radius - kRadius, x + width, y + radius);
        contentStream.lineTo(x + width, y + height - radius);
        contentStream.curveTo(x + width, y + height - radius + kRadius, x + width - radius + kRadius, y + height, x + width - radius, y + height);
        contentStream.lineTo(x + radius, y + height);
        contentStream.curveTo(x + radius - kRadius, y + height, x, y + height - radius + kRadius, x, y + height - radius);
        contentStream.lineTo(x, y + radius);
        contentStream.curveTo(x, y + radius - kRadius, x + radius - kRadius, y, x + radius, y);
        contentStream.fillAndStroke();
    }

    /**
     * Helper to draw the special < shape for the start rule.
     */
    private void drawLHSStartShape(PDPageContentStream contentStream, float x, float y, float width, float height, boolean fillOnly) throws IOException {
        float indent = 10;
        contentStream.moveTo(x + indent, y);
        contentStream.lineTo(x + width, y);
        contentStream.lineTo(x + width, y + height);
        contentStream.lineTo(x + indent, y + height);
        contentStream.lineTo(x, y + (height / 2));
        contentStream.closePath();
        if (fillOnly) contentStream.fill(); else contentStream.fillAndStroke();
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new GrammarViewApp()).execute(args);
        System.exit(exitCode);
    }
}
