// Copyright (c) 2026 Kelly Morrison
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License version 3.

package com.grammarview;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

/**
 * PdfGenerator handles the rendering of a GrammarModel into a PDF document.
 * This class is immutable.
 */
public final class PdfGenerator {

    private final String yaccFileName;
    private final float fontSize;
    private final String pageSizeName;
    private final boolean portrait;
    private final boolean legend;
    private final boolean footer;
    private final java.io.File outputDir;

    public PdfGenerator(String yaccFileName, float fontSize, String pageSizeName, boolean portrait, boolean legend, boolean footer, java.io.File outputDir) {
        this.yaccFileName = yaccFileName;
        this.fontSize = fontSize;
        this.pageSizeName = pageSizeName;
        this.portrait = portrait;
        this.legend = legend;
        this.footer = footer;
        this.outputDir = outputDir;
    }

    /**
     * Renders the extracted grammar rules into a multi-page PDF document.
     */
    public void generate(GrammarModel model) throws IOException {
        String outputFileName = yaccFileName + ".pdf";
        java.io.File outputFile = (outputDir == null) ? new java.io.File(outputFileName) : new java.io.File(outputDir, outputFileName);
        System.out.println("Generating PDF: " + outputFile.getPath() + " with font size " + fontSize + ", size " + pageSizeName);
        List<GrammarModel.Rule> rules = model.getRules();
        PDRectangle baseSize = getPageSize(pageSizeName);
        PDRectangle pageSize = portrait ? baseSize : new PDRectangle(baseSize.getHeight(), baseSize.getWidth());
        PDType1Font boldFont = PDType1Font.HELVETICA_BOLD;
        float capHeight = boldFont.getFontDescriptor().getCapHeight() / PdfLayoutConstants.FONT_UNIT_CONVERSION * fontSize;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(pageSize);
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            PdfSymbolRenderer renderer = new PdfSymbolRenderer(contentStream, boldFont);
            int pageNum = 1;

            if (footer) drawFooter(contentStream, pageSize, boldFont, pageNum++);

            // Title
            contentStream.beginText();
            contentStream.setFont(boldFont, PdfLayoutConstants.FONT_SIZE_TITLE);
            contentStream.newLineAtOffset(PdfLayoutConstants.MARGIN_X, pageSize.getHeight() - PdfLayoutConstants.MARGIN_Y);
            contentStream.showText("Grammar View: " + yaccFileName);
            contentStream.endText();

            float yOffset = pageSize.getHeight() - PdfLayoutConstants.INITIAL_Y_OFFSET;

            for (int ruleIdx = 0; ruleIdx < rules.size(); ruleIdx++) {
                GrammarModel.Rule rule = rules.get(ruleIdx);
                float ruleHeight = calculateRuleHeight(model, rule, boldFont, fontSize, pageSize.getWidth());

                float bottomLimit = PdfLayoutConstants.BOTTOM_OVERFLOW_LIMIT;
                float maxPageContentHeight = pageSize.getHeight() - (PdfLayoutConstants.MARGIN_Y + bottomLimit);

                // If the entire rule can fit on a fresh page, move it if it doesn't fit here.
                if (yOffset - ruleHeight < bottomLimit && ruleHeight <= maxPageContentHeight) {
                    contentStream.close();
                    page = new PDPage(pageSize);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    renderer = new PdfSymbolRenderer(contentStream, boldFont);
                    if (footer) drawFooter(contentStream, pageSize, boldFont, pageNum++);
                    yOffset = pageSize.getHeight() - PdfLayoutConstants.MARGIN_Y;
                }

                boolean isStartRule = (ruleIdx == 0);
                boolean isRecursive = model.isRecursive(rule.name);
                boolean isNullable = model.isNullable(rule.name);

                float ruleNameWidth = boldFont.getStringWidth(rule.name) / PdfLayoutConstants.FONT_UNIT_CONVERSION * fontSize;
                float boxWidth = ruleNameWidth + (PdfLayoutConstants.BOX_PADDING_X * 2);
                float boxHeight = fontSize + (PdfLayoutConstants.BOX_PADDING_Y * 2);
                float xPos = PdfLayoutConstants.MARGIN_X;

                // Ensure LHS fits on the starting page
                float lhsHeight = boxHeight + (boxWidth > 0.2f * pageSize.getWidth() ? (boxHeight + PdfLayoutConstants.SPACING_ALTERNATIVES) : 0);
                if (yOffset - lhsHeight < bottomLimit) {
                    contentStream.close();
                    page = new PDPage(pageSize);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    renderer = new PdfSymbolRenderer(contentStream, boldFont);
                    if (footer) drawFooter(contentStream, pageSize, boldFont, pageNum++);
                    yOffset = pageSize.getHeight() - PdfLayoutConstants.MARGIN_Y;
                }

                float boxY = yOffset - PdfLayoutConstants.BOX_PADDING_Y;
                contentStream.setNonStrokingColor(PdfSymbolRenderer.COLOR_SHADOW);
                if (isStartRule) {
                    renderer.drawLHSStartShape(xPos + PdfLayoutConstants.SHADOW_OFFSET, boxY - PdfLayoutConstants.SHADOW_OFFSET, boxWidth, boxHeight, true);
                } else {
                    contentStream.addRect(xPos + PdfLayoutConstants.SHADOW_OFFSET, boxY - PdfLayoutConstants.SHADOW_OFFSET, boxWidth, boxHeight);
                    contentStream.fill();
                }

                Color lhsColor = isNullable ? PdfSymbolRenderer.COLOR_LIGHT_GRAY : PdfSymbolRenderer.COLOR_VIVID_YELLOW;
                renderer.drawNonTerminalShape(xPos, boxY, boxWidth, boxHeight, lhsColor, isRecursive, isStartRule);
                renderer.drawCenteredText(rule.name, xPos, boxY, boxWidth, boxHeight, fontSize, capHeight, isStartRule);

                boolean longRuleName = boxWidth > 0.2f * pageSize.getWidth();
                float ruleNameEndX = xPos + boxWidth;
                float ruleNameCenterY = boxY + (boxHeight / 2);
                float altVerticalLineX = longRuleName 
                        ? (PdfLayoutConstants.MARGIN_X + PdfLayoutConstants.RHS_START_X_OFFSET / 2)
                        : (ruleNameEndX + PdfLayoutConstants.RHS_START_X_OFFSET / 2);

                if (longRuleName) {
                    float yTop = ruleNameCenterY + (isRecursive ? 2 * PdfLayoutConstants.RECURSION_STACK_OFFSET : 0);
                    float xStart = ruleNameEndX + (isRecursive ? 2 * PdfLayoutConstants.RECURSION_STACK_OFFSET : 0);
                    float yDrop = (boxHeight + PdfLayoutConstants.SPACING_ALTERNATIVES) / 2f;
                    float yMid = yTop - yDrop;
                    float yBottom = yTop - (yDrop * 2);
                    // Shorter wrap for long rule names
                    float xWrapRight = Math.min(xStart + 50f, pageSize.getWidth() - PdfLayoutConstants.MARGIN_X);
                    renderer.drawWrappingCurve(xStart, xWrapRight, altVerticalLineX, yTop, yMid, yBottom, PdfLayoutConstants.WRAP_RADIUS, false);
                    
                    yOffset -= (boxHeight + PdfLayoutConstants.SPACING_ALTERNATIVES);
                }

                float previousTargetY = -1f;

                for (int i = 0; i < rule.alternatives.size(); i++) {
                    GrammarModel.RuleAlternative alt = rule.alternatives.get(i);
                    
                    boolean containsRecursiveItem = false;
                    for (String symbol : alt.getSymbols()) {
                        if (model.isRecursive(symbol)) {
                            containsRecursiveItem = true;
                            break;
                        }
                    }
                    
                    if (containsRecursiveItem && i > 0) {
                        yOffset -= 10f; // Add extra space above recursive items
                    }

                    float altHeight = calculateAlternativeHeight(alt, boldFont, fontSize, pageSize.getWidth(), altVerticalLineX, model);

                    // Check if this alternative fits on the current page
                    if (yOffset - altHeight < bottomLimit) {
                        contentStream.close();
                        page = new PDPage(pageSize);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        renderer = new PdfSymbolRenderer(contentStream, boldFont);
                        if (footer) drawFooter(contentStream, pageSize, boldFont, pageNum++);
                        yOffset = pageSize.getHeight() - PdfLayoutConstants.MARGIN_Y;
                        
                        // New page resets connection point to top
                        previousTargetY = -1f;
                    }

                    List<String> items = alt.getSymbols();
                    float currentX = altVerticalLineX + PdfLayoutConstants.RHS_START_X_OFFSET / 2;
                    float currentY = yOffset;
                    float targetY = (currentY - PdfLayoutConstants.BOX_PADDING_Y) + (boxHeight / 2);

                    contentStream.setStrokingColor(PdfSymbolRenderer.COLOR_STROKE);
                    if (i == 0 && !longRuleName) {
                        // Short rule, first alternative: connect from rule name to alt vertical line, then to currentX
                        float startX = ruleNameEndX + (isRecursive ? 2 * PdfLayoutConstants.RECURSION_STACK_OFFSET : 0);
                        float startY = ruleNameCenterY + (isRecursive ? 2 * PdfLayoutConstants.RECURSION_STACK_OFFSET : 0);
                        contentStream.moveTo(startX, startY);
                        contentStream.lineTo(altVerticalLineX, startY);
                        contentStream.lineTo(altVerticalLineX, targetY);
                        contentStream.lineTo(currentX, targetY);
                        contentStream.stroke();
                    } else {
                        // All other cases: vertical line from previous alternative (or top of page) down to current
                        float prevY = (previousTargetY > 0) ? previousTargetY : (pageSize.getHeight() - PdfLayoutConstants.MARGIN_Y);
                        // If i=0 and longRuleName, previousTargetY is -1, so it connects from top (correctly, as S-curve ends at targetY)
                        // Actually, if i=0 and longRuleName, targetY is exactly where the S-curve ends, so prevY = targetY is fine.
                        if (i == 0 && longRuleName) {
                            prevY = targetY;
                        }
                        contentStream.moveTo(altVerticalLineX, prevY);
                        contentStream.lineTo(altVerticalLineX, targetY);
                        contentStream.lineTo(currentX, targetY);
                        contentStream.stroke();
                    }
                    
                    previousTargetY = targetY;

                    if (!items.isEmpty()) {
                        boolean linkDrawn = false;
                        for (int j = 0; j < items.size(); j++) {
                            String itemText = items.get(j);
                            float itemWidth = boldFont.getStringWidth(itemText) / PdfLayoutConstants.FONT_UNIT_CONVERSION * fontSize;
                            float rectWidth = itemWidth + (PdfLayoutConstants.BOX_PADDING_X * 2);
                            float rectHeight = fontSize + (PdfLayoutConstants.BOX_PADDING_Y * 2);

                            if (currentX + rectWidth > (pageSize.getWidth() - PdfLayoutConstants.MARGIN_X)) {
                                float xWrapLeft = altVerticalLineX + PdfLayoutConstants.RHS_WRAP_X_OFFSET;
                                float xWrapRight = pageSize.getWidth() - PdfLayoutConstants.MARGIN_X;
                                float yDrop = (rectHeight + PdfLayoutConstants.SPACING_ALTERNATIVES) * 0.75f;
                                float yTop = targetY;
                                float yMid = targetY - yDrop;
                                float yBottom = targetY - (yDrop * 2);

                                float xStart = currentX;
                                float yStartTop = yTop;
                                if (j > 0 && model.isRecursive(items.get(j - 1)) && !linkDrawn) {
                                    float offset = 2 * PdfLayoutConstants.RECURSION_STACK_OFFSET;
                                    xStart += offset;
                                    yStartTop += offset;
                                }

                                renderer.drawWrappingCurve(xStart, xWrapRight, xWrapLeft, yStartTop, yMid, yBottom, PdfLayoutConstants.WRAP_RADIUS, true);
                                yOffset -= (yDrop * 2);
                                currentY -= (yDrop * 2);
                                targetY -= (yDrop * 2);
                                currentX = xWrapLeft + (PdfLayoutConstants.WRAP_RADIUS * 2);
                            }

                            float itemBoxY = currentY - PdfLayoutConstants.BOX_PADDING_Y;
                            boolean isItemRecursive = model.isRecursive(itemText);
                            boolean isItemNullable = model.isNullable(itemText);
                            boolean isTerminal = !model.getNonTerminals().contains(itemText) || itemText.startsWith("'") || itemText.startsWith("\"");

                            if (isTerminal) {
                                renderer.drawRoundedRectangle(currentX, itemBoxY, rectWidth, rectHeight, PdfLayoutConstants.ROUNDED_RECT_RADIUS, PdfSymbolRenderer.COLOR_LIGHT_BLUE);
                            } else {
                                Color itemColor = isItemNullable ? PdfSymbolRenderer.COLOR_LIGHT_GRAY : PdfSymbolRenderer.COLOR_VIVID_YELLOW;
                                renderer.drawNonTerminalShape(currentX, itemBoxY, rectWidth, rectHeight, itemColor, isItemRecursive, false);
                            }
                            renderer.drawCenteredText(itemText, currentX, itemBoxY, rectWidth, rectHeight, fontSize, capHeight, false);

                            currentX += rectWidth;
                            linkDrawn = false;
                            if (j < items.size() - 1) {
                                float nextLineEndX = currentX + PdfLayoutConstants.RHS_ITEM_LINK_LENGTH;
                                if (nextLineEndX <= (pageSize.getWidth() - PdfLayoutConstants.MARGIN_X)) {
                                    float startX = currentX + (isItemRecursive ? 2 * PdfLayoutConstants.RECURSION_STACK_OFFSET : 0);
                                    float startY = targetY + (isItemRecursive ? 2 * PdfLayoutConstants.RECURSION_STACK_OFFSET : 0);
                                    contentStream.moveTo(startX, startY);
                                    if (isItemRecursive) {
                                        contentStream.lineTo(startX, targetY);
                                    }
                                    contentStream.lineTo(nextLineEndX, targetY);
                                    contentStream.stroke();
                                    currentX = nextLineEndX;
                                    linkDrawn = true;
                                }
                            }
                        }
                    }
                    if (i < rule.alternatives.size() - 1) {
                        yOffset -= (boxHeight + PdfLayoutConstants.SPACING_ALTERNATIVES);
                    }
                }
                yOffset -= (boxHeight + PdfLayoutConstants.SPACING_RULES + (isRecursive ? 10 : 0));
            }

            if (legend) {
                contentStream.close();
                page = new PDPage(pageSize);
                document.addPage(page);
                contentStream = new PDPageContentStream(document, page);
                if (footer) drawFooter(contentStream, pageSize, boldFont, pageNum++);
                contentStream.beginText();
                contentStream.setFont(boldFont, PdfLayoutConstants.FONT_SIZE_TITLE);
                contentStream.newLineAtOffset(PdfLayoutConstants.MARGIN_X, pageSize.getHeight() - PdfLayoutConstants.MARGIN_Y);
                contentStream.showText("Legend (All text is BOLD)");
                contentStream.endText();
                float legendY = pageSize.getHeight() - PdfLayoutConstants.INITIAL_Y_OFFSET;
                String[] legendTexts = {
                    "Yellow Rectangular: Rule Definition or Non-terminal Symbol",
                    "Light Gray Rectangular: Nullable Symbol (Has at least one empty RHS)",
                    "Light Blue Rounded Rect: Terminal Item (Token/Literal)",
                    "Stacked Symbols: Recursive Symbol (Appears on its own RHS)",
                    "< Shape: Start/Top-level Rule",
                    "Empty Line: Epsilon (Represents an empty rule)"
                };
                for (String text : legendTexts) {
                    contentStream.beginText();
                    contentStream.setFont(boldFont, fontSize);
                    contentStream.newLineAtOffset(PdfLayoutConstants.MARGIN_X, legendY);
                    contentStream.showText(text);
                    contentStream.endText();
                    legendY -= PdfLayoutConstants.LEGEND_LINE_SPACING;
                }
            }
            contentStream.close();
            document.save(outputFile);
        }
    }

    /**
     * Draws a footer at the bottom of the page.
     */
    private void drawFooter(PDPageContentStream contentStream, PDRectangle pageSize, PDType1Font font, int pageNum) throws IOException {
        float footerY = PdfLayoutConstants.MARGIN_Y / 2;
        float lineY = footerY + 15;
        
        // Draw horizontal line
        contentStream.setStrokingColor(Color.BLACK);
        contentStream.setLineWidth(1f);
        contentStream.moveTo(PdfLayoutConstants.MARGIN_X, lineY);
        contentStream.lineTo(pageSize.getWidth() - PdfLayoutConstants.MARGIN_X, lineY);
        contentStream.stroke();
        
        // Draw file name on the left
        contentStream.beginText();
        contentStream.setFont(font, PdfLayoutConstants.FONT_SIZE_FOOTER);
        contentStream.newLineAtOffset(PdfLayoutConstants.MARGIN_X, footerY);
        contentStream.showText(yaccFileName);
        contentStream.endText();
        
        // Draw page number on the right
        String pageText = "Page " + pageNum;
        float textWidth = font.getStringWidth(pageText) / PdfLayoutConstants.FONT_UNIT_CONVERSION * PdfLayoutConstants.FONT_SIZE_FOOTER;
        contentStream.beginText();
        contentStream.setFont(font, PdfLayoutConstants.FONT_SIZE_FOOTER);
        contentStream.newLineAtOffset(pageSize.getWidth() - PdfLayoutConstants.MARGIN_X - textWidth, footerY);
        contentStream.showText(pageText);
        contentStream.endText();
    }

    /**
     * Calculates the height of a single alternative, accounting for line wrapping.
     */
    private float calculateAlternativeHeight(GrammarModel.RuleAlternative alt, PDType1Font font, float fontSize, float pageWidth, float altVerticalLineX, GrammarModel model) throws IOException {
        float boxHeight = fontSize + (PdfLayoutConstants.BOX_PADDING_Y * 2);
        float currentX = altVerticalLineX + PdfLayoutConstants.RHS_START_X_OFFSET / 2;
        float altHeight = boxHeight;

        List<String> symbols = alt.getSymbols();
        for (int j = 0; j < symbols.size(); j++) {
            String itemText = symbols.get(j);
            float itemWidth = font.getStringWidth(itemText) / PdfLayoutConstants.FONT_UNIT_CONVERSION * fontSize;
            float rectWidth = itemWidth + (PdfLayoutConstants.BOX_PADDING_X * 2);

            if (currentX + rectWidth > (pageWidth - PdfLayoutConstants.MARGIN_X)) {
                float yDrop = (boxHeight + PdfLayoutConstants.SPACING_ALTERNATIVES) * 0.75f;
                altHeight += (yDrop * 2);
                float xWrapLeft = altVerticalLineX + PdfLayoutConstants.RHS_WRAP_X_OFFSET;
                currentX = xWrapLeft + (PdfLayoutConstants.WRAP_RADIUS * 2);
            }
            currentX += rectWidth;
            if (j < symbols.size() - 1) {
                currentX += PdfLayoutConstants.RHS_ITEM_LINK_LENGTH;
            }
        }
        return altHeight;
    }

    private float calculateRuleHeight(GrammarModel ruleModel, GrammarModel.Rule rule, PDType1Font font, float fontSize, float pageWidth) throws IOException {
        float boxHeight = fontSize + (PdfLayoutConstants.BOX_PADDING_Y * 2);
        float totalHeight = 0;
        float ruleNameWidth = font.getStringWidth(rule.name) / PdfLayoutConstants.FONT_UNIT_CONVERSION * fontSize;
        float lhsWidth = ruleNameWidth + (PdfLayoutConstants.BOX_PADDING_X * 2);

        boolean longRuleName = lhsWidth > 0.2f * pageWidth;
        if (longRuleName) {
            totalHeight += (boxHeight + PdfLayoutConstants.SPACING_ALTERNATIVES);
        }

        float altVerticalLineX = longRuleName 
                ? (PdfLayoutConstants.MARGIN_X + PdfLayoutConstants.RHS_START_X_OFFSET / 2)
                : (PdfLayoutConstants.MARGIN_X + lhsWidth + PdfLayoutConstants.RHS_START_X_OFFSET / 2);

        for (int i = 0; i < rule.alternatives.size(); i++) {
            GrammarModel.RuleAlternative alt = rule.alternatives.get(i);
            
            boolean containsRecursiveItem = false;
            for (String symbol : alt.getSymbols()) {
                if (ruleModel.isRecursive(symbol)) {
                    containsRecursiveItem = true;
                    break;
                }
            }
            if (containsRecursiveItem && i > 0) {
                totalHeight += 10f;
            }

            float altHeight = calculateAlternativeHeight(alt, font, fontSize, pageWidth, altVerticalLineX, ruleModel);
            totalHeight += altHeight;
            if (i < rule.alternatives.size() - 1) {
                totalHeight += PdfLayoutConstants.SPACING_ALTERNATIVES;
            }
        }
        return Math.max(boxHeight, totalHeight);
    }

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
}
